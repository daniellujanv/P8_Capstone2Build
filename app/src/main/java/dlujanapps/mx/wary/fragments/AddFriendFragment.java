package dlujanapps.mx.wary.fragments;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.Scene;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import dlujanapps.mx.wary.R;
import dlujanapps.mx.wary.adapters.MyAddFriendRecyclerViewAdapter;
import dlujanapps.mx.wary.data.DBContract;
import dlujanapps.mx.wary.finder.Location.LocatorActivity;
import dlujanapps.mx.wary.finder.WDReceiver;
import dlujanapps.mx.wary.objects.Friend;
import dlujanapps.mx.wary.objects.PeerAvailable;
import dlujanapps.mx.wary.objects.Utils;

/**
 * A fragment representing a list of Items.
 */
public class AddFriendFragment extends AppCompatDialogFragment
        implements LoaderManager.LoaderCallbacks<Cursor>
        , SwipeRefreshLayout.OnRefreshListener{

    public static final String ACTION_HANDSHAKE_FAILED = "dlujanapps.mx.wary.handshake_failed";
    public static final String ACTION_HANDSHAKE_SUCCESS = "dlujanapps.mx.wary.handshake_success";

    private static final String ARG_STATE = "state";
    private String TAG = getClass().getSimpleName();
//    private OnAddFriendInteractionListener mListener;
    private MyAddFriendRecyclerViewAdapter mAddFriendAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private static final int PEERS_LOADER = 0;

    Friend mFutureFriend;

    private int mCurrentState = 0;

    public static int STATE_INIT = 0;
    public static int STATE_ADDING = 1;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if(mSwipeRefreshLayout != null){
            mSwipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefreshLayout.setRefreshing(true);
                }
            });
        }else{
            Log.i(TAG, "SWIPEREFRESHLAYOUTISNULL");
        }
        return new CursorLoader(
                getActivity()
                , DBContract.AvailablePeerEntry.CONTENT_URI
                , PeerAvailable.PEERS_PROJECTION
                , null, null, null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAddFriendAdapter.onDataSetChanged(data);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAddFriendAdapter.onDataSetChanged(null);
    }


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public AddFriendFragment() {
    }

    public static AddFriendFragment newInstance(int state) {
        AddFriendFragment fragment = new AddFriendFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_STATE, state);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, getTheme());
        mAddFriendAdapter =
                new MyAddFriendRecyclerViewAdapter(getContext(), this);

        if(getArguments() != null){
            mCurrentState = getArguments().getInt(ARG_STATE, STATE_INIT);
        }
    }

    @Override
    public void onStop(){
        super.onStop();

        Intent intent = new Intent(getContext(), WDReceiver.class);
        intent.setAction(WDReceiver.ACTION_DISCONNECT);
        getContext().sendBroadcast(intent);

    }

    @Bind(R.id.scene_root) ViewGroup mRootScene;
    @Bind(R.id.fragment_addfriend_title) TextView mTitleView;
    private Scene mInitScene;
    private Scene mAddingScene;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.fragment_addfriend_list, container, false);
        ButterKnife.bind(this, view);

        Context context = view.getContext();


        mInitScene = Scene.getSceneForLayout(mRootScene, R.layout.scene_addfriends_init
                , context);
        mInitScene.setEnterAction(new Runnable() {
            @Override
            public void run() {
               initSceneEnterAction();
            }
        });

        mAddingScene = Scene.getSceneForLayout(mRootScene, R.layout.scene_addfriend_adding
                , context);
        mAddingScene.setEnterAction(new Runnable() {
            @Override
            public void run() {
                addingSceneEnterAction();
            }
        });
//        if(mCurrentState == STATE_INIT) {
//            view = inflater.inflate(R.layout.fragment_addfriend_list, container, false);

        TransitionManager.go(mInitScene);

