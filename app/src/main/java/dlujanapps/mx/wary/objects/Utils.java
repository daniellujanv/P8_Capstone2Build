package dlujanapps.mx.wary.objects;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

import dlujanapps.mx.wary.R;
import dlujanapps.mx.wary.data.DBContract;
import dlujanapps.mx.wary.widget.WaryWidget;

import static dlujanapps.mx.wary.data.DBContract.FriendEntry;
import static dlujanapps.mx.wary.data.DBContract.HistoryEntry;

/**
 * Created by daniellujanvillarreal on 3/2/16.
 *
 */
public class Utils {

    public static final String SHARED_PREFS_CONNECTED_TO = "mx.dlujanapps.wary.connected_to";

    public static class DBUtils {

        public static String TAG = "DBUtils";

        public static boolean addFutureFriend(Context context, Friend futureFriend){

            String selection = FriendEntry.COLUMN_NAME_ADDRESS+" = ?";
            String[] selectionArgs = new String[]{futureFriend.getAddress()};
            String[] projection = Friend.FULL_PROJECTION;

            Cursor cursor = context.getContentResolver()
                    .query(FriendEntry.CONTENT_URI, projection, selection, selectionArgs, null, null);
            if(cursor != null && cursor.moveToFirst()){

                Friend friend = new Friend(cursor);
                Log.i(TAG, " futureFriend already on DB \n " + friend.toString());

                cursor.close();
                return true;
            }

            ContentValues contentValues = futureFriend.getFutureFriendContentValues();
            Uri uri = context.getContentResolver().insert(FriendEntry.CONTENT_URI, contentValues);
            if(uri != null) {
                Log.i(TAG , " added friend " + uri.toString() + "\n" + futureFriend.toString());
                return true;
            }
            Log.i(TAG , " addFutureFriend returning FALSE");

            return false;
        }

        public static boolean updateFriend(Context context, Friend friend
                , ContentValues contentValues){
            Uri uri = FriendEntry.CONTENT_URI;
            String selection = FriendEntry.COLUMN_NAME_ADDRESS+" = ?";
            String[] selectionArgs = new String[]{friend.getAddress()};
            int updated = context.getContentResolver()
                    .update(uri, contentValues, selection, selectionArgs);
            Log.i(TAG, "updateFriend .... result :: "+updated);
            updateMyWidgets(context);
            return updated == 1;
        }

        public static void deletePossibleFriend(Context context, String address){
            Uri uri = FriendEntry.CONTENT_URI;
            String selection = FriendEntry.COLUMN_NAME_ADDRESS+" = ?";
            String[] selectionArgs = new String[]{address};

            Log.i(TAG, "deleting posible friend::" + context.getContentResolver()
                    .delete(uri, selection, selectionArgs));
        }

        public static boolean deleteFriend(Context context, Friend friend){

            if(context.getContentResolver().delete(
                    FriendEntry.buildFriendUri(friend.getId())
                    , null
                    , null) == 1){

//                String selection_history =
//                        HistoryEntry.COLUMN_NAME_FRIEND_ID + " = ?";

                context.getContentResolver()
                        .delete(
                                HistoryEntry.buildHistoryUri(friend.getId())
                                , null
                                , null);
                updateMyWidgets(context);
                return true;
            }else{
                return false;
            }
        }

        public static void addHistoryItem(Context context, HistoryItem history){
            if(history.getActionId() != -1) {
                Log.i(TAG, "inserting historyItem :: " + history.toString());
                context.getContentResolver().insert(
                        HistoryEntry.CONTENT_URI, history.getContentValues());
            }
        }

