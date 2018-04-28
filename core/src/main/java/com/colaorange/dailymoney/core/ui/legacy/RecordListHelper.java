package com.colaorange.dailymoney.core.ui.legacy;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.colaorange.commons.util.CalendarHelper;
import com.colaorange.dailymoney.core.context.Preference;
import com.colaorange.dailymoney.core.data.Record;
import com.colaorange.dailymoney.core.util.I18N;
import com.colaorange.dailymoney.core.context.Contexts;
import com.colaorange.dailymoney.core.R;
import com.colaorange.dailymoney.core.data.Account;
import com.colaorange.dailymoney.core.data.AccountType;
import com.colaorange.dailymoney.core.data.IDataProvider;
import com.colaorange.dailymoney.core.ui.Constants;

/**
 * @author dennis
 */
public class RecordListHelper implements OnItemClickListener {

    private static String[] mappingKeys = new String[]{"layout", "layout_inner", "from", "to", "money", "note", "date"};

    private static int[] mappingResIds = new int[]{R.id.detail_mgnt_item_layout, R.id.detail_mgnt_item_layout_inner, R.id.detail_mgnt_item_from, R.id.detail_mgnt_item_to, R.id.detail_mgnt_item_money, R.id.detail_mgnt_item_note, R.id.detail_mgnt_item_date};


    private List<Record> listViewData = new ArrayList<Record>();

    private List<Map<String, Object>> listViewMapList = new ArrayList<Map<String, Object>>();

    private ListView listView;

    private SimpleAdapter listViewAdapter;

    private Map<String, Account> accountCache = new HashMap<String, Account>();

    private boolean clickeditable;

    private OnRecordListener listener;

    private Activity activity;

    public RecordListHelper(Activity activity, boolean clickeditable, OnRecordListener listener) {
        this.activity = activity;
        this.clickeditable = clickeditable;
        this.listener = listener;
    }


    public void setup(ListView listview) {

        int layout = 0;
        switch (Contexts.instance().getPreference().getDetailListLayout()) {
            case 2:
                layout = R.layout.record_list_item2;
                break;
            case 3:
                layout = R.layout.record_list_item3;
                break;
            case 4:
                layout = R.layout.record_list_item4;
                break;
            default:
                layout = R.layout.record_list_item1;
        }

        listViewAdapter = new SimpleAdapter(activity, listViewMapList, layout, mappingKeys, mappingResIds);
        listViewAdapter.setViewBinder(new ListViewBinder());

        listView = listview;
        listView.setAdapter(listViewAdapter);
        if (clickeditable) {
            listView.setOnItemClickListener(this);
        }

        IDataProvider idp = Contexts.instance().getDataProvider();
        for (Account acc : idp.listAccount(null)) {
            accountCache.put(acc.getId(), acc);
        }
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
        if (parent == listView) {
            doEditRecord(pos);
        }
    }


    public void reloadData(List<Record> data) {
        listViewData = data;
        listViewMapList.clear();
        Preference preference = Contexts.instance().getPreference();
        DateFormat dateFormat = preference.getDateFormat();//for 2010/01/01
        DateFormat weekDayFormat = preference.getWeekDayFormat();// Wed.
        for (Record det : listViewData) {
            Map<String, Object> row = toRecordMap(det, dateFormat, weekDayFormat);
            listViewMapList.add(row);
        }

        listViewAdapter.notifyDataSetChanged();
    }

    private Map<String, Object> toRecordMap(Record det, DateFormat dateFormat, DateFormat weekDayFormat) {
        CalendarHelper calHelper = Contexts.instance().getCalendarHelper();
        I18N i18n = Contexts.instance().getI18n();

        Map<String, Object> row = new HashMap<String, Object>();
        Account fromAcc = accountCache.get(det.getFrom());
        Account toAcc = accountCache.get(det.getTo());

        String from = fromAcc == null ? det.getFrom() : (i18n.string(R.string.label_reclist_from, fromAcc.getName(), AccountType.getDisplay(i18n, fromAcc.getType())));
        String to = toAcc == null ? det.getTo() : (i18n.string(R.string.label_reclist_to, toAcc.getName(), AccountType.getDisplay(i18n, toAcc.getType())));
        String money = Contexts.instance().toFormattedMoneyString(det.getMoney());
        row.put(mappingKeys[0], new NamedItem(mappingKeys[0], det, mappingKeys[0]));
        row.put(mappingKeys[1], new NamedItem(mappingKeys[1], det, mappingKeys[1]));
        row.put(mappingKeys[2], new NamedItem(mappingKeys[2], det, from));
        row.put(mappingKeys[3], new NamedItem(mappingKeys[3], det, to));
        row.put(mappingKeys[4], new NamedItem(mappingKeys[4], det, money));
        row.put(mappingKeys[5], new NamedItem(mappingKeys[5], det, det.getNote()));
        row.put(mappingKeys[6], new NamedItem(mappingKeys[6], det, dateFormat.format(det.getDate()) + " " + weekDayFormat.format(det.getDate())));

        return row;
    }

