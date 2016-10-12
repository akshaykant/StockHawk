package com.sam_chordas.android.stockhawk.appwidget;

import android.content.Intent;
import android.widget.RemoteViewsService;

/**
 * Created by Akshay Kant on 12-10-2016.
 */

public class StockWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StockWidgetFactory(getApplicationContext());
    }
}
