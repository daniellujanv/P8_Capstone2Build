package dlujanapps.mx.wary.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import dlujanapps.mx.wary.MainActivity;
import dlujanapps.mx.wary.R;


/**
 * Implementation of App Widget functionality.
 */
public class WaryWidget extends AppWidgetProvider {

    public static final String WIDGET_ACTION_ADD_FRIEND = "dlujanapps.mx.wary.widget.add_friend";
    public static final String WIDGET_ACTION_START_DISCOVERY = "dlujanapps.mx.wary.widget.start_discovery";
    public static final String WIDGET_ACTION_GO_TO_FRIEND = "dlujanapps.mx.wary.widget.go_to_friend";
    public static final String WIDGET_EXTRA_FRIEND_ID = "dlujanapps.mx.wary.widget.friend_id";
    public static final String WIDGET_EXTRA_FRIEND_NAME = "dlujanapps.mx.wary.widget.friend_name";
    public static final String WIDGET_EXTRA_FRIEND_ADDRESS = "dlujanapps.mx.wary.widget.friend_address";

    public static final String WIDGET_ID_KEYS = "dlujanapps.mx.wary.widget_keys";
    private final String TAG = getClass().getSimpleName();

    public static final String TOAST_ACTION = "com.example.android.stackwidget.TOAST_ACTION";
    public static final String EXTRA_ITEM = "com.example.android.stackwidget.EXTRA_ITEM";

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.i(TAG, "onReceive :: "+intent.getAction());
        if(intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
            int[] ids = intent.getExtras().getIntArray(WIDGET_ID_KEYS);
            if (ids != null) {
//            onUpdate(context, AppWidgetManager.getInstance(context), ids);

                for (int appWidgetId : ids) {
                    Log.i(TAG, "forcing updateAppWidget " + appWidgetId);
                    boolean inDiscoveryMode =
                            PreferenceManager.getDefaultSharedPreferences(context)
                                    .getBoolean(context.getString(R.string.pref_key_discover), true);

                    updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId, inDiscoveryMode);
                }
            }
        } else if (intent.getAction().equals(TOAST_ACTION)) {
//            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
//                    AppWidgetManager.INVALID_APPWIDGET_ID);
            int viewIndex = intent.getIntExtra(EXTRA_ITEM, 0);
            Toast.makeText(context, "Touched view " + viewIndex, Toast.LENGTH_SHORT).show();

        }else{
            super.onReceive(context, intent);
        }
    }


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        Log.i(TAG, "onUpdate");
        boolean inDiscoveryMode =
                PreferenceManager.getDefaultSharedPreferences(context)
                        .getBoolean(context.getString(R.string.pref_key_discover), true);
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, inDiscoveryMode);
        }
    }

    static void updateAppWidget(Context context
            , AppWidgetManager appWidgetManager
            , int appWidgetId, boolean inDiscoveryMode) {

        Log.i("waryWidget", "updatig AppWidget ... "+appWidgetId);
        // Construct the RemoteViews object

        RemoteViews views;

        if(inDiscoveryMode){

            Intent intent = new Intent(context, WaryWidgetService.class);
            // Add the app widget ID to the intent extras.

            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

            // Set up the RemoteViews object to use a RemoteViews adapter.
            // This adapter connects
            // to a RemoteViewsService  through the specified intent.
            // This is how you populate the data.
            views = new RemoteViews(context.getPackageName(), R.layout.widget_list);
            views.setRemoteAdapter(R.id.widget_friends_list, intent);
            // The empty view is displayed when the collection has no items.
            // It should be in the same layout used to instantiate the RemoteViews
            // object above.
            views.setEmptyView(R.id.widget_friends_list, R.id.widget_empty_view);

            /**
             * pendingIntent for individual behaviour
             */
            Intent individualIntent = new Intent(context, MainActivity.class);
            individualIntent.setAction(WIDGET_ACTION_GO_TO_FRIEND);
            individualIntent.setData(Uri.parse(individualIntent.toUri(Intent.URI_INTENT_SCHEME)));

            PendingIntent friendPendingIntent = PendingIntent
                    .getActivity(context, 0, individualIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            views.setPendingIntentTemplate(R.id.widget_friends_list, friendPendingIntent);

            /**
             * empty view click --> add Friend
             */
            Intent addFriendIntent = new Intent(context, MainActivity.class);
            addFriendIntent.setAction(WIDGET_ACTION_ADD_FRIEND);
            addFriendIntent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

            PendingIntent pendingIntent = PendingIntent
                    .getActivity(context, 0, addFriendIntent, 0);

            views.setOnClickPendingIntent(R.id.widget_empty_view, pendingIntent);


        }else {
            // Create an Intent to launch ExampleActivity
            Intent intent = new Intent(context, MainActivity.class);
            intent.setAction(WIDGET_ACTION_START_DISCOVERY);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

            PendingIntent pendingIntent = PendingIntent
                    .getActivity(context, 0, intent, 0);

            CharSequence widgetText = "Discover";
            views = new RemoteViews(context.getPackageName(), R.layout.widget_start);
            views.setOnClickPendingIntent(R.id.widget_discover_button, pendingIntent);

            views.setTextViewText(R.id.widget_discover_text, widgetText);
        }

        // Instruct the widget_start manager to update the widget_start
        appWidgetManager.updateAppWidget(appWidgetId, views);
        Log.i("waryWidget", "updated AppWidget ... "+appWidgetId
                +" .. DISCOVER :: "+inDiscoveryMode);
    }

    @Override
    public void onEnabled(Context context) {
        Log.i(TAG, "onEnable");
        // Enter relevant functionality for when the first widget_start is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget_start is disabled
        Log.i(TAG, "onDisable");
    }
}

