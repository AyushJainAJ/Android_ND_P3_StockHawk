package ayush.stockhawk;

import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.support.v7.widget.RecyclerView;

public abstract class CursorRecyclerViewAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    private static final String LOG_TAG = CursorRecyclerViewAdapter.class.getSimpleName();
    private Cursor mCursor;
    private boolean mDataIsValid;
    private int mRowIdColumn;
    private DataSetObserver mDataSetObserver;

    public CursorRecyclerViewAdapter(Context context, Cursor cursor) {
        mCursor = cursor;
        mDataIsValid = cursor != null;
        mRowIdColumn = mDataIsValid ? mCursor.getColumnIndex("_id") : -1;
        mDataSetObserver = new NotifyingDataSetObserver();

        if (mDataIsValid) {
            mCursor.registerDataSetObserver(mDataSetObserver);
        }
    }

    public Cursor getCursor() {
        return mCursor;
    }

    @Override
    public int getItemCount() {
        return mDataIsValid && mCursor != null ? mCursor.getCount() : 0;
    }

    @Override
    public long getItemId(int position) {
        return mDataIsValid && mCursor != null && mCursor.moveToPosition(position) ?
                mCursor.getLong(mRowIdColumn) : 0;
    }

    @Override
    public void setHasStableIds(boolean hasStableIds) {
        super.setHasStableIds(true);
    }

    public abstract void onBindViewHolder(VH viewHolder, Cursor cursor);

    @Override
    public void onBindViewHolder(VH viewHolder, int position) {

        if (!mDataIsValid) {
            throw new IllegalStateException("Invalid cursor");
        }

        if (!mCursor.moveToPosition(position)) {
            throw new IllegalStateException("Cursor cannot be moved to position: " + position);
        }

        onBindViewHolder(viewHolder, mCursor);
    }

    public Cursor swapCursor(Cursor newCursor) {

        if (newCursor == mCursor) {
            return null;
        }

        final Cursor oldCursor = mCursor;

        if (oldCursor != null && mDataSetObserver != null) {
            oldCursor.unregisterDataSetObserver(mDataSetObserver);
        }

        mCursor = newCursor;

        if (mCursor != null) {

            if (mDataSetObserver != null) {
                mCursor.registerDataSetObserver(mDataSetObserver);
            }

            mRowIdColumn = newCursor.getColumnIndexOrThrow("_id");
            mDataIsValid = true;
            notifyDataSetChanged();
        } else {
            mRowIdColumn = -1;
            mDataIsValid = false;
            notifyDataSetChanged();
        }

        return oldCursor;
    }

    private class NotifyingDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            super.onChanged();
            mDataIsValid = true;
            notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            mDataIsValid = false;
            notifyDataSetChanged();
        }
    }
}