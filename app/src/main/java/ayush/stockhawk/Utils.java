package ayush.stockhawk;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import ayush.stockhawk.database.QuoteColumns;
import ayush.stockhawk.database.QuoteProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class Utils {

    public static boolean showPercent = true;

    public static ArrayList quoteJsonToContentVals(String JSON) {

        JSONObject jsonObject;

        try {
            jsonObject = new JSONObject(JSON);

            if (jsonObject != null && jsonObject.length() != 0) {
                return parseJSON(jsonObject);
            }
        } catch (JSONException e) {
        }
        return null;
    }

    public static String truncateBidPrice(String bidPrice) {
        bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));

        return bidPrice;
    }

    public static String truncateChange(String change, boolean isPercentChange) {
        String weight = change.substring(0, 1);
        String ampersand = "";

        if (isPercentChange) {
            ampersand = change.substring(change.length() - 1, change.length());
            change = change.substring(0, change.length() - 1);
        }

        change = change.substring(1, change.length());
        double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
        change = String.format("%.2f", round);
        StringBuffer changeBuffer = new StringBuffer(change);
        changeBuffer.insert(0, weight);
        changeBuffer.append(ampersand);
        change = changeBuffer.toString();

        return change;
    }

    public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject) {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                QuoteProvider.Quotes.CONTENT_URI);
        try {
            String change = jsonObject.getString("Change");
            builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol"));
            builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Bid")));
            builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
                    jsonObject.getString("ChangeinPercent"), true));
            builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
            builder.withValue(QuoteColumns.ISCURRENT, 1);

            if (change.charAt(0) == '-') {
                builder.withValue(QuoteColumns.ISUP, 0);
            } else {
                builder.withValue(QuoteColumns.ISUP, 1);
            }

        } catch (JSONException e) {

        }

        return builder.build();
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();

        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    public static String convertDate(String inputDate) {
        StringBuilder outputFormattedDate = new StringBuilder();
        outputFormattedDate.append(inputDate.substring(6))
                .append("/")
                .append(inputDate.substring(4, 6))
                .append("/")
                .append(inputDate.substring(2, 4));

        return outputFormattedDate.toString();
    }

    private static ArrayList parseJSON(JSONObject jsonObject) {

        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();

        try {
            jsonObject = jsonObject.getJSONObject("query");

            if (Integer.parseInt(jsonObject.getString("count")) == 1) {
                jsonObject = jsonObject.getJSONObject("results").getJSONObject("quote");

                //If invalid stock symbol
                if (jsonObject.getString("Bid") == null || jsonObject.getString("Bid").equals("null")) {
                    return null;

                } else {
                    batchOperations.add(buildBatchOperation(jsonObject));
                }
            } else {
                JSONArray resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

                if (resultsArray != null && resultsArray.length() != 0) {

                    for (int i = 0; i < resultsArray.length(); i++) {

                        jsonObject = resultsArray.getJSONObject(i);
                        batchOperations.add(buildBatchOperation(jsonObject));
                    }
                }
            }
        } catch (Exception e) {
        }

        return batchOperations;
    }
}
