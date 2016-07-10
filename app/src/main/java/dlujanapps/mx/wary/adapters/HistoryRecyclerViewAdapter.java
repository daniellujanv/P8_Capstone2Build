package dlujanapps.mx.wary.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import dlujanapps.mx.wary.R;
import dlujanapps.mx.wary.fragments.HistoryFragment.OnListHistoryInteractionListener;
import dlujanapps.mx.wary.objects.HistoryItem;

/**
 */
public class HistoryRecyclerViewAdapter extends RecyclerView.Adapter<HistoryRecyclerViewAdapter.ViewHolder> {

    private final OnListHistoryInteractionListener mListener;
    private ArrayList<HistoryItem> mHistoryItems = new ArrayList<>();
    private Context mContext;
    private View mEmptyView;

    public HistoryRecyclerViewAdapter(
            Context context
            , OnListHistoryInteractionListener listener
            , View emptyView
    ) {
        mListener = listener;
        mContext = context;
        mEmptyView = emptyView;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.fragment_history_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListHistoryItemInteraction();
                }
            }
        });

        holder.mContentView.setText(
                mHistoryItems.get(position).getPrettyString());
        holder.mDateView.setText(
                mHistoryItems.get(position).getPrettyDate());
    }

    @Override
    public int getItemCount() {

        Log.i("itemCount", "::"+mHistoryItems.size());
        return mHistoryItems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mDateView;
        public final TextView mContentView;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mDateView = (TextView) view.findViewById(R.id.date);
            mContentView = (TextView) view.findViewById(R.id.content);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }

    public void changeDataSet(ArrayList<HistoryItem> historyItems){
        mHistoryItems = historyItems;
        if(mHistoryItems.size() > 0){
            mEmptyView.setVisibility(View.GONE);
        }else{
            mEmptyView.setVisibility(View.VISIBLE);
        }
        notifyDataSetChanged();
    }

}
