package com.sam_chordas.android.stockhawk.data;

import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;

/**
 * Created by Akshay Kant on 11-10-2016.
 */

public class HistoryColumns {

    @DataType(DataType.Type.INTEGER)
    @PrimaryKey
    @AutoIncrement
    public static final String _ID = "_id";

    @DataType(DataType.Type.TEXT)
    public static final String SYMBOL = "symbol";

    @DataType(DataType.Type.TEXT)
    @NotNull
    public static final String DATE = "date";

    @DataType(DataType.Type.REAL)
    @NotNull
    public static final String LOW_PRICE = "low_price";

    @DataType(DataType.Type.REAL)
    @NotNull
    public static final String HIGH_PRICE = "high_price";
}