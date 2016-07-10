package dlujanapps.mx.wary.finder;

import android.Manifest;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;

import dlujanapps.mx.wary.data.DBContract;
import dlujanapps.mx.wary.objects.Action;
import dlujanapps.mx.wary.objects.Friend;
import dlujanapps.mx.wary.objects.HistoryItem;
import dlujanapps.mx.wary.objects.Utils;
import dlujanapps.mx.wary.objects.Utils.DBUtils;

/**
 * A service that process each file transfer request i.e Intent by opening a
 * socket connection with the WiFi Direct Group Owner and writing the file
 */
public class ClientService extends IntentService implements
        GoogleApiClient.ConnectionCallbacks
        , GoogleApiClient.OnConnectionFailedListener
        , LocationListener
{
    private static final int SOCKET_TIMEOUT = 5000;
    public static final String ACTION_START_HANDSHAKE = "dlujanapps.wary.START_HANDSHAKE";
    public static final String ACTION_SEND_LOCATION = "dlujanapps.wary.SEND_LOCATION";
    public static final String ACTION_STOP_SERVICE = "dlujanapps.wary.STOP_CLIENT_SERVICE";
    public static final String ACTION_LOCATION_CHANGED = "dlujanapps.wary.LOCATION_CHANGED";

    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";
    public static final String EXTRAS_FRIEND_ADDRESS = "friend_address";
    private static final int INTERVAL_LOCATION_UPDATES = 20000;
    private final String SHARED_PREFS_CONNECTED_TO = "mx.dlujanapps.wary.connected_to";
    public static final String LOCATION_UNAVAILABLE = "location_unavailable";

    private Context mContext;
    private Intent mIntent;
    String TAG = getClass().getSimpleName();
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private Friend mFriend;

    public ClientService(String name) {
        super(name);
    }
    public ClientService() {
        super("FileTransferService");
    }

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "RECEIVED BROADCAST :: "+action);
            switch(action){
                case ACTION_STOP_SERVICE: {
                    Log.i(TAG, "Client Disconnecting to GAPIClient*************");
                    if(mGoogleApiClient.isConnected()) {
                        //remove updates from callback
                        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient
                                , ClientService.this);
                        //remove updates from pendingIntent
                        Intent locationChangedIntent = new Intent(ACTION_LOCATION_CHANGED);
                        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext
                                , 0, locationChangedIntent, 0);
                        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient
                                ,pendingIntent);

                        mGoogleApiClient.disconnect();
                        try {
                            LocalBroadcastManager.getInstance(getApplicationContext())
                                    .unregisterReceiver(mBroadcastReceiver);
                        }catch(Exception e){
                            e.printStackTrace();
                        }

                        try{
                            unregisterReceiver(mBroadcastReceiver);
                        }catch(Exception e){
                            e.printStackTrace();
                        }

                    }
                    break;
                }
                case ACTION_LOCATION_CHANGED : {
                    Log.i(TAG, "RECEIVED LOCATION CHANGEED BROADCAST************* :: "
                            +LocationResult.hasResult(intent));
                    if(LocationResult.hasResult(intent)) {
                        sendLocation(LocationResult
                                .extractResult(intent).getLastLocation());
                    }
                    break;
                }case ACTION_START_HANDSHAKE : {

                    new HandshakeClientTask().execute();
                    LocalBroadcastManager.getInstance(getApplicationContext())
                            .unregisterReceiver(mBroadcastReceiver);
                }
                default: break;
            }
        }
    };

    /*
     * (non-Javadoc)
     * @see android.app.IntentService#onHandleIntent(android.content.Intent)
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        mIntent = intent;
        mContext = getApplicationContext();

        String action = mIntent.getAction();
        if (action.equals(ACTION_SEND_LOCATION)) {

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_STOP_SERVICE);
            intentFilter.addAction(ACTION_LOCATION_CHANGED);
            LocalBroadcastManager.getInstance(getApplicationContext())
                    .registerReceiver(mBroadcastReceiver, intentFilter);

            mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            Log.i(TAG, "Client Connecting to GAPIClient");
            mGoogleApiClient.connect();

        }else if(action.equals(ACTION_START_HANDSHAKE)){
            Log.i(TAG, "START HANDSHAKE");

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_START_HANDSHAKE);
            LocalBroadcastManager.getInstance(getApplicationContext())
                    .registerReceiver(mBroadcastReceiver, intentFilter);

            LocalBroadcastManager.getInstance(getApplicationContext())
                    .sendBroadcast(new Intent(ACTION_START_HANDSHAKE));

        }
    }

    @Override
    public void onConnected(Bundle bundle) {

        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission == PackageManager.PERMISSION_GRANTED) {

            mLocationRequest = LocationRequest.create();
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            mLocationRequest.setInterval(INTERVAL_LOCATION_UPDATES); // 20 secs

            Log.i(TAG, "Connected... requesting location updates WITH PendingIntent");
            IntentFilter intentFilter = new IntentFilter(ACTION_LOCATION_CHANGED);
            getApplicationContext().registerReceiver(mBroadcastReceiver, intentFilter);

            Intent locationChangedIntent = new Intent(ACTION_LOCATION_CHANGED);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, locationChangedIntent, 0);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, pendingIntent);
        }else{
            Log.i(TAG, "Connected... permission not available... sending last known location");
            sendLocation(LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient));
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "onConnectionFailed");

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "Location Changed");
        updateHistoryItem(mFriend);
        new ClientTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, location);
    }

    /**
     * Send Location to Server
     */
    private class ClientTask extends AsyncTask<Location, Void, Void>{

        @Override
        protected Void doInBackground(Location... params) {
            Log.i(TAG, "sending location");
            Location location = params[0];
            String host = mIntent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            int port = mIntent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);

            Socket socket = new Socket();
            try {
//                Log.i(TAG, "Opening client socket - ");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
//                Log.i(TAG, "Client socket connected - " + socket.isConnected());
                OutputStream stream = socket.getOutputStream();

                byte[] array;
                if(location != null) {
                    array = location.toString().getBytes();
                }else{
                    array = LOCATION_UNAVAILABLE.getBytes();
                }
                ByteArrayInputStream inputStream = new ByteArrayInputStream(array);

                sendInfo(array, inputStream, stream);
//                Log.i(TAG, "Client: Data written");
//            Toast.makeText(mContext, "Data written to Server", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Log.e(TAG, "Error sending location to server ... "+e.getMessage());
//                Toast.makeText(getApplicationContext(), "Error sending location to server"
//                        , Toast.LENGTH_SHORT).show();

            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }

            return null;
        }
    }

    /**
     * Client Handshake
     */
    private class HandshakeClientTask extends AsyncTask<Void, Void, String>{

        @Override
        protected String doInBackground(Void... params) {
            Log.i(TAG, "starting Handshake");
            String result = "error::";

            String host = mIntent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            int port = mIntent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);

            Socket socket = null;
            PrintWriter socketWriter = null;
            BufferedReader socketReader = null;
            try {
                Log.i(TAG, "Opening client socket - ");
                socket = new Socket();
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
                Log.i(TAG, "Client socket connected - " + socket.isConnected());

                socketWriter = new PrintWriter(
                        new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                socketReader =
                        new BufferedReader(new InputStreamReader(socket.getInputStream()));

                /**** RECEIVE FRIENDS INFO ****/
                String serverInfo = socketReader.readLine();
                Log.i(TAG, "received :: server_info :: "+serverInfo);
                JSONObject jServerInfo = new JSONObject(serverInfo);

                if(jServerInfo.get(ServerTask.KEY_ACTION) != null
                        && jServerInfo.get(ServerTask.KEY_ACTION).equals(ServerTask.ACTION_ADD_ME)){

                    //Update friend info in DB

                    String address = jServerInfo.getString(ServerTask.KEY_ADDRESS);
                    String name = jServerInfo.getString(ServerTask.KEY_NAME);

                    if(addNewFriendInfo(address, name, 0)) {
                        DBUtils.addHistoryItem(mContext,
                                new HistoryItem(mContext, address, Action.ACTION_ADDED_YOU));
                        Log.i(TAG, "Friend added correctly... sending ACK_1");

                        /**** SEND ACK_1 ****/

                        JSONObject jACK_1 = new JSONObject();
                        jACK_1.put(ServerTask.KEY_ACTION, ServerTask.ACTION_ACK_1);
                        socketWriter.println(jACK_1.toString());
                        Log.i(TAG, "Handshake - Client: sent ACK1 :: "+jACK_1.toString());

                        /**** RECEIVE ACK_2 ****/
                        String serverACK2 = socketReader.readLine();
                        Log.i(TAG, "received :: serverACK2 :: "+ serverACK2);
                        JSONObject jACK2 = new JSONObject(serverACK2);

                        if(jACK2.get(ServerTask.KEY_ACTION) != null
                                && jACK2.get(ServerTask.KEY_ACTION).equals(ServerTask.ACTION_ACK_2)) {

                            socketWriter.close();
                            socketReader.close();
                            socket.close();
                            result = "success";
                            return result;
                        }else{
                            updateNewFriendInfo(address, name, 1);
                            socket.close();
                            throw new Exception("error:: Expected ACK_2 .. received :: "+serverACK2);
                        }
                    }else{
                        throw new Exception(result+"Error adding friend in DB");
                    }
                }else{
                    socket.close();
                    throw new Exception("error:: Expected ADD_ME action ... received :: "+serverInfo);
                }
            }
            catch(ConnectException connectException){
                result = "error::connectException";
            } catch (Exception e) {
                Log.e(TAG, "Client - Error doing handshake ... "+e.getMessage());
                e.printStackTrace();
                result = e.toString();
                if (socket != null && socket.isConnected()) {
                    try {
                        if (socketWriter != null) {
                            socketWriter.close();
                        }
                        if (socketReader != null) {
                            socketReader.close();
                        }
                        socket.close();
                    } catch (IOException e2) {
                        // Give up
                        e.printStackTrace();
                    }
                }
            }

            return result;
        }

        @Override
        protected void onPostExecute(String result){
            if(result.contains("error::connectException")){
                new HandshakeClientTask().execute();
            }else if(result.contains("error::")){
                Log.e(TAG, "Handshake Error :: "+result);
                DBUtils.deletePossibleFriend(getApplicationContext(), mFriend.getAddress());
            }else{

                Log.i(TAG, "Handshake result :: "+result);
            }
        }

        /**
         *
         * @param address --> server wifip2p address
         * @param name --> server name
         * @param status --> new/added
         * @return
         */
        private boolean updateNewFriendInfo(String address, String name, int status){
            ContentValues contentValues = new ContentValues();
            contentValues.put(DBContract.FriendEntry.COLUMN_NAME_IS_NEW, status);
            contentValues.put(DBContract.FriendEntry.COLUMN_NAME_NAME, name);

            Friend friend = new Friend();
            friend.setAddress(address);

            boolean updated = Utils.DBUtils.updateFriend(mContext, friend ,contentValues);

//            int updated = mContext.getContentResolver().update(
//                    DBContract.FriendEntry.CONTENT_URI
//                    , contentValues, selection, selectionArgs);
            Log.i(TAG, "UPDATED :: "+updated);
            return updated;
        }

        private boolean addNewFriendInfo(String address, String name, int is_new){
// COLUMN_NAME_NAME = "friend_name";
//            public static final String COLUMN_NAME_PHOTO_URI = "photo_uri";
            Friend friend = new Friend();
            friend.setIsNew(is_new == 1);
            friend.setName(name);
            friend.setAddress(address);
            friend.setRole(1);
            friend.setPhotoUri("N/A");

            boolean added = DBUtils.addFutureFriend(mContext, friend);
//            Uri uri = Uri.parse("content://dlujanapps.mx.wary.provider")
//                    .buildUpon().appendPath("friends").build();
//            Uri newUri = mContext.getContentResolver().insert(uri, contentValues);
            Log.i(TAG, "ADDED :: "+added);
            return added;
        }

        public String getMsg(InputStream inputStream) throws IOException{

            StringBuilder message = new StringBuilder();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String read;
            Log.i(TAG, "start reading buffer");
            while((read = bufferedReader.readLine()) != null) {
                Log.i(TAG, read);
                message.append(read);
            }

            Log.i(TAG, "read from buffer buffer :: "+message.toString());
            return message.toString();
        }
    }

    public boolean sendInfo(byte[] array, InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[array.length];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(TAG, e.toString());
            return false;
        }
        return true;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.i(TAG, "Destroying SendMessageService");
