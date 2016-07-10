package dlujanapps.mx.wary.finder.Compass;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.location.Location;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import dlujanapps.mx.wary.R;

public class Compass
//        extends AppCompatActivity implements SensorEventListener,
//        GoogleApiClient.ConnectionCallbacks
//        , GoogleApiClient.OnConnectionFailedListener
{

    private float[] mRotationData = new float[3];
    private float[] mR = new float[16];
    private float[] mRotationFromVector = new float[9];
    private float[] mOrientation = new float[3];
    private float mCurrentBearing = 0f;

    private ImageView mPointer;
    private TextView mTextView;
    private Location mCurrentLocation;
    private Location mPeerLocation = null;

    private float[] weights = new float[]{0.06f, 0.08f, 0.1f, 0.13f, 0.18f
            , 0.16f, 0.11f, 0.08f, 0.06f, 0.04f};
    private double[] bearingsHist = new double[]{0.0, 0.0, 0.0, 0.0, 0.0
            ,0.0, 0.0, 0.0, 0.0, 0.0};

    private String TAG = getClass().getSimpleName();
    private Context mContext;
    private String mFriendName;

    public Compass(Context context, ImageView pointer
            , String friendName
            , TextView textView){
        mContext = context;
        mPointer = pointer;
        mTextView = textView;
        mFriendName = friendName;
    }

    public void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();

        if(type == Sensor.TYPE_ROTATION_VECTOR){
            System.arraycopy(event.values, 0, mRotationData, 0 , 3);
        } else {
            return;
        }

        SensorManager.getRotationMatrixFromVector(mRotationFromVector, mRotationData);
        SensorManager.getOrientation(mR, mOrientation);

        float bearing = 0;

        if(mCurrentLocation != null && mPeerLocation != null) {
            bearing = mCurrentLocation.bearingTo(mPeerLocation); // (it's already in degrees) .. bearing to friend
        }

        float[] orientation = new float[3];
        SensorManager.getOrientation(mRotationFromVector, orientation);

        float heading = (float) Math.toDegrees(orientation[0]);// IN DEGREES... device orientation
        if(heading < 0 ){
            heading = 360 + ( heading); //heading is negative from -PI (SOUTH) to -0 (NORTH)...
        }

        float direction = (heading + bearing); // both in degrees... in world coordinate system

        RotateAnimation rotateBearing = new RotateAnimation(
                -mCurrentBearing, //from degrees
                -direction, //to degrees
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);

        rotateBearing.setDuration(1);
        rotateBearing.setFillAfter(true);

        mPointer.startAnimation(rotateBearing);
        mCurrentBearing = direction;
    }


    private double getBearingForLocation(Location location, float azimuthInDegrees) {
        if(location == null){
            mTextView.setText(R.string.retrieving_location);
            return 0;
        }else if(mPeerLocation == null) {
            mTextView.setText(String.format(
                    mContext.getString(R.string.retrieving_friend_location),mFriendName ));
            return 0;
        }else{
            String distance = String.format("%.2f",location.distanceTo(mPeerLocation));
            String orientation =getOrientationString(
                    location.bearingTo(mPeerLocation)+azimuthInDegrees);
            String location_string =  String.format(
                    mContext.getString(R.string.location_orientation), distance, orientation);

            mTextView.setText(location_string);
            mPointer.setContentDescription(location_string);

            return addNewBearing(azimuthInDegrees+location.bearingTo(mPeerLocation));
        }
    }

    private double addNewBearing(double bearing){
        double weighted  = 0;
        bearing = Math.abs(bearing);
        for(int i = bearingsHist.length -1; i > 0; i-- ){
            bearingsHist[i] = bearingsHist[i-1];
            weighted += bearingsHist[i]*weights[i];
        }
        bearingsHist[0] = Math.ceil(bearing);
        weighted += bearingsHist[0]*weights[0];

        return weighted;
    }

    private String getOrientationString(double degrees){
        /**
         * bearingTo() --> initial bearing in degrees East of true North
         * thus ... 0 == NORTH, 90 == EAST, 180 == SOUTH, 270 == WEST
         */
        degrees = Math.abs(degrees);
        String NORTH = "NORTH";
        String EAST = "EAST";
        String SOUTH = "SOUTH";
        String SOUTH_WEST = "SOUTH WEST";
        String SOUTH_EAST = "SOUTH EAST";
        String WEST = "WEST";
        String NORTH_WEST = "NORTH WEST";
        String NORTH_EAST = "NORTH EAST";

        if(degrees > 0 && degrees <= 23) {
//            Log.i("Orientation", "degrees :: "+degrees+" ::"+NORTH);
            return NORTH;
        }else

        if(degrees > 23 && degrees <= 67) {
//            Log.i("Orientation", "degrees :: " + degrees + " ::" + NORTH_EAST);
            return NORTH_EAST;
        }else
        if(degrees > 67 && degrees <= 112){
//            Log.i("Orientation", "degrees :: "+degrees+" ::"+EAST);
            return EAST;
        }else if(degrees > 112 && degrees <= 157){
//            Log.i("Orientation", "degrees :: "+degrees+" :: "+SOUTH_EAST);
            return SOUTH_EAST;
        }else if(degrees > 157 && degrees <= 202){
//            Log.i("Orientation", "degrees :: "+degrees+" ::"+SOUTH);
            return SOUTH;
        }else if(degrees > 202 && degrees <= 247) {
//            Log.i("Orientation", "degrees :: "+degrees+" ::"+SOUTH_WEST);
            return SOUTH_WEST;
        }else if(degrees > 247 && degrees <= 291) {
//            Log.i("Orientation", "degrees :: "+degrees+" ::"+WEST);
            return WEST;
        }else if(degrees > 291 && degrees <= 336) {
//            Log.i("Orientation", "degrees :: "+degrees+" ::"+ NORTH_WEST);
            return NORTH_WEST;
        }else if(degrees > 336 ) {
//            Log.i("Orientation", "degrees :: "+degrees+" ::"+NORTH);
            return NORTH;
        }
        Log.i("Orientation", "degrees :: "+degrees+" ::NOPE");
        return "NOPE";
    }

    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        Log.i( TAG ,"LOCATION CHANGED"+location.toString());
        mCurrentBearing = (float) getBearingForLocation(location, 0);
    }

    public void peerLocationChanged(Location location){
        mPeerLocation = location;
        Log.i(TAG, "peer location changed ");
    }
}
