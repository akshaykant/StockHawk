package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.appwidget.AppWidgetManager;
import android.content.AsyncQueryHandler;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.appwidget.StockWidgetProvider;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.databinding.ActivityMyStocksBinding;
import com.sam_chordas.android.stockhawk.utilities.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.utilities.RecyclerViewItemClickListener;
import com.sam_chordas.android.stockhawk.utilities.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;

import static com.sam_chordas.android.stockhawk.utilities.Constants.ACTION_ADD;
import static com.sam_chordas.android.stockhawk.utilities.Constants.EXTRA_SYMBOL;
import static com.sam_chordas.android.stockhawk.utilities.Constants.EXTRA_TAG;
import static com.sam_chordas.android.stockhawk.utilities.Constants.SYMBOL_NAME;
import static com.sam_chordas.android.stockhawk.utilities.Constants.TAG_PERIODIC;

public class MyStocksActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int SYMBOL_SEARCH_QUERY_TAG = 1001;

    ActivityMyStocksBinding binding;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private Intent mServiceIntent;
    private ItemTouchHelper mItemTouchHelper;
    private static final int CURSOR_LOADER_ID = 0;
    private QuoteCursorAdapter mCursorAdapter;
    private Context mContext;
    private Cursor mCursor;
    boolean isConnected;
    private boolean mTwoPane;

    int symbolColumnIndex;

    private SharedPreferences mSharedPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;

        //Get the Network Info : isConnected
        isConnected = Utils.isNetworkAvailable(mContext);

        //Instantiate binding to the layout
        binding = DataBindingUtil.setContentView(this, R.layout.activity_my_stocks);


        if (findViewById(R.id.stock_detail_container) != null) {
            mTwoPane = true;
        }

        mSharedPreference = PreferenceManager.getDefaultSharedPreferences(this);

        initializeService(savedInstanceState);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));

        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        mCursorAdapter = new QuoteCursorAdapter(this, null);
        binding.recyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {
                        if (mCursor.moveToPosition(position)) {
                            if (mTwoPane) {
                                Bundle arguments = new Bundle();
                                arguments.putString(SYMBOL_NAME, mCursor.getString(symbolColumnIndex));
                                StockDetailFragment fragment = new StockDetailFragment();
                                fragment.setArguments(arguments);
                                getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.stock_detail_container, fragment)
                                        .commit();
                            } else {
                                Intent intent = new Intent(mContext, StockDetailActivity.class);
                                intent.putExtra(SYMBOL_NAME, mCursor.getString(symbolColumnIndex));
                                startActivity(intent);
                            }
                        }
                    }
                }));

        binding.recyclerView.setAdapter(mCursorAdapter);
        emptyViewBehavior();


        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(binding.recyclerView);

        mTitle = getTitle();
        if (isConnected) {
            long period = 3600L;
            long flex = 10L;

            // create a periodic task to pull stocks once every hour after the app has been opened. This
            // is so Widget data stays up to date.
            PeriodicTask periodicTask = new PeriodicTask.Builder()
                    .setService(StockTaskService.class)
                    .setPeriod(period)
                    .setFlex(flex)
                    .setTag(TAG_PERIODIC)
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    .setRequiresCharging(false)
                    .build();
            // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
            // are updated.
            GcmNetworkManager.getInstance(this).schedule(periodicTask);


            binding.fab.attachToRecyclerView(binding.recyclerView);
            binding.fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isConnected) {
                        new MaterialDialog.Builder(mContext).title(R.string.symbol_search)
                                .content(R.string.content_test)
                                .inputType(InputType.TYPE_CLASS_TEXT)
                                .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                                    @Override
                                    public void onInput(MaterialDialog dialog, CharSequence input) {
                                        String symbol = input.toString().toUpperCase();
                                        SymbolQuery symbolQuery = new SymbolQuery(getContentResolver(), symbol);
                                        symbolQuery.startQuery(SYMBOL_SEARCH_QUERY_TAG,
                                                null,
                                                QuoteProvider.Quotes.CONTENT_URI,
                                                new String[]{QuoteColumns.SYMBOL},
                                                QuoteColumns.SYMBOL + "=?",
                                                new String[]{symbol},
                                                null);
                                    }
                                })
                                .titleColor(Color.BLACK)
                                .contentColor(Color.BLACK)
                                .show();
                    } else {
                        Snackbar.make(binding.coordinateLayoutActivityMyStocks, getString(R.string.no_internet_connection),
                                Snackbar.LENGTH_LONG).show();
                    }

                }
            });
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
        emptyViewBehavior();
    }


    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_stocks, menu);
        restoreActionBar();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_change_units) {
            // this is for changing stock changes from percent value to dollar value
            Utils.showPercent = !Utils.showPercent;
            this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Executes the {@link StockIntentService} for application initialization task
     *
     * @param savedInstanceState
     */
    private void initializeService(Bundle savedInstanceState) {
        // The intent service is for executing immediate pulls from the Yahoo API
        // GCMTaskService can only schedule tasks, they cannot execute immediately
        mServiceIntent = new Intent(this, StockIntentService.class);
        if (savedInstanceState == null) {
            // Run the initialize task service so that some stocks appear upon an empty database
            mServiceIntent.putExtra("tag", "init");
            if (isConnected) {
                startService(mServiceIntent);
            } else {
                Snackbar.make(binding.coordinateLayoutActivityMyStocks, getString(R.string.no_internet_connection),
                        Snackbar.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Method to show error message when the data is not available.
     */
    public void emptyViewBehavior() {

        if (mCursorAdapter.getItemCount() <= 0) {
            //The data is not available


            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
            @StockTaskService.StockStatuses int stockStatus = sp.getInt(getString(R.string.stockStatus), -1);

            String message = getString(R.string.data_not_available);

            switch (stockStatus) {
                case StockTaskService.STATUS_OK:
                    message += getString(R.string.string_status_ok);
                    break;

                case StockTaskService.STATUS_NO_NETWORK:
                    message += getString(R.string.string_status_no_network);
                    break;

                case StockTaskService.STATUS_ERROR_JSON:
                    message += getString(R.string.string_error_json);
                    break;

                case StockTaskService.STATUS_SERVER_DOWN:
                    message += getString(R.string.string_server_down);
                    break;

                case StockTaskService.STATUS_SERVER_ERROR:
                    message += getString(R.string.string_error_server);
                    break;

                case StockTaskService.STATUS_UNKNOWN:
                    message += getString(R.string.string_status_unknown);
                    break;
                default:
                    break;

            }

            binding.emptyViewAcitivityMyStocks.setText(message);


            binding.recyclerView.setVisibility(View.INVISIBLE);
            binding.emptyViewAcitivityMyStocks.setVisibility(View.VISIBLE);
        } else {
            //the data is available
            binding.recyclerView.setVisibility(View.VISIBLE);
            binding.emptyViewAcitivityMyStocks.setVisibility(View.INVISIBLE);
            symbolColumnIndex = mCursor.getColumnIndex(QuoteColumns.SYMBOL);
        }
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This narrows the return to only the stocks that are most current.
        return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursorAdapter.swapCursor(data);
        mCursor = data;
        emptyViewBehavior();

        /**
         * As onLoadFinished would be called everytime the data is updated,
         * as well as if any stocks are dismissed, updating the widget at the same time
         * is a good idea.
         * Along with this, this method is called whenever a new item is added to the database.
         */

        updateStocksWidget();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
        emptyViewBehavior();
    }

    /**
     * Class to check database for existing symbol in an
     * asynchronous manner so that the UI thread is not affected.
     */
    public class SymbolQuery extends AsyncQueryHandler {

        String symbol;

        public SymbolQuery(ContentResolver cr, String symbol) {
            /**
             * We are taking the symbol as a parameter
             * so that we can later call the StockIntentService with bundle arguments.
             */
            super(cr);
            this.symbol = symbol;
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {

            /**
             * This method will be called when completed the execution of
             * the query for symbol's in database.
             * We first check if the token is matching with the query that we fired
             * then on, check the conditional logic for the cursor.
             */

            if (token == SYMBOL_SEARCH_QUERY_TAG) {
                if (cursor != null && cursor.getCount() != 0) {
                    /**
                     * Query is found so no need to add it.
                     * Notify the user about the same.
                     */
                    Snackbar.make(binding.coordinateLayoutActivityMyStocks, getString(R.string.already_saved),
                            Snackbar.LENGTH_LONG).show();
                } else {
                    /**
                     * The symbol is not available in the database
                     * so, now we can query Yahoo api services to fetch the symbol and it's equivalent results.
                     */
                    mServiceIntent.putExtra(EXTRA_TAG, ACTION_ADD);
                    mServiceIntent.putExtra(EXTRA_SYMBOL, this.symbol);
                    startService(mServiceIntent);
                    updateStocksWidget();
                }

            }
        }
    }

    /**
     * If any widget is added on the homescreen, this helper method helps in updating it's content from here.
     */
    private void updateStocksWidget() {
        Intent intent = new Intent(Utils.STOCK_APPWIDGET_UPDATE);
        sendBroadcast(intent);
    }
}
