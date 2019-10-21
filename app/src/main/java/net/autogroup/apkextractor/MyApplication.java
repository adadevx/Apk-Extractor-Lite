package net.autogroup.apkextractor;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.StrictMode;

import androidx.appcompat.app.AppCompatDelegate;

import net.autogroup.apkextractor.data.Constants;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences settings=Global.getGlobalSharedPreferences(this);
        int night_mode=settings.getInt(Constants.PREFERENCE_NIGHT_MODE,Constants.PREFERENCE_NIGHT_MODE_DEFAULT);
        AppCompatDelegate.setDefaultNightMode(night_mode);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
    }
}
