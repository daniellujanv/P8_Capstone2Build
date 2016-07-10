package dlujanapps.mx.wary;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import dlujanapps.mx.wary.finder.Location.LocatorActivity;
import dlujanapps.mx.wary.finder.WDReceiver;
import dlujanapps.mx.wary.fragments.AddFriendFragment;
import dlujanapps.mx.wary.fragments.FriendsFragment;
import dlujanapps.mx.wary.fragments.HistoryFragment;
import dlujanapps.mx.wary.fragments.StartFragment;
import dlujanapps.mx.wary.fragments.TutorialFragment;
import dlujanapps.mx.wary.objects.Friend;
import dlujanapps.mx.wary.widget.WaryWidget;

import static dlujanapps.mx.wary.objects.Utils.updateMyWidgets;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener
        , FriendsFragment.OnFriendsInteractionListener
        , StartFragment.OnStartFragmentInteractionListener
        , HistoryFragment.OnListHistoryInteractionListener
        , SharedPreferences.OnSharedPreferenceChangeListener
{

    private static final String TUTORIAL_FAGMENT_TRANSACTION = "tutorial";

    private String TAG = getClass().getSimpleName();

    //    @Bind(R.id.fab) FloatingActionButton mFab;
    private Toolbar toolbar;
    @Bind(R.id.nav_view) NavigationView mNavigationView;
    @Bind(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    private TextView mDisplayNameView;

    public static String FRAGMENT_TAG_FRIENDS = "friends";
    public static String FRAGMENT_TAG_START_FRIENDS = "start_friends";
    private String FRAGMENT_TAG_HELP_FEEDBACK = "help_feedback";
    private String FRAGMENT_TAG_HISTORY = "history";
    private boolean isWifiDisplayNameSet = false;

    private String FIRST_OPEN_PREF_KEY = "firs_open_wary";

    private BroadcastReceiver mWDReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.WaryTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        SharedPreferences sharedPref = PreferenceManager.
                getDefaultSharedPreferences(getApplicationContext());
        String wifiName = sharedPref
                .getString(getString(R.string.pref_key_display_name), "");
        isWifiDisplayNameSet = !TextUtils.isEmpty(wifiName);

        mNavigationView.setNavigationItemSelectedListener(this);

        mDisplayNameView = (TextView) mNavigationView.getHeaderView(0)
                .findViewById(R.id.nav_header_display_name);
        if(isWifiDisplayNameSet){
            mDisplayNameView.setText(wifiName);
        }


        sharedPref.registerOnSharedPreferenceChangeListener(this);

        if(sharedPref.getBoolean(FIRST_OPEN_PREF_KEY, true)){
            TutorialFragment.newInstance().show(
                    getSupportFragmentManager(), TUTORIAL_FAGMENT_TRANSACTION);
        }

        if(savedInstanceState == null) {
            onNavigationItemSelected(mNavigationView.getMenu().getItem(0));
        }
//        mAppBarLayout.setOnDragListener(new View.OnDragListener() {
//            @Override
//            public boolean onDrag(View v, DragEvent event) {
//                Log.i(TAG, "DRAGGING");
//                return false;
//            }
//        });
//        mAppBarLayout.setActivated(false);
        if(getIntent() != null){
            handleExtras(getIntent());
        }else{
            Log.i(TAG, "intent && extras null");
        }
//        updateMyWidgets(getApplicationContext());
    }

    private void handleExtras(Intent intent){
        String action = intent.getAction();
        switch (action){
            case WaryWidget.WIDGET_ACTION_START_DISCOVERY:
                onStartDiscoveryInteraction();
                break;
            case WaryWidget.WIDGET_ACTION_GO_TO_FRIEND:
                int id = intent.getIntExtra(WaryWidget.WIDGET_EXTRA_FRIEND_ID, -1);
                String name = intent.getStringExtra(WaryWidget.WIDGET_EXTRA_FRIEND_NAME);
                String address = intent.getStringExtra(WaryWidget.WIDGET_EXTRA_FRIEND_ADDRESS);

                Friend friendAvailable = new Friend();
                friendAvailable.setId(id);
                friendAvailable.setName(name);
                friendAvailable.setAddress(address);

                //find friend
                onFriendItemInteraction(friendAvailable);
                break;

            case WaryWidget.WIDGET_ACTION_ADD_FRIEND:
//                Toast.makeText(MainActivity.this, "ADD FRIEND "
//                        , Toast.LENGTH_SHORT).show();
                addFriend();

            default:
                Log.i(TAG, "wtf - "+action);
        }
    }

    private void addFriend(){
        AddFriendFragment addFriendFragment = AddFriendFragment.newInstance(AddFriendFragment.STATE_INIT);
        addFriendFragment.show(getSupportFragmentManager(), FriendsFragment.FRAGMENT_TAG_ADD_FRIENDS);
    }

    @Override
    public void onResume(){
        super.onResume();

        if(mNavigationView.getMenu().getItem(2).isChecked()){
            mNavigationView.getMenu().getItem(2).setChecked(false);
            selectMenuItem();
        }
//        else{
//        }
    }

    @Override
    public void onStop(){
        super.onStop();

//        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
//                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    private void selectMenuItem(){

        Fragment fragmentFriends = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_FRIENDS);
        Fragment fragmentStart = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_START_FRIENDS);
        if(fragmentFriends != null && fragmentFriends.isVisible()
                || fragmentStart != null && fragmentStart.isVisible()
                ) {
            mNavigationView.getMenu().getItem(0).setChecked(true);
            onNavigationItemSelected(mNavigationView.getMenu().getItem(0));
            return;
        }

        Fragment fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_HISTORY);
        if(fragment != null && fragment.isVisible()) {
            mNavigationView.getMenu().getItem(1).setChecked(true);
//            onNavigationItemSelected(mNavigationView.getMenu().getItem(0));
//            Log.i(TAG, "on resume :: first selected "
//                    + mNavigationView.getMenu().getItem(0).isChecked());
            return;
        }
        fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_HELP_FEEDBACK);
        if(fragment != null && fragment.isVisible()) {
            mNavigationView.getMenu().getItem(3).setChecked(true);
//            onNavigationItemSelected(mNavigationView.getMenu().getItem(0));
//            Log.i(TAG, "on resume :: first selected "
//                    + mNavigationView.getMenu().getItem(0).isChecked());
            return;
        }


    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }
