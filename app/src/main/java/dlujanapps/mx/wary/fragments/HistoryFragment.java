package dlujanapps.mx.wary.fragments;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import dlujanapps.mx.wary.R;
import dlujanapps.mx.wary.adapters.HistoryRecyclerViewAdapter;
import dlujanapps.mx.wary.data.DBContract;
import dlujanapps.mx.wary.objects.HistoryItem;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListHistoryInteractionListener}
 * interface.
 */
public class HistoryFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>{

    private static final int HISTORY_LOADER = 1;
    private String TAG = getClass().getSimpleName();

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.i(TAG, "onCreateLoader");
        String orderBy = DBContract.HistoryEntry.COLUMN_NAME_TIMESTAMP+" DESC";
        return new CursorLoader(
                getContext()
                , DBContract.HistoryEntry.CONTENT_URI
                , HistoryItem.PRETTY_PROJECTION
                , null, null, orderBy);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        ArrayList<HistoryItem> historyItems = new ArrayList<>();
        if(data != null && data.moveToFirst()){
            do{
                historyItems.add(new HistoryItem(data));
            }while(data.moveToNext());
        }
        Log.i(TAG, "onLoadFinished :: "+historyItems.size());

        mListAdapter.changeDataSet(historyItems);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mListAdapter.changeDataSet(new ArrayList<HistoryItem>());
    }

    private OnListHistoryInteractionListener mListener;
    private HistoryRecyclerViewAdapter mListAdapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public HistoryFragment() {
    }

    public static HistoryFragment newInstance() {
        HistoryFragment fragment = new HistoryFragment();
//        Bundle args = new Bundle();r
//        args.putInt(ARG_COLUMN_COUNT, columnCount);
//        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if (getArguments() != null) {
//            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
//        }
    }

    @Override
    public void onResume(){
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        View emptyView = view.findViewById(R.id.history_emptyView);
        emptyView.findViewById(R.id.history_add_friends_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mListener.onAddFriendHistoryInteraction();
                    }
                });


        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.history_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mListAdapter = new HistoryRecyclerViewAdapter(getContext(), mListener, emptyView);

        recyclerView.setAdapter(mListAdapter);
        getLoaderManager().initLoader(HISTORY_LOADER, null, this);

        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListHistoryInteractionListener) {
            mListener = (OnListHistoryInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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
    public interface OnListHistoryInteractionListener {
        void onListHistoryItemInteraction();

        void onAddFriendHistoryInteraction();
    }
}
