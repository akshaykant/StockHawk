package com.sam_chordas.android.stockhawk.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.utilities.Constants;
import com.sam_chordas.android.stockhawk.utilities.Utils;

import static com.sam_chordas.android.stockhawk.utilities.Constants.ACTION_ADD;
import static com.sam_chordas.android.stockhawk.utilities.Constants.EXTRA_SYMBOL;
import static com.sam_chordas.android.stockhawk.utilities.Constants.EXTRA_TAG;

/**
 * Created by sam_chordas on 10/1/15.
 * </p>
 * Updated by Akshay Kant
 */
public class StockIntentService extends IntentService {


    public StockIntentService() {
        super(StockIntentService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(StockIntentService.class.getSimpleName(), "Stock Intent Service");

        /* EXTRA_TAG = "tag"
        *  ACTION_ADD = "add"
        *  EXTRA_SYMBOL = "symbol"
        */
        Bundle args = new Bundle();
        if (intent.getStringExtra(EXTRA_TAG).equals(ACTION_ADD)) {
            args.putString(EXTRA_SYMBOL, intent.getStringExtra(EXTRA_SYMBOL));
        }

        // We can call OnRunTask from the intent service to force it to run immediately instead of
        // scheduling a task.
        StockTaskService stockTaskService = new StockTaskService(this);
        int result = stockTaskService.onRunTask(new TaskParams(intent.getStringExtra(EXTRA_TAG), args));

        if(result == GcmNetworkManager.RESULT_SUCCESS){
            //send broadcast to stock hawk app widgets
            Intent broadCastIntent = new Intent(Utils.STOCK_APPWIDGET_UPDATE);
            sendBroadcast(broadCastIntent);
        }
    }
}
