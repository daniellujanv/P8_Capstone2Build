package dlujanapps.mx.wary.objects;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import static dlujanapps.mx.wary.data.DBContract.FriendEntry;

/**
 * Created by daniellujanvillarreal on 3/2/16.
 */
public class Friend {

    private String TAG = getClass().getSimpleName();

    private String name;
    private String photoUri;
    private String address;
    private boolean isNew;
    private int role;
    private int status = -1;
    private int id;

    public static String[] FULL_PROJECTION = new String[]{
            FriendEntry.COLUMN_NAME_NAME
            , FriendEntry.COLUMN_NAME_ADDRESS
            , FriendEntry.COLUMN_NAME_ROLE
            , FriendEntry.COLUMN_NAME_IS_NEW
            , FriendEntry.COLUMN_NAME_PHOTO_URI
            , FriendEntry.COLUMN_NAME_STATUS
            , FriendEntry._ID
    };

    private int CURSOR_ID_NAME = 0;
    private int CURSOR_ID_ADDRESS = 1;
    private int CURSOR_ID_ROLE = 2;
    private int CURSOR_ID_IS_NEW = 3;
    private int CURSOR_ID_PHOTO_URI = 4;
    private int CURSOR_ID_STATUS = 5;
    private int CURSOR_ID_ID = 6;

    public Friend(){}

    public Friend(Cursor cursor){
        id = cursor.getInt(CURSOR_ID_ID);
        name = cursor.getString(CURSOR_ID_NAME);
        address = cursor.getString(CURSOR_ID_ADDRESS);
        role = cursor.getInt(CURSOR_ID_ROLE);
        isNew = cursor.getInt(CURSOR_ID_IS_NEW) == 1;
        photoUri = cursor.getString(CURSOR_ID_PHOTO_URI);
        status = cursor.getInt(CURSOR_ID_STATUS);
    }

    public Friend(Context context, String friendAddress){
        String selection = FriendEntry.COLUMN_NAME_ADDRESS+" = ?";
        String[] selectionArgs = new String[]{friendAddress};

        Cursor cursor = context.getContentResolver()
                .query(FriendEntry.CONTENT_URI, Friend.FULL_PROJECTION
                        , selection, selectionArgs, null);
        if(cursor != null && cursor.moveToFirst()){
            id = cursor.getInt(CURSOR_ID_ID);
            name = cursor.getString(CURSOR_ID_NAME);
            address = cursor.getString(CURSOR_ID_ADDRESS);
            role = cursor.getInt(CURSOR_ID_ROLE);
            isNew = cursor.getInt(CURSOR_ID_IS_NEW) == 1;
            photoUri = cursor.getString(CURSOR_ID_PHOTO_URI);
            status = cursor.getInt(CURSOR_ID_STATUS);

            cursor.close();
            Log.i(TAG, "initialized friend");
        }else{
            Log.e(TAG, "Error initializing friend... cursor null");
        }
    }

    /**
     *
     * @param peerAvailable
     * @param isNew
     * @param role
     */
    public Friend(PeerAvailable peerAvailable, boolean isNew, int role){
        name = peerAvailable.getName();
        address = peerAvailable.getWifiAddress();
        photoUri = "NA";
        this.isNew = isNew;
        this.role = role;
        status = FriendEntry.STATUS_AVAILABLE;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhotoUri() {
        return photoUri;
    }

    public void setPhotoUri(String photoUri) {
        this.photoUri = photoUri;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setIsNew(boolean is_new) {
        this.isNew = is_new;
    }

    public int getRole() {
        return role;
    }

    public void setRole(int role) {
        this.role = role;
    }

    public ContentValues getFutureFriendContentValues(){

        ContentValues contentValues = new ContentValues();
        contentValues.put(FriendEntry.COLUMN_NAME_NAME, getName());
        contentValues.put(FriendEntry.COLUMN_NAME_ADDRESS, getAddress());
        contentValues.put(FriendEntry.COLUMN_NAME_IS_NEW, isNew());
        contentValues.put(FriendEntry.COLUMN_NAME_PHOTO_URI, getPhotoUri());
        contentValues.put(FriendEntry.COLUMN_NAME_ROLE, getRole());
        contentValues.put(FriendEntry.COLUMN_NAME_STATUS,
                FriendEntry.STATUS_AVAILABLE);

        return contentValues;
    }

    public ContentValues getUpdateAvailableFriendContentValues(int status){

        ContentValues contentValues = new ContentValues();
        contentValues.put(FriendEntry.COLUMN_NAME_STATUS, status);

        return contentValues;
    }

    public String toString(){
        return "name::"+name
                + " photoUri::"+photoUri
                + " address::"+address
                + " isNew::"+isNew
                + " role::"+role;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