//        if(mGoogleApiClient.isConnected()) {
//            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
//            mGoogleApiClient.disconnect();
//        }

    }

    private void updateHistoryItem(Friend friend){
        if(friend != null) {
            HistoryItem historyItem = new HistoryItem(getApplicationContext()
                    , friend.getId()
                    , DBContract.ActionEntry.ACTION_SEARCHED_YOU);

            HistoryItem newHistory = new HistoryItem(getApplicationContext()
                    , friend.getId()
                    , DBContract.ActionEntry.ACTION_FOUND_YOU);

            Log.i(TAG, "updating historyitem to :: " + Action.ACTION_FOUND_YOU);
            DBUtils.updateHistoryItem(getApplicationContext()
                    , historyItem, newHistory.getActionId());
        }else{
            Log.i(TAG, "could not update history item :: friend is null");
        }
    };

    private String getAddressFromPrefs(){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(
                mContext);
        return sharedPref.getString(SHARED_PREFS_CONNECTED_TO, null);
    }

    public void sendLocation(Location location){
        String friendAddress = getAddressFromPrefs();
        mFriend = new Friend(getApplicationContext(), friendAddress);
        updateHistoryItem(mFriend);

        Log.i(TAG, "Location Changed .. " + location + " ... sending to friend :: "+friendAddress);
        new ClientTask().executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR, location);
    }
}
