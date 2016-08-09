package ayush.stockhawk;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import ayush.stockhawk.database.QuoteColumns;
import ayush.stockhawk.database.QuoteProvider;
import ayush.stockhawk.assist.ItemTouchHelperAdapter;
import ayush.stockhawk.assist.ItemTouchHelperViewHolder;

public class QuoteCursorAdapter extends CursorRecyclerViewAdapter<QuoteCursorAdapter.ViewHolder>
        implements ItemTouchHelperAdapter {

    private static Context mContext;
    private static Typeface mRobotoLight;

    public QuoteCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor);
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mRobotoLight = Typeface.createFromAsset(mContext.getAssets(), "fonts/Roboto-Light.ttf");

        return new ViewHolder(LayoutInflater.from(parent.getContext()).
                inflate(R.layout.list_item_quote, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final Cursor cursor) {
        viewHolder.symbol.setText(cursor.getString(cursor.getColumnIndex("symbol")));
        viewHolder.bidPrice.setText(cursor.getString(cursor.getColumnIndex("bid_price")));

        setViewHolderValue(viewHolder, cursor.getInt(cursor.getColumnIndex("is_up")) == 1);

        viewHolder.change.setText(cursor.getString(cursor.getColumnIndex(Utils.showPercent ? "percent_change"
                : "change")));
    }

    @Override
    public void onItemDismiss(int position) {
        Cursor c = getCursor();
        c.moveToPosition(position);
        String symbol = c.getString(c.getColumnIndex(QuoteColumns.SYMBOL));
        mContext.getContentResolver().delete(QuoteProvider.Quotes.withSymbol(symbol), null, null);
        notifyItemRemoved(position);
    }

    @Override
    public int getItemCount() {
        return super.getItemCount();
    }

    private void setViewHolderValue(ViewHolder viewHolder, boolean isUp) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            viewHolder.change.setBackgroundDrawable(
                    mContext.getResources().getDrawable(isUp ? R.drawable.percent_change_pill_green :
                            R.drawable.percent_change_pill_red));
        } else {
            viewHolder.change.setBackground(
                    mContext.getResources().getDrawable(isUp ? R.drawable.percent_change_pill_green
                            : R.drawable.percent_change_pill_red));
        }
    }


    public static class ViewHolder extends RecyclerView.ViewHolder
            implements ItemTouchHelperViewHolder, View.OnClickListener {
        public final TextView symbol;
        public final TextView bidPrice;
        public final TextView change;

        public ViewHolder(View itemView) {
            super(itemView);
            symbol = (TextView) itemView.findViewById(R.id.stock_symbol);
            symbol.setTypeface(mRobotoLight);
            bidPrice = (TextView) itemView.findViewById(R.id.bid_price);
            change = (TextView) itemView.findViewById(R.id.change);
        }

        @Override
        public void onItemSelected() {
            itemView.setBackgroundColor(Color.LTGRAY);
        }

        @Override
        public void onItemClear() {
            itemView.setBackgroundColor(0);
        }

        @Override
        public void onClick(View v) {
        }
    }
}