        public static void updateHistoryItem(Context context, HistoryItem historyItem
                , int newActionId) {

            Uri uri = HistoryEntry.CONTENT_URI;
            String preSelection = HistoryEntry.COLUMN_NAME_ACTION_ID + " = ? AND "
                    + HistoryEntry.COLUMN_NAME_FRIEND_ID + " = ?";
            String[] preSelectionArgs = new String[]{
                    Integer.toString(historyItem.getActionId())
                    , Integer.toString(historyItem.getFriendId())};
            String orderBy = HistoryEntry.TABLE_NAME+"."+HistoryEntry._ID + " DESC LIMIT 1";

            Cursor cursor = context.getContentResolver()
                    .query(uri, new String[]{ HistoryEntry.TABLE_NAME+"."+HistoryEntry._ID}
                            , preSelection, preSelectionArgs, orderBy);
            if (cursor != null && cursor.moveToFirst()) {
                int id = cursor.getInt(0);
                cursor.close();

                Log.i(TAG, "updating history item ... "+historyItem.toString()+" ... " + id);
                String selection =  HistoryEntry.TABLE_NAME+"."+HistoryEntry._ID + " = ?";
                String[] selectionArgs = new String[]{Integer.toString(id)};

                ContentValues contentValues = new ContentValues();
                contentValues.put(HistoryEntry.COLUMN_NAME_ACTION_ID, newActionId);
                contentValues.put(HistoryEntry.COLUMN_NAME_TIMESTAMP, System.currentTimeMillis());

                context.getContentResolver()
                        .update(uri, contentValues, selection, selectionArgs);
            }else{
                Log.i(TAG, "could not update history item "+historyItem.toString());
            }
        }

        public static HashMap<String, Long> getFriendsDevices(Context context) {
            HashMap<String, Long> friendsDevices = new HashMap<>();
            Uri content_uri_friends = DBContract.FriendEntry.CONTENT_URI;
            String[] projection = new String[]{
                    DBContract.FriendEntry._ID
                    , DBContract.FriendEntry.COLUMN_NAME_ADDRESS};

            String selection = DBContract.FriendEntry.COLUMN_NAME_IS_NEW+" = ?";
            String[] selectionArgs = new String[]{Integer.toString(0)};

            Cursor friendsCursor = context.getContentResolver().query(content_uri_friends, projection
                    , selection, selectionArgs, null);
            if (friendsCursor != null && friendsCursor.moveToFirst()) {
                Log.i(TAG, "friends in DB :: " + friendsCursor.getCount());
                do {
                    Log.i(TAG, "putting friend .... address::"+ friendsCursor.getString(1) +"... id :: "+ friendsCursor.getLong(0));
                    friendsDevices.put(friendsCursor.getString(1), friendsCursor.getLong(0));
                } while (friendsCursor.moveToNext());
                friendsCursor.close();
            }
            return friendsDevices;
        }

        public static int updateAwayFriends(Context mContext) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(DBContract.FriendEntry.COLUMN_NAME_STATUS, DBContract.FriendEntry.STATUS_AWAY);
            return mContext.getContentResolver().update(
                    DBContract.FriendEntry.CONTENT_URI
                    , contentValues, null, null);
        }

        /**
         * Puts available friends in ContentValue
         * @param lPeers list of available peers
         * @param friendsDevices list of friends
         * @return list of available friends
         */
        public static ArrayList<ContentValues> getContentValues(
                Context context
                , ArrayList<WifiP2pDevice> lPeers
                , HashMap<String, Long> friendsDevices){
//            Object[] keys  = friendsDevices.keySet().toArray();
//            for (Object key : keys) {
//                Log.i(TAG, "getContentValues ... friends devices ... key::" + key + ".. values:: "
//                        + friendsDevices.get(key).toString());
//            }
            ArrayList<ContentValues> contentValues = new ArrayList<>();
            for (int i = 0; i < lPeers.size(); i++) {
                Log.i(TAG, "peer :: " + lPeers.get(i).deviceName
                        + "... addr :: " + lPeers.get(i).deviceAddress);

                if (friendsDevices.get(lPeers.get(i).deviceAddress) != null) {
                    Log.i(TAG, "friend :: " + lPeers.get(i).deviceName
                            + " ... status :: " + getDeviceStatus(lPeers.get(i).status));

                    ContentValues cv = new ContentValues();

                    if(lPeers.get(i).status == WifiP2pDevice.CONNECTED) {

                        Log.i(TAG, "Writting connected :: "+lPeers.toString());
                        writeAddressInPrefs(context, lPeers.get(i).deviceAddress);

                        cv.put(DBContract.FriendEntry.COLUMN_NAME_STATUS
                                , DBContract.FriendEntry.STATUS_CONNECTED);
                    }else{
                        cv.put(DBContract.FriendEntry.COLUMN_NAME_STATUS
                                , DBContract.FriendEntry.STATUS_AVAILABLE);
                    }

                    cv.put(DBContract.FriendEntry.COLUMN_NAME_ADDRESS
                            , lPeers.get(i).deviceAddress);
                    contentValues.add(cv);

                }else if(lPeers.get(i).status == WifiP2pDevice.CONNECTED) {
                    Log.i(TAG, "Writting connected :: "+lPeers.toString());
                    writeAddressInPrefs(context, lPeers.get(i).deviceAddress);
                }else{
                    Log.i(TAG, "getContentValues :: peer not found :: ");
                }
            }
            Log.i(TAG, "getContentValues :: returning :: "+contentValues.toString());
            return contentValues;
        }

