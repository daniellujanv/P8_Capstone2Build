package dlujanapps.mx.wary.finder;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;

import dlujanapps.mx.wary.MainActivity;
import dlujanapps.mx.wary.R;
import dlujanapps.mx.wary.data.DBContract;
import dlujanapps.mx.wary.finder.Location.LocatorActivity;
import dlujanapps.mx.wary.fragments.AddFriendFragment;
import dlujanapps.mx.wary.objects.Action;
import dlujanapps.mx.wary.objects.Friend;
import dlujanapps.mx.wary.objects.HistoryItem;
import dlujanapps.mx.wary.objects.Utils;


public class WDReceiver extends BroadcastReceiver
        implements WifiP2pManager.PeerListListener, WifiP2pManager.ConnectionInfoListener{

    private WifiP2pManager mWifiP2pManager;
    private WifiP2pManager.Channel mChannel;
    private LocatorActivity mLocatorActivity;

    // String myPhone = "22:55:31:f0:53:79" // J5
//    String myPhone = "36:fc:ef:e7:45:94"; // nexus 5
//    String myPhone = "b6:ce:f6:07:f4:5a"; // nexus 9

    ArrayList<WifiP2pDevice> mConnectedDevices;
    private Context mContext;

    public final static String ACTION_STOP_DISCOVER_PEERS = "wary.action.stop_discovery";
    public final static String ACTION_ALARM_DISCOVER_PEERS = "wary.action.discover_peers";
    public final static String ACTION_CONNECT = "wary.action.connect";
    public final static String ACTION_DISCONNECT ="wary.action.disconnect";

    public final String DISCOVER_PEERS_FAILED = "discover_peers_failed";

    private String TAG = getClass().getSimpleName();
    private Location mLocation;
    private ServerTask mServerTask;
    private String mConnectedTo = null;

    public WDReceiver(){
        mConnectedDevices = new ArrayList<>();
    };

    public WDReceiver(WifiP2pManager wifiP2pManager
            , WifiP2pManager.Channel channel, LocatorActivity locatorActivity){
        mLocatorActivity = locatorActivity;
        mWifiP2pManager = wifiP2pManager;
        mChannel = channel;
        mConnectedDevices = new ArrayList<>();
    }

    /*****************************/
    /**** CALLBACKS RECEIVER *****/
    /*****************************/
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "on receive :: "+intent.getAction());

        mContext = context;
        mWifiP2pManager = (WifiP2pManager) mContext.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mWifiP2pManager.initialize(mContext, mContext.getMainLooper(), null);

        if(mLocatorActivity != null) {
            mLocation = mLocatorActivity.getCurrentLocation();
        }

        String action = intent.getAction();

        switch (action){
            case ACTION_ALARM_DISCOVER_PEERS:
                Log.i(TAG, "DISCOVER PEERS");
                discoverPeers(mChannel);
                break;

            case ACTION_STOP_DISCOVER_PEERS:
                Log.i(TAG, "STOP PEERDISCOVERY");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mWifiP2pManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Log.i(TAG, "onSuccess stopPeerDiscovery");
                        }

                        @Override
                        public void onFailure(int reasonCode) {
                            Log.i(TAG, "onFailure stopPeerDiscovery :: " + Utils.getReason(reasonCode));
                        }
                    });
                }
                break;

            case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION :
                stateChangedAction(intent);
                break;

            case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION :
                // request available peers from the wifi p2p manager. This is an
                // asynchronous call and the calling activity is notified with a
                // callback on PeerListListener.onPeersAvailable()
                // Call WifiP2pManager.requestPeers() to get a list of current peers
                Log.i(TAG, "WIFI_P2P_PEERS_CHANGED_ACTION");
                mWifiP2pManager.requestPeers(mChannel, this);
                break;

            case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION :
                connectionChangedAction(intent);
                break;

            case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION :
                // Respond to this device's wifi state changing
                thisDeviceChangedAction(mContext, intent);
                break;

            case ACTION_CONNECT :
                Log.i(TAG, "ACTION_CONNECT");
