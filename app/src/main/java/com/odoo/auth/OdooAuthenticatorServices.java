package com.odoo.auth;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by sha on 19/4/16.
 */
public class OdooAuthenticatorServices extends Service {

    private static final Object mAuthenticatorLock = new Object();
    private OdooAuthenticator authenticator;

    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (mAuthenticatorLock) {
            if (authenticator == null) {
                authenticator = new OdooAuthenticator(getApplicationContext());
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return authenticator.getIBinder();
    }
}