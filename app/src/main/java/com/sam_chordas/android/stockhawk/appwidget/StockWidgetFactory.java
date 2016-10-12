package com.sam_chordas.android.stockhawk.appwidget;

/**
 * Created by Akshay Kant on 12-10-2016.
 */

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.utilities.Constants;
import com.sam_chordas.android.stockhawk.utilities.Utils;

import static com.sam_chordas.android.stockhawk.utilities.Constants.EXTRA_SYMBOL;

/**
 * Equivalent to a CursorAdapter/ArrayAdapter with ListView.
 */
public class StockWidgetFactory implements RemoteViewsService.RemoteViewsFactory {

    private Cursor mCursor;
    private Context mContext;

    public StockWidgetFactory(Context context) {
        mContext = context;
    }

    /**
     * Method where we should initialize all our data collections.
     * In this case, our cursor.
     */
    @Override
    public void onCreate() {

        mCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                new String[]{ QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);

    }


    /**
     * Applies the data set changes.
     */
    @Override
    public void onDataSetChanged() {
        onCreate();
    }


    @Override
    public void onDestroy() {
        //close the cursor.
        if (this.mCursor != null)
            this.mCursor.close();
    }

    /**
     * Returns the total number of rows in cursor.
     * @return
     */
    @Override
    public int getCount() {
        if(mCursor != null) {
            return mCursor.getCount();
        }
        return 0;
    }

    @Override
    public RemoteViews getViewAt(int index) {

        mCursor.moveToPosition(index);

        int symbolIndex = mCursor.getColumnIndex(QuoteColumns.SYMBOL);
        int bidPriceIndex = mCursor.getColumnIndex(QuoteColumns.BIDPRICE);
        int changeIndex = mCursor.getColumnIndex(QuoteColumns.CHANGE);
        int percentChangeIndex = mCursor.getColumnIndex(QuoteColumns.PERCENT_CHANGE);
        int isUpIndex = mCursor.getColumnIndex(QuoteColumns.ISUP);

        String symbol = mCursor.getString(symbolIndex);
        String bidPrice = mCursor.getString(bidPriceIndex);
        String change = mCursor.getString(changeIndex);
        String percentChange = mCursor.getString(percentChangeIndex);
        int isUp = mCursor.getInt(isUpIndex);

        RemoteViews views = new RemoteViews(mContext.getPackageName(),
                R.layout.list_item_quote);

        views.setTextViewText(R.id.stock_symbol, symbol);
        views.setTextViewText(R.id.bid_price, bidPrice);

        if (Utils.showPercent) {
            views.setTextViewText(R.id.stock_change,percentChange);
        } else {
            views.setTextViewText(R.id.stock_change,change);
        }

        Intent fillInIntent  = new Intent();
        fillInIntent.putExtra("symbol",symbol);

        views.setOnClickFillInIntent(R.id.item_linear_layout, fillInIntent);

        return views;

    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        //we have only one type of view to display so returning 1.
        return 1;
    }

    @Override
    public long getItemId(int position) {
        //Return the data from the ID column of the table.
        if (mCursor != null) {
            return mCursor.getLong(mCursor.getColumnIndex(QuoteColumns._ID));
        }
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }
}