//                createOwnGroup(intent.getExtras().getString(FRIEND_ADDRESS_KEY));
                mConnectedTo = intent.getExtras().
                        getString(LocatorActivity.FRIEND_ADDRESS_KEY);
                boolean isFriend = intent.getExtras().
                        getBoolean(LocatorActivity.IS_FRIEND_KEY);

                if(!isFriend){
                    Utils.writeAddressInPrefs(mContext, mConnectedTo);
                }else{
                    Utils.DBUtils.addHistoryItem(mContext,
                            new HistoryItem(mContext, mConnectedTo, Action.ACTION_SEARCHED));
                }
                connect(mConnectedTo, isFriend);
                break;

            case ACTION_DISCONNECT :
                Log.i(TAG, "ACTION_DISCONNECT");
                disconnect();
                // disconnect from peer takes the user to the friend's list...
                // we call discover peers so that when the user is taken there
                // the list is updated ... or in process to ...
                discoverPeers(mChannel);
                break;

            default:
                Log.i(TAG, "unknown ACTION "+action);
                break;
        }
    }


    /*****************************/
    /**** CALLBACKS CONNECTION ***/
    /*****************************/
    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers){
        Log.i(TAG, "ON PEERS AVAILABLE");
        Utils.DBUtils.updateAwayFriends(mContext);
        ArrayList<WifiP2pDevice> availablePeers = new ArrayList<>(peers.getDeviceList());
        if(!availablePeers.isEmpty()) {
            Utils.DBUtils.doBulkInsert( mContext
                    , Utils.DBUtils.getContentValues(availablePeers)
                    , DBContract.AvailablePeerEntry.CONTENT_URI);
        }else{
            Utils.DBUtils.deleteAvailablePeers(mContext);
        }

        ArrayList<WifiP2pDevice> lPeers = new ArrayList<>(peers.getDeviceList());
        HashMap<String, Long> friendsDevices = Utils.DBUtils.getFriendsDevices(mContext);

        Utils.DBUtils.updateFriendsStatus(mContext, lPeers, friendsDevices);
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        Log.i(TAG, "onConnectionInfoAvailable :: " + info.toString());
        Log.i(TAG, "Starting communication");

        mConnectedTo = Utils.getAddressFromPrefs(mContext);
        if(mConnectedTo != null && info.groupFormed) {
            if (info.isGroupOwner) {
                Log.i(TAG, "Starting SERVER Task... connectedTo::" + mConnectedTo);
                mServerTask = new ServerTask(mContext, mConnectedTo);
                mServerTask.execute();

            } else if (Utils.DBUtils.isNewFriend(mContext, mConnectedTo)) {

                Log.i(TAG, "Starting CLIENT Service - Handshake");
                startClientService(info.groupOwnerAddress, true);

            } else {
                Log.i(TAG, "Starting CLIENT Service - Location Updates");
                startClientService(info.groupOwnerAddress, false);
                Utils.DBUtils.addHistoryItem(mContext,
                        new HistoryItem(mContext, mConnectedTo, Action.ACTION_SEARCHED_YOU));
                showNotification(mConnectedTo);
            }
        }else{
            Log.i(TAG, "COULD NOT FIND CONNECTED_TO PROBABLY NOT OUR CONNECTION");
        }
    }

    /*****************************/
    /******** CONNECTION *********/
    /*****************************/

    /**
     * Get connection status and send to receiver in LocatorActivity
     * to update UI
     * @param context of intent
     * @param intent received
     */
    private void thisDeviceChangedAction(Context context, Intent intent){
        WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
        Log.i(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION " +
                "... name :: " + device.deviceName + "... status :: "
                + Utils.getDeviceStatus(device.status));
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(
                mContext);
        String prefkeyWifiAddress  = mContext.getString(R.string.pref_key_my_wifip2p_address);
        String address = sharedPref.getString(
                prefkeyWifiAddress, "null");
        if(address.equals("null")){
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(prefkeyWifiAddress, device.deviceAddress);
            Log.i(TAG, "wrote pref p2p address :: " + editor.commit());

        }

//        if(device.status == WifiP2pDevice.CONNECTED){
//            writeAddressInPrefs(device.deviceAddress);
//        }

//        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
//        editor.putString(mContext.getString(R.string.pref_key_my_wifip2p_address), device.deviceAddress);
//        editor.commit();
    }

    /**
     * Change in connection
     * If connected request info to know who is server/client
     * if disconnected stop client/server communication
     * @param intent
     */
    private void connectionChangedAction(Intent intent){
        // Respond to new connection or disconnections
        NetworkInfo networkInfo = intent
                .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

        Log.i(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION :: " + networkInfo.toString());

        Intent intent_connected = new Intent();
        intent_connected.setAction(LocatorActivity.CONN_STATUS_ACTION);
        intent_connected.putExtra(LocatorActivity.CONN_STATUS_KEY, networkInfo.getState().toString());
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent_connected);

        if (networkInfo.isConnected()) {
            Log.i(TAG, "... Connected ");
            mWifiP2pManager.requestConnectionInfo(mChannel, this);
        } else if(networkInfo.getState() == NetworkInfo.State.DISCONNECTED
                || networkInfo.getState() == NetworkInfo.State.DISCONNECTING) {
            Log.i(TAG, "... Disconnected");
            stopCommunicationServices();
        }
//        else{
//            Log.i(TAG, " N/A ... "+ networkInfo.getState().toString());
//        }
    }

    /**
     * Change in Wi-Fi state
     * @param intent
     */
    private void stateChangedAction(Intent intent){
        Log.i(TAG, "WIFI_P2P_STATE_CHANGED_ACTION");
        // Check to see if Wi-Fi is enabled and notify appropriate activity
        int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(
                mContext);

        SharedPreferences.Editor editor = sharedPref.edit();
        if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
            // Wifi P2P is enabled
            Log.i(TAG, "Wifi P2P Enabled ");
            editor.putBoolean(mContext.getString(R.string.pref_key_wifip2p_enabled), true);

        } else {
            // Wi-Fi P2P is not enabled
            Log.i(TAG, "Wifi P2P NOT Enabled ");
            editor.putBoolean(mContext.getString(R.string.pref_key_wifip2p_enabled), false);
        }
        Log.i(TAG, "wrote pref p2p enabled :: " + editor.commit());
    }

    //TODO doc this methods
    public void discoverPeers(WifiP2pManager.Channel channel){
        mWifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess Discover Peers");
            }

            @Override
            public void onFailure(int reasonCode) {
                Log.i(TAG, "onFailure Discover Peers :: " + Utils.getReason(reasonCode));

                Intent intent_discover_failed = new Intent();
                intent_discover_failed.setAction(DISCOVER_PEERS_FAILED);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent_discover_failed);

            }
        });
    }

    public void connect(final String device_address, boolean isFriend) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device_address;
        config.groupOwnerIntent = 15;

