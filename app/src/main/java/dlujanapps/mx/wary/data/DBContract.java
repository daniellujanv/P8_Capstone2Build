package dlujanapps.mx.wary.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by daniellujanvillarreal on 1/13/16.
 *
 */
public class DBContract {


    private static final String INTEGER_TYPE = " INTEGER";
    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";

    public static final String CONTENT_AUTHORITY = "dlujanapps.mx.wary.provider";

    // Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    // the content provider.
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // PATHS FOR TABLES

    //    public static final String PATH_USER = "user";
    public static final String PATH_FRIENDS = "friends";
//    public static final String PATH_AVAILABLE_FRIENDS = "available_friends";
    public static final String PATH_AVAILABLE_PEERS = "available_peers";
    public static final String PATH_HISTORY = "history";
    public static final String PATH_ACTIONS = "actions";

    public DBContract(){}

    /************** USER ********************/
//    public static abstract class UserEntry implements BaseColumns {
//
//        /** content provider stuff ***/
//        public static final Uri CONTENT_URI =
//                BASE_CONTENT_URI.buildUpon().appendPath(PATH_USER).build();
//
//        public static final String CONTENT_TYPE =
//                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_USER;
//
//        public static final String CONTENT_ITEM_TYPE =
//                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_USER;
//
//        public static Uri buildUserUri(long id) {
//            return ContentUris.withAppendedId(CONTENT_URI, id);
//        }
//
//
//        /** sqlite stuff ***/
//        public static final String TABLE_NAME = "user";
//        public static final String COLUMN_NAME_NAME = "user_name";
//        public static final String COLUMN_NAME_ADDRESS = "user_address";
//
//        public static final String CREATE =
//                "CREATE TABLE " + TABLE_NAME + " (" +
//                        _ID + " INTEGER PRIMARY KEY," +
//                        COLUMN_NAME_NAME + TEXT_TYPE + COMMA_SEP +
//                        COLUMN_NAME_ADDRESS + TEXT_TYPE +
//                        " ) ";
//    }

    /************* FRIENDS *********************/
    public static abstract class FriendEntry implements BaseColumns {

        /** content provider stuff ***/
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_FRIENDS).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_FRIENDS;

        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_FRIENDS;

        public static Uri buildFriendUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildFriendIntentUri(String address) {
            return Uri.withAppendedPath(Uri.parse(CONTENT_AUTHORITY), address);
        }

        public static String getIdFromUri(Uri uri){
            return uri.getPathSegments().get(1);
        }

        public static String getAddressFromUri(Uri uri){
            return uri.getPathSegments().get(1);
        }

        /** sqlite stuff ***/
        public static final String TABLE_NAME = "friends";
        public static final String COLUMN_NAME_NAME = "friend_name";
        public static final String COLUMN_NAME_PHOTO_URI = "photo_uri";
        public static final String COLUMN_NAME_ADDRESS = "friend_address";
        public static final String COLUMN_NAME_IS_NEW = "is_new";
        public static final String COLUMN_NAME_ROLE = "role";
        public static final String COLUMN_NAME_STATUS = "status";

        public static final int STATUS_CONNECTED = 2;
        public static final int STATUS_AVAILABLE = 1;
        public static final int STATUS_AWAY = 0;

        public static final int ROLE_ADDER = 1;
        public static final int ROLE_ADDED = 2;


