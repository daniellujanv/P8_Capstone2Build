package dlujanapps.mx.wary.widget;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import dlujanapps.mx.wary.MainActivity;
import dlujanapps.mx.wary.R;
import dlujanapps.mx.wary.data.DBContract;
import dlujanapps.mx.wary.objects.Friend;

/**
 *
 * Created by DanielLujanApps on lunes13/06/16.
 */
public class WaryRemoteViewsFactory
        implements RemoteViewsService.RemoteViewsFactory
{

    private Context mContext;
    private List<Friend> mFriends = new ArrayList<>();

    Cursor mCursor;

    public WaryRemoteViewsFactory(Context context, Intent intent){
        mContext = context;
    }

    @Override
    public void onCreate() {
        Log.i("WaryRemoteViewsFactory", "onCreate");

    }

    @Override
    public void onDataSetChanged() {
        String selection = DBContract.FriendEntry.COLUMN_NAME_IS_NEW + " = ?";
        String[] selectionArgs = new String[]{
                Integer.toString(0)
        };


        String orderBy = DBContract.FriendEntry.COLUMN_NAME_STATUS + " DESC";
        mCursor = mContext.getContentResolver().query(
                DBContract.FriendEntry.CONTENT_URI
                , Friend.FULL_PROJECTION
                , selection, selectionArgs, orderBy);

        mFriends = new ArrayList<>();
        if (mCursor != null && mCursor.moveToFirst()) {
            do {
                mFriends.add(new Friend(mCursor));
            } while (mCursor.moveToNext());
            Log.i("WaryRemoteViewsFactory", "widget - found friends "+mFriends.size());
            mCursor.close();

        }else{
            Log.i("WaryRemoteViewsFactory", "widget- cursor null");
        }

    }

    @Override
    public void onDestroy() {

    }

    @Override
    public int getCount() {
        return mFriends.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
// Construct a remote views item based on the app widget item XML file,
        // and set the text based on the position.
        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_list_item);

        // Next, set a fill-intent, which will be used to fill in the pending intent template
        // that is set on the collection view in StackWidgetProvider.
        Bundle extras = new Bundle();
        extras.putInt(WaryWidget.WIDGET_EXTRA_FRIEND_ID
                , mFriends.get(position).getId());
        extras.putString(WaryWidget.WIDGET_EXTRA_FRIEND_NAME
                , mFriends.get(position).getName());
        extras.putString(WaryWidget.WIDGET_EXTRA_FRIEND_ADDRESS
                , mFriends.get(position).getAddress());

        String friendName = mFriends.get(position).getName();
        int status = mFriends.get(position).getStatus();

        if(friendName.length() > 9){
            friendName = friendName.substring(0,8)+"...";
        }
        rv.setTextViewText(R.id.widget_list_item_name
                , friendName);

        Log.i("Widget", "getViewAt :: friend name "+friendName+" ... status "+status);

        switch (status){
            case DBContract.FriendEntry.STATUS_CONNECTED:
                Log.i("Widget", "getViewAt :: friend status :: CONNECTED ");
                rv.setTextColor(R.id.widget_list_item_name,
                        ContextCompat.getColor(mContext, R.color.colorAccent));

                rv.setTextViewText(R.id.widget_list_item_status
                        , mContext.getString(R.string.friend_connected));
                rv.setTextColor(R.id.widget_list_item_status
                        , ContextCompat.getColor(mContext, R.color.colorAccent));

                break;

            case DBContract.FriendEntry.STATUS_AVAILABLE:
                Log.i("Widget", "getViewAt :: friend status :: AVAILABLE "+friendName);
                rv.setTextColor(R.id.widget_list_item_name,
                        ContextCompat.getColor(mContext, R.color.colorPrimary));

                rv.setTextViewText(R.id.widget_list_item_status
                        , mContext.getString(R.string.friend_online));
                rv.setTextColor(R.id.widget_list_item_status
                        , ContextCompat.getColor(mContext, R.color.colorPrimary));

                Intent fillInIntent = new Intent();
                fillInIntent.setAction(WaryWidget.WIDGET_ACTION_GO_TO_FRIEND);
                fillInIntent.putExtras(extras);

                // Make it possible to distinguish the individual on-click
                // action of a given item
                fillInIntent.setData(Uri.parse(fillInIntent.toUri(Intent.URI_INTENT_SCHEME)));

                rv.setOnClickFillInIntent(R.id.widget_list_item_wrapper, fillInIntent);

                break;

            case DBContract.FriendEntry.STATUS_AWAY:
                Log.i("Widget", "getViewAt :: friend status :: AWAY "+friendName);
                rv.setTextColor(R.id.widget_list_item_name,
                        ContextCompat.getColor(mContext, R.color.greyText));

                rv.setTextViewText(R.id.widget_list_item_status
                        , mContext.getString(R.string.friend_offline));
                rv.setTextColor(R.id.widget_list_item_status
                        , ContextCompat.getColor(mContext, R.color.greyText));
                break;

            default:
                Log.i("Widget", "getViewAt :: DEFAULT "+friendName);
                rv.setTextColor(R.id.widget_list_item_name,
                        ContextCompat.getColor(mContext, R.color.greyText));

                rv.setTextViewText(R.id.widget_list_item_status
                        , mContext.getString(R.string.friend_offline));
                rv.setTextColor(R.id.widget_list_item_status
                        , ContextCompat.getColor(mContext, R.color.greyText));
        }
        Log.i("Widget", "getViewAt :: DONE");

        return rv;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

}
