package com.sam_chordas.android.stockhawk.exception;

/**
 * Created by Akshay Kant on 11-10-2016.
 */

public class NonExistentSymbolException extends Exception {

    public NonExistentSymbolException(){
        super();
    }

    @Override
    public String toString() {
        return "NonExistentSymbolException: Symbol does not exist.";
    }
}
