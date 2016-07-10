package dlujanapps.mx.wary.finder.Location;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.Timer;

import butterknife.Bind;
import butterknife.ButterKnife;
import dlujanapps.mx.wary.R;
import dlujanapps.mx.wary.data.DBContract;
import dlujanapps.mx.wary.finder.ClientService;
import dlujanapps.mx.wary.finder.Compass.Compass;
import dlujanapps.mx.wary.finder.WDReceiver;
import dlujanapps.mx.wary.objects.Action;
import dlujanapps.mx.wary.objects.Friend;
import dlujanapps.mx.wary.objects.HistoryItem;
import dlujanapps.mx.wary.objects.Utils;


public class LocatorActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks
        , GoogleApiClient.OnConnectionFailedListener
        , LocationListener
        , SensorEventListener
{
    private static final int INTERVAL_LOCATION_REQUESTS = 5000;
    private final int MY_PERMISSIONS_REQUEST = 100;
    public static final String CONN_STATUS_KEY = "connection_status";
    public static final String CONN_FAILED_ACTION = "CONNECTION_FAILED";
    public static final String CONN_STATUS_ACTION = "CONNECTION_STATUS_CHANGED";
    public static final String PEER_LOC_CHANGED_ACTION = "PEER_LOCATION_CHANGED";
    public static final String PEER_LOC_KEY = "peer_location";
    public static final String CONN_STATUS_NOT_CONNECTED = "Not Connected";
    public static final String CONN_STATUS_CONNECTED = "CONNECTED";
    public static final String CONN_STRING_CONNECTED = "Connected";
    public final static String CONN_STATUS_DISCONNECTED = "DISCONNECTED";
    public final static String CONN_INFO_AVAILABLE_ACTION = "CONNECTION_INFO_AVAILABLE";

    public final static String FRIEND_ADDRESS_KEY = "device_address";
    public final static String FRIEND_NAME_KEY = "device_name";
    public final static String FRIEND_ID_KEY = "friend_id";
    public final static String IS_FRIEND_KEY = "is_friend";


    private GoogleApiClient mGoogleApiClient;
    private SensorManager mSensorManager;
    private boolean hasEnoughSensors = false;
    private Compass mCompass;

    private Context mContext;
    private Timer mTimer;

    @Bind(R.id.content_locator_pointer) ImageView mPointer;
    @Bind(R.id.location_info) TextView mLocationInfoView;
    @Bind(R.id.connection_info) TextView mConnectionStatusView;
    @Bind(R.id.connecting_overlay) View mConnectingOverlay;
    @Bind(R.id.progress_bar_locator) ProgressBar mProgressBar;
    @Bind(R.id.not_enough_sensors) TextView mNoSensorsView;

    LocationRequest mLocationRequest;

    private Location mCurrentLocation;
    private String TAG = getClass().getSimpleName();

    private boolean updatedActionInDB = false;
    private boolean mIsFriend = false;
    private Friend mFriend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locator);
        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mContext = getApplicationContext();

        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);

        String friendAddress = getIntent().getExtras().getString(FRIEND_ADDRESS_KEY);
        int friendId = getIntent().getExtras().getInt(FRIEND_ID_KEY);
        mIsFriend = getIntent().getExtras().getBoolean(IS_FRIEND_KEY);

        mFriend = new Friend();
        mFriend.setAddress(friendAddress);
        mFriend.setId(friendId);
        mFriend.setName(getIntent().getExtras().getString(LocatorActivity.FRIEND_NAME_KEY));

        mConnectionStatusView.setText(
                String.format(getString(R.string.connecting_to_message),
                        mFriend.getName()));

        mCompass = new Compass(mContext, mPointer
                , mFriend.getName()
                , mLocationInfoView);

        checkCompassSensors();
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && requestCode == 100) {
            boolean fine_location_granted = ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
            boolean coarse_location_granted = ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
            if (fine_location_granted && coarse_location_granted) {
                LocationServices.FusedLocationApi
                        .requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            }else{
                Toast.makeText(getApplicationContext()
                        , R.string.location_permission_needed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onStart(){
        super.onStart();
    }

    /* register the broadcast receiver with the intent values to be matched */
    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CONN_STATUS_ACTION);
        intentFilter.addAction(CONN_FAILED_ACTION);
        intentFilter.addAction(PEER_LOC_CHANGED_ACTION);
        intentFilter.addAction(CONN_INFO_AVAILABLE_ACTION);
        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(mBroadcastReceiver, intentFilter);


        Intent intent = new Intent(this, WDReceiver.class);
        intent.setAction(WDReceiver.ACTION_CONNECT);
        intent.putExtra(FRIEND_ADDRESS_KEY, mFriend.getAddress());
        intent.putExtra(IS_FRIEND_KEY, mIsFriend);

        sendBroadcast(intent);

    }

    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        Log.i("onPause", "paused");

        disconnect();
    }

    private void disconnect(){
        if(mTimer != null) {
            Log.i(TAG, "canceling TIMER");
            mTimer.cancel();
            mTimer.purge();
        }

        Log.i(TAG, "TIMER = null");
        mTimer = null;


        Intent intent = new Intent(this, WDReceiver.class);
        intent.setAction(WDReceiver.ACTION_DISCONNECT);
        sendBroadcast(intent);

        LocalBroadcastManager.getInstance(getApplicationContext())
                .unregisterReceiver(mBroadcastReceiver);

        mSensorManager.unregisterListener(this);
        if(mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
        }
    }

    @Override
    public void onStop(){
        super.onStop();
        if(mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_activity_locator, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            disconnect();
            super.onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed(){
        disconnect();
        super.onBackPressed();
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(INTERVAL_LOCATION_REQUESTS);//10 secs

        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }else{
            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    100);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
//        mConnStatus.setText("Connection Suspended");
        Log.d(TAG, "connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
//        mConnStatus.setText("Connection Failed");
        Log.d(TAG, "connection failed");

    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mCompass.onLocationChanged(mCurrentLocation);
    }

    public Location getCurrentLocation(){
        return mCurrentLocation;
    }

    public void onSensorChanged(SensorEvent event) {

        mCompass.onSensorChanged(event);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void checkCompassSensors(){
        Sensor gSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
        Sensor rSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        if(gSensor != null && mSensor != null) {
            hasEnoughSensors = true;
            mSensorManager.registerListener(this, gSensor, SensorManager.SENSOR_DELAY_GAME);
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_GAME);
            mSensorManager.registerListener(this, rSensor, SensorManager.SENSOR_DELAY_GAME);
        }else{
            hasEnoughSensors = false;
            mPointer.setVisibility(View.GONE);
            mNoSensorsView.setVisibility(View.VISIBLE);
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "receiving broadcast :: "+intent.getAction());
            String action = intent.getAction();
            Bundle bundle = intent.getExtras();
            switch (action){

                case CONN_INFO_AVAILABLE_ACTION :
//                        startCommunication(bundle);
                    break;
                case CONN_STATUS_ACTION :
                    connectionStatusChanged(bundle);
                    break;
                case PEER_LOC_CHANGED_ACTION :
                    peerLocationChanged(bundle);
                    break;
                case CONN_FAILED_ACTION :
                    connectionFailed();
                    break;
                default: break;
            }
        }
    };

    private void connectionFailed(){

        Toast.makeText(LocatorActivity.this, "Connection failed!", Toast.LENGTH_SHORT).show();
        mProgressBar.setVisibility(View.INVISIBLE);
        onBackPressed();
    }

    private void connectionStatusChanged(Bundle bundle){
        if(bundle != null){
            String connection_status = bundle.getString(CONN_STATUS_KEY);

            mConnectionStatusView.setText(connection_status);
            Log.i(TAG, "RECEIVED "+connection_status+" IN LOCATORact");

            if(connection_status.equals(CONN_STATUS_DISCONNECTED)) {
//                mPointerNorth.setVisibility(View.INVISIBLE);
                mPointer.setVisibility(View.INVISIBLE);
                mNoSensorsView.setVisibility(View.VISIBLE);

                Toast.makeText(LocatorActivity.this, R.string.connection_lost
                        , Toast.LENGTH_SHORT).show();

                onBackPressed();
            }else if(connection_status.equals(CONN_STATUS_CONNECTED)){
                if(hasEnoughSensors) {
//                                mPointerNorth.setVisibility(View.VISIBLE);
                    mPointer.setVisibility(View.VISIBLE);
                    mNoSensorsView.setVisibility(View.GONE);

                }else{
                    mPointer.setVisibility(View.INVISIBLE);
                    mNoSensorsView.setVisibility(View.VISIBLE);
                }
                mConnectionStatusView.setText(CONN_STRING_CONNECTED);
                mLocationInfoView.setText(R.string.retrieving_location);
                mConnectingOverlay.setVisibility(View.GONE);
                mGoogleApiClient.connect();
            }
        }else{
            mConnectionStatusView.setText(R.string.connection_status_error);
        }
        mProgressBar.setVisibility(View.INVISIBLE);
    }

    private void peerLocationChanged(Bundle bundle){
        //  Location[fused 25.642114,-100.350934 acc=23 et=+19h2m27s339ms alt=640.0 vel=0.0]
        String locationString = bundle.getString(PEER_LOC_KEY);
        if(!locationString.equals(ClientService.LOCATION_UNAVAILABLE)) {
            try {
                String[] locationArray = locationString.split(" ")[1].split(",");
                double latitude = Double.parseDouble(locationArray[0]);
                double longitude = Double.parseDouble(locationArray[1]);

                Location location = new Location("peer");
                location.setLatitude(latitude);
                location.setLongitude(longitude);

                mCompass.peerLocationChanged(location);

                if (!updatedActionInDB) {
                    HistoryItem historyItem = new HistoryItem(getApplicationContext()
                            , mFriend.getId()
                            , DBContract.ActionEntry.ACTION_SEARCHED);

                    HistoryItem newHistory = new HistoryItem(getApplicationContext()
                            , mFriend.getId()
                            , DBContract.ActionEntry.ACTION_FOUND);
                    Log.i(TAG, "updating historyitem to :: " + Action.ACTION_FOUND);
                    Utils.DBUtils.updateHistoryItem(getApplicationContext()
                            , historyItem, newHistory.getActionId());
                }
            } catch (NullPointerException e) {
                Log.i(TAG, "Error processing peer location");
            }
        }else{
            mLocationInfoView.setText(
                    String.format(getString(R.string.retrieving_friend_location)
                            , mFriend.getName()));
            mPointer.setVisibility(View.INVISIBLE);
        }
    }
}
