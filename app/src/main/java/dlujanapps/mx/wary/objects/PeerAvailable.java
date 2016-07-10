package dlujanapps.mx.wary.objects;

import android.database.Cursor;

import dlujanapps.mx.wary.data.DBContract;

/**
 * Created by daniellujanvillarreal on 2/12/16.
 *
 */
public class PeerAvailable {

    private String name;
    private String wifiAddress;

    public static final int COL_NAME_ID = 0;
    public static final int COL_ADDRESS_ID = 1;

    public static String[] PEERS_PROJECTION = new String[]{
            DBContract.AvailablePeerEntry.COLUMN_PEER_NAME,
            DBContract.AvailablePeerEntry.COLUMN_PEER_ADDRESS};

    public PeerAvailable(){

    }

    public PeerAvailable(Cursor cursor){
        this.name = cursor.getString(COL_NAME_ID);
        this.wifiAddress = cursor.getString(COL_ADDRESS_ID);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWifiAddress() {
        return wifiAddress;
    }

    public void setWifiAddress(String wifiAddress) {
        this.wifiAddress = wifiAddress;
    }

}
