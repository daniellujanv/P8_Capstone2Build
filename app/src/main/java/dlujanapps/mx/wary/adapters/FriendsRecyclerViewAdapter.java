package dlujanapps.mx.wary.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import dlujanapps.mx.wary.R;
import dlujanapps.mx.wary.finder.WDReceiver;
import dlujanapps.mx.wary.fragments.FriendDetailFragment;
import dlujanapps.mx.wary.fragments.FriendsFragment;
import dlujanapps.mx.wary.objects.Friend;
import dlujanapps.mx.wary.objects.Utils;

import static dlujanapps.mx.wary.data.DBContract.FriendEntry;


/**
 *
 * Created by daniellujanvillarreal on 12/18/15.
 */
public class FriendsRecyclerViewAdapter
        extends
        RecyclerView.Adapter<FriendsRecyclerViewAdapter.ViewHolder>
        implements Filterable{

    private String TAG = getClass().getSimpleName();

    private ArrayList<Friend> mFriendsAvailable;
    private ArrayList<Friend> mFriendsAvailableFilterable;
    private View mEmptyView;
    private Context mContext;
    private String friend_detail_tag = "friend_detail";
    private FragmentManager mFragmentManager;

    public class ViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.friends_item_wrapper) LinearLayout mFriendsItemWrapper;
        @Bind(R.id.friends_item_name) TextView mFriendNameView;
        @Bind(R.id.friends_item_more) ImageView mFriendMoreView;
//        @Bind(R.id.friends_item_circle) ImageView mCircleView;
        @Bind(R.id.friends_item_status) TextView mFriendStatus;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

        }
    }

    private FriendsFragment.OnFriendsInteractionListener mFriendsFragmentListener;

    public FriendsRecyclerViewAdapter(
            FragmentManager fragmentManager
            , FriendsFragment.OnFriendsInteractionListener friendsInteractionListener
            , Context context, View emptyView){
        mFragmentManager = fragmentManager;
        mFriendsFragmentListener = friendsInteractionListener;
        mFriendsAvailable = new ArrayList<>();
        mFriendsAvailableFilterable = new ArrayList<>();
        mEmptyView = emptyView;
        mContext = context;
    }

    @Override
    public FriendsRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.fragment_friends_item, parent, false);
        FriendsRecyclerViewAdapter.ViewHolder viewHolder =
                new FriendsRecyclerViewAdapter.ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final int pos = position;

        holder.mFriendNameView.setText( mFriendsAvailableFilterable.get(pos).getName());

        int status = mFriendsAvailableFilterable.get(pos).getStatus();

        switch (status){
            case FriendEntry.STATUS_CONNECTED:
                holder.mFriendNameView.setOnClickListener(null);
                holder.mFriendNameView.setTextColor(ContextCompat.getColor(mContext, R.color.colorAccent));
//                holder.mCircleView.setImageBitmap(
//                        BitmapFactory.decodeResource(mContext.getResources(), R.drawable.connected_circle));
                holder.mFriendStatus.setText(mContext.getString(R.string.friend_connected));
                holder.mFriendStatus.setTextColor(ContextCompat
                        .getColor(mContext, R.color.colorAccent));

                holder.mFriendMoreView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PopupMenu popup = new PopupMenu(
                                mContext, holder.mFriendMoreView);
                        MenuItemClickListener listener = new MenuItemClickListener();
                        listener.setFriend(mFriendsAvailableFilterable.get(pos));
                        popup.setOnMenuItemClickListener(listener);
                        MenuInflater inflater = popup.getMenuInflater();
                        inflater.inflate(R.menu.menu_fragment_friends_item_connected, popup.getMenu());
                        popup.show();
                    }
                });

                break;
            case FriendEntry.STATUS_AVAILABLE:
                holder.mFriendNameView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mFriendsFragmentListener.onFriendItemInteraction(mFriendsAvailableFilterable.get(pos));
                    }
                });
                holder.mFriendNameView.setTextColor(ContextCompat.getColor(mContext, R.color.colorPrimary));
//                holder.mCircleView.setImageBitmap(
//                        BitmapFactory.decodeResource(mContext.getResources(), R.drawable.available_circle));
                holder.mFriendStatus.setText(mContext.getString(R.string.friend_online));
                holder.mFriendStatus.setTextColor(ContextCompat
                        .getColor(mContext, R.color.colorPrimary));

                holder.mFriendMoreView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PopupMenu popup = new PopupMenu(
                                mContext, holder.mFriendMoreView);

                        MenuItemClickListener listener = new MenuItemClickListener();
                        listener.setFriend(mFriendsAvailableFilterable.get(pos));
                        popup.setOnMenuItemClickListener(listener);

                        MenuInflater inflater = popup.getMenuInflater();
                        inflater.inflate(R.menu.menu_fragment_friends_item, popup.getMenu());
                        popup.show();
                    }
                });
                break;
            case FriendEntry.STATUS_AWAY:
                holder.mFriendNameView.setTextColor(ContextCompat
                        .getColor(mContext, R.color.greyText));
                holder.mFriendNameView.setOnClickListener(null);
