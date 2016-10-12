package com.sam_chordas.android.stockhawk.ui;

import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.databinding.StockDetailBinding;
import com.sam_chordas.android.stockhawk.service.HistoricalData;
import com.sam_chordas.android.stockhawk.service.SymbolParcelable;
import com.sam_chordas.android.stockhawk.utilities.Utils;

import android.support.v4.app.Fragment;
import android.widget.TextView;

import java.util.ArrayList;

import static com.sam_chordas.android.stockhawk.utilities.Constants.SYMBOL_NAME;

/**
 * Created by Akshay Kant on 11-10-2016.
 */
public class StockDetailFragment extends Fragment implements HistoricalData.HistoricalDataCallback {

    StockDetailBinding binding;
    HistoricalData historicalData;

    ArrayList<SymbolParcelable> symbolParcelables = null;

    private String mSymbol;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(SYMBOL_NAME)) {
            mSymbol = getArguments().getString(SYMBOL_NAME);
        }

        if (getActionBar() != null) {
            getActionBar().setElevation(0);
            if (getActivity() instanceof StockDetailActivity) {
                getActionBar().setTitle("");
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.stock_detail, container, false);

        View rootView = binding.getRoot();

        historicalData = new HistoricalData(getActivity().getBaseContext(), StockDetailFragment.this);
        historicalData.getHistoricalData(mSymbol);

        return rootView;
    }

    private ActionBar getActionBar() {

        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            return activity.getSupportActionBar();
        }

        return null;
    }

    @Override
    public void onSuccess(ArrayList<SymbolParcelable> list) {

        this.symbolParcelables = list;

        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> xvalues = new ArrayList<>();

        for (int i = 0; i < this.symbolParcelables.size(); i++) {

            SymbolParcelable symbolParcelable = this.symbolParcelables.get(i);
            double yValue = symbolParcelable.close;

            xvalues.add(Utils.convertDate(symbolParcelable.date));
            entries.add(new Entry((float) yValue, i));
        }

        XAxis xAxis = binding.lineChart.getXAxis();
        xAxis.setLabelsToSkip(4);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(12f);
        YAxis left = binding.lineChart.getAxisLeft();
        left.setEnabled(true);
        left.setLabelCount(5, true);

        binding.lineChart.getAxisRight().setEnabled(false);

        binding.lineChart.getLegend().setTextSize(16f);

        LineDataSet dataSet = new LineDataSet(entries, mSymbol);
        LineData lineData = new LineData(xvalues, dataSet);
        binding.lineChart.setBackgroundColor(Color.WHITE);
        binding.lineChart.setData(lineData);
    }

    @Override
    public void onFailure() {
        String errorMessage = "";

        @HistoricalData.HistoricalDataStatuses
        int status = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext())
                .getInt(getString(R.string.historicalDataStatus), -1);

        switch (status) {
            case HistoricalData.STATUS_ERROR_JSON:
                errorMessage += getString(R.string.data_error_json);
                break;
            case HistoricalData.STATUS_ERROR_NO_NETWORK:
                errorMessage += getString(R.string.data_no_internet);
                break;
            case HistoricalData.STATUS_ERROR_PARSE:
                errorMessage += getString(R.string.data_error_parse);
                break;
            case HistoricalData.STATUS_ERROR_UNKNOWN:
                errorMessage += getString(R.string.data_unknown_error);
                break;
            case HistoricalData.STATUS_ERROR_SERVER:
                errorMessage += getString(R.string.data_server_down);
                break;
            case HistoricalData.STATUS_OK:
                errorMessage += getString(R.string.data_no_error);
                break;
            default:
                break;
        }

        final Snackbar snackbar = Snackbar
                .make(binding.layoutLineGraph, getString(R.string.no_data_show) + errorMessage, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.retry, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        historicalData.getHistoricalData(mSymbol);
                    }
                })
                .setActionTextColor(Color.GREEN);

        View subview = snackbar.getView();
        TextView tv = (TextView) subview.findViewById(android.support.design.R.id.snackbar_text);
        tv.setTextColor(Color.RED);
        snackbar.show();
    }
}