        public static ArrayList<ContentValues> getContentValues(
                ArrayList<WifiP2pDevice> lPeers){
            ArrayList<ContentValues> contentValues = new ArrayList<>();
            for (int i = 0; i < lPeers.size(); i++) {

                ContentValues cv = new ContentValues();
                cv.put("name", lPeers.get(i).deviceName);
                cv.put("address", lPeers.get(i).deviceAddress);
                contentValues.add(cv);
            }
            return contentValues;
        }

        public static int deleteAvailablePeers(Context mContext) {
            Uri content_uri_available_friends = DBContract.AvailablePeerEntry.CONTENT_URI;
            return mContext.getContentResolver()
                    .delete(content_uri_available_friends, null, null);
        }

        public static boolean isNewFriend(Context context, String address){

            String selection = FriendEntry.COLUMN_NAME_IS_NEW+" = ? AND " +
                    FriendEntry.COLUMN_NAME_ADDRESS+" = ?";
            String[] selectionArgs = new String[]{Integer.toString(0), address};
            String[] projection = new String[]{FriendEntry._ID, FriendEntry.COLUMN_NAME_ADDRESS};

            Cursor friendsCursor = context.getContentResolver().query(
                    FriendEntry.CONTENT_URI
                    , projection
                    , selection
                    , selectionArgs, null);
            if (friendsCursor != null && friendsCursor.moveToFirst()) {
                friendsCursor.close();
                return false;
            }

            return true;
        }

        public static int doBulkInsert(Context context, ArrayList<ContentValues> contentValues, Uri uri){
            ContentValues[] arContentValues = new ContentValues[contentValues.size()];
            for(int i = 0; i< contentValues.size(); i++){
                arContentValues[i] = contentValues.get(i);
            }
            return context.getContentResolver().bulkInsert(uri, arContentValues);
        }

        public static void updateFriendsStatus(Context context
                , ArrayList<WifiP2pDevice> lPeers
                , HashMap<String, Long> friendsDevices) {
            Log.i(TAG, "updatingFriendsStatus :: "+friendsDevices.size());
            if(!friendsDevices.isEmpty()){
                ArrayList<ContentValues> contentValues =
                        Utils.DBUtils.getContentValues(context, lPeers, friendsDevices);
                if(contentValues.size() > 0){
                    String selection;
                    String[] selectionArgs = new String[1];
                    int updated = 0;
                    for(ContentValues contentValue : contentValues){
                        selection = DBContract.FriendEntry.COLUMN_NAME_ADDRESS+" = ?";
                        selectionArgs[0] = contentValue.getAsString(
                                DBContract.FriendEntry.COLUMN_NAME_ADDRESS);

                        Log.i(TAG, "updating friend :: "+contentValue.toString());
                        updated += context.getContentResolver().update(
                                DBContract.FriendEntry.CONTENT_URI
                                , contentValue
                                , selection
                                , selectionArgs);
                    }
                    Log.i(TAG, "Friends Available updated :: "+updated);
                }else{
                    updateAwayFriends(context);
                }
            }else {
                updateAwayFriends(context);
                for (int i = 0; i < lPeers.size(); i++) {
                    if(lPeers.get(i).status == WifiP2pDevice.CONNECTED) {
                        Log.i(TAG, "Writting connected :: "+lPeers.toString());
                        Utils.writeAddressInPrefs(context, lPeers.get(i).deviceAddress);
                        break;
                    }
                }
            }
            updateMyWidgets(context);
        }

