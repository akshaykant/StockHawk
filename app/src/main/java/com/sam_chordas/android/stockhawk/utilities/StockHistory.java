package com.sam_chordas.android.stockhawk.utilities;

/**
 * Created by Akshay Kant on 11-10-2016.
 */
public class StockHistory {
    private float mLow;
    private float mHigh;
    private String mDate;

    public String getDate() {
        return mDate;
    }

    public void setDate(String mDate) {
        this.mDate = mDate;
    }

    public float getHigh() {
        return mHigh;
    }

    public void setHigh(float mHigh) {
        this.mHigh = mHigh;
    }

    public float getLow() {
        return mLow;
    }

    public void setLow(float mLow) {
        this.mLow = mLow;
    }

    @Override
    public String toString() {
        return "StockHistory:[" + mLow + ", " + mHigh + ", " + mDate + "]";
    }
}