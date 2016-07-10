package dlujanapps.mx.wary.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 *
 * Created by daniellujanvillarreal on 1/13/16.
 */
public class DBHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Wary.db";

    private static final String COMMA_SEP = ",";

    private static final String SQL_CREATE =
//            DBContract.UserEntry.CREATE
//            + COMMA_SEP +
            DBContract.FriendEntry.CREATE
//                    + COMMA_SEP + DBContract.AvailableFriendEntry.CREATE
                    + COMMA_SEP + DBContract.HistoryEntry.CREATE
                    + COMMA_SEP + DBContract.ActionEntry.CREATE
                    + COMMA_SEP + DBContract.AvailablePeerEntry.CREATE;

    private static final String SQL_DELETE_FRIENDS =
            "DROP TABLE IF EXISTS " + DBContract.FriendEntry.TABLE_NAME;
//    private static final String SQL_DELETE_AVAILABLE_FRIENDS =
//            "DROP TABLE IF EXISTS " + DBContract.AvailableFriendEntry.TABLE_NAME;
    private static final String SQL_DELETE_HISTORY =
            "DROP TABLE IF EXISTS " + DBContract.HistoryEntry.TABLE_NAME;
    private static final String SQL_DELETE_AVAILABLE_PEERS =
            "DROP TABLE IF EXISTS " + DBContract.AvailablePeerEntry.TABLE_NAME;
    private static final String SQL_DELETE_ACTIONS =
            "DROP TABLE IF EXISTS " + DBContract.ActionEntry.TABLE_NAME;
    private static final String TAG = "DBHelper";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DBContract.FriendEntry.CREATE);
//        db.execSQL(DBContract.AvailableFriendEntry.CREATE);
        db.execSQL(DBContract.AvailablePeerEntry.CREATE);
        db.execSQL(DBContract.HistoryEntry.CREATE);
        db.execSQL(DBContract.ActionEntry.CREATE);

        insertActions(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_HISTORY);
        db.execSQL(SQL_DELETE_FRIENDS);
        db.execSQL(SQL_DELETE_ACTIONS);
        db.execSQL(SQL_DELETE_AVAILABLE_PEERS);
//        db.execSQL(SQL_DELETE_AVAILABLE_FRIENDS);

        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_HISTORY);
        db.execSQL(SQL_DELETE_FRIENDS);
        db.execSQL(SQL_DELETE_ACTIONS);
        db.execSQL(SQL_DELETE_AVAILABLE_PEERS);
//        db.execSQL(SQL_DELETE_AVAILABLE_FRIENDS);

        onCreate(db);
    }

    private void insertActions(SQLiteDatabase db){
        for(String action : DBContract.ActionEntry.ACTIONS){
            ContentValues contentValues = new ContentValues();
            contentValues.put(DBContract.ActionEntry.COLUMN_NAME_ACTION, action);
            Log.i(TAG, "inserted  " + action + "::" +
                    db.insert(DBContract.ActionEntry.TABLE_NAME, null, contentValues));
        }

    }

}
