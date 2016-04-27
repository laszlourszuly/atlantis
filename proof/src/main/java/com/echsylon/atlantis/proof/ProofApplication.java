package com.echsylon.atlantis.proof;

import android.app.Application;
import android.util.Log;

import com.echsylon.atlantis.Atlantis;

public class ProofApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Atlantis.start(this, "localhost", 8080, "atlantis.json",
                new Atlantis.OnSuccessListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(getClass().getSimpleName(), "Atlantis loaded successfully");
                    }
                }, new Atlantis.OnErrorListener() {
                    @Override
                    public void onError(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });
    }

}