    public void doNewRecord() {
        doNewRecord(null);
    }

    public void doNewRecord(Date date) {
        Record d = new Record("", "", date == null ? new Date() : date, 0D, "");
        Intent intent = null;
        intent = new Intent(activity, RecordEditorActivity.class);
        intent.putExtra(RecordEditorActivity.PARAM_MODE_CREATE, true);
        intent.putExtra(RecordEditorActivity.PARAM_RECORD, d);
        activity.startActivityForResult(intent, Constants.REQUEST_RECORD_EDITOR_CODE);
    }


    public void doEditRecord(int pos) {
        Record d = listViewData.get(pos);
        Intent intent = null;
        intent = new Intent(activity, RecordEditorActivity.class);
        intent.putExtra(RecordEditorActivity.PARAM_MODE_CREATE, false);
        intent.putExtra(RecordEditorActivity.PARAM_RECORD, d);
        activity.startActivityForResult(intent, Constants.REQUEST_RECORD_EDITOR_CODE);
    }

    public void doDeleteRecord(int pos) {
        Record d = listViewData.get(pos);
        boolean r = Contexts.instance().getDataProvider().deleteRecord(d.getId());
        if (r) {
            if (listener != null) {
                listener.onRecordDeleted(d);
            } else {
                listViewData.remove(pos);
                listViewMapList.remove(pos);
                listViewAdapter.notifyDataSetChanged();
            }
        }
    }


    public void doCopyRecord(int pos) {
        Record d = listViewData.get(pos);
        Intent intent = null;
        intent = new Intent(activity, RecordEditorActivity.class);
        intent.putExtra(RecordEditorActivity.PARAM_MODE_CREATE, true);
        intent.putExtra(RecordEditorActivity.PARAM_RECORD, d);
        activity.startActivityForResult(intent, Constants.REQUEST_RECORD_EDITOR_CODE);
    }


    public interface OnRecordListener {
        void onRecordDeleted(Record record);
    }

    class ListViewBinder implements SimpleAdapter.ViewBinder {
        AccountType last = null;
        AccountType lastFrom = null;
        AccountType lastTo = null;

