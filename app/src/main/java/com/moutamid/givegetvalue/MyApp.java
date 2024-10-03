package com.moutamid.givegetvalue;

import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Stash.init(this);
        FirebaseApp.initializeApp(this);
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);

    }
}
