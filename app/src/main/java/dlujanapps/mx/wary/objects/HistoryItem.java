package dlujanapps.mx.wary.objects;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;

import static dlujanapps.mx.wary.data.DBContract.ActionEntry;
import static dlujanapps.mx.wary.data.DBContract.FriendEntry;
import static dlujanapps.mx.wary.data.DBContract.HistoryEntry;

/**
 * Created by daniellujanvillarreal on 3/30/16.
 *
 */
public class HistoryItem {

    private String TAG = getClass().getSimpleName();
    public static final String[] FULL_PROJECTION = new String[]{
            HistoryEntry._ID
            , HistoryEntry.COLUMN_NAME_FRIEND_ID
            , HistoryEntry.COLUMN_NAME_ACTION_ID
            , HistoryEntry.COLUMN_NAME_TIMESTAMP
    };

    public static final String[] PRETTY_PROJECTION = new String[]{
            FriendEntry.COLUMN_NAME_NAME
            , ActionEntry.COLUMN_NAME_ACTION
            , HistoryEntry.COLUMN_NAME_TIMESTAMP
    };

    public static final int COL_FRIEND_ID = 0;
    public static final int COL_ACTION_ID = 1;
    public static final int COL_TIMESTAMP_ID = 2;



    private int friendId = -1;
    private String friendName;
    private int actionId = -1;
    private String actionName;
    private long timestamp = -1;
    private String date;

    public HistoryItem(Context context, int friendId, String action){
        if(setActionId(context, action)){
            this.friendId = friendId;
            timestamp = System.currentTimeMillis();
        }
    }

    public HistoryItem(Context context, String friendAddress, String action)
    {
        if(setActionId(context, action)){
            setFriendId(context, friendAddress);
            timestamp = System.currentTimeMillis();
        }
    }

    public HistoryItem(Cursor cursor){
        friendName = cursor.getString(COL_FRIEND_ID);
        actionName = cursor.getString(COL_ACTION_ID);
        timestamp = cursor.getLong(COL_TIMESTAMP_ID);
    }

    public int getFriendId() {
        return friendId;
    }

    public void setFriendId(int friendId) {
        this.friendId = friendId;
    }

    public void setFriendId(Context context, String friendAddress){
        String selection = FriendEntry.COLUMN_NAME_ADDRESS + " = ?";
        String[] selectionArgs = new String[]{friendAddress};
        String[] projection = new String[]{FriendEntry.TABLE_NAME+"."+FriendEntry._ID};

        Cursor cursor = context.getContentResolver().query(
                FriendEntry.CONTENT_URI
                ,projection
                , selection
                , selectionArgs
                , null
        );

        if(cursor != null && cursor.moveToFirst()){
            this.friendId = cursor.getInt(0);
            cursor.close();
        }else{
            Log.e(TAG, "Error retrieving friend");
        }
    }

    public int getActionId() {
        return actionId;
    }

    public void setActionId(int actionId) {
        this.actionId = actionId;
    }

    private boolean setActionId(Context context, String action) {

        String selection = ActionEntry.COLUMN_NAME_ACTION + " = ?";
        String[] selectionArgs = new String[]{action};
        String[] projection = new String[]{ActionEntry.TABLE_NAME+"."+ActionEntry._ID};
        ContentResolver contentResolver = context.getContentResolver();
        if(contentResolver != null) {
            Cursor cursor = contentResolver.query(
                    ActionEntry.CONTENT_URI
                    , projection
                    , selection
                    , selectionArgs
                    , null
            );
//            Cursor cursor = contentResolver.query(
//                    ActionEntry.CONTENT_URI
//                    , projection
//                    , null
//                    , null
//                    , null
//            );

            if (cursor != null && cursor.moveToFirst()) {

//                do{
//                    Log.e(TAG, "action ids :: "+cursor.getInt(0));
//
//                }while(cursor.moveToNext());
                this.actionId = cursor.getInt(0);

                cursor.close();
                return true;
            } else {
                Log.e(TAG, "Error adding historyItem");
                try {
                    Toast.makeText(context, "Error adding item to History", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }
        }else{
            Log.e(TAG, "Error adding historyItem :: Content Resolver == null");
            return false;
        }
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public ContentValues getContentValues(){
        ContentValues contentValues = new ContentValues();
        contentValues.put(HistoryEntry.COLUMN_NAME_FRIEND_ID, friendId);
        contentValues.put(HistoryEntry.COLUMN_NAME_ACTION_ID, actionId);
        contentValues.put(HistoryEntry.COLUMN_NAME_TIMESTAMP, timestamp);
        return contentValues;
    }

    public String toString(){
        return "FriendId::"+friendId
                +" ActionId::"+actionId
                +" timestamp::"+timestamp;
    }

    public String getPrettyDate(){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);

        return
                String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))
                        + "." + String.format("%02d", calendar.get(Calendar.MONTH)) + " "
                + String.format("%02d",calendar.get(Calendar.HOUR_OF_DAY))
                + ":"+ String.format("%02d", calendar.get(Calendar.MINUTE)) + " "
                ;
    }

    public String getPrettyString(){
        switch (actionName){
            case Action.ACTION_ADDED:
            case Action.ACTION_FOUND:
                return "You "+actionName+" "+ friendName;
            case Action.ACTION_SEARCHED:
                return "You searched for "+friendName;
            case Action.ACTION_SEARCHED_YOU:
                return friendName+" searched for you";
            case Action.ACTION_ADDED_YOU:
            case Action.ACTION_FOUND_YOU:
                return friendName+" "+actionName;
            case Action.ACTION_CONNECTION_FAILED:
                return actionName+" "+friendName;
            default:
                return "N/a";
        }
    }
}
