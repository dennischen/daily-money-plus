package com.colaorange.dailymoney.ui.legacy;


import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.colaorange.dailymoney.context.Contexts;
import com.colaorange.dailymoney.R;
import com.colaorange.dailymoney.ui.Constants;

/**
 * 
 * @author dennis
 *
 */
public class PrefsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    boolean dirty = false;
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.prefs);
    }
    
    protected void onResume(){
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }
    
    protected void onPause(){
        super.onPause();
        if(dirty){
            Contexts.instance().reloadPreference();
        }
        dirty = false;
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        dirty = true;
    }
}