//                holder.mCircleView.setImageBitmap(
//                        BitmapFactory.decodeResource(mContext.getResources()
//                                , R.drawable.offline_circle));
                holder.mFriendStatus.setText(mContext.getString(R.string.friend_offline));
                holder.mFriendStatus.setTextColor(ContextCompat
                        .getColor(mContext, R.color.greyText));

                holder.mFriendMoreView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PopupMenu popup = new PopupMenu(
                                mContext, holder.mFriendMoreView);

                        MenuItemClickListener listener = new MenuItemClickListener();
                        listener.setFriend(mFriendsAvailableFilterable.get(pos));
                        popup.setOnMenuItemClickListener(listener);

                        MenuInflater inflater = popup.getMenuInflater();
                        inflater.inflate(R.menu.menu_fragment_friends_item, popup.getMenu());
                        popup.show();
                    }
                });
                break;
            default:
                Log.e(TAG, "onBindViewHolder status not found");
        }
    }

    @Override
    public int getItemCount() {
//        Log.i(TAG, "itemCount :: "+mFriendsAvailable.size());
        return mFriendsAvailableFilterable.size();
    }

    @Override
    public Filter getFilter() {
        Log.i(TAG, "creating filter");
        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();        // Holds the results of a filtering operation in values
                List<Friend> FilteredArrList = new ArrayList<Friend>();

                mFriendsAvailableFilterable = mFriendsAvailable;

                /********
                 *
                 *  If constraint(CharSequence that is received) is null returns the mOriginalValues(Original) values
                 *  else does the Filtering and returns FilteredArrList(Filtered)
                 *
                 ********/
                if (constraint == null || constraint.length() == 0) {

                    // set the Original result to return
                    results.count = mFriendsAvailable.size();
                    results.values = mFriendsAvailable;
                } else {
                    constraint = constraint.toString().toLowerCase();
                    for (int i = 0; i < mFriendsAvailableFilterable.size(); i++) {
                        String data = mFriendsAvailableFilterable.get(i).getName();
                        if (data.toLowerCase().startsWith(constraint.toString())) {
                            FilteredArrList.add(mFriendsAvailableFilterable.get(i));
                        }
                    }
                    // set the Filtered result to return
                    results.count = FilteredArrList.size();
                    results.values = FilteredArrList;
                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                // has the filtered values
                mFriendsAvailableFilterable = (ArrayList<Friend>) results.values;
                notifyDataSetChanged();
            }
        };

        return filter;
    }

    public void setFriendsAvailable(Cursor cursor){
//        Log.i(TAG, "setFriendsAvailable");
        mFriendsAvailable = new ArrayList<>();
        if(cursor != null && cursor.moveToFirst()) {
            do{
                mFriendsAvailable.add(new Friend(cursor));
            }while(cursor.moveToNext());

            mFriendsAvailableFilterable = mFriendsAvailable;
            mEmptyView.setVisibility(View.GONE);
        }else{
            mFriendsAvailable = new ArrayList<>();
            mFriendsAvailableFilterable = new ArrayList<>();
            mEmptyView.setVisibility(View.VISIBLE);
        }
        notifyDataSetChanged();
    }

    class MenuItemClickListener implements PopupMenu.OnMenuItemClickListener {
        Friend mFriend;

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()){

                case R.id.menu_item_disconnect:
                    Intent intent = new Intent(mContext, WDReceiver.class);
                    intent.setAction(WDReceiver.ACTION_DISCONNECT);
                    mContext.sendBroadcast(intent);
//                    Toast.makeText(mContext, "item::"+item.getTitle(), Toast.LENGTH_SHORT).show();
                    break;

                case R.id.menu_item_detail:
                    FriendDetailFragment.newInstance(mFriend.getName(), mFriend.getAddress())
                            .show(mFragmentManager, friend_detail_tag);
                    break;

                case R.id.menu_item_delete:
                    showDeleteDialog(mFriend);
                    break;
            }
            return false;
        }

        public void setFriend(Friend friend){
            mFriend = friend;
        }
    }

    public void showDeleteDialog(final Friend friend){
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

        // 2. Chain together various setter methods to set the dialog characteristics
        String message = String.format(
                mContext.getResources().getString(R.string.dialog_delete_friend_message)
                , friend.getName());
        builder.setMessage(message);
        builder.setTitle(R.string.dialog_delete_friend_title);

        // Add the buttons
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                if(Utils.DBUtils.deleteFriend(mContext, friend)){
                    Snackbar.make((View)mEmptyView.getParent(), R.string.friend_deleted, Snackbar.LENGTH_LONG)
                            .setAction(R.string.undo,
                                    new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            Utils.DBUtils.addFutureFriend(mContext, friend);
                                        }
                                    }).show();
                }else{
                    Toast.makeText(mContext, R.string.error_deleting_friend
                            , Toast.LENGTH_SHORT).show();
                }

            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
//               dialog.dismiss();
            }
        });

        // 3. Get the AlertDialog from create()
        dialog = builder.create();
        dialog.show();

    }
}
