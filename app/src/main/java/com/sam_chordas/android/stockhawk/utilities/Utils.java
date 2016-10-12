package com.sam_chordas.android.stockhawk.utilities;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.sam_chordas.android.stockhawk.data.HistoryColumns;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.exception.CountZeroException;
import com.sam_chordas.android.stockhawk.exception.NonExistentSymbolException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.sam_chordas.android.stockhawk.utilities.Constants.EXTRA_SYMBOL;
import static com.sam_chordas.android.stockhawk.utilities.Constants.TAG_CHANGE;
import static com.sam_chordas.android.stockhawk.utilities.Constants.TAG_CHANGE_IN_PERCNTAGE;
import static com.sam_chordas.android.stockhawk.utilities.Constants.TAG_COUNT;
import static com.sam_chordas.android.stockhawk.utilities.Constants.TAG_DATE;
import static com.sam_chordas.android.stockhawk.utilities.Constants.TAG_HIGH;
import static com.sam_chordas.android.stockhawk.utilities.Constants.TAG_LOW;
import static com.sam_chordas.android.stockhawk.utilities.Constants.TAG_QUERY;
import static com.sam_chordas.android.stockhawk.utilities.Constants.TAG_QUOTE;
import static com.sam_chordas.android.stockhawk.utilities.Constants.TAG_RESULTS;

/**
 * Created by sam_chordas on 10/8/15.
 * <p/>
 * Updated By: Akshay Kant
 */
public class Utils {

    private static String LOG_TAG = Utils.class.getSimpleName();

    public static boolean showPercent = true;

    // input date format from yahoo apis.
    private static SimpleDateFormat mInputDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    //output date format for stock hawk
    private static SimpleDateFormat mOutputDateFormat = new SimpleDateFormat("d MMM");

    // intent filter for updates to stock widgets.
    final static public String STOCK_APPWIDGET_UPDATE = "com.sam_chordas.android.stockhawk.widget." +
            "STOCK_APPWIDGET_UPDATE";


