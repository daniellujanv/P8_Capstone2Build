package dlujanapps.mx.wary.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

/**
 * Created by DanielLujanApps on lunes13/06/16.
 */
public class WaryWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new WaryRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}