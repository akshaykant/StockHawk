package com.sam_chordas.android.stockhawk.exception;

/**
 * Created by Akshay Kant on 11-10-2016.
 */

public class CountZeroException extends Exception {

    public CountZeroException(){
        super();
    }

    @Override
    public String toString() {
        return "CountZeroException: Empty Results.";
    }
}
