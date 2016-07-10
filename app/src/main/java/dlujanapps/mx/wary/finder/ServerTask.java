package dlujanapps.mx.wary.finder;

/**
 * Created by daniellujanvillarreal on 11/18/15.
 *
 */

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import dlujanapps.mx.wary.R;
import dlujanapps.mx.wary.data.DBContract;
import dlujanapps.mx.wary.fragments.AddFriendFragment;
import dlujanapps.mx.wary.objects.Action;
import dlujanapps.mx.wary.objects.Friend;
import dlujanapps.mx.wary.objects.HistoryItem;
import dlujanapps.mx.wary.objects.Utils;

import static dlujanapps.mx.wary.data.DBContract.FriendEntry;


/**
 * A simple server socket that accepts connection and writes some data on
 * the stream.
 */
public class ServerTask extends AsyncTask<Void, Void, Intent> {

    private Context mContext;
    private Friend mFriend;

    String TAG = getClass().getSimpleName();
    public static final String STOP_TASK_ACTION = "stop_task";
    public static final String ACTION_PEER_LOC_CHANGED = "PEER_LOCATION_CHANGED";
    public static final String PEER_LOC_KEY = "peer_location";
    public static final String ACTION_HANDSHAKE_SUCCESS = "dlujanapps.mx.wary.handshake_success";
    public static final String ACTION_HANDSHAKE_FAILED = "dlujanapps.mx.wary.handshake_failed";

    public static final String TASK_RESULT = "task_result";
    public static final String TASK_RESULT_LOCATION_CHANGED = "task_result_location_changed";
    public static final String TASK_RESULT_SUCCESS = "task_result_success";
    public static final String TASK_RESULT_ERROR = "task_result_error";

    public static final String TASK_RESULT_ERROR_VALUE = "task_result_error_value";

    public static final String ACTION_ADD_ME = "add_me";
    public static final String ACTION_ACK_1 = "ack_1";
    public static final String ACTION_ACK_2 = "ack_2";

