package dlujanapps.mx.wary.fragments;


import android.content.ContentValues;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dlujanapps.mx.wary.R;
import dlujanapps.mx.wary.data.DBContract;
import dlujanapps.mx.wary.objects.Friend;
import dlujanapps.mx.wary.objects.Utils;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FriendDetailFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FriendDetailFragment extends DialogFragment {

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_FRIEND_NAME = "friend_name";
    private static final String ARG_FRIEND_ADDRESS = "param2";
    private final String TAG = getClass().getSimpleName();

    private String mFriendName;
    private String mOldFriendName;
    private String mFriendAddress;


    public FriendDetailFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param friendName Parameter 1.
     * @param friendAddress Parameter 2.
     * @return A new instance of fragment FriendDetailFragment.
     */
    public static FriendDetailFragment newInstance(String friendName, String friendAddress) {
        FriendDetailFragment fragment = new FriendDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FRIEND_NAME, friendName);
        args.putString(ARG_FRIEND_ADDRESS, friendAddress);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mFriendName = getArguments().getString(ARG_FRIEND_NAME);
            mOldFriendName = mFriendName;
            mFriendAddress = getArguments().getString(ARG_FRIEND_ADDRESS);
        }
    }

    @Bind(R.id.friend_detail_name)
    TextInputEditText mNameView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_friend_detail, container, false);
        ButterKnife.bind(this, view);
        mNameView.setText(mFriendName);
        getDialog().setTitle(R.string.detail_dialog_title);
        return view;
    }

    @OnClick(R.id.friend_detail_ok)
    public void onOkClick(){
        //TODO check
        String newName = mNameView.getText().toString();

        if(!mFriendName.equals(newName)){

            Log.i(TAG, "changing friend name to :: "+newName);
            mOldFriendName = mFriendName;
            mFriendName = newName;

            Friend friend = new Friend();
            friend.setAddress(mFriendAddress);

            ContentValues contentValues = new ContentValues();
            contentValues.put(DBContract.FriendEntry.COLUMN_NAME_NAME, mFriendName);

            if(Utils.DBUtils.updateFriend(getContext(), friend, contentValues)){

                Snackbar.make(getActivity().findViewById(R.id.coordinatorLayout),
                        String.format(getString(R.string.friend_name_changed)
                                , mOldFriendName, mFriendName), Snackbar.LENGTH_LONG)
                        .setAction(R.string.undo, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mFriendName = mOldFriendName;
                                Friend friend = new Friend();
                                friend.setAddress(mFriendAddress);

                                ContentValues contentValues = new ContentValues();
                                contentValues.put(DBContract.FriendEntry.COLUMN_NAME_NAME
                                        , mOldFriendName);

                                Utils.DBUtils.updateFriend(getContext(), friend, contentValues);
                                mNameView.setText(mFriendName);
                            }
                        })
                        .show();
                this.dismiss();
            }else{
                mFriendName = mOldFriendName;
                Toast.makeText(getContext(), R.string.unable_update_friend
                        , Toast.LENGTH_SHORT).show();
            }
        }else {
            Log.i(TAG, "friend name didn't change");

            this.dismiss();
        }
    }

    @OnClick(R.id.friend_detail_cancel)
    public void onCancelClick(){
        this.dismiss();
    }


}
