package com.sam_chordas.android.stockhawk.utilities;

/**
 * Created by Akshay Kant on 11-10-2016.
 */

public class Constants {

    public static final String EXTRA_TAG = "tag";
    public static final String EXTRA_SYMBOL = "symbol";

    public static final String ACTION_INIT = "init";
    public static final String ACTION_ADD = "add";

    public final static String INIT_QUOTES = "\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\"";
    public final static String TAG_PERIODIC = "periodic";
    public final static String TAG_HISTORY = "history";

    public final static String BASE_URL = "https://query.yahooapis.com/v1/public/yql?q=";
    public final static String FORMAT = "&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
            + "org%2Falltableswithkeys&callback=";

    public final static String UTF_8 = "UTF-8";


    public final static String NETWORK_STATE_CONNECTED = "CONNECTED";
    public final static String NETWORK_STATE_DISCONNECTED = "DISCONNECTED";
    public final static String RESULT_SUCCESS = "SUCCESS";
    public final static String RESULT_FAILURE = "FAILURE";

    public final static String TAG_QUERY = "query";
    public final static String TAG_COUNT = "count";
    public final static String TAG_RESULTS = "results";
    public final static String TAG_QUOTE = "quote";
    public final static String TAG_CHANGE = "Change";
    public final static String TAG_CHANGE_IN_PERCNTAGE = "ChangeinPercent";
    public final static String TAG_LOW = "Low";
    public final static String TAG_HIGH = "High";
    public final static String TAG_DATE = "Date";

    public final static String SYMBOL_NAME = "symbol_name";


}
