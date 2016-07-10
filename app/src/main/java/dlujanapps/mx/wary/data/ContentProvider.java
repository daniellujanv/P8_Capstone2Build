package dlujanapps.mx.wary.data;

import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import static dlujanapps.mx.wary.data.DBContract.ActionEntry;
import static dlujanapps.mx.wary.data.DBContract.AvailablePeerEntry;
import static dlujanapps.mx.wary.data.DBContract.CONTENT_AUTHORITY;
import static dlujanapps.mx.wary.data.DBContract.FriendEntry;
import static dlujanapps.mx.wary.data.DBContract.HistoryEntry;
import static dlujanapps.mx.wary.data.DBContract.PATH_ACTIONS;
import static dlujanapps.mx.wary.data.DBContract.PATH_AVAILABLE_PEERS;
import static dlujanapps.mx.wary.data.DBContract.PATH_FRIENDS;
import static dlujanapps.mx.wary.data.DBContract.PATH_HISTORY;

public class ContentProvider extends android.content.ContentProvider {


    private static final UriMatcher sUriMatcher = buildUriMatcher();

    static final int USER = 100;
    static final int FRIENDS = 200;
    static final int FRIENDS_WITH_ID = 201;
    //    static final int AVAILABLE_FRIENDS = 300;
//    static final int AVAILABLE_FRIENDS_WITH_ID = 301;
    static final int HISTORY = 400;
    static final int HISTORY_WITH_ID = 401;
    static final int ACTIONS = 500;
    static final int ACTIONS_WITH_ID = 501;
    static final int AVAILABLE_PEERS = 600;
    static final int AVAILABLE_PEERS_WITH_ID = 601;

    static final String HASHTAG = "/#";

    private DBHelper mOpenHelper;


    private static final SQLiteQueryBuilder sFriendsByAvailableFriendsBuilder;
    private static final SQLiteQueryBuilder sHistoryItems;

    static{
        sFriendsByAvailableFriendsBuilder = new SQLiteQueryBuilder();
        //This is an inner join which looks like
        //weather INNER JOIN location ON weather.location_id = location._id
//        sFriendsByAvailableFriendsBuilder.setTables(
//                FriendEntry.TABLE_NAME + " INNER JOIN " +
//                        AvailableFriendEntry.TABLE_NAME +
//                        " ON " + FriendEntry.TABLE_NAME +
//                        "." + FriendEntry._ID +
//                        " = " + AvailableFriendEntry.TABLE_NAME +
//                        "." + AvailableFriendEntry.COLUMN_NAME_FRIEND_ID+
//                        " AND "+ FriendEntry.COLUMN_NAME_IS_NEW+" = 0");

        sHistoryItems = new SQLiteQueryBuilder();
        sHistoryItems.setTables(
                HistoryEntry.TABLE_NAME
                        + " INNER JOIN "+ ActionEntry.TABLE_NAME
                        + " ON "
                        + HistoryEntry.TABLE_NAME+"."+HistoryEntry.COLUMN_NAME_ACTION_ID
                        + " = " + ActionEntry.TABLE_NAME+"."+ActionEntry._ID
                        + " INNER JOIN "+ FriendEntry.TABLE_NAME
                        + " ON "
                        + HistoryEntry.TABLE_NAME+"."+HistoryEntry.COLUMN_NAME_FRIEND_ID
                        + " = "+ FriendEntry.TABLE_NAME+"."+FriendEntry._ID);
    }

    static UriMatcher buildUriMatcher() {

        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        //USER
//        matcher.addURI(authority, DBContract.PATH_USER, USER);

        // FRIENDS
        matcher.addURI(authority, PATH_FRIENDS, FRIENDS);
        matcher.addURI(authority, PATH_FRIENDS+ HASHTAG, FRIENDS_WITH_ID);

        // AVAILABLE FRIENDS
//        matcher.addURI(authority, PATH_AVAILABLE_FRIENDS, AVAILABLE_FRIENDS);
//        matcher.addURI(authority, PATH_AVAILABLE_FRIENDS+ HASHTAG, AVAILABLE_FRIENDS_WITH_ID);

        // AVAILABLE AVAILABLE_PEERS
        matcher.addURI(authority, PATH_AVAILABLE_PEERS, AVAILABLE_PEERS);
        matcher.addURI(authority, PATH_AVAILABLE_PEERS+ HASHTAG, AVAILABLE_PEERS_WITH_ID);

        //HISTORY
        matcher.addURI(authority, PATH_HISTORY, HISTORY);
        matcher.addURI(authority, PATH_HISTORY+ HASHTAG, HISTORY_WITH_ID);

        //ACTIONS
        matcher.addURI(authority, PATH_ACTIONS, ACTIONS);
        matcher.addURI(authority, PATH_ACTIONS+ HASHTAG, ACTIONS_WITH_ID);


        return matcher;
    }