//        }else if(mCurrentState == STATE_ADDING){
//            view = inflater.inflate(R.layout.fragment_addfriend_finding, container, false);
//
//        }
        return view;
    }

    private void initSceneEnterAction(){
        mTitleView.setText(R.string.title_add_friends);
        mFutureFriend = null;
        LocalBroadcastManager.getInstance(getContext()).
                unregisterReceiver(mBroadcastReceiver);

        RecyclerView recyclerView = (RecyclerView) mInitScene.getSceneRoot().findViewById(R.id.fragment_addfriend_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(mAddFriendAdapter);
        mSwipeRefreshLayout = (SwipeRefreshLayout) mInitScene.getSceneRoot().findViewById(R.id.refreshlayout_peers);
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
        this.onRefresh();

        getLoaderManager().restartLoader(PEERS_LOADER, null, this);
    }

    private void addingSceneEnterAction(){

        mTitleView.setText(R.string.title_adding_friend);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_HANDSHAKE_FAILED);
        intentFilter.addAction(ACTION_HANDSHAKE_SUCCESS);

        LocalBroadcastManager.getInstance(getContext()).
                registerReceiver(mBroadcastReceiver, intentFilter);


        Button button = (Button) mAddingScene.getSceneRoot().
                findViewById(R.id.addfriend_button);
        if(button != null){
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG, "ontransitionend click");
                    String friendsName = ((TextInputEditText) mAddingScene.getSceneRoot().
                            findViewById(R.id.addfriend_friendname)).getText().toString();
                    if(updateFriendsName(friendsName)) {
                        AddFriendFragment.this.dismiss();
                    }else{
                        Toast.makeText(getContext()
                                , "Friend not updated correctly"
                                , Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        ((TextView)mAddingScene.getSceneRoot()
                .findViewById(R.id.addfriend_friendname))
                .setText(mFutureFriend.getName());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
//        if (context instanceof OnAddFriendInteractionListener) {
//            mListener = (OnAddFriendInteractionListener) context;
//        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnListFragmentInteractionListener");
//        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
//        mListener = null;
        deleteAvailablePeers();

        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mBroadcastReceiver);
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
        getLoaderManager().initLoader(PEERS_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onRefresh() {
        mSwipeRefreshLayout.setRefreshing(true);
        Log.i(TAG, "sending alarm log intent broadcast");
        Intent intent = new Intent(getContext(), WDReceiver.class);
        intent.setAction(WDReceiver.ACTION_ALARM_DISCOVER_PEERS);
        getContext().sendBroadcast(intent);
    }

    public void deleteAvailablePeers(){
        Uri content_uri_available_friends = Uri.parse("content://dlujanapps.mx.wary.provider")
                .buildUpon().appendPath("available_peers").build();
        int deleted = getContext().getContentResolver()
                .delete(content_uri_available_friends, null, null);
        Log.i(TAG, "deleted peers :: " + deleted);
    }

//    @Override
//    public void onBackPressed() {
//
//    }

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
//    public interface OnAddFriendInteractionListener {
//        void onAddFriendItemInteraction(PeerAvailable item);
//    }

    public boolean changeScene(int state, PeerAvailable peerAvailable){

        if(state == STATE_INIT){
            TransitionManager.go(mInitScene);
            return true;
        }else if(state == STATE_ADDING){
            mFutureFriend = new Friend(peerAvailable, true, DBContract.FriendEntry.ROLE_ADDED);

            if(addFutureFriend()) {

                TransitionManager.go(mAddingScene);
                Intent intent = new Intent(getContext(), WDReceiver.class);
                intent.setAction(WDReceiver.ACTION_CONNECT);
                intent.putExtra(LocatorActivity.FRIEND_ADDRESS_KEY, peerAvailable.getWifiAddress());
                intent.putExtra(LocatorActivity.IS_FRIEND_KEY, false);
                getContext().sendBroadcast(intent);
                Log.i(TAG, "dismissing fragment ");
                return true;
            }
//            }else{
//                Toast.makeText(getContext(),
//                        "Problems with the database...\n Please try again!", Toast.LENGTH_LONG).show();
//            }
        }
        return false;
    }

    private boolean addFutureFriend(){
        return Utils.DBUtils.addFutureFriend(getContext(), mFutureFriend);
    }

    private boolean updateFriendsName(String newName){

        mFutureFriend.setName(newName);

        ContentValues contentValues = new ContentValues();
        contentValues.put(DBContract.FriendEntry.COLUMN_NAME_NAME, mFutureFriend.getName());

        return Utils.DBUtils.updateFriend(getContext(), mFutureFriend, contentValues);

    }

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            switch (action){
                case ACTION_HANDSHAKE_FAILED: {
                    Log.i(TAG, ACTION_HANDSHAKE_FAILED);
                    Toast.makeText(getContext(), "Error adding friend \n Please try again!"
                            , Toast.LENGTH_SHORT).show();
                    changeScene(STATE_INIT, null);
                    break;
                }
                case ACTION_HANDSHAKE_SUCCESS: {
                    Log.i(TAG, ACTION_HANDSHAKE_SUCCESS);
                    mAddingScene.getSceneRoot().findViewById(R.id.addfriend_progressbar_wrapper)
                            .setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Friend Added!", Toast.LENGTH_SHORT).show();
                    break;
                }
                default:
                    break;
            }
        }
    };

//    public void onAddFriendItemInteraction(PeerAvailable peerAvailable) {
//        //TODO change scene
////        FragmentManager fm = getSupportFragmentManager();
////
////        AddFriendFragment addFriendFragment =
////                (AddFriendFragment) fm.findFragmentByTag(FRAGMENT_TAG_ADD_FRIENDS);
////
////        if(addFriendFragment != null) {
////
////            if(addFriendFragment.changeScene(AddFriendFragment.STATE_ADDING, peerAvailable)) {
////                Intent intent = new Intent(this, WDReceiver.class);
////                intent.setAction(WDReceiver.ACTION_CONNECT);
////                intent.putExtra(LocatorActivity.FRIEND_ADDRESS_KEY, peerAvailable.getWifiAddress());
////                intent.putExtra(LocatorActivity.IS_FRIEND_KEY, false);
////                sendBroadcast(intent);
////                Log.i(TAG, "dismissing fragment ");
////            }
////        }
//    }

}
