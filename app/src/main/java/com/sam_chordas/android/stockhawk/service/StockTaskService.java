package com.sam_chordas.android.stockhawk.service;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.HistoryColumns;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.exception.CountZeroException;
import com.sam_chordas.android.stockhawk.exception.NonExistentSymbolException;
import com.sam_chordas.android.stockhawk.utilities.StockHistory;
import com.sam_chordas.android.stockhawk.utilities.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.sam_chordas.android.stockhawk.utilities.Constants.ACTION_ADD;
import static com.sam_chordas.android.stockhawk.utilities.Constants.ACTION_INIT;
import static com.sam_chordas.android.stockhawk.utilities.Constants.BASE_URL;
import static com.sam_chordas.android.stockhawk.utilities.Constants.EXTRA_SYMBOL;
import static com.sam_chordas.android.stockhawk.utilities.Constants.FORMAT;
import static com.sam_chordas.android.stockhawk.utilities.Constants.INIT_QUOTES;
import static com.sam_chordas.android.stockhawk.utilities.Constants.TAG_HISTORY;
import static com.sam_chordas.android.stockhawk.utilities.Constants.TAG_PERIODIC;
import static com.sam_chordas.android.stockhawk.utilities.Constants.UTF_8;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 * </p>
 * Updated by: Akshay Kant
 */
public class StockTaskService extends GcmTaskService {

    private String LOG_TAG = StockTaskService.class.getSimpleName();