    public static final String KEY_ACTION = "action";
    public static final String KEY_NAME = "name";
    public static final String KEY_ADDRESS = "address";

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(STOP_TASK_ACTION)) {
                Log.i(TAG, "cancelling task :: " + cancel(true));
                LocalBroadcastManager.getInstance(context)
                        .sendBroadcast(new Intent(AddFriendFragment.ACTION_HANDSHAKE_FAILED));
            }
        }
    };

    /**
     * @param context
     */
    public ServerTask(Context context, String p2pAddress) {
        this.mContext = context;
        mFriend = new Friend(context,p2pAddress);
    }

    public ServerTask(Context context, Friend friend) {
        this.mContext = context;
        mFriend = friend;
    }

    @Override
    protected Intent doInBackground(Void... params) {
        Log.i(TAG, "ServerTask :: doInBackground");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(STOP_TASK_ACTION);
        LocalBroadcastManager.getInstance(mContext)
                .registerReceiver(mBroadcastReceiver, intentFilter);

        ServerSocket serverSocket;
        Socket client;

        Intent result = new Intent();

        try {
            serverSocket = new ServerSocket(8988);
            client = serverSocket.accept();

            Log.i(TAG, "ServerTask :: connection accepted ... isBound:: "+serverSocket.isBound()
                    + " \n p2pFriendAddress :: "+mFriend.getAddress());

            /******* HANDSHAKE ***********/
            if(mFriend != null && Utils.DBUtils.isNewFriend(mContext, mFriend.getAddress())) {

                Utils.DBUtils.addHistoryItem(mContext,
                        new HistoryItem(mContext, mFriend.getAddress(), Action.ACTION_ADDED));

                Log.i(TAG, "Server Handshake :: starting");
                result = newFriendHandshake(client);

                /******* LOCATION ***********/
            }else{
                updateHistoryItem(mFriend);

                Log.i(TAG, "Server :: Receiving location updates");
                InputStream inputStream = client.getInputStream();
                String message = getMsg(inputStream);
                inputStream.close();
//                if(message.contains("Location")){
                result.putExtra(TASK_RESULT, TASK_RESULT_LOCATION_CHANGED);
                result.putExtra(PEER_LOC_KEY, message);
//                }
            }

            Log.i(TAG, "Server: connection done");
            serverSocket.close();

        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            result.putExtra(TASK_RESULT, TASK_RESULT_ERROR);
            result.putExtra(TASK_RESULT_ERROR_VALUE, e.toString());
        }
        return result;
    }
    /*
     * (non-Javadoc)
     * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
     */
    @Override
    protected void onPostExecute(Intent result) {
        if (result.getStringExtra(TASK_RESULT).equals(TASK_RESULT_SUCCESS)) {

//            if(message.contains("success")){

            result.setAction(ACTION_HANDSHAKE_SUCCESS);
            LocalBroadcastManager.getInstance(mContext)
                    .sendBroadcast(result);

        }else if (result.getStringExtra(TASK_RESULT).equals(TASK_RESULT_ERROR)) {

            result.setAction(ACTION_HANDSHAKE_FAILED);
            LocalBroadcastManager.getInstance(mContext)
                    .sendBroadcast(result);

        }else if (result.getStringExtra(TASK_RESULT).equals(TASK_RESULT_LOCATION_CHANGED)) {
            // --> addedFriend --> update friend status
            // --> locationChanged --> update location
            result.setAction(ACTION_PEER_LOC_CHANGED);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(result);

//                Toast.makeText(mContext, "Received from client ::: " + message, Toast.LENGTH_LONG).show();
//                Log.i(TAG, message);

            new ServerTask(mContext, mFriend).execute();
        }else{
            // if message does not contain any of those then error
            Log.e(TAG, "Error receiving info in Server :: Message Format Unknown");
        }
//        }else{
//            Log.e(TAG, "Error onPostExecute:: result null");
//        }
    }

    @Override
    protected void onCancelled(Intent result){
        Log.i(TAG, "CANCELLING SERVER TASK");

        LocalBroadcastManager.getInstance(mContext)
                .unregisterReceiver(mBroadcastReceiver);
    }

    public Intent newFriendHandshake(Socket client){
        Intent response = new Intent();

        PrintWriter socketWriter = null;
        BufferedReader socketReader = null;

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(
                mContext);

        String wifiAddress  = sharedPref.getString(
                mContext.getString(R.string.pref_key_my_wifip2p_address), "");
        String wifiDisplayName = sharedPref.getString(
                mContext.getString(R.string.pref_key_display_name), wifiAddress);

        try {
            socketWriter = new PrintWriter(
                    new BufferedWriter(new OutputStreamWriter(client.getOutputStream())), true);
            socketReader =
                    new BufferedReader(new InputStreamReader(client.getInputStream()));

            /*** SEND MY INFO ****/
            JSONObject jMyInfo = new JSONObject();
            jMyInfo.put(KEY_ACTION, ACTION_ADD_ME);
            jMyInfo.put(KEY_NAME, wifiDisplayName);
            jMyInfo.put(KEY_ADDRESS, wifiAddress);

            socketWriter.println(jMyInfo.toString());
            Log.i(TAG, "sent :: ADD ME :: "+jMyInfo.toString());

            /**** RECEIVE ACK_1 ****/
            String ack1 = socketReader.readLine();
            Log.i(TAG, "received :: ack1 :: "+ack1);
            JSONObject jACK1 = new JSONObject(ack1);

            if(jACK1.get(KEY_ACTION) != null
                    && jACK1.get(KEY_ACTION).equals(ACTION_ACK_1)){

                if(updateFutureFriend()){

                    /**** SEND ACK_2 ****/
                    JSONObject jACK2 = new JSONObject();
                    jACK2.put(KEY_ACTION, ACTION_ACK_2);

                    socketWriter.println(jACK2.toString());
                    Log.i(TAG, "sent :: " + jACK2.toString());

                    socketWriter.close();
                    socketReader.close();
                    client.close();

                    response.putExtra(TASK_RESULT, TASK_RESULT_SUCCESS);


                }else{
                    throw new Exception("error:: Error Adding friend in DB");
                }
            }else{
                Utils.DBUtils.deletePossibleFriend(mContext, wifiAddress);
                throw new Exception("error:: Expected ACK1 .. received :: "+ack1);

            }
        }catch(Exception e){
            response.putExtra(TASK_RESULT, TASK_RESULT_ERROR);
            response.putExtra(TASK_RESULT_ERROR_VALUE, e.toString());

            if(client.isConnected()){
                try{

                    if (socketWriter != null) {
                        socketWriter.close();
                    }
                    if (socketReader != null) {
                        socketReader.close();
                    }
                    client.close();
                    Utils.DBUtils.deletePossibleFriend(mContext, wifiAddress);
                }catch (Exception e1){
                    //Give up
                    e.printStackTrace();
                }
            }
        }
        return response;
    }

    private boolean updateFutureFriend() {

        ContentValues contentValues = new ContentValues();
        contentValues.put(FriendEntry.COLUMN_NAME_IS_NEW, 0);

        return Utils.DBUtils.updateFriend(mContext, mFriend, contentValues);

//        String selection;
//        String[] selectionArgs;
//        if (mFriend != null) {
//            selection = FriendEntry.COLUMN_NAME_ADDRESS+" = ?";
//            selectionArgs = new String[]{mFriend.getAddress()};
//
//            int updated = mContext.getContentResolver().update(
//                    FriendEntry.CONTENT_URI
//                    , contentValues
//                    , selection
//                    , selectionArgs);
//            Log.i(TAG, "UPDATED :: " + updated);
//            return updated == 1;
//        }else{
//            return false;
//        }
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

    private void updateHistoryItem(Friend friend){
        if(friend != null) {
            HistoryItem historyItem = new HistoryItem(mContext
                    , friend.getId()
                    , DBContract.ActionEntry.ACTION_SEARCHED);

            HistoryItem newHistory = new HistoryItem(mContext
                    , friend.getId()
                    , DBContract.ActionEntry.ACTION_FOUND);

            Log.i(TAG, "updating historyitem to :: " + Action.ACTION_FOUND);
            Utils.DBUtils.updateHistoryItem(mContext
                    , historyItem, newHistory.getActionId());
        }else{
            Log.i(TAG, "could not update history item :: friend is null");
        }
    };
}
