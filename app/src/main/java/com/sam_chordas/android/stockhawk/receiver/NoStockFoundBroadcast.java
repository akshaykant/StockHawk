package com.sam_chordas.android.stockhawk.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.sam_chordas.android.stockhawk.R;

/**
 * Created by Akshay Kant on 11-10-2016.
 */

public class NoStockFoundBroadcast extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast toast = Toast.makeText(context, context.getString(R.string.not_found), Toast.LENGTH_LONG);
        View view = toast.getView();
        view.setBackgroundResource(R.drawable.percent_change_pill);
        TextView text = (TextView) view.findViewById(android.R.id.message);
/*Here you can do anything with above textview like text.setTextColor(Color.parseColor("#000000"));*/
        toast.show();

    }
}