        public static final String CREATE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY," +
                        COLUMN_NAME_NAME + TEXT_TYPE + COMMA_SEP +
                        COLUMN_NAME_PHOTO_URI + TEXT_TYPE + COMMA_SEP +
                        COLUMN_NAME_ADDRESS + TEXT_TYPE + COMMA_SEP +
                        COLUMN_NAME_IS_NEW + INTEGER_TYPE + COMMA_SEP +
                        COLUMN_NAME_ROLE + INTEGER_TYPE + COMMA_SEP +
                        COLUMN_NAME_STATUS + INTEGER_TYPE +
                        " ) ";
    }

    /************* HISTORY *********************/
    public static abstract class HistoryEntry implements BaseColumns {
        /** content provider stuff ***/
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_HISTORY).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_HISTORY;

        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_HISTORY;

        public static Uri buildHistoryUri(long friendId) {
            return ContentUris.withAppendedId(CONTENT_URI, friendId);
        }


        /** sqlite stuff ***/
        public static final String TABLE_NAME = "histories";
        public static final String COLUMN_NAME_FRIEND_ID = "friend_id";
        public static final String COLUMN_NAME_ACTION_ID = "action_id";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";


        public static final String CREATE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY," +
                        COLUMN_NAME_FRIEND_ID + INTEGER_TYPE + COMMA_SEP +
                        COLUMN_NAME_ACTION_ID + INTEGER_TYPE + COMMA_SEP +
                        COLUMN_NAME_TIMESTAMP + TEXT_TYPE +
                        " ) ";

        public static String getIdFromUri(Uri uri){
            return uri.getPathSegments().get(1);
        }

    }

    /**********************************/
    public static abstract class ActionEntry implements BaseColumns {
        /** content provider stuff ***/
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_ACTIONS).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_ACTIONS;

        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_ACTIONS;

        public static Uri buildActionUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static String getIdFromUri(Uri uri){
            return uri.getPathSegments().get(1);
        }

        /** sqlite stuff ***/
        public static final String TABLE_NAME = "actions";
        public static final String COLUMN_NAME_ACTION = "actionName";

        public static final String ACTION_SEARCHED = "searched for";
        public static final String ACTION_SEARCHED_YOU = "searched you";
        public static final String ACTION_FOUND = "found";
        public static final String ACTION_FOUND_YOU = "found you";
        public static final String ACTION_ADDED = "added";
        public static final String ACTION_ADDED_YOU = "added you";
        public static final String ACTION_CONNECTION_FAILED = "Unable to connect with ";

        public static final String[] ACTIONS = new String[]{
                ACTION_SEARCHED, ACTION_SEARCHED_YOU
                , ACTION_FOUND, ACTION_FOUND_YOU
                , ACTION_ADDED, ACTION_ADDED_YOU, ACTION_CONNECTION_FAILED};


        public static final String INSERT_ACTIONS =
                "INSERT INTO "+TABLE_NAME + "("+COLUMN_NAME_ACTION+")"
                        + " VALUES ('" + ACTION_SEARCHED + "'); "
                        + "INSERT INTO "+TABLE_NAME + "("+COLUMN_NAME_ACTION+")"
                        + " VALUES ('" + ACTION_SEARCHED_YOU + "'); "
                        + "INSERT INTO "+TABLE_NAME + "("+COLUMN_NAME_ACTION+")"
                        + " VALUES ('" + ACTION_FOUND + "'); "
                        + "INSERT INTO "+TABLE_NAME + "("+COLUMN_NAME_ACTION+")"
                        + " VALUES ('" + ACTION_FOUND_YOU + "'); "
                        + "INSERT INTO "+TABLE_NAME + "("+COLUMN_NAME_ACTION+")"
                        + " VALUES ('" + ACTION_ADDED + "'); "
                        + "INSERT INTO "+TABLE_NAME + "("+COLUMN_NAME_ACTION+")"
                        + " VALUES ('" + ACTION_ADDED_YOU + "');";

        public static final String CREATE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY," +
                        COLUMN_NAME_ACTION + TEXT_TYPE +
                        " ) ";
    }

    /************* AVAILABLE FRIENDS *********************/
    public static abstract class AvailablePeerEntry implements BaseColumns {
        /** content provider stuff ***/
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_AVAILABLE_PEERS).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_AVAILABLE_PEERS;

        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_AVAILABLE_PEERS;

        public static Uri buildAvailablePeerUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static String getIdFromUri(Uri uri){
            return uri.getPathSegments().get(0);
        }

        /** sqlite stuff ***/
        public static final String TABLE_NAME = "available_peers";
        public static final String COLUMN_PEER_NAME = "name";
        public static final String COLUMN_PEER_ADDRESS = "address";


        public static final String CREATE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY," +
                        COLUMN_PEER_NAME +  TEXT_TYPE + COMMA_SEP
                        + COLUMN_PEER_ADDRESS + TEXT_TYPE +
                        " ) ";
    }
}
