package com.colaorange.dailymoney.core.context;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;

import com.colaorange.commons.util.CalendarHelper;
import com.colaorange.dailymoney.core.R;
import com.colaorange.dailymoney.core.data.AccountType;
import com.colaorange.dailymoney.core.ui.GUIs;
import com.colaorange.dailymoney.core.ui.StartupActivity;
import com.colaorange.dailymoney.core.util.I18N;
import com.colaorange.dailymoney.core.util.Logger;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * provide life cycle and easy access to contexts
 *
 * @author dennis
 */
@InstanceState(stopLookup = true)
public class ContextsActivity extends AppCompatActivity {

    public static final String ARG_TITLE = "activity.title";
    public static final String DEFAULT_EVENT_QUEUE = "default";

    private long onCreateTime;
    private static volatile long globalRecreateTimeMark;
    private static volatile boolean globalColdRestartMark;

    private Map<AccountType, Integer> accountTextColorMap;
    private Map<AccountType, Integer> accountBgColorMap;
    private int[] chartColorTemplate;

    private InstanceStateHelper instanceStateHelper;

    private Float dpRatio;

    private Boolean lightTheme;

    private ThemeApplier themeApplier;

    protected static final int homeAsUpNone = -1;
    protected int homeAsUpBackId;
    protected int homeAsUpAppId;

    protected int selectableBackgroundId;
    protected int selectedBackgroundColor;

    private Map<String, EventQueue> eventQueueMap;

    private boolean recreating;

    private boolean processing;


    public ContextsActivity() {
        instanceStateHelper = new InstanceStateHelper(this);
    }

    protected void markProcessing() {
        processing = true;
        GUIs.lockOrientation(this);
    }

    protected void unmarkProcessing() {
        processing = false;
        GUIs.releaseOrientation(this);
    }

    protected boolean isProcessing() {
        return processing;
    }

    @Override
    public void onBackPressed() {
        if (processing) {
            toastProcessing();
            return;
        }
        super.onBackPressed();
    }

    protected void toastProcessing() {
        GUIs.shortToast(this, i18n().string(R.string.msg_busy));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //for init ui related resource in ui thread
        GUIs.touch();

        //do before super on create;
        themeApplier = new ThemeApplier(this);
        themeApplier.applyTheme();
        homeAsUpBackId = resolveThemeAttrResId(R.attr.ic_arrow_back);
        homeAsUpAppId = resolveThemeAttrResId(R.attr.ic_apps);


        selectableBackgroundId = resolveThemeAttrResId(android.R.attr.selectableItemBackground);

        if (isLightTheme()) {
            selectedBackgroundColor = resolveThemeAttrResData(R.attr.appSecondaryLightColor);
        } else {
            selectedBackgroundColor = resolveThemeAttrResData(R.attr.appSecondaryDarkColor);
        }

        super.onCreate(savedInstanceState);
        instanceStateHelper.onRestore(savedInstanceState);


        Logger.d("activity created:" + this);
        onCreateTime = System.currentTimeMillis();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        instanceStateHelper.onBackup(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        instanceStateHelper.onRestore(savedInstanceState);
    }

    public void markWholeRecreate() {
        globalRecreateTimeMark = System.currentTimeMillis();
    }

    /*
     * for quick provide action bar in all sub class
     */
    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
        Toolbar toolbar = findViewById(R.id.appToolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            doInitActionBar(getSupportActionBar());
        }
    }


//    protected void restartApp(boolean passedProtection) {
//        //TODO passProtection
//        Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
//        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//
//        i = (Intent) i.clone();
////        String bypassId = Strings.randomUUID();
////        i.putExtra(StartupActivity.ARG_BYPASS_PROTECTION,true);
//
//        startActivity(i);
//    }

    protected void restartAppColdly() {


//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            Intent mStartActivity = new Intent(this, StartupActivity.class);
//            int mPendingIntentId = 123456;
//            PendingIntent mPendingIntent = PendingIntent.getActivity(this, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
//            AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
//            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
//            finishAndRemoveTask();
//        } else {
        globalColdRestartMark = true;
        finish();
//        }
    }

    public boolean isNoActionBarTheme() {
        return true;
    }

    public boolean isDialogTheme() {
        return false;
    }

    public int resolveThemeAttrResId(int attrId) {
        TypedValue attr = resolveThemeAttr(attrId);
        return attr.resourceId;
    }

    public int resolveThemeAttrResData(int attrId) {
        TypedValue attr = resolveThemeAttr(attrId);
        return attr.data;
    }

    public boolean resolveThemeAttrBoolean(int attrId) {
        TypedValue attr = resolveThemeAttr(attrId);
        return attr.data != 0;
    }

