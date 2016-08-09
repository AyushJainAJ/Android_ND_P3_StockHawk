

package ayush.stockhawk.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import ayush.stockhawk.R;
import ayush.stockhawk.database.QuoteColumns;
import ayush.stockhawk.Utils;
import ayush.stockhawk.service.stockhistory.HistoricData;
import ayush.stockhawk.service.stockhistory.StockMeta;
import ayush.stockhawk.service.stockhistory.StockSymbol;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

public class StockDetails extends AppCompatActivity implements HistoricData.HistoricalDataCallback {

    private static final String TAG = StockDetails.class.getSimpleName();

    private HistoricData historicData;
    private ArrayList<StockSymbol> stockSymbols;

    @Bind(R.id.lineChart)
    public LineChart lineChart;

    @Bind(R.id.ll_stock_details)
    public LinearLayout linearLayout;

    @Bind(R.id.tvStockName)
    public TextView stockName;

    @Bind(R.id.tvStockSymbol)
    public TextView stockSymbol;

    @Bind(R.id.tvfirstTrade)
    public TextView firstTrade;

    @Bind(R.id.tvlastTrade)
    public TextView lastTrade;

    @Bind(R.id.tvCurrency)
    public TextView currency;

    @Bind(R.id.tvBidPrice)
    public TextView tvBidPrice;

    @Bind(R.id.tvExchangeName)
    public TextView exchangeName;

    private String symbol;
    private String bidPrice;

    private ActionBar actionBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_details);

        ButterKnife.bind(this);

        lineChart.setNoDataText(getString(R.string.loading_stock_data));

        symbol = getIntent().getStringExtra(QuoteColumns.SYMBOL);
        bidPrice = getIntent().getStringExtra(QuoteColumns.BIDPRICE);

        stockSymbol.setText(symbol);
        tvBidPrice.setText(bidPrice);

        actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        actionBar.setTitle(String.format(getString(R.string.symbol_details), symbol));

        historicData = new HistoricData(this, this);

        if (Utils.isNetworkAvailable(this)) {
            historicData.getHistoricData(symbol);
        } else {
            historicData.setHistoricalDataStatus(HistoricData.STATUS_ERROR_NO_NETWORK);
            onFailure();
        }

    }

    @Override
    public void onSuccess(StockMeta stockMeta) {
        this.stockSymbols = stockMeta.stockSymbols;

        stockName.setText(stockMeta.companyName);
        firstTrade.setText(Utils.convertDate(stockMeta.firstTrade));
        lastTrade.setText(Utils.convertDate(stockMeta.lastTrade));
        currency.setText(stockMeta.currency);
        exchangeName.setText(stockMeta.exchangeName);

        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> xvalues = new ArrayList<>();

        for (int i = 0; i < this.stockSymbols.size(); i++) {

            StockSymbol stockSymbol = this.stockSymbols.get(i);
            double yValue = stockSymbol.close;

            xvalues.add(Utils.convertDate(stockSymbol.date));
            entries.add(new Entry((float) yValue, i));
        }

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(10f);
        YAxis left = lineChart.getAxisLeft();
        left.setEnabled(true);
        left.setLabelCount(5, true);

        xAxis.setTextColor(Color.WHITE);
        left.setTextColor(Color.WHITE);

        lineChart.getAxisRight().setEnabled(false);

        lineChart.getLegend().setTextSize(12f);

        LineDataSet dataSet = new LineDataSet(entries, symbol);
        LineData lineData = new LineData(xvalues, dataSet);

        dataSet.setColor(Color.WHITE);
        dataSet.setValueTextColor(Color.WHITE);
        lineData.setValueTextColor(Color.WHITE);
        lineChart.setDescriptionColor(Color.WHITE);

        lineData.setDrawValues(false);
        dataSet.setDrawCircles(false);

        lineChart.setDescription(getString(R.string.last_12month_comparision));
        lineChart.setData(lineData);
        lineChart.animateX(3000);
    }

    @Override
    public void onFailure() {
        String errorMessage = "";

        @HistoricData.HistoricalDataStatuses
        int status = PreferenceManager.getDefaultSharedPreferences(this)
                .getInt(HistoricData.HISTORICAL_DATA_STATUS, -1);

        switch (status) {
            case HistoricData.STATUS_ERROR_JSON:
                errorMessage += getString(R.string.data_error_json);
                break;
            case HistoricData.STATUS_ERROR_NO_NETWORK:
                errorMessage += getString(R.string.data_no_internet);
                break;
            case HistoricData.STATUS_ERROR_PARSE:
                errorMessage += getString(R.string.data_error_parse);
                break;
            case HistoricData.STATUS_ERROR_UNKNOWN:
                errorMessage += getString(R.string.data_unknown_error);
                break;
            case HistoricData.STATUS_ERROR_SERVER:
                errorMessage += getString(R.string.data_server_down);
                break;
            case HistoricData.STATUS_OK:
                errorMessage += getString(R.string.data_no_error);
                break;
            default:
                break;
        }

        lineChart.setNoDataText(errorMessage);

        final Snackbar snackbar = Snackbar
                .make(linearLayout, getString(R.string.no_data_show) + errorMessage, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.retry, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        historicData.getHistoricData(symbol);
                    }
                })
                .setActionTextColor(Color.GREEN);

        View subview = snackbar.getView();
        TextView tv = (TextView) subview.findViewById(android.support.design.R.id.snackbar_text);
        tv.setTextColor(Color.RED);
        snackbar.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
