package dlujanapps.mx.wary.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dlujanapps.mx.wary.R;
import dlujanapps.mx.wary.adapters.FriendsRecyclerViewAdapter;
import dlujanapps.mx.wary.finder.WDReceiver;
import dlujanapps.mx.wary.objects.Friend;

import static dlujanapps.mx.wary.data.DBContract.FriendEntry;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnFriendsInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FriendsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FriendsFragment extends Fragment implements
        SearchView.OnQueryTextListener
        , SwipeRefreshLayout.OnRefreshListener
        , LoaderManager.LoaderCallbacks<Cursor>
        , SharedPreferences.OnSharedPreferenceChangeListener {

    private String TAG  = getClass().getSimpleName();

    private static final int FRIENDS_LOADER = 0;
    private OnFriendsInteractionListener mFriendsFragmentListener;
    private FriendsRecyclerViewAdapter mFriendsAdapter;
    private boolean mIsWifiP2pEnabled = false;

    private AlertDialog mDialog;
    public static final String DISCOVER_PEERS_FAILED_ACTION = "discover_peers_failed";
    public static String FRAGMENT_TAG_ADD_FRIENDS = "add_friends";

    /*********** LOADER ******************/
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.i(TAG, "onCreateLoader .. ");
        if(mSwipeRefreshLayout != null){
            mSwipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "posted  mSwipeRefreshLayout..true ");
                    mSwipeRefreshLayout.setRefreshing(true);
                }
            });
        }

        String selection = FriendEntry.COLUMN_NAME_IS_NEW+" = ?";
        String[] selectionArgs = new String[]{Integer.toString(0)};

        String orderBy = FriendEntry.COLUMN_NAME_STATUS+" DESC";

        return new CursorLoader(
                getActivity()
                , FriendEntry.CONTENT_URI
                , Friend.FULL_PROJECTION
                , selection, selectionArgs, orderBy
        );
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        Log.i(TAG, "onLoadFinished");
        mFriendsAdapter.setFriendsAvailable(data);
        mSwipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "posted  mSwipeRefreshLayout..false ");
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
        mFriendsAdapter.setFriendsAvailable(null);
    }

    /*********** CLASS ******************/
    public FriendsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment FriendsFragment.
     */
    public static FriendsFragment newInstance() {
        FriendsFragment fragment = new FriendsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        buildAlertDialog();
    }

    @Bind(R.id.recycler_friends)
    RecyclerView mRecyclerView;
    @Bind(R.id.refreshlayout_friends)
    SwipeRefreshLayout mSwipeRefreshLayout;
    @Bind(R.id.friends_fragment_fab)
    FloatingActionButton mFab;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view;
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_friends, container, false);

        ButterKnife.bind(this, view);
        setHasOptionsMenu(true);

        View emptyView = view.findViewById(R.id.friends_emptyView);
        emptyView.findViewById(R.id.empty_view_friends_add_friends_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        addFriend();
                    }
                });

        mFriendsAdapter = new FriendsRecyclerViewAdapter(
                getChildFragmentManager(), mFriendsFragmentListener, getContext(), emptyView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(mFriendsAdapter);

        mSwipeRefreshLayout.setColorSchemeResources(
                R.color.colorAccent
                , R.color.colorPrimary
                , R.color.colorPrimaryExtraDark
        );

        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshLayout.setRefreshing(true);
            }
        });
        return view;
    }

    @Override
    public void onStart(){
        super.onStart();
        checkWifiState();
    }

    @Override
    public void onResume(){
        super.onResume();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DISCOVER_PEERS_FAILED_ACTION);
        LocalBroadcastManager.getInstance(getContext())
                .registerReceiver(mBroadcastReceiver, intentFilter);
        PreferenceManager.getDefaultSharedPreferences(
                getContext()).registerOnSharedPreferenceChangeListener(this);


        getLoaderManager().initLoader(FRIENDS_LOADER, null, this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater)
    {
        menuInflater.inflate(R.menu.menu_fragment_friends, menu);

        // Associate searchable configuration with the SearchView
//        SearchManager searchManager =
//                (SearchManager) getContext().getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.action_filter_list).getActionView();
        searchView.setOnQueryTextListener(this);
//        searchView.setSearchableInfo(
//                searchManager.getSearchableInfo(getActivity().getComponentName()));

        super.onCreateOptionsMenu(menu, menuInflater);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_stop_discovery) {

            Intent intent = new Intent(getActivity().getApplicationContext(), WDReceiver.class);
            intent.setAction(WDReceiver.ACTION_DISCONNECT);
            getActivity().sendBroadcast(intent);

            mFriendsFragmentListener.onMenuItemInteraction(id);
        }