    /*Denotes that the annotated element of integer type,
    * represents a logical type and that its value should be one of the explicitly named constants.
    */

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STATUS_OK, STATUS_SERVER_ERROR, STATUS_NO_NETWORK, STATUS_ERROR_JSON,
            STATUS_UNKNOWN, STATUS_SERVER_DOWN})
    public @interface StockStatuses {
    }

    public static final int STATUS_OK = 0;
    public static final int STATUS_ERROR_JSON = 1;
    public static final int STATUS_SERVER_ERROR = 2;
    public static final int STATUS_SERVER_DOWN = 3;
    public static final int STATUS_NO_NETWORK = 4;
    public static final int STATUS_UNKNOWN = 5;

    private OkHttpClient client = new OkHttpClient();
    private Context mContext;
    private StringBuilder mStoredSymbols = new StringBuilder();
    private boolean isUpdate;


    public StockTaskService() {
    }

    public StockTaskService(Context context) {
        mContext = context;
    }

    /**
     * Fetches the response from the specified url.
     *
     * @param url The url from which to get response.
     * @return response from the server
     * @throws IOException
     */
    String fetchData(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    /**
     * Executes the task requested which can be
     * init         Application initialization task.
     * periodic     To periodically update stock symbol bid price.
     * history      To periodically update the stock history.
     */
    @Override
    public int onRunTask(TaskParams params) {

        Cursor initQueryCursor;
        String tag = params.getTag();

        if (mContext == null) {
            mContext = this;

        }

        if (params.getTag().equals(TAG_HISTORY)) {
            return executeHistoryTask();
        }

        StringBuilder urlStringBuilder = new StringBuilder();

        try {
            // Base URL for the Yahoo query
            urlStringBuilder.append(BASE_URL);

            urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol "
                    + "in (", UTF_8));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            setStockStatus(mContext, STATUS_UNKNOWN);
        }

        if (tag.equals(ACTION_INIT) || tag.equals(TAG_PERIODIC)) {
            isUpdate = true;
            initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{"Distinct " + QuoteColumns.SYMBOL}, null,
                    null, null);

            if (initQueryCursor.getCount() == 0 || initQueryCursor == null) {
                // Init task. Populates DB with quotes for the symbols seen below
                try {
                    urlStringBuilder.append(
                            URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    setStockStatus(mContext, STATUS_UNKNOWN);
                }
            } else if (initQueryCursor != null) {
                DatabaseUtils.dumpCursor(initQueryCursor);
                initQueryCursor.moveToFirst();
                for (int i = 0; i < initQueryCursor.getCount(); i++) {
                    mStoredSymbols.append("\"" +
                            initQueryCursor.getString(initQueryCursor.getColumnIndex(EXTRA_SYMBOL)) + "\",");
                    initQueryCursor.moveToNext();
                }
                mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
                try {
                    urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    setStockStatus(mContext, STATUS_UNKNOWN);
                }
            }
            initQueryCursor.close();
        } else if (tag.equals(ACTION_ADD)) {
            isUpdate = false;
            // get symbol from params.getExtra and build query
            String stockInput = params.getExtras().getString(EXTRA_SYMBOL);
            try {
                urlStringBuilder.append(URLEncoder.encode("\"" + stockInput + "\")", UTF_8));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                setStockStatus(mContext, STATUS_UNKNOWN);
            }
        }

        // finalize the URL for the API query.
        urlStringBuilder.append(FORMAT);

        String urlString = null;
        String getResponse;
        int result = GcmNetworkManager.RESULT_FAILURE;

        urlString = urlStringBuilder.toString();

        try {
            //fetch data from the server
            getResponse = fetchData(urlString);
            Log.i("URL", "onRunTask: " + urlString);
            Log.i("RESPONSE", "onRunTask: " + getResponse);
            result = GcmNetworkManager.RESULT_SUCCESS;
            try {
                ContentValues contentValues = new ContentValues();
                // update ISCURRENT to 0 (false) so new data is current
                if (isUpdate) {
                    contentValues.put(QuoteColumns.ISCURRENT, 0);
                    mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                            null, null);
                }

                ArrayList<ContentProviderOperation> batchOperations = Utils.quoteJsonToContentVals(getResponse);


                mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                        batchOperations);

            } catch (RemoteException | OperationApplicationException e) {
                Log.e(LOG_TAG, "Error applying batch insert", e);
                setStockStatus(mContext, STATUS_ERROR_JSON);
                return result;
            } catch (JSONException e) {
                e.printStackTrace();
                setStockStatus(mContext, STATUS_ERROR_JSON);
                Intent intent = new Intent();
                intent.setAction("com.sam_chordas.android.stockhawk.ui.MyStocksActivity.STOCK_NOT_FOUND");
                mContext.sendBroadcast(intent);
                return result;
            } catch (CountZeroException e) {
                e.printStackTrace();
                setStockStatus(mContext, STATUS_ERROR_JSON);
                return result;
            } catch (NonExistentSymbolException e) {
                e.printStackTrace();
                setStockStatus(mContext, STATUS_ERROR_JSON);
                Intent intent = new Intent();
                intent.setAction("com.sam_chordas.android.stockhawk.ui.MyStocksActivity.STOCK_NOT_FOUND");
                mContext.sendBroadcast(intent);
                return result;
            }
        } catch (IOException e) {
            e.printStackTrace();
            setStockStatus(mContext, STATUS_SERVER_DOWN);
            return result;
        }

        setStockStatus(mContext, STATUS_OK);
        return result;
    }

    public static void setStockStatus(Context context, @StockStatuses int stockStatus) {
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(context.getString(R.string.stockStatus), stockStatus);
        editor.apply();
    }

    /**
     * Builds the stock history query.
     *
     * @param symbol
     * @param startDate
     * @param endDate
     * @return
     */

    private String buildQuery(String symbol, String startDate, String endDate) {
        StringBuilder queryBuilder = new StringBuilder();
        String query = "select * from yahoo.finance.historicaldata where";
        queryBuilder.append(query);
        queryBuilder.append(" symbol=\"" + symbol + "\"");
        queryBuilder.append(" and startDate=\"" + startDate + "\"");
        queryBuilder.append(" and endDate=\"" + endDate + "\"");
        return queryBuilder.toString();
    }

    /**
     * Executes the stock history to get stock historical data.
     *
     * @return The status of the task
     */
    private int executeHistoryTask() {
        String query = "select * from yahoo.finance.historicaldata where";

        Cursor cursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                new String[]{"Distinct " + QuoteColumns.SYMBOL}, null,
                null, null);

        String response;
        int result = GcmNetworkManager.RESULT_FAILURE;

        if (cursor != null && cursor.getCount() != 0) {

            StringBuilder stringBuilder;
            String symbol;
            while (cursor.moveToNext()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(BASE_URL);
                symbol = cursor.getString(cursor.getColumnIndex("symbol"));
                String startDate = Utils.getPreviousDayDate();
                String endDate = String.format("%tF", new Date());
                try {
                    stringBuilder.append(URLEncoder.encode(buildQuery(symbol, startDate, endDate), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    setStockStatus(mContext, STATUS_UNKNOWN);
                    return result;
                }

                stringBuilder.append(FORMAT);

                try {

                    response = fetchData(stringBuilder.toString());
                    List<StockHistory> stockHistoryList = Utils.quoteToStockHistory(response);

                    //delete old entry for this symbol from history table and insert a new one
                    String where = HistoryColumns.DATE + "= ?";
                    String selectionArg[] = {Utils.getTenDaysBeforePreviousDate()};
                    int delete = mContext.getContentResolver().delete(QuoteProvider.History.withSymbol(symbol)
                            , where, selectionArg);

                    int i = Utils.bulkInsert(stockHistoryList, symbol, mContext.getContentResolver());

                    if (i == 1)
                        result = GcmNetworkManager.RESULT_SUCCESS;

                } catch (IOException e) {
                    setStockStatus(mContext, STATUS_SERVER_DOWN);
                    return result;
                } catch (JSONException e) {
                    setStockStatus(mContext, STATUS_ERROR_JSON);
                    Intent intent = new Intent();
                }
            }
        }
        return GcmNetworkManager.RESULT_SUCCESS;
    }
}