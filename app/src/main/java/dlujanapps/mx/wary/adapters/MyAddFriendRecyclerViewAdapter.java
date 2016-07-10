package dlujanapps.mx.wary.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import dlujanapps.mx.wary.R;
import dlujanapps.mx.wary.fragments.AddFriendFragment;
import dlujanapps.mx.wary.objects.PeerAvailable;

//import dlujanapps.mx.wary.fragments.AddFriendFragment.OnAddFriendInteractionListener;

/**
 */
public class MyAddFriendRecyclerViewAdapter extends RecyclerView.Adapter<MyAddFriendRecyclerViewAdapter.ViewHolder> {

//    private final AddFriendFragment.OnAddFriendInteractionListener mListener;
    private final Context mContext;

    private ArrayList<PeerAvailable> mPeersAvailable;
    private AddFriendFragment mAddFriendFragment;

    public MyAddFriendRecyclerViewAdapter(
            Context context
            , AddFriendFragment addFriendFragment
//            ,   AddFriendFragment.OnAddFriendInteractionListener listener
    ) {
        mContext = context;
//        mListener = listener;
        mPeersAvailable = new ArrayList<>();
        mAddFriendFragment = addFriendFragment;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_addfriend_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.mItem = mPeersAvailable.get(position);
        holder.mNameView.setText(mPeersAvailable.get(position).getName());

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (mListener != null) {
//                    // Notify the active callbacks interface (the activity, if the
//                    // fragment is attached to one) that an item has been selected.
//                    mListener.onAddFriendItemInteraction(holder.mItem);
                mAddFriendFragment.changeScene(
                        AddFriendFragment.STATE_ADDING, mPeersAvailable.get(position));
//                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mPeersAvailable.size();
    }

    public void onDataSetChanged(Cursor cursor){
        mPeersAvailable = new ArrayList<>();
        if(cursor != null && cursor.moveToFirst()) {
            do{
                mPeersAvailable.add(new PeerAvailable(cursor));
            }while(cursor.moveToNext());
        }
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mNameView;
        public PeerAvailable mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mNameView = (TextView) view.findViewById(R.id.available_peer_name);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mNameView.getText() + "'";
        }
    }

}