//        Log.i(TAG, "... connecting to device "+device_address+"... is config null ?? " + config.wps.toString());
        mWifiP2pManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Connect success to :: " + device_address);
            }

            @Override
            public void onFailure(int reason) {
                discoverPeers(mChannel);

                Utils.DBUtils.addHistoryItem(mContext,
                        new HistoryItem(mContext, mConnectedTo, Action.ACTION_CONNECTION_FAILED));

                Log.i(TAG, "Connection to " + device_address + " FAILED :: "
                        + Utils.getReason(reason));
                Intent intent_connection_failed = new Intent();
                intent_connection_failed.setAction(LocatorActivity.CONN_FAILED_ACTION);
                LocalBroadcastManager.getInstance(mContext).
                        sendBroadcast(intent_connection_failed);
            }
        });
    }

    public void disconnect(){
        mWifiP2pManager.cancelConnect(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Success cancelConnect");
                if (mConnectedDevices != null) {
//                    mConnectedDevices.clear();
                    mConnectedDevices = null;
                }
            }

            @Override
            public void onFailure(int reason) {
                Log.i(TAG, "failed cancelConnect :: " + Utils.getReason(reason));
            }
        });
        mWifiP2pManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Success removeGroup");
            }

            @Override
            public void onFailure(int reason) {
                Log.i(TAG, "failed removeGroup :: " + Utils.getReason(reason));
            }
        });
        Utils.removeAddressFromPrefs(mContext);

    }

    public void startClientService(InetAddress hostAddress, boolean handshake){
        Intent serviceIntent = new Intent(mContext, ClientService.class);
        if(handshake){
            addFutureFriend(hostAddress.toString());

            serviceIntent.setAction(ClientService.ACTION_START_HANDSHAKE);
        }else {
            serviceIntent.setAction(ClientService.ACTION_SEND_LOCATION);
            serviceIntent.putExtra(ClientService.EXTRAS_FRIEND_ADDRESS, mConnectedTo);
            serviceIntent.putExtra("location", mLocation);
        }
        serviceIntent.putExtra(ClientService.EXTRAS_GROUP_OWNER_ADDRESS
                , hostAddress.getHostAddress());
        serviceIntent.putExtra(ClientService.EXTRAS_GROUP_OWNER_PORT, 8988);
        mContext.startService(serviceIntent);

    }

    public void stopCommunicationServices(){
        Log.i(TAG, "Stopping client service");

        Intent intent_disconnect_client = new Intent();
        intent_disconnect_client.setAction(ClientService.ACTION_STOP_SERVICE);

        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent_disconnect_client);

        Intent intent_disconnect_server = new Intent();
        intent_disconnect_server.setAction(ServerTask.STOP_TASK_ACTION);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent_disconnect_server);


        LocalBroadcastManager.getInstance(mContext)
                .sendBroadcast(new Intent(AddFriendFragment.ACTION_HANDSHAKE_FAILED));
    }

    public void addFutureFriend(String address){
        Friend friend = new Friend();
        friend.setName("N/A");
        friend.setAddress(address);
        friend.setPhotoUri("N/A");
        friend.setIsNew(true);
        friend.setRole(1);

        Utils.DBUtils.addFutureFriend(mContext, friend);
    }

    public void showNotification(String connectedTo){

// Because clicking the notification launches a new ("special") activity,
// there's no need to create an artificial back stack.
        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                mContext,
                0,
                new Intent(mContext, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        String friendName = Utils.DBUtils.getFriendName(mContext, connectedTo);
        String message;
        if(friendName != null) {
            message = String.format(mContext.getString(R.string.who_found_notification)
                    , friendName);
        }else{
            message = String.format(mContext.getString(R.string.who_found_notification)
                    , connectedTo);
        }

        Bitmap largeIcon = BitmapFactory.decodeResource(
                mContext.getResources(), R.mipmap.ic_pointer_circle_light);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mContext);
        builder.setContentTitle(mContext.getString(R.string.found_notification));
        builder.setContentText(message);
        builder.setStyle(new NotificationCompat.BigTextStyle()
                .bigText(message));
        builder.setContentIntent(resultPendingIntent);
        builder.setLargeIcon(largeIcon);
        builder.setSmallIcon(R.drawable.wary_notification);
        builder.setCategory(NotificationCompat.CATEGORY_SOCIAL);

        // Sets an ID for the notification
        int mNotificationId = 001;
// Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
// Builds the notification and issues it.
        mNotifyMgr.notify(mNotificationId, builder.build());
    }
}