        @Override
        public boolean setViewValue(View view, Object data, String text) {
            NamedItem item = (NamedItem) data;
            String name = item.getName();
            Record record = (Record) item.getValue();

            CalendarHelper calHelper = Contexts.instance().getCalendarHelper();

            if ("layout".equals(name)) {

                RelativeLayout layout = (RelativeLayout) view;

                Account fromAcc = accountCache.get(record.getFrom());
                Account toAcc = accountCache.get(record.getTo());
                int flag = 0;
                if (toAcc != null) {
                    if (AccountType.EXPENSE.getType().equals(toAcc.getType())) {
                        flag |= 1;
                    } else if (AccountType.INCOME.getType().equals(toAcc.getType())) {
                        flag |= 2;
                    } else if (AccountType.ASSET.getType().equals(toAcc.getType())) {
                        flag |= 4;
                    } else if (AccountType.LIABILITY.getType().equals(toAcc.getType())) {
                        flag |= 8;
                    } else if (AccountType.OTHER.getType().equals(toAcc.getType())) {
                        flag |= 16;
                    }
                    lastTo = AccountType.find(toAcc.getType());
                } else {
                    lastTo = AccountType.UNKONW;
                }
                if (fromAcc != null) {
                    if (AccountType.INCOME.getType().equals(fromAcc.getType())) {
                        flag |= 2;
                    }
                    lastFrom = AccountType.find(fromAcc.getType());
                } else {
                    lastFrom = AccountType.UNKONW;
                }
                int drawid;
                if ((flag & 1) == 1) {//expense
                    drawid = R.drawable.selector_detail_expense;
                    last = AccountType.EXPENSE;
                } else if ((flag & 2) == 2) {//income
                    drawid = R.drawable.selector_detail_income;
                    last = AccountType.INCOME;
                } else if ((flag & 4) == 4) {
                    drawid = R.drawable.selector_detail_asset;
                    last = AccountType.ASSET;
                } else if ((flag & 8) == 8) {
                    drawid = R.drawable.selector_detail_liability;
                    last = AccountType.LIABILITY;
                } else if ((flag & 16) == 16) {
                    drawid = R.drawable.selector_detail_other;
                    last = AccountType.OTHER;
                } else {
                    drawid = R.drawable.selector_detail_unknow;
                    last = null;
                }
                Drawable draw = activity.getResources().getDrawable(drawid);
                layout.setBackgroundDrawable(draw);
                return true;
            }


            if ("layout_inner".equals(name)) {
                //make it light
                LinearLayout inner = (LinearLayout) view;
                Date ddate = record.getDate();
                Date now = new Date();
                Date day3 = calHelper.dateBefore(now, 3);
                Date day7 = calHelper.dateBefore(now, 7);
                Drawable draw = null;
                if (calHelper.isFutureDay(now, ddate)) {
                    //future
                    draw = new ColorDrawable(0x4FFFFFFF);
                } else if (calHelper.isSameDay(now, ddate)) {
                    //today
                    draw = new ColorDrawable(0x3FFFFFFF);
                } else if (calHelper.isFutureDay(day3, ddate)) {
                    draw = new ColorDrawable(0x1fFFFFFF);
                } else if (calHelper.isFutureDay(day7, ddate)) {
                    draw = new ColorDrawable(0x0FFFFFFF);
                }
                inner.setBackgroundDrawable(draw);
                return true;
            }

            if (!(view instanceof TextView)) {
                return false;
            }

            if (AccountType.INCOME.equals(last)) {
                ((TextView) view).setTextColor(activity.getResources().getColor(R.color.income_fg));
//               if("money".equals(name)){
//                   ((TextView)view).setText(text);
//                   return true;
//               }
            } else if (AccountType.ASSET.equals(last)) {
                ((TextView) view).setTextColor(activity.getResources().getColor(R.color.asset_fg));
            } else if (AccountType.EXPENSE.equals(last)) {
                ((TextView) view).setTextColor(activity.getResources().getColor(R.color.expense_fg));
//                if("money".equals(name)){
//                    ((TextView)view).setText(text);
//                    return true;
//                }
            } else if (AccountType.LIABILITY.equals(last)) {
                ((TextView) view).setTextColor(activity.getResources().getColor(R.color.liability_fg));
            } else if (AccountType.OTHER.equals(last)) {
                ((TextView) view).setTextColor(activity.getResources().getColor(R.color.other_fg));
            } else {
                ((TextView) view).setTextColor(activity.getResources().getColor(R.color.unknow_fg));
            }


            if ("from".equals(name)) {
                view.setBackgroundDrawable(null);
                if (last != lastFrom) {
                    if (AccountType.INCOME == lastFrom) {
                        view.setBackgroundDrawable(activity.getResources().getDrawable(R.drawable.selector_detail_income));
                    } else if (AccountType.EXPENSE == lastFrom) {
                        view.setBackgroundDrawable(activity.getResources().getDrawable(R.drawable.selector_detail_expense));
                    } else if (AccountType.ASSET == lastFrom) {
                        view.setBackgroundDrawable(activity.getResources().getDrawable(R.drawable.selector_detail_asset));
                    } else if (AccountType.LIABILITY == lastFrom) {
                        view.setBackgroundDrawable(activity.getResources().getDrawable(R.drawable.selector_detail_liability));
                    } else if (AccountType.OTHER == lastFrom) {
                        view.setBackgroundDrawable(activity.getResources().getDrawable(R.drawable.selector_detail_other));
                    } else {
                        view.setBackgroundDrawable(activity.getResources().getDrawable(R.drawable.selector_detail_unknow));
                    }
                }
            }

            if ("to".equals(name)) {
                view.setBackgroundDrawable(null);
                if (AccountType.INCOME == lastFrom) {
                    if (AccountType.ASSET == lastTo) {
                        view.setBackgroundDrawable(activity.getResources().getDrawable(R.drawable.selector_detail_asset));
                    } else if (AccountType.LIABILITY == lastTo) {
                        view.setBackgroundDrawable(activity.getResources().getDrawable(R.drawable.selector_detail_liability));
                    } else if (AccountType.OTHER == lastTo) {
                        view.setBackgroundDrawable(activity.getResources().getDrawable(R.drawable.selector_detail_other));
                    }
                }
            }


            return false;
        }
    }

}