    public TypedValue resolveThemeAttr(int attrId) {
        Resources.Theme theme = getTheme();
        TypedValue attr = new TypedValue();
        theme.resolveAttribute(attrId, attr, true);
        return attr;
    }


    public void trackEvent(String action) {
        Contexts ctxs = Contexts.instance();
        ctxs.trackEvent(Contexts.getTrackerPath(getClass()), action, "", null);
    }

    protected I18N i18n() {
        return contexts().getI18n();
    }

    protected CalendarHelper calendarHelper() {
        return preference().getCalendarHelper();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Logger.d("activity destroyed:" + this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkGlobalRecreate();
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (globalColdRestartMark) {

            //pending restart
            Intent mStartActivity = new Intent(this, StartupActivity.class);
            int mPendingIntentId = 1;
            PendingIntent mPendingIntent = PendingIntent.getActivity(this, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);

            globalColdRestartMark = false;
            //force quit
            int pid = Process.myPid();
            Process.killProcess(pid);
            System.exit(0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Bundle b = getIntentExtras();
        String t = b.getString(ARG_TITLE);
        if (t != null) {
            setTitle(t);
        }

        checkGlobalRecreate();
    }

    private void checkGlobalRecreate() {
        if (globalRecreateTimeMark > onCreateTime) {
            recreate();
        }
    }

    public void recreate() {
        if (recreating) {
            return;
        }
        recreating = true;
        //since android will reuse fragment after you recreate activity (shit fragments)
        //it cause many bugs, so we give it a chance to remove all fragment when recreating,
        if (!handleRecreate()) {
            super.recreate();
        }
    }

    /**
     * @return true if you handle the recreate or false if didn't. default return false
     */
    protected boolean handleRecreate() {
        return false;
    }


    protected void makeGlobalRecreate() {
        globalRecreateTimeMark = System.currentTimeMillis();
    }

    protected boolean isRecreating() {
        return recreating;
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

    protected Preference preference() {
        return contexts().getPreference();
    }

    public Map<AccountType, Integer> getAccountTextColorMap() {
        if (accountTextColorMap != null) {
            return accountTextColorMap;
        }
        Map<AccountType, Integer> map = new HashMap<>();
        map.put(AccountType.INCOME, resolveThemeAttrResData(R.attr.accountIncomeTextColor));
        map.put(AccountType.EXPENSE, resolveThemeAttrResData(R.attr.accountExpenseTextColor));
        map.put(AccountType.ASSET, resolveThemeAttrResData(R.attr.accountAssetTextColor));
        map.put(AccountType.LIABILITY, resolveThemeAttrResData(R.attr.accountLiabilityTextColor));
        map.put(AccountType.OTHER, resolveThemeAttrResData(R.attr.accountOtherTextColor));
        map.put(AccountType.UNKONW, resolveThemeAttrResData(R.attr.accountUnknownTextColor));
        return this.accountTextColorMap = map;
    }

    public Map<AccountType, Integer> getAccountBgColorMap() {
        if (accountBgColorMap != null) {
            return accountBgColorMap;
        }
        Map<AccountType, Integer> map = new HashMap<>();
        map.put(AccountType.INCOME, resolveThemeAttrResData(R.attr.accountIncomeBgColor));
        map.put(AccountType.EXPENSE, resolveThemeAttrResData(R.attr.accountExpenseBgColor));
        map.put(AccountType.ASSET, resolveThemeAttrResData(R.attr.accountAssetBgColor));
        map.put(AccountType.LIABILITY, resolveThemeAttrResData(R.attr.accountLiabilityBgColor));
        map.put(AccountType.OTHER, resolveThemeAttrResData(R.attr.accountOtherBgColor));
        map.put(AccountType.UNKONW, resolveThemeAttrResData(R.attr.accountUnknownBgColor));
        return this.accountBgColorMap = map;
    }

    public float getDpRatio() {
        if (dpRatio == null) {
            dpRatio = GUIs.getDPRatio(this);
        }
        return dpRatio.floatValue();
    }

    public float getDpWidth() {
        //don't catch this, orientation imght change
        return GUIs.getDPWidth(this);
    }

    public float getDpHeight() {
        //don't catch this, orientation might change
        return GUIs.getDPHeight(this);
    }

    public boolean isLightTheme() {
        if (lightTheme == null) {
            lightTheme = resolveThemeAttrBoolean(R.attr.appIsLightTheme);
        }
        return lightTheme;
    }


    protected int getActionBarHomeAsUp() {
        return homeAsUpBackId;
    }

    protected void doInitActionBar(ActionBar supportActionBar) {
        int resId = getActionBarHomeAsUp();
        if (resId > 0) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
            supportActionBar.setHomeAsUpIndicator(resId);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //home button clicked
            case android.R.id.home:
                this.onActionBarHomeAsUp(getActionBarHomeAsUp());
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onActionBarHomeAsUp(int resId) {
        if (resId == homeAsUpBackId) {
            this.finish();
        } else if (resId == homeAsUpAppId) {
            //todo
        }
    }

    public void expandAppbar(boolean expand) {
        AppBarLayout appbar = findViewById(R.id.appbar);
        if (appbar != null) {
            appbar.setExpanded(expand, true);
        }
    }

    public void enableAppbarHideOnScroll(boolean enable) {
        setAppbarHideOnScrollFlag(enable ? AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS : 0);
    }

    public void setAppbarHideOnScrollFlag(int flag) {
        Toolbar toolbar = findViewById(R.id.appToolbar);
        if (toolbar != null) {
            AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) toolbar.getLayoutParams();
            params.setScrollFlags(flag);
        }
    }

    public int[] getChartColorTemplate() {
        if (chartColorTemplate == null) {

            //https://material.io/tools/color/
            String red, pink, purple, deeppurple, indigo, blue, lightblue, cyan, teal, green, lightgreen, lime;
            if (isLightTheme()) {
                //300
                red = "#E57373";
                pink = "#F06292";
                purple = "#BA68C8";
                deeppurple = "#9575CD";
                indigo = "#7986CB";
                blue = "#64B5F6";
                lightblue = "#4FC3F7";
                cyan = "#4DD0E1";
                teal = "#4DB6AC";
                green = "#81C784";
                lightgreen = "#AED581";
                lime = "#DCE775";
            } else {
                //700
                red = "#D32F2F";
                pink = "#C2185B";
                purple = "#7B1FA2";
                deeppurple = "#512DA8";
                indigo = "#303F9F";
                blue = "#1976D2";
                lightblue = "#0288D1";
                cyan = "#0097A7";
                teal = "#00796B";
                green = "#388E3C";
                lightgreen = "#689F38";
                lime = "#AFB42B";
            }

            chartColorTemplate = new int[]{
                    Color.parseColor(blue),
                    Color.parseColor(green),
                    Color.parseColor(purple),
                    Color.parseColor(lime),
                    Color.parseColor(red),
                    Color.parseColor(lightblue),
                    Color.parseColor(pink),
                    Color.parseColor(teal),
                    Color.parseColor(deeppurple),
                    Color.parseColor(cyan),
                    Color.parseColor(indigo),
                    Color.parseColor(lightgreen)
            };
        }
        return chartColorTemplate;
    }

    //shortcut
    public interface TE extends Contexts.TE {
    }

    /**
     * limited by getRrawable wihtout theme in our api level, I can just provide drawable id
     *
     * @return
     */
    public int getSelectableBackgroundId() {
        return selectableBackgroundId;
    }

    /**
     * @return a state drawable which focus on selected state
     */
    public Drawable getSelectedBackground() {
        StateListDrawable drawable = new StateListDrawable();
        //transparent mask for selecting ripple effect
        drawable.addState(new int[]{android.R.attr.state_selected},
                isLightTheme() ? new ColorDrawable(0xE0FFFFFF & selectedBackgroundColor) :
                        new ColorDrawable(0xC0FFFFFF & selectedBackgroundColor));
        return drawable;
    }

    /**
     * to get a drawable icon or build a lighten/darken icon when it is disabled
     */
    public Drawable buildDisabledIcon(int drawableResId, boolean enabled) {
        Drawable drawable = getResources().getDrawable(drawableResId);
        if (enabled) {
            return drawable;
        }

        drawable = drawable.mutate();
        if (isLightTheme()) {
            //porterduff
            //https://blog.csdn.net/t12x3456/article/details/10432935

            //we use lighting, not porterduff
            ColorFilter filter = new LightingColorFilter(0xFFFFFFFF, 0x005F5F5F); // lighten
            drawable.setColorFilter(filter);
        } else {
            ColorFilter filter = new LightingColorFilter(0xFF5F5F5F, 0x00000000);  // darken
            drawable.setColorFilter(filter);
        }
        return drawable;
    }

    public Drawable buildGrayIcon(int drawableResId, boolean skip) {
        Drawable drawable = getResources().getDrawable(drawableResId);
        if (skip) {
            return drawable;
        }

        drawable = drawable.mutate();

        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);

        drawable.setColorFilter(filter);
        return drawable;
    }

    /**
     * to get a drawable icon or build a lighten/darken icon when it is disabled
     */
    public Drawable buildNonSelectedIcon(int drawableResId, boolean selected) {
        Drawable drawable = getResources().getDrawable(drawableResId);
        if (selected) {
            return drawable;
        }

        drawable = drawable.mutate();
        if (isLightTheme()) {
            //https://blog.csdn.net/t12x3456/article/details/10432935
            drawable.setColorFilter(0xE0C0C0C0, PorterDuff.Mode.MULTIPLY);
        } else {
            drawable.setColorFilter(0xE0C0C0C0, PorterDuff.Mode.MULTIPLY);
        }
        return drawable;
    }

    private synchronized Map<String, EventQueue> getEventQueueMap() {
        if (eventQueueMap == null) {
            eventQueueMap = java.util.Collections.synchronizedMap(new HashMap<String, EventQueue>());
        }
        return eventQueueMap;
    }

    public EventQueue lookupQueue() {
        return lookupQueue(DEFAULT_EVENT_QUEUE);
    }

    public EventQueue lookupQueue(String queueName) {
        return new EventQueuePhantom(queueName);
    }

    private EventQueue lookupQueue(String queueName, boolean create) {
        Map<String, EventQueue> m = getEventQueueMap();
        EventQueue q = m.get(queueName);
        if (q == null && create) {
            synchronized (this) {
                q = m.get(queueName);
                if (q != null) {
                    return q;
                }
                m.put(queueName, q = new EventQueueImpl(queueName));
                Logger.d("Event queue '{}:{}' created", getClass().getSimpleName(), queueName);
            }
        }
        return q;
    }

    private class EventQueuePhantom implements EventQueue {

        String queueName;

        public EventQueuePhantom(String name) {
            this.queueName = name;
        }

        @Override
        public void subscribe(EventListener l) {
            lookupQueue(queueName, true).subscribe(l);
        }

        @Override
        public void unsubscribe(EventListener l) {
            EventQueue q = lookupQueue(queueName, false);
            if (q != null) {
                q.unsubscribe(l);
            }
        }

        @Override
        public void publish(Event event) {
            EventQueue q = lookupQueue(queueName, false);
            if (q != null) {
                q.publish(event);
            }
        }

        @Override
        public void publish(String name, Object data) {
            EventQueue q = lookupQueue(queueName, false);
            if (q != null) {
                q.publish(name, data);
            }
        }

        @Override
        public void publish(String name) {
            publish(name, null);
        }

    }

    private class EventQueueImpl implements EventQueue {

        String queueName;

        List<WeakReference<EventListener>> listeners = new LinkedList<>();

        public EventQueueImpl(String queueName) {
            this.queueName = queueName;
        }


        @Override
        public void subscribe(EventListener listener) {
            synchronized (listeners) {
                listeners.add(new WeakReference<EventListener>(listener));
                Logger.d("Event queue '{}:{}', subscriber {}", getClass().getSimpleName(), queueName, listener);
            }
        }

        @Override
        public void unsubscribe(EventListener listener) {
            trimOrUnsubscribe(listener);
        }

        private synchronized void trimOrUnsubscribe(EventListener listener) {
            Iterator<WeakReference<EventListener>> it = listeners.iterator();
            while (it.hasNext()) {
                WeakReference<EventListener> w = it.next();
                EventListener l = w.get();
                if (l == null || l == listener) {
                    it.remove();
                    Logger.d("Event queue '{}:{}', unsubscriber {}", getClass().getSimpleName(), queueName, l);
                }
            }
            if (listeners.size() == 0) {
                synchronized (this) {
                    getEventQueueMap().remove(queueName);
                    Logger.d("Event queue '{}:{}' destroyed", getClass().getSimpleName(), queueName);
                }
            }
        }

        @Override
        public void publish(Event event) {
            Logger.d("Receive event {} to queue '{}:{}'", event.getName(), getClass().getSimpleName(), queueName);

            trimOrUnsubscribe(null);
            List<EventListener> ls = new LinkedList<>();
            synchronized (listeners) {

                Iterator<WeakReference<EventListener>> it = listeners.iterator();
                while (it.hasNext()) {
                    WeakReference<EventListener> w = it.next();
                    EventListener l = w.get();
                    if (l == null) {
                        it.remove();
                    } else {
                        ls.add(l);
                    }
                }
            }

            for (EventListener l : ls) {
                Logger.d("> Sending event {} to listener {}", event.getName(), l);
                l.onEvent((Event) event);
            }
        }

        @Override
        public void publish(String name, Object data) {
            publish(new Event(name, data));
        }

        @Override
        public void publish(String name) {
            publish(name, null);
        }
    }


    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    /**
     * the method copy for @link {@link View#generateViewId()}, it is api > 17 only, but we use 15
     * Generate a value suitable for use in {@link View#setId(int)}.
     * This value will not collide with ID values generated at build time by aapt for R.id.
     *
     * @return a generated ID value
     */
    public static int generateViewId() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return View.generateViewId();
        }
        for (; ; ) {
            final int result = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }
}