//        if (id == R.id.action_stop_search) {
//            mFriendsFragmentListener.onFriendItemInteraction(id);
//            return true;
//        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnFriendsInteractionListener) {
//            Log.i(TAG, "Attaching Friends Listener ************");
            mFriendsFragmentListener = (OnFriendsInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onStop(){
        super.onStop();
        Log.i(TAG, "onStop");
        mSwipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "posted  mSwipeRefreshLayout..false ");
                mSwipeRefreshLayout.setRefreshing(false);
                mSwipeRefreshLayout.destroyDrawingCache();
                mSwipeRefreshLayout.clearAnimation();
            }
        });
        LocalBroadcastManager.getInstance(getContext())
                .unregisterReceiver(mBroadcastReceiver);

        PreferenceManager.getDefaultSharedPreferences(
                getContext()).unregisterOnSharedPreferenceChangeListener(this);

        getLoaderManager().destroyLoader(FRIENDS_LOADER);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFriendsFragmentListener = null;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // We hold for transition here just in-case the activity
        // needs to be re-created. In a standard return transition,
        // this doesn't actually make a difference.
//        if ( mHoldForTransition ) {
//            getActivity().supportPostponeEnterTransition();
//        }

        Log.i(TAG, "init loader");
        getLoaderManager().initLoader(FRIENDS_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    /*********** LISTENERS ******************/
    @Override
    public boolean onQueryTextSubmit(String query) {
        Log.i(TAG, query);
        mFriendsAdapter.getFilter().filter(query);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        Log.i(TAG, newText);
        mFriendsAdapter.getFilter().filter(newText);
        return false;
    }

    @Override
    public void onRefresh() {
        Log.i(TAG, "onRefresh::sending alarm log intent broadcast");
        Intent intent = new Intent(getContext(), WDReceiver.class);
        intent.setAction(WDReceiver.ACTION_ALARM_DISCOVER_PEERS);
        getContext().sendBroadcast(intent);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.i(TAG, "changed preference ..... "+key);
        if (key.equals(getResources().getString(R.string.pref_key_wifip2p_enabled))) {
            mIsWifiP2pEnabled = sharedPreferences.getBoolean(key, false);
            checkWifiState();
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFriendsInteractionListener {
        /**
         * Interaction with an Item from the list of friends
         * @param friendAvailable
         */
        void onFriendItemInteraction(Friend friendAvailable);

        /**
         * Interaction with Item to see Detail
         */
        void onFriendItemDetailInteraction(View v, Friend friendAvailable);

        /**
         * Menu Item interaction
         * @param id --> menu item id
         */
        void onMenuItemInteraction(int id);

    }

    @OnClick(R.id.friends_fragment_fab)
    public void onFabClicked(){
        addFriend();
    }

    private void addFriend(){
        AddFriendFragment addFriendFragment = AddFriendFragment.newInstance(AddFriendFragment.STATE_INIT);
        addFriendFragment.show(getFragmentManager(), FRAGMENT_TAG_ADD_FRIENDS);
    }

    private void buildAlertDialog(){
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setMessage(R.string.dialog_message)
                .setTitle(R.string.wifi_dialog_title);

        // Add the buttons
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                Context context = getContext();
                if(context != null) {
                    WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                    wifiManager.setWifiEnabled(true);
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
        mDialog = builder.create();
    }

    private void checkWifiState(){
        if(!mIsWifiP2pEnabled){
            WifiManager wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
            if(!wifiManager.isWifiEnabled()){
                Log.i(TAG, "WIFI IS NOT ENABLED ... ENABLE WIFI PLEASE MOFO");
                showAlertDialog();
            }
        }
    }

    private void showAlertDialog(){
        if(!mDialog.isShowing()){
            mDialog.show();
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action){
                case DISCOVER_PEERS_FAILED_ACTION :
                    mSwipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(getContext(), "Error discoverying peers", Toast.LENGTH_SHORT).show();
                    break;
                default :
                    break;
            }
        }
    };
}