    public ContentProvider() {
    }

    /***** FRIEND BY ID ***********/
    private static final String sFriendById =
            FriendEntry.TABLE_NAME+
                    "." + FriendEntry._ID + " = ? ";

    private Cursor getFriendsById(Uri uri, String[] projection, String sortOrder) {

        String id = FriendEntry.getIdFromUri(uri);

        String selection = sFriendById;
        String[] selectionArgs = new String[]{id};

        return mOpenHelper.getReadableDatabase().query(
                FriendEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    /***** AVAILABLE FRIEND BY ID ***********
     *
     * get FriendEntry from AvailableFriendEntry.friend_id
     * ***********/
//    private static final String sAvailableFriendById =
//            FriendEntry.TABLE_NAME+
//                    "." + FriendEntry._ID + " = ? ";

//    private Cursor getAvailableFriendsById(Uri uri, String[] projection, String sortOrder) {
//
//        // available_friends/##
//        // ## == friend_id from table friends
//        String id = AvailableFriendEntry.getIdFromUri(uri);
//
//        String selection = sAvailableFriendById;
//        String[] selectionArgs = new String[]{id};
//
//        return mOpenHelper.getReadableDatabase().query(
//                FriendEntry.TABLE_NAME,
//                projection,
//                selection,
//                selectionArgs,
//                null,
//                null,
//                sortOrder
//        );
//    }

    private Cursor getFriendsByAvailableFriends(Uri uri, String[] projection, String sortOrder) {
        return sFriendsByAvailableFriendsBuilder.query(
                mOpenHelper.getReadableDatabase()
                , projection
                , null, null, null, null, sortOrder
        );
    }

    /***** ACTION BY ID ***********/
    private static final String sActionById =
            ActionEntry.TABLE_NAME+
                    "." + ActionEntry._ID + " = ? ";

    private Cursor getActionById(Uri uri, String[] projection, String sortOrder) {

        String id = ActionEntry.getIdFromUri(uri);

        String selection = sActionById;
        String[] selectionArgs = new String[]{id};

        return mOpenHelper.getReadableDatabase().query(
                ActionEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    /****** HISTORY ITEMS ********/
    private Cursor getHistoryItems(Uri uri, String[] projection, String sortOrder){
        return sHistoryItems.query(
                mOpenHelper.getReadableDatabase()
                , projection
                , null
                , null
                , null
                , null
                , sortOrder
        );
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new DBHelper(getContext());
        return true;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Implement this to handle requests to delete one or more rows.
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;
        int deletions = -1;
        switch (match) {
            // FRIENDS
//            case AVAILABLE_FRIENDS: {
//                deletions = db.delete(AvailableFriendEntry.TABLE_NAME, null, null);
//                break;
//            }
            // PEERS
            case AVAILABLE_PEERS: {
                deletions = db.delete(AvailablePeerEntry.TABLE_NAME, null, null);
                break;
            }
            case FRIENDS_WITH_ID:
                deletions = deleteFriendById(db, uri, selection, selectionArgs);
                break;
            case HISTORY_WITH_ID:
                deletions = deleteHistoryByFriendId(db, uri, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return deletions;
    }

    public int deleteFriendById(SQLiteDatabase db, Uri uri, String selection, String[] selectionArgs){
        int deleted = 0;
        String friendId = FriendEntry.getIdFromUri(uri);

        selection = FriendEntry._ID+" = ?";
        selectionArgs = new String[]{friendId};

        deleted = db.delete(FriendEntry.TABLE_NAME, selection, selectionArgs);
        return deleted;
    }

    public int deleteHistoryByFriendId(SQLiteDatabase db, Uri uri, String selection, String[] selectionArgs){
        int deleted = 0;
        String friendId = HistoryEntry.getIdFromUri(uri);

        selection = HistoryEntry.COLUMN_NAME_FRIEND_ID+" = ?";
        selectionArgs = new String[]{friendId};

        deleted = db.delete(HistoryEntry.TABLE_NAME, selection, selectionArgs);
        return deleted;
    }

    @Override
    public String getType(Uri uri) {
        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);

        switch (match) {
            // Student: Uncomment and fill out these two cases
            case FRIENDS:
                return FriendEntry.CONTENT_ITEM_TYPE;
            case FRIENDS_WITH_ID:
                return FriendEntry.CONTENT_TYPE;
//            case AVAILABLE_FRIENDS:
//                return AvailableFriendEntry.CONTENT_TYPE;
//            case AVAILABLE_FRIENDS_WITH_ID:
//                return AvailableFriendEntry.CONTENT_TYPE;
            case HISTORY:
                return HistoryEntry.CONTENT_TYPE;
            case HISTORY_WITH_ID:
                return HistoryEntry.CONTENT_TYPE;
            case ACTIONS:
                return ActionEntry.CONTENT_TYPE;
            case ACTIONS_WITH_ID:
                return ActionEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match){
            // FRIENDS
            case FRIENDS : {
                long id = db.insert(FriendEntry.TABLE_NAME, null, values);
                if (id > 0) {
                    returnUri = FriendEntry.buildFriendUri(id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            }
            // AVAILABLE FRIENDS
//            case AVAILABLE_FRIENDS: {
//                long id = db.insert(AvailableFriendEntry.TABLE_NAME, null, values);
//                if (id > 0) {
//                    returnUri = AvailableFriendEntry.buildAvailableFriendUri(id);
//                } else {
//                    throw new android.database.SQLException("Failed to insert row into " + uri);
//                }
//                break;
//            }
            // HISTORY
            case HISTORY:{
                long id = db.insert(HistoryEntry.TABLE_NAME, null, values);
                if (id > 0) {
                    returnUri = HistoryEntry.buildHistoryUri(id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
//            case AVAILABLE_FRIENDS: {
//                db.beginTransaction();
//                int deleted = db.delete(AvailableFriendEntry.TABLE_NAME, null, null);
//                Log.i("BULK INSERT", "deleted # available_friends :: " + deleted);
//
//                int returnCount = 0;
//                try {
//                    for (ContentValues value : values) {
//                        long _id = db.insert(AvailableFriendEntry.TABLE_NAME, null, value);
//                        if (_id != -1) {
//                            returnCount++;
//                        }
//                    }
//                    db.setTransactionSuccessful();
//                } finally {
//                    db.endTransaction();
//                }
//                Log.i("BULK INSERT", "inserted # available_friends :: " + returnCount);
////                Log.i("BULK INSERT", "notifying uri "+uri.toString());
//                getContext().getContentResolver().notifyChange(uri, null);
//                return returnCount;
//            }
            case AVAILABLE_PEERS: {
                db.beginTransaction();
                int deleted = db.delete(AvailablePeerEntry.TABLE_NAME, null, null);
                Log.i("BULK INSERT", "deleted # available_peers :: " + deleted);

                int returnCount = 0;
                try {
                    for (ContentValues value : values) {

                        String selection = FriendEntry.COLUMN_NAME_ADDRESS
                                +" = ? AND is_new = 0";
                        String[] selectionArgs = {value.getAsString(
                                AvailablePeerEntry.COLUMN_PEER_ADDRESS)};
                        // try to match availablePeer with devices already in Friends
                        Cursor isFriendCursor = db.query(FriendEntry.TABLE_NAME, null, selection, selectionArgs
                                , null, null,null);
                        //IF not in friends then add to availablePeers
                        if(!isFriendCursor.moveToFirst()) {

                            long _id = db.insert(AvailablePeerEntry.TABLE_NAME, null, value);
                            if (_id != -1) {
                                returnCount++;
                            }
                        }
                        isFriendCursor.close();
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                Log.i("BULK INSERT", "inserted # available_peers :: " + returnCount);
//                Log.i("BULK INSERT", "notifying uri "+uri.toString());
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            }
            default:
                return super.bulkInsert(uri, values);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        Cursor retCursor;

        switch (sUriMatcher.match(uri)){
            // friends/
            case FRIENDS :
                retCursor = mOpenHelper.getReadableDatabase().query(
                        FriendEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            // friends/*
            case FRIENDS_WITH_ID :
                retCursor = getFriendsById(uri, projection, sortOrder);
                break;

            // available_friends/
//            case AVAILABLE_FRIENDS :
//                retCursor = getFriendsByAvailableFriends(uri, projection, sortOrder);
//                break;
            // available_friends/*
//            case AVAILABLE_FRIENDS_WITH_ID :
//                retCursor = getAvailableFriendsById(uri, projection, sortOrder);
//                break;
            // available_peers
            case AVAILABLE_PEERS :
                retCursor = mOpenHelper.getReadableDatabase().query(
                        AvailablePeerEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            // history/
            case HISTORY :
                retCursor = getHistoryItems(uri, projection, sortOrder);
                break;
            // history/*
            case HISTORY_WITH_ID :
                retCursor = mOpenHelper.getReadableDatabase().query(
                        HistoryEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
//                retCursor = getAvailableFriendsById(uri, projection, sortOrder); dunno if we will need history by ID
                break;
            // action/
            case ACTIONS :
                retCursor = mOpenHelper.getReadableDatabase().query(
                        ActionEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            // actions/*
            case ACTIONS_WITH_ID :
                retCursor = getActionById(uri, projection, sortOrder);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int updated = 0;

        switch (match) {
            // FRIENDS
            case FRIENDS:
                updated = db.update(FriendEntry.TABLE_NAME
                        , values, selection, selectionArgs);
                break;
            case HISTORY:
                updated = db.update(HistoryEntry.TABLE_NAME
                        , values, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return updated;
    }
}
