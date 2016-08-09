package ayush.stockhawk.activity;

import android.app.LoaderManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.afollestad.materialdialogs.MaterialDialog;

import ayush.stockhawk.R;
import ayush.stockhawk.database.QuoteColumns;
import ayush.stockhawk.database.QuoteProvider;
import ayush.stockhawk.QuoteCursorAdapter;
import ayush.stockhawk.RecyclerViewItemClickListener;
import ayush.stockhawk.Utils;
import ayush.stockhawk.service.StockIntentService;
import ayush.stockhawk.service.StockTaskService;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;

import ayush.stockhawk.widget.StockQuoteWidget;
import ayush.stockhawk.assist.SimpleItemTouchHelperCallback;

import butterknife.Bind;
import butterknife.ButterKnife;

public class StockList extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private CharSequence mTitle;
    private Intent mServiceIntent;
    private ItemTouchHelper mItemTouchHelper;
    private static final int CURSOR_LOADER_ID = 0;
    private QuoteCursorAdapter mCursorAdapter;
    private Context mContext;
    private Cursor mCursor;

    @Bind(R.id.recycler_view)
    public RecyclerView recyclerView;

    @Bind(R.id.coordinatorLayout)
    public CoordinatorLayout coordinatorLayout;

    @Bind(R.id.fab)
    public FloatingActionButton fab;

    @Bind(R.id.llNoInternetView)
    public LinearLayout noInternetView;

    @Bind(R.id.llNoStocksView)
    public LinearLayout noStockView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_my_stocks);

        ButterKnife.bind(this);


        mServiceIntent = new Intent(this, StockIntentService.class);

        if (savedInstanceState == null) {
            mServiceIntent.putExtra("tag", "init");

            if (Utils.isNetworkAvailable(mContext)) {
                startService(mServiceIntent);
            } else {
                networkSnackBar();
            }
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        mCursorAdapter = new QuoteCursorAdapter(this, null);

        recyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {
                        itemClickListener(position);
                    }
                }));

        recyclerView.setAdapter(mCursorAdapter);

        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onFABClickListener();
                }
            });
        }

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);

        mTitle = getTitle();
        if (Utils.isNetworkAvailable(mContext)) {
            long period = 3600L;
            long flex = 10L;
            String periodicTag = "periodic";

            PeriodicTask periodicTask = new PeriodicTask.Builder()
                    .setService(StockTaskService.class)
                    .setPeriod(period)
                    .setFlex(flex)
                    .setTag(periodicTag)
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    .setRequiresCharging(false)
                    .build();

            GcmNetworkManager.getInstance(this).schedule(periodicTask);
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
    }

    public void networkSnackBar() {
        Snackbar.make(coordinatorLayout, getString(R.string.network_toast), Snackbar.LENGTH_SHORT).show();
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

        int id = item.getItemId();


        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_change_units) {
            Utils.showPercent = !Utils.showPercent;
            this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

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

        updateEmptyView();

        updateStocksWidget();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }

    public void updateEmptyView() {

        if (mCursorAdapter.getItemCount() == 0) {
            if (!Utils.isNetworkAvailable(mContext)) {
                noInternetView.setVisibility(View.VISIBLE);
                noStockView.setVisibility(View.GONE);
            } else {
                noInternetView.setVisibility(View.GONE);
                noStockView.setVisibility(View.VISIBLE);
            }
        } else {
            noInternetView.setVisibility(View.GONE);
            noStockView.setVisibility(View.GONE);
        }
    }

    private void updateStocksWidget() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext.getApplicationContext());

        int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(this, StockQuoteWidget.class));

        if (ids.length > 0) {
            appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.stock_list);
        }
    }

    private void itemClickListener(int position) {
        if (mCursor.moveToPosition(position)) {
            Intent intent = new Intent(mContext, StockDetails.class);
            intent.putExtra(QuoteColumns.SYMBOL, mCursor.getString(mCursor.getColumnIndex(QuoteColumns.SYMBOL)));
            intent.putExtra(QuoteColumns.BIDPRICE, mCursor.getString(mCursor.getColumnIndex(QuoteColumns.BIDPRICE)));
            startActivity(intent);
        }
    }

    private void onFABClickListener() {

        if (Utils.isNetworkAvailable(mContext)) {

            new MaterialDialog.Builder(mContext).title(R.string.symbol_search)
                    .content(R.string.content_test)
                    .inputType(InputType.TYPE_CLASS_TEXT)
                    .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {

                        @Override
                        public void onInput(MaterialDialog dialog, CharSequence input) {

                            String newSymbol = input.toString().toUpperCase();

                            Cursor c = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                    new String[]{QuoteColumns.SYMBOL}, QuoteColumns.SYMBOL + "= ?",
                                    new String[]{newSymbol}, null);

                            if (c.getCount() != 0) {
                                Snackbar.make(coordinatorLayout,
                                        getString(R.string.stock_already_saved),
                                        Snackbar.LENGTH_SHORT).show();

                                return;
                            } else {

                                mServiceIntent.putExtra("tag", "add");
                                mServiceIntent.putExtra("symbol", newSymbol);
                                startService(mServiceIntent);
                            }
                        }
                    }).show();
        } else {
            networkSnackBar();
        }
    }

}
