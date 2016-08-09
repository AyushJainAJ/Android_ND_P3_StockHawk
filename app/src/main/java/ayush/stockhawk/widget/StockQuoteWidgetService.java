

package ayush.stockhawk.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;


public class StockQuoteWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsService.RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StockQuoteWidgetFactory(getApplicationContext(),intent);
    }
}
