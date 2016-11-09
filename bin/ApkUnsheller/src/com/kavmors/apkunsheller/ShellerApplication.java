package com.kavmors.apkunsheller;

import android.app.Application;
import android.content.Context;

public class ShellerApplication extends Application {
    private Unsheller unsheller = new Unsheller(this);

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        unsheller.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        unsheller.onCreate();
    }
}
