/*
 * Copyright 2016 Rohit Sharma (skyrohithigh)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ayush.stockhawk.service.stockhistory;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by rohit on 7/8/16.
 */
public class StockSymbol implements Parcelable {
    public String date;
    public double close;

    public StockSymbol(String date, double close) {
        this.date = date;
        this.close = close;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.date);
        dest.writeDouble(this.close);
    }

    public StockSymbol() {
    }

    protected StockSymbol(Parcel in) {
        this.date = in.readString();
        this.close = in.readDouble();
    }

    public static final Parcelable.Creator<StockSymbol> CREATOR = new Parcelable.Creator<StockSymbol>() {
        @Override
        public StockSymbol createFromParcel(Parcel source) {
            return new StockSymbol(source);
        }

        @Override
        public StockSymbol[] newArray(int size) {
            return new StockSymbol[size];
        }
    };
}