        public static String getFriendName(Context context
                , String wifiAddress){
            Uri content_uri_friends = DBContract.FriendEntry.CONTENT_URI;
            String[] projection = new String[]{FriendEntry.COLUMN_NAME_NAME};

            String selection = FriendEntry.COLUMN_NAME_ADDRESS+" = ?";
            String[] selectionArgs = new String[]{wifiAddress};

            Cursor friendsCursor = context.getContentResolver().query(content_uri_friends
                    , projection, selection, selectionArgs, null);

            if (friendsCursor != null && friendsCursor.moveToFirst()) {
                Log.i(TAG, "friends in DB :: " + friendsCursor.getCount());
                String friendName = friendsCursor.getString(0);
                friendsCursor.close();
                return friendName;
            }
            return null;
        }
    }

    /**
     * Gets connection status in String representation
     * @param status system status
     * @return Human readable string
     */
    public static String getDeviceStatus(int status){
        switch(status){
//            public static final int CONNECTED   = 0;
//            public static final int INVITED     = 1;
//            public static final int FAILED      = 2;
//            public static final int AVAILABLE   = 3;
//            public static final int UNAVAILABLE = 4;
            case 0 : return "Connected";
            case 1 : return "Invited";
            case 2 : return "Failed";
            case 3 : return "Available";
            case 4 : return "Unavailable";
            default: return "Unknown";
        }
    }

    /**
     * Gets reason for failure in String representation
     * @param reason system reason
     * @return Human readable string
     */
    public static String getReason(int reason) {
        switch (reason){
            case WifiP2pManager.BUSY : return "Busy";
            case WifiP2pManager.ERROR : return "Error";
            case WifiP2pManager.NO_SERVICE_REQUESTS: return "No Service Requests";
            case WifiP2pManager.P2P_UNSUPPORTED: return "P2P Unsupported";
            default : return "Unknown";
        }
    }

    public static boolean writeAddressInPrefs(Context context, String wifiAddress){

        SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(context).edit();
        // Wifi P2P is enabled
        editor.putString(SHARED_PREFS_CONNECTED_TO, wifiAddress);
        boolean commited = editor.commit();
        Log.i("writeAddressInPrefs", "wrote pref connectedTo ::"+wifiAddress+":: " + commited);
        return commited;
    }

    public static void removeAddressFromPrefs(Context context){
        SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        // Wifi P2P is enabled
        editor.remove(SHARED_PREFS_CONNECTED_TO);
        editor.apply();
//        boolean commited = editor.commit();
//        Log.i("removeAddressFromPrefs", "removed pref connectedTo :: " + editor.commit());
    }

    public static String getAddressFromPrefs(Context context){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(
                context);
        return sharedPref.getString(SHARED_PREFS_CONNECTED_TO, null);
    }

    public static void updateMyWidgets(Context context) {
        AppWidgetManager man = AppWidgetManager.getInstance(context);

        int[] ids = man.getAppWidgetIds(
                new ComponentName(context, WaryWidget.class));

        Intent updateIntent = new Intent();
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateIntent.putExtra(WaryWidget.WIDGET_ID_KEYS, ids);
//        updateIntent.putExtra(WaryWidget.WIDGET_DATA_KEY, data);
        context.sendBroadcast(updateIntent);
        Log.i("Utils", "sending update broadcast to widget_start");

        AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(
                ids, R.id.widget_friends_list
        );
    }
}
