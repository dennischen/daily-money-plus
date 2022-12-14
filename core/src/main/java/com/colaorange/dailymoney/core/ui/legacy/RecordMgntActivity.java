package com.colaorange.dailymoney.core.ui.legacy;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.colaorange.commons.util.CalendarHelper;
import com.colaorange.dailymoney.core.R;
import com.colaorange.dailymoney.core.context.Contexts;
import com.colaorange.dailymoney.core.context.ContextsActivity;
import com.colaorange.dailymoney.core.context.EventQueue;
import com.colaorange.dailymoney.core.context.PeriodMode;
import com.colaorange.dailymoney.core.context.RecordTemplate;
import com.colaorange.dailymoney.core.context.RecordTemplateCollection;
import com.colaorange.dailymoney.core.data.Record;
import com.colaorange.dailymoney.core.ui.Constants;
import com.colaorange.dailymoney.core.ui.QEvents;
import com.colaorange.dailymoney.core.ui.GUIs;
import com.colaorange.dailymoney.core.util.I18N;
import com.colaorange.dailymoney.core.util.Logger;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author dennis
 */
public class RecordMgntActivity extends ContextsActivity implements EventQueue.EventListener {

    public static final String ARG_PERIOD_MODE = "periodMode";
    public static final String ARG_BASE_DATE = "baseData";

    private ViewPager vPager;
    private RecordPagerAdapter adapter;

    private Date baseDate;

    private PeriodMode periodMode;

    private Map<Integer, RecordMgntFragment.FragInfo> fragInfoMap;