//        else {
//            super.onBackPressed();
//        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        item.setChecked(true);
        Log.i(TAG, "SELECTING FRAGMENT ID :: "+id);
        if (id == R.id.nav_friends) {
            //Use history
            startDiscoveryFragment();
        } else if (id == R.id.nav_history) {

//            mAppBarLayout.setExpanded(false, true);
//            mAppBarLayout.setEnabled(false);
            //Use history
            HistoryFragment historyFragment = HistoryFragment.newInstance();
            // Add the fragment to the 'fragment_container' FrameLayout
            getSupportFragmentManager().beginTransaction()
                    .addToBackStack(null)
                    .replace(R.id.framelayout_maincontent
                    , historyFragment
                    , FRAGMENT_TAG_HISTORY
            ).commit();

//            ((CoordinatorLayout.LayoutParams)mFab.getLayoutParams()).setAnchorId(View.NO_ID);
//            mFab.setVisibility(View.GONE);

        } else if (id == R.id.nav_settings) {
//            mAppBarLayout.setExpanded(false, true);
//            mAppBarLayout.setEnabled(false);
            //Settings
            mDrawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, SettingsActivity.class));

        }
//        else if (id == R.id.nav_helpfeedback) {
//            //Help & Feedback
//
//            HelpFeedbackFragment helpFeedbackFragment = HelpFeedbackFragment.newInstance();
//            // Add the fragment to the 'fragment_container' FrameLayout
//            getSupportFragmentManager().beginTransaction().replace(
//                    R.id.framelayout_maincontent
//                    , helpFeedbackFragment
//                    , FRAGMENT_TAG_HELP_FEEDBACK).commit();
//
//            ((CoordinatorLayout.LayoutParams)mFab.getLayoutParams()).setAnchorId(View.NO_ID);
//            mFab.setVisibility(View.GONE);
//        }

        mDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /*********** Interfaces for Fragments ******************/
    @Override
    public void onFriendItemInteraction(Friend friendAvailable) {
        // CONNECT TO FRIEND
        Intent intent =new Intent(this, LocatorActivity.class);
        intent.putExtra(LocatorActivity.FRIEND_ADDRESS_KEY, friendAvailable.getAddress());
        intent.putExtra(LocatorActivity.FRIEND_NAME_KEY, friendAvailable.getName());
        intent.putExtra(LocatorActivity.FRIEND_ID_KEY, friendAvailable.getId());
        intent.putExtra(LocatorActivity.IS_FRIEND_KEY, true);

        startActivity(intent);

        /**
         * DO NOT CALL STOP DISCOVERY
         * IT LEADS TO ERROR WHILE CONNECTING TO PEER
         */
//        stopPeerDiscovery();
    }

    @Override
    public void onFriendItemDetailInteraction(View view, Friend friendAvailable) {
        Toast.makeText(MainActivity.this, "Friend Detail :: "+friendAvailable.getName()
                , Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onMenuItemInteraction(int id) {
        if(id == R.id.action_stop_discovery){
            toggleDiscovery(false);
        }
    }

    @Override
    public void onListHistoryItemInteraction() {
//        Snackbar.make(mFab, "onListHistoryItemInteraction", Snackbar.LENGTH_LONG)
//                .setAction("Action", null).show();
    }

    @Override
    public void onAddFriendHistoryInteraction() {
        mNavigationView.getMenu().getItem(0).setChecked(true);
        onStartDiscoveryInteraction();
        addFriend();
    }

    @Override
    public void onStartDiscoveryInteraction() {


        if(!isWifiDisplayNameSet){
            showChangeWifiNameDialog();
        }else if(isWifiEnabled()){
            toggleDiscovery(true);
        }
    }

    private boolean isWifiEnabled(){
        final WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if(!wifiManager.isWifiEnabled()) {
            Log.i(TAG, "WIFI IS NOT ENABLED ... ENABLE WIFI PLEASE MOFO");
            // 1. Instantiate an AlertDialog.Builder with its constructor
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            // 2. Chain together various setter methods to set the dialog characteristics
            builder.setMessage(R.string.dialog_message)
                    .setTitle(R.string.wifi_dialog_title);

            // Add the buttons
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User clicked OK button
//                    WifiManager wifiManager = (WifiManager) getApplicationContext()
//                            .getSystemService(Context.WIFI_SERVICE);
                    wifiManager.setWifiEnabled(true);
                    try {
                        Thread.sleep(0050);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }finally {
                        onStartDiscoveryInteraction();
                    }
                }
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog
                    Log.i(TAG, " WHY YOU LITTLE AARGGGRGARRRDDAF");
                }
            });

            // 3. Get the AlertDialog from create()
            builder.create().show();
            return false;
        }

        return true;
    }



    /**
     * If true == toggle discovery on, else off
     * @param startDiscovery
     */
    private void toggleDiscovery(boolean startDiscovery){

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext()).edit();

        if(startDiscovery){

            editor.putBoolean(getString(R.string.pref_key_discover), true);

            Fragment friendsFragment = FriendsFragment.newInstance();
            friendsFragment.setEnterTransition(new Fade());
            friendsFragment.setExitTransition(new Fade());

            Transition transition = TransitionInflater.from(this)
                    .inflateTransition(R.transition.changebounds_archmotion);
            friendsFragment.setSharedElementEnterTransition(transition);
            friendsFragment.setSharedElementReturnTransition(new ChangeBounds());

            Log.i(TAG, "starting FRIENDS with transition");

            View button = findViewById(R.id.button_start_fragment);
            if(button != null) {
                getSupportFragmentManager().beginTransaction()
                        .addSharedElement(button, getString(R.string.start_to_friends_transition))
                        .addToBackStack(null)
                        .replace(R.id.framelayout_maincontent
                                , friendsFragment
                                , FRAGMENT_TAG_FRIENDS)
                        .commit();
            }else{
                getSupportFragmentManager().beginTransaction()
                        .addToBackStack(null)
                        .replace(R.id.framelayout_maincontent
                                , friendsFragment
                                , FRAGMENT_TAG_FRIENDS)
                        .commit();
            }

            startPeerDiscovery();
            Log.i(TAG, "ALARM SET");
        }else{
            editor.putBoolean(getString(R.string.pref_key_discover), false);

            Fragment startFragment = StartFragment.newInstance();
            startFragment.setEnterTransition(new Fade());
            startFragment.setExitTransition(new Fade());

            Transition transition = TransitionInflater.from(this)
                    .inflateTransition(R.transition.changebounds_archmotion);
            startFragment.setSharedElementEnterTransition(transition);
            startFragment.setSharedElementReturnTransition(new ChangeBounds());
            // Add the fragment to the 'fragment_container' FrameLayout
            View fab = findViewById(R.id.friends_fragment_fab);
            if (fab != null) {

                Log.i(TAG, "starting START with transition");
                getSupportFragmentManager().beginTransaction()
                        .addSharedElement(fab, getString(R.string.start_to_friends_transition))
                        .addToBackStack(null)
                        .replace(R.id.framelayout_maincontent
                                , startFragment
                                , FRAGMENT_TAG_START_FRIENDS)
                        .commit();

            }else{
                getSupportFragmentManager().beginTransaction()
                        .addToBackStack(null)
                        .replace(R.id.framelayout_maincontent
                                , startFragment
                                , FRAGMENT_TAG_START_FRIENDS)
                        .commit();
            }

            stopPeerDiscovery();
            Log.i(TAG, "ALARM CANCELED");
        }

        editor.commit();
        updateMyWidgets(getApplicationContext());
    }

    /**
     * Starts peer discovery
     */
    private void startPeerDiscovery(){
        //discover peers intent
        Intent intent = new Intent(this, WDReceiver.class);
        intent.setAction(WDReceiver.ACTION_ALARM_DISCOVER_PEERS);
        // discover peers alarm
        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, 0, 1000 * 60, pendingIntent);
    }

    /**
     * Stops peer discovery
     */
    private void stopPeerDiscovery(){
        //discover peers intent
        Intent intent = new Intent(this, WDReceiver.class);
        intent.setAction(WDReceiver.ACTION_ALARM_DISCOVER_PEERS);
        // discover peers alarm
        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);

        Intent intent_stop_discovery = new Intent(this, WDReceiver.class);
        intent_stop_discovery.setAction(WDReceiver.ACTION_STOP_DISCOVER_PEERS);
        sendBroadcast(intent_stop_discovery);
    }

    /**
     * Method to get either Start Fragment or Friends Fragment
     * depending on the saved preference
     * @return Start Fragment or Friends Fragment
     */
    public void startDiscoveryFragment() {
        if(isWifiEnabled()) {
            boolean discoveryOn = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                    .getBoolean(getString(R.string.pref_key_discover), false);
            toggleDiscovery(discoveryOn);
        }else{
            toggleDiscovery(false);
        }
    }

    public void showChangeWifiNameDialog(){
        final AlertDialog alertDialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        // Get the layout inflater
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_set_wifi_name, null);

        final TextInputEditText mDisplayNameView = (TextInputEditText)
                dialogView.findViewById(R.id.wifi_display_name);

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder
                .setView(dialogView)
                .setTitle(R.string.change_wifi_display_name_dialog_title)
                .setMessage(R.string.change_wifi_display_name_dialog_message)
                // Add action buttons
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        String displayName = mDisplayNameView.getText().toString();
                        if(!TextUtils.isEmpty(displayName)) {
                            Toast.makeText(MainActivity.this, mDisplayNameView.getText().toString()
                                    , Toast.LENGTH_SHORT).show();

                            SharedPreferences.Editor editor = PreferenceManager.
                                    getDefaultSharedPreferences(getApplicationContext()).edit();
                            editor.putString(getString(R.string.pref_key_display_name), displayName);
                            editor.commit();
                            isWifiDisplayNameSet = true;
                        }else{
                            Toast.makeText(MainActivity.this,
                                    getString(R.string.re_enter_name), Toast.LENGTH_SHORT).show();
                            isWifiDisplayNameSet = false;
                        }
                        onStartDiscoveryInteraction();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
//                        alertDialog.cancel();
                        Toast.makeText(MainActivity.this,
                                getString(R.string.name_required_to_discover), Toast.LENGTH_SHORT).show();
                    }
                });

        alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(getString(R.string.pref_key_display_name))){
            String wifiName = sharedPreferences.getString(
                    getString(R.string.pref_key_display_name), "");
            isWifiDisplayNameSet = !TextUtils.isEmpty(wifiName);
            if(isWifiDisplayNameSet) {
                mDisplayNameView.setText(wifiName);
            }
        }
    }

}
