package com.colaorange.dailymoney.context;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.colaorange.commons.util.CalendarHelper;
import com.colaorange.commons.util.I18N;
import com.colaorange.commons.util.Logger;

/**
 * provide life cycle and easy access to contexts
 *
 * @author dennis
 */
public class ContextsActivity extends AppCompatActivity {

    public static final String PARAM_TITLE = "activity.title";

    protected I18N i18n;
    protected CalendarHelper calHelper;


    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Logger.d("activity created:" + this);

        refreshUtil();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        for (int g : grantResults) {
            if (g == PackageManager.PERMISSION_GRANTED) {
                //simply reload this activie
                makeRestart();
                break;
            }
        }
    }

    protected void makeRestart() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    protected void trackEvent(String action) {
        Contexts ctxs = Contexts.instance();
        ctxs.trackEvent(getTrackerPath(), action, "", null);
    }

    private void refreshUtil() {
        i18n = contexts().getI18n();
        calHelper = contexts().getCalendarHelper();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Logger.d("activity destroyed:" + this);
    }

    @SuppressWarnings("rawtypes")
    private String getTrackerPath() {
        Class clz = getClass();
        String name = clz.getSimpleName();
        String pkg = clz.getPackage() == null ? "" : clz.getPackage().getName();
        StringBuilder sb = new StringBuilder("/a/");
        int i;
        if ((i = pkg.lastIndexOf('.')) != -1) {
            pkg = pkg.substring(i + 1);
        }
        sb.append(pkg).append(".").append(name);
        return sb.toString();
    }


    @Override
    protected void onResume() {
        super.onResume();

        Bundle b = getIntentExtras();
        String t = b.getString(PARAM_TITLE);
        if (t != null) {
            setTitle(t);
        }

        refreshUtil();
    }


    Bundle fakeExtra;

    protected Bundle getIntentExtras() {
        if (getIntent() != null && getIntent().getExtras() != null) {
            return getIntent().getExtras();
        }
        // if extra is null;
        if (fakeExtra == null) {
            fakeExtra = new Bundle();
        }
        return fakeExtra;
    }

    protected Contexts contexts() {
        return Contexts.instance();
    }


}