    private ActionMode actionMode;
    private Record actionObj;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record_mgnt);
        initArgs();
        initMembers();
        refreshToolbar();
        resetPager();
    }

    @Override
    public void onStart() {
        lookupQueue().subscribe(this);
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        lookupQueue().unsubscribe(this);
    }

    private void initArgs() {
        Bundle args = getIntentExtras();
        periodMode = (PeriodMode) args.getSerializable(ARG_PERIOD_MODE);
        if (periodMode == null) {
            periodMode = PeriodMode.MONTHLY;
        }
        Object o = args.get(ARG_BASE_DATE);
        if (o instanceof Date) {
            baseDate = (Date) o;
        } else {
            baseDate = new Date();
        }
    }

    private void initMembers() {

        vPager = findViewById(R.id.viewpager);
        vPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                publishClearSelection();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        fragInfoMap = new LinkedHashMap<>();
    }


    private void doSelectRecord(Record record) {
        if (record == null && actionMode != null) {
            actionMode.finish();
            return;
        }

        if (record != null) {
            actionObj = record;
            if (actionMode == null) {
                actionMode = this.startSupportActionMode(new RecordActionModeCallback());
            } else {
                actionMode.invalidate();
            }
            actionMode.setTitle(Contexts.instance().toFormattedMoneyString(record.getMoney()));
        }

    }


    private void refreshToolbar() {
//        toolbarView.setVisibility(TextView.VISIBLE);
//        btnMode.setVisibility(ImageButton.VISIBLE);

        switch (periodMode) {
            case ALL:
//                toolbarView.setVisibility(TextView.GONE);
                setTitle(R.string.label_all);
                break;
            case WEEKLY:
                setTitle(R.string.nav_pg_weekly_list);
                break;
            case DAILY:
                setTitle(R.string.nav_pg_daily_list);
                break;
            case YEARLY:
                setTitle(R.string.nav_pg_yearly_list);
                break;
            case MONTHLY:
            default:
                setTitle(R.string.nav_pg_monthly_list);
                break;
        }
    }

    private void resetPager() {
        fragInfoMap.clear();

        adapter = new RecordPagerAdapter(getSupportFragmentManager());
        vPager.setAdapter(adapter);
        vPager.setCurrentItem(adapter.getBasePos());

        trackEvent(TE.RECORD + periodMode);
    }

    private void doChangeMode() {
        switch (periodMode) {
            case ALL:
                periodMode = PeriodMode.ALL;//not switchable
                break;
            case WEEKLY:
                periodMode = PeriodMode.MONTHLY;
                break;
            case DAILY:
                periodMode = PeriodMode.WEEKLY;
                break;
            case MONTHLY:
                periodMode = PeriodMode.YEARLY;
                break;
            case YEARLY:
                periodMode = PeriodMode.DAILY;
                break;
        }
        refreshToolbar();
        resetPager();
    }

    private void doNext() {
        int c = vPager.getCurrentItem();
        if (c < adapter.getCount() - 1) {
            vPager.setCurrentItem(c + 1);
        }
    }

    private void doPrev() {
        int c = vPager.getCurrentItem();
        if (c > 0) {
            vPager.setCurrentItem(c - 1);
        }
    }

    private void doGoToday() {
        vPager.setCurrentItem(adapter.getBasePos(), true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.record_mgnt_menu, menu);

        menu.findItem(R.id.menu_slide_hint).setVisible(!preference().checkEver(Constants.Hint.RECORD_SLIDE, false));

        //poi doesn't work before 5.0 (xml parser error)
        menu.findItem(R.id.menu_export_excel).setVisible(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.menu_new) {
            doNewRecord();
            return true;
        } else if (item.getItemId() == R.id.menu_go_today) {
            doGoToday();
        } else if (item.getItemId() == R.id.menu_change_mode) {
            doChangeMode();
        } else if (item.getItemId() == R.id.menu_slide_hint) {
            preference().checkEver(Constants.Hint.RECORD_SLIDE, true);
            GUIs.shortToast(this, i18n().string(R.string.msg_slide_hint));
            doPrev();
            GUIs.delayPost(new Runnable() {
                @Override
                public void run() {
                    doNext();
                }
            }, 400);
        } else if (item.getItemId() == R.id.menu_export_excel) {
            lookupQueue().publish(new EventQueue.EventBuilder(QEvents.RecordListFrag.ON_EXPORT_EXCEL).withArg(BalanceMgntFragment.ARG_POS, vPager.getCurrentItem()).build());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void doNewRecord() {
        RecordMgntFragment.FragInfo fragInfo = fragInfoMap.get(vPager.getCurrentItem());
        if (fragInfo == null) {
            Logger.w("fragInfo is null on {}", vPager.getCurrentItem());
            return;
        }
        Intent intent = null;
        intent = new Intent(this, RecordEditorActivity.class);
        intent.putExtra(RecordEditorActivity.ARG_MODE_CREATE, true);
        intent.putExtra(RecordEditorActivity.ARG_CREATED_DATE, fragInfo.date);
        startActivityForResult(intent, Constants.REQUEST_RECORD_EDITOR_CODE);
    }

    @Override
    public void onEvent(EventQueue.Event event) {
        switch (event.getName()) {
            case QEvents.RecordListFrag.ON_SELECT_RECORD:
                doSelectRecord((Record) event.getData());
                break;
            case QEvents.RecordListFrag.ON_RESELECT_RECORD:
                doEditRecord((Record) event.getData());
                break;
            case QEvents.RecordMgntFrag.ON_FRAGMENT_START:
                RecordMgntFragment.FragInfo info = (RecordMgntFragment.FragInfo) event.getData();
                fragInfoMap.put(info.pos, info);

                break;
            case QEvents.RecordMgntFrag.ON_FRAGMENT_STOP:
                fragInfoMap.remove(event.getData());

                break;
        }
    }

    private void doEditRecord(Record record) {
        Intent intent = null;
        intent = new Intent(this, RecordEditorActivity.class);
        intent.putExtra(RecordEditorActivity.ARG_MODE_CREATE, false);
        intent.putExtra(RecordEditorActivity.ARG_RECORD, record);
        startActivityForResult(intent, Constants.REQUEST_RECORD_EDITOR_CODE);
    }

    public void doDeleteRecord(final Record record) {
        GUIs.confirm(this, i18n().string(R.string.qmsg_delete_record, Contexts.instance().toFormattedMoneyString(record.getMoney())), new GUIs.OnFinishListener() {
            public boolean onFinish(int which, Object data) {
                if (which == GUIs.OK_BUTTON) {
                    boolean r = Contexts.instance().getDataProvider().deleteRecord(record.getId());
                    if (r) {
                        if (record.equals(actionObj)) {
                            if (actionMode != null) {
                                actionMode.finish();
                            }
                        }
                        GUIs.shortToast(RecordMgntActivity.this, i18n().string(R.string.msg_record_deleted));
                        publishReloadFragment();
                        trackEvent(TE.DELETE_RECORD);
                    }
                }
                return true;
            }
        });


    }


    public void doCopyRecord(final Record record) {
        Intent intent = null;
        intent = new Intent(this, RecordEditorActivity.class);
        intent.putExtra(RecordEditorActivity.ARG_MODE_CREATE, true);
        intent.putExtra(RecordEditorActivity.ARG_RECORD, record);
        startActivityForResult(intent, Constants.REQUEST_RECORD_EDITOR_CODE);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REQUEST_RECORD_EDITOR_CODE && resultCode == Activity.RESULT_OK) {
            GUIs.delayPost(new Runnable() {
                @Override
                public void run() {
                    //user might add record, reload it.
                    publishReloadFragment();

                    //refresh action mode
                    if (actionMode != null) {
                        actionObj = contexts().getDataProvider().findRecord(actionObj.getId());
                        if (actionObj == null) {
                            actionMode.finish();
                        } else {
                            actionMode.setTitle(Contexts.instance().toFormattedMoneyString(actionObj.getMoney()));
                        }
                    }
                }
            });
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void publishReloadFragment() {
        lookupQueue().publish(new EventQueue.EventBuilder(QEvents.RecordMgntFrag.ON_RELOAD_FRAGMENT).build());
    }

    private void publishClearSelection() {
        lookupQueue().publish(new EventQueue.EventBuilder(QEvents.RecordListFrag.ON_CLEAR_SELECTION).build());
    }

    public class RecordPagerAdapter extends FragmentPagerAdapter {


        int basePos;
        int maxPos;

        public RecordPagerAdapter(FragmentManager fm) {
            super(fm);
            calculatePos();
        }

        private void calculatePos() {

            CalendarHelper calHelper = calendarHelper();

            Calendar cal0 = calHelper.calendar(new Date(0));
            Calendar calbase = calHelper.calendar(baseDate);

            int diffYear = calbase.get(Calendar.YEAR) - cal0.get(Calendar.YEAR) - 1; //-1 for possible bundory issue

            if (PeriodMode.MONTHLY.equals(periodMode)) {
                basePos = diffYear * 12 + calbase.get(Calendar.MONTH) - cal0.get(Calendar.MONTH);
                maxPos = basePos + Constants.MONTH_LOOK_AFTER;
            } else if (PeriodMode.WEEKLY.equals(periodMode)) {
                basePos = diffYear * 52 + calbase.get(Calendar.WEEK_OF_YEAR) - cal0.get(Calendar.WEEK_OF_YEAR);
                maxPos = basePos + Constants.WEEK_LOOK_AFTER;
            } else if (PeriodMode.DAILY.equals(periodMode)) {
                basePos = diffYear * 365 + calbase.get(Calendar.DAY_OF_YEAR) - cal0.get(Calendar.DAY_OF_YEAR);
                maxPos = basePos + Constants.DAY_LOOK_AFTER;
            } else if (PeriodMode.YEARLY.equals(periodMode)) {
                basePos = diffYear;
                maxPos = basePos + Constants.MONTH_LOOK_AFTER;
            } else {
                basePos = maxPos = 0;
            }

        }

        public int getBasePos() {
            return basePos;
        }

        @Override
        public int getCount() {
            return maxPos + 1;
        }

        @Override
        public Fragment getItem(int position) {
            CalendarHelper calHelper = calendarHelper();
            Date targetDate;

            int diff = position - basePos;
            if (PeriodMode.MONTHLY.equals(periodMode)) {
                targetDate = calHelper.monthAfter(baseDate, diff);
            } else if (PeriodMode.WEEKLY.equals(periodMode)) {
                targetDate = calHelper.dateAfter(baseDate, diff * 7);
            } else if (PeriodMode.DAILY.equals(periodMode)) {
                targetDate = calHelper.dateAfter(baseDate, diff);
            } else if (PeriodMode.YEARLY.equals(periodMode)) {
                targetDate = calHelper.yearAfter(baseDate, diff);
            } else {
                targetDate = baseDate;
            }

            RecordMgntFragment f = new RecordMgntFragment();
            Bundle b = new Bundle();
            b.putInt(RecordMgntFragment.ARG_POS, position);
            b.putSerializable(RecordMgntFragment.ARG_PERIOD_MODE, periodMode);
            b.putSerializable(RecordMgntFragment.ARG_TARGET_DATE, targetDate);
            f.setArguments(b);
            return f;
        }

    }


    private class RecordActionModeCallback implements ActionMode.Callback {

        //onCreateActionMode(ActionMode, Menu) once on initial creation.
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.record_mgnt_item_menu, menu);//Inflate the menu over action mode
            return true;
        }

        //onPrepareActionMode(ActionMode, Menu) after creation and any time the ActionMode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {

            //Sometimes the meu will not be visible so for that we need to set their visibility manually in this method
            //So here show action menu according to SDK Levels

            if (RecordMgntActivity.this.periodMode == PeriodMode.ALL) {
                //TODO
            }


            return true;
        }

        //onActionItemClicked(ActionMode, MenuItem) any time a contextual action button is clicked.
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.menu_edit) {
                doEditRecord(actionObj);
                return true;
            } else if (item.getItemId() == R.id.menu_delete) {
                doDeleteRecord(actionObj);
                mode.finish();//Finish action mode
                return true;
            } else if (item.getItemId() == R.id.menu_copy) {
                doCopyRecord(actionObj);
                return true;
            } else if (item.getItemId() == R.id.menu_set_template) {
                doSetTemplate(actionObj);
            }
            return false;
        }

        //onDestroyActionMode(ActionMode) when the action mode is closed.
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            //When action mode destroyed remove selected selections and set action mode to null
            //First check current fragment action mode
            actionMode = null;
            actionObj = null;
            publishClearSelection();
        }
    }

    private void doSetTemplate(final Record actionObj) {

        I18N i18n = i18n();
        List<String> items = new LinkedList<>();
        RecordTemplateCollection col = preference().getRecordTemplates();
        String nodata = i18n.string(R.string.msg_no_data);
        for (int i = 0; i < col.size(); i++) {
            RecordTemplate t = col.getTemplateIfAny(i);
            items.add((i + 1) + ". " + (t == null ? nodata : (t.toString(i18n))));
        }

        new AlertDialog.Builder(this).setTitle(i18n.string(R.string.qmsg_set_tempalte))
                .setItems(items.toArray(new String[items.size()]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int which) {

                        RecordTemplateCollection col = preference().getRecordTemplates();
                        col.setTemplate(which, actionObj.getFrom(), actionObj.getTo(), actionObj.getNote());
                        preference().updateRecordTemplates(col);
                        trackEvent(TE.SET_TEMPLATE + which);
                    }
                }).show();
    }
}