    /**
     * Checks whether internet is available or not.
     *
     * @param context The application context
     * @return
     */
    public static boolean isNetworkAvailable(Context context) {

        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    /**
     * Converts the json data to the list of content provider operation.
     *
     * @param JSON The json string
     * @return
     * @throws JSONException              In case if invalid json string.
     * @throws CountZeroException         If empty response form yahoo apis.
     * @throws NonExistentSymbolException If symbol data is not present.
     */
    public static ArrayList quoteJsonToContentVals(String JSON) throws JSONException, CountZeroException, NonExistentSymbolException {
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
        JSONObject jsonObject = null;
        JSONArray resultsArray = null;

        jsonObject = new JSONObject(JSON);
        if (jsonObject != null && jsonObject.length() != 0) {
            jsonObject = jsonObject.getJSONObject(TAG_QUERY);
            int count = Integer.parseInt(jsonObject.getString(TAG_COUNT));
            if (count == 0)
                throw new CountZeroException();
            if (count == 1) {
                jsonObject = jsonObject.getJSONObject(TAG_RESULTS)
                        .getJSONObject(TAG_QUOTE);
                batchOperations.add(buildBatchOperation(jsonObject));
            } else {
                resultsArray = jsonObject.getJSONObject(TAG_RESULTS).getJSONArray(TAG_QUOTE);

                if (resultsArray != null && resultsArray.length() != 0) {
                    for (int i = 0; i < resultsArray.length(); i++) {
                        jsonObject = resultsArray.getJSONObject(i);
                        batchOperations.add(buildBatchOperation(jsonObject));
                    }
                }
            }
        }

        return batchOperations;
    }

    /**
     * Truncates bid price.
     *
     * @param bidPrice
     * @return
     * @throws NonExistentSymbolException
     */
    public static String truncateBidPrice(String bidPrice) throws NonExistentSymbolException {

        if (bidPrice == null)
            throw new NonExistentSymbolException();

        try {
            float fbidPrice = Float.parseFloat(bidPrice);
            bidPrice = String.format("%.2f", fbidPrice);
        } catch (NumberFormatException e) {
            throw new NonExistentSymbolException();
        }
        return bidPrice;
    }

    /**
     * Truncates the change if percent change is required.
     *
     * @param change
     * @param isPercentChange
     * @return
     */
    public static String truncateChange(String change, boolean isPercentChange) {
        String weight = change.substring(0, 1);
        String ampersand = "";
        if (isPercentChange) {
            ampersand = change.substring(change.length() - 1, change.length());
            change = change.substring(0, change.length() - 1);
        }
        change = change.substring(1, change.length());
        double round = Math.round(Double.parseDouble(change) * 100) / 100;
        change = String.format("%.2f", round);
        StringBuffer changeBuffer = new StringBuffer(change);
        changeBuffer.insert(0, weight);
        changeBuffer.append(ampersand);
        change = changeBuffer.toString();
        return change;
    }


    /**
     * Constructs the content provider operation for symbol
     *
     * @param jsonObject
     * @return
     * @throws JSONException
     * @throws NonExistentSymbolException
     */
    public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject) throws
            JSONException, NonExistentSymbolException {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                QuoteProvider.Quotes.CONTENT_URI);
        try {
            String change = jsonObject.getString(TAG_CHANGE);
            builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString(EXTRA_SYMBOL));
            builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Bid")));
            builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
                    jsonObject.getString(TAG_CHANGE_IN_PERCNTAGE), true));
            builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
            builder.withValue(QuoteColumns.ISCURRENT, 1);
            if (change.charAt(0) == '-') {
                builder.withValue(QuoteColumns.ISUP, 0);
            } else {
                builder.withValue(QuoteColumns.ISUP, 1);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return builder.build();
    }

    /**
     * Converts the json string to the list of stock histories.
     *
     * @param jsonData
     * @return
     * @throws JSONException
     */

    public static List<StockHistory> quoteToStockHistory(String jsonData) throws JSONException {
        List<StockHistory> stockHistoryList = null;

        JSONObject jsonObject = new JSONObject(jsonData);

        if (jsonObject != null && jsonObject.length() > 0) {

            jsonObject = jsonObject.getJSONObject(TAG_QUERY);
            int count = jsonObject.getInt(TAG_COUNT);

            if (count > 0) {
                jsonObject = jsonObject.getJSONObject(TAG_RESULTS);
                JSONArray quoteArray = jsonObject.getJSONArray(TAG_QUOTE);
                stockHistoryList = new ArrayList<StockHistory>();
                for (int index = 0; index < count; index++) {
                    jsonObject = quoteArray.getJSONObject(index);
                    StockHistory stockHistory = new StockHistory();
                    stockHistory.setLow((float) jsonObject.getDouble(TAG_LOW));
                    stockHistory.setHigh((float) jsonObject.getDouble(TAG_HIGH));
                    stockHistory.setDate(jsonObject.getString(TAG_DATE));
                    stockHistoryList.add(stockHistory);
                }
            }
        }
        return stockHistoryList;
    }


    /**
     * Calculate the last 11th day date.
     *
     * @return The formatted date.
     */
    public static String getTenDaysBeforePreviousDate() {
        Calendar mCalendar = Calendar.getInstance();
        mCalendar.add(Calendar.DATE, -11);
        return String.format("%tF", mCalendar.getTime());
    }

    /**
     * Returns the previous day date.
     *
     * @return The formatted date.
     */
    public static String getPreviousDayDate() {
        Calendar mCalendar = Calendar.getInstance();
        mCalendar.add(Calendar.DATE, -1);
        return String.format("%tF", mCalendar.getTime());
    }

    /**
     * Formats the input label with input date format.
     *
     * @param label
     * @return
     * @throws ParseException
     */

    public static String formatLabel(String label) throws ParseException {
        Date outputDate = mInputDateFormat.parse(label);
        return mOutputDateFormat.format(outputDate);
    }

    /**
     * Inserts the each stock history object into stock history table.
     *
     * @param stockHistoryList The list of stock history object.
     * @param symbol           The symbol.
     * @param contentResolver
     * @return
     */
    public static int bulkInsert(List<StockHistory> stockHistoryList, String symbol, ContentResolver contentResolver) {

        if (stockHistoryList == null)
            return 0;

        ContentValues contentValues[] = new ContentValues[stockHistoryList.size()];
        StockHistory stockHistory;
        for (int index = 0; index < contentValues.length; index++) {

            contentValues[index] = new ContentValues();
            stockHistory = stockHistoryList.get(index);
            contentValues[index].put(HistoryColumns.SYMBOL, symbol);
            contentValues[index].put(HistoryColumns.LOW_PRICE, stockHistory.getLow());
            contentValues[index].put(HistoryColumns.HIGH_PRICE, stockHistory.getHigh());
            contentValues[index].put(HistoryColumns.DATE, stockHistory.getDate());
        }

        return contentResolver.bulkInsert(QuoteProvider.History.CONTENT_URI, contentValues);
    }

    public static String convertDate(String inputDate){
        StringBuilder outputFormattedDate = new StringBuilder();
        outputFormattedDate.append(inputDate.substring(6))
                .append("/")
                .append(inputDate.substring(4,6))
                .append("/")
                .append(inputDate.substring(2, 4));
        return outputFormattedDate.toString();
    }

}
