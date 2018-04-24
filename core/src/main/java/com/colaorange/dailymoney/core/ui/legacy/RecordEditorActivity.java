package com.colaorange.dailymoney.core.ui.legacy;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.colaorange.commons.util.CalendarHelper;
import com.colaorange.commons.util.Formats;
import com.colaorange.dailymoney.core.util.GUIs;
import com.colaorange.dailymoney.core.util.I18N;
import com.colaorange.dailymoney.core.util.Logger;
import com.colaorange.calculator2.Calculator;
import com.colaorange.dailymoney.core.context.Contexts;
import com.colaorange.dailymoney.core.context.ContextsActivity;
import com.colaorange.dailymoney.core.R;
import com.colaorange.dailymoney.core.data.Account;
import com.colaorange.dailymoney.core.data.AccountType;
import com.colaorange.dailymoney.core.data.Detail;
import com.colaorange.dailymoney.core.data.IDataProvider;
import com.colaorange.dailymoney.core.ui.Constants;
import com.colaorange.dailymoney.core.ui.legacy.AccountUtil.IndentNode;

/**
 * Edit or create a detail
 *
 * @author dennis
 */
public class RecordEditorActivity extends ContextsActivity implements android.view.View.OnClickListener {

    public static final String PARAM_MODE_CREATE = "dteditor.modeCreate";
    public static final String PARAM_DETAIL = "dteditor.detail";


    private boolean modeCreate;
    private int counterCreate;
    private Detail detail;
    private Detail workingDetail;

    private DateFormat dateFormat;

    boolean archived = false;

    private List<IndentNode> fromAccountList;
    private List<IndentNode> toAccountList;

    List<Map<String, Object>> fromAccountMapList;
    List<Map<String, Object>> toAccountMapList;

    private SimpleAdapter fromAccountAdapter;
    private SimpleAdapter toAccountAdapter;


    private static String[] spfrom = new String[]{Constants.DISPLAY, Constants.DISPLAY};
    private static int[] spto = new int[]{R.id.simple_spinner_item_display, R.id.simple_spinner_dropdown_item_display};

    Spinner fromEditor;
    Spinner toEditor;

    EditText dateEditor;
    EditText noteEditor;
    EditText moneyEditor;

    Button okBtn;
    Button cancelBtn;
    Button closeBtn;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record_editor);
        dateFormat = preference().getDateFormat();
        initParams();
        initMembers();
        refreshUI();
    }


    /**
     * clone a detail without id
     **/
    private Detail clone(Detail detail) {
        Detail d = new Detail(detail.getFrom(), detail.getTo(), detail.getDate(), detail.getMoney(), detail.getNote());
        d.setArchived(detail.isArchived());
        return d;
    }


    private void initParams() {
        Bundle bundle = getIntentExtras();
        modeCreate = bundle.getBoolean(PARAM_MODE_CREATE, true);
        detail = (Detail) bundle.get(PARAM_DETAIL);

        //issue 51, for direct call from outside action, 
        if (detail == null) {
            detail = new Detail("", "", new Date(), 0D, "");
        }

        workingDetail = clone(detail);

        if (modeCreate) {
            setTitle(R.string.title_deteditor_create);
        } else {
            setTitle(R.string.title_deteditor_update);
        }
    }


    private void initMembers() {

        boolean archived = workingDetail.isArchived();


        dateEditor = findViewById(R.id.detail_editor_date);
        dateEditor.setText(dateFormat.format(workingDetail.getDate()));
        dateEditor.setEnabled(!archived);

        moneyEditor = findViewById(R.id.detail_editor_money);
        moneyEditor.setText(workingDetail.getMoney() <= 0 ? "" : Formats.double2String(workingDetail.getMoney()));
        moneyEditor.setEnabled(!archived);

        noteEditor = findViewById(R.id.detail_editor_note);
        noteEditor.setText(workingDetail.getNote());

        if (!archived) {
            findViewById(R.id.detail_editor_prev).setOnClickListener(this);
            findViewById(R.id.detail_editor_next).setOnClickListener(this);
            findViewById(R.id.detail_editor_today).setOnClickListener(this);
            findViewById(R.id.detail_editor_datepicker).setOnClickListener(this);
        }
        findViewById(R.id.detail_editor_cal2).setOnClickListener(this);

        okBtn = findViewById(R.id.detail_editor_ok);
        if (modeCreate) {
            okBtn.setCompoundDrawablesWithIntrinsicBounds(resolveThemeAttrResId(R.attr.ic_add), 0, 0, 0);
            okBtn.setText(R.string.cact_create);
        } else {
            okBtn.setCompoundDrawablesWithIntrinsicBounds(resolveThemeAttrResId(R.attr.ic_save), 0, 0, 0);
            okBtn.setText(R.string.cact_update);
            moneyEditor.requestFocus();
        }
        okBtn.setOnClickListener(this);

        cancelBtn = findViewById(R.id.detail_editor_cancel);
        closeBtn = findViewById(R.id.detail_editor_close);

        cancelBtn.setOnClickListener(this);
        closeBtn.setOnClickListener(this);

        fromEditor = findViewById(R.id.detail_editor_from);
        toEditor = findViewById(R.id.detail_editor_to);

        fromAccountList = new ArrayList<IndentNode>();
        fromAccountMapList = new ArrayList<Map<String, Object>>();
        fromAccountAdapter = new SimpleAdapterEx(this, fromAccountMapList, R.layout.simple_spinner_dropdown_item, spfrom, spto);
        fromAccountAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown);
        fromAccountAdapter.setViewBinder(new AccountViewBinder() {
            public Account getSelectedAccount() {
                int pos = fromEditor.getSelectedItemPosition();
                if (pos >= 0) {
                    return fromAccountList.get(pos).getAccount();
                }
                return null;
            }
        });
        fromEditor.setAdapter(fromAccountAdapter);

        toAccountList = new ArrayList<IndentNode>();
        toAccountMapList = new ArrayList<Map<String, Object>>();
        toAccountAdapter = new SimpleAdapterEx(this, toAccountMapList, R.layout.simple_spinner_dropdown_item, spfrom, spto);
        toAccountAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown);
        toAccountAdapter.setViewBinder(new AccountViewBinder() {
            public Account getSelectedAccount() {
                int pos = toEditor.getSelectedItemPosition();
                if (pos >= 0) {
                    return toAccountList.get(pos).getAccount();
                }
                return null;
            }
        });
        toEditor.setAdapter(toAccountAdapter);

        fromEditor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                IndentNode tn = fromAccountList.get(pos);
                if (tn.getAccount() != null) {
                    onFromChanged(tn.getAccount());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        toEditor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                IndentNode tn = toAccountList.get(pos);
                if (tn.getAccount() != null) {
                    onToChanged(tn.getAccount());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void refreshUI() {
        refreshSpinner();
    }

    private void refreshSpinner() {
        IDataProvider idp = contexts().getDataProvider();
        // initial from
        AccountType[] avail = AccountType.getFromType();
        fromAccountList.clear();
        fromAccountMapList.clear();
        for (AccountType at : avail) {
            List<Account> accl = idp.listAccount(at);
            fromAccountList.addAll(AccountUtil.toIndentNode(accl));
        }
        String fromAccount = workingDetail.getFrom();
        int fromsel, firstfromsel, i;
        fromsel = firstfromsel = i = -1;
        String fromType = null;
        for (IndentNode pn : fromAccountList) {
            i++;
            Map<String, Object> row = new HashMap<String, Object>();
            fromAccountMapList.add(row);

            row.put(spfrom[0], new NamedItem(spfrom[0], pn, ""));
            row.put(spfrom[1], new NamedItem(spfrom[1], pn, ""));
            if (pn.getAccount() != null) {
                if (firstfromsel == -1) {
                    firstfromsel = i;
                }
                if (fromsel == -1 && pn.getAccount().getId().equals(fromAccount)) {
                    fromsel = i;
                    fromType = pn.getAccount().getType();
                }

            }
        }

        // initial to
        avail = AccountType.getToType(fromType);
        toAccountList.clear();
        toAccountMapList.clear();
        for (AccountType at : avail) {
            List<Account> accl = idp.listAccount(at);
            toAccountList.addAll(AccountUtil.toIndentNode(accl));
        }
        String toAccount = workingDetail.getTo();
        int tosel, firsttosel;
        tosel = firsttosel = i = -1;
        // String toType = null;
        for (IndentNode pn : toAccountList) {
            i++;
            Map<String, Object> row = new HashMap<String, Object>();
            toAccountMapList.add(row);

            row.put(spfrom[0], new NamedItem(spfrom[0], pn, ""));
            row.put(spfrom[1], new NamedItem(spfrom[1], pn, ""));
            if (pn.getAccount() != null) {
                if (firsttosel == -1) {
                    firsttosel = i;
                }
                if (tosel == -1 && pn.getAccount().getId().equals(toAccount)) {
                    tosel = i;
                }

            }
        }

        if (fromsel > -1) {
            fromEditor.setSelection(fromsel);
        } else if (firstfromsel > -1) {
            fromEditor.setSelection(firstfromsel);
            workingDetail.setFrom(fromAccountList.get(firstfromsel).getAccount().getId());
        } else {
            workingDetail.setFrom("");
        }

        if (tosel > -1) {
            toEditor.setSelection(tosel);
        } else if (firsttosel > -1) {
            toEditor.setSelection(firsttosel);
            workingDetail.setTo(toAccountList.get(firsttosel).getAccount().getId());
        } else {
            workingDetail.setTo("");
        }

        fromAccountAdapter.notifyDataSetChanged();
        toAccountAdapter.notifyDataSetChanged();
    }


    private void onFromChanged(Account acc) {
        workingDetail.setFrom(acc.getId());
        refreshSpinner();
    }

    private void onToChanged(Account acc) {
        workingDetail.setTo(acc.getId());
    }

    private void updateDateEditor(Date d) {
        dateEditor.setText(dateFormat.format(d));
    }

    @Override
    public void onClick(View v) {
        CalendarHelper cal = calendarHelper();
        if (v.getId() == R.id.detail_editor_ok) {
            doOk();
        } else if (v.getId() == R.id.detail_editor_cancel) {
            doCancel();
        } else if (v.getId() == R.id.detail_editor_close) {
            doClose();
        } else if (v.getId() == R.id.detail_editor_prev) {
            try {
                Date d = dateFormat.parse(dateEditor.getText().toString());
                updateDateEditor(cal.yesterday(d));
            } catch (ParseException e) {
                Logger.e(e.getMessage(), e);
            }
        } else if (v.getId() == R.id.detail_editor_next) {
            try {
                Date d = dateFormat.parse(dateEditor.getText().toString());
                updateDateEditor(cal.tomorrow(d));
            } catch (ParseException e) {
                Logger.e(e.getMessage(), e);
            }
        } else if (v.getId() == R.id.detail_editor_today) {
            updateDateEditor(cal.today());
        } else if (v.getId() == R.id.detail_editor_datepicker) {
            try {
                Date d = dateFormat.parse(dateEditor.getText().toString());
                GUIs.openDatePicker(this, d, new GUIs.OnFinishListener() {
                    @Override
                    public boolean onFinish(Object data) {
                        updateDateEditor((Date) data);
                        return true;
                    }
                });
            } catch (ParseException e) {
                Logger.e(e.getMessage(), e);
            }
        } else if (v.getId() == R.id.detail_editor_cal2) {
            doCalculator2();
        }
    }

    private void doCalculator2() {
        Intent intent = null;
        intent = new Intent(this, Calculator.class);
        intent.putExtra(Calculator.PARAM_NEED_RESULT, true);

        String start = "";
        try {
            start = Formats.editorTextNumberDecimalToCal2(moneyEditor.getText().toString());
        } catch (Exception x) {
        }

        intent.putExtra(Calculator.PARAM_START_VALUE, start);
        startActivityForResult(intent, Constants.REQUEST_CALCULATOR_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_CALCULATOR_CODE && resultCode == Activity.RESULT_OK) {
            String result = data.getExtras().getString(Calculator.PARAM_RESULT_VALUE);
            try {
                moneyEditor.setText(Formats.cal2ToEditorTextNumberDecimal(result));
            } catch (Exception x) {
            }
        }
    }

    private void doOk() {

        I18N i18n = i18n();

        // verify
        int fromPos = fromEditor.getSelectedItemPosition();
        if (Spinner.INVALID_POSITION == fromPos || fromAccountList.get(fromPos).getAccount() == null) {
            GUIs.alert(this,
                    i18n.string(R.string.cmsg_field_empty, i18n.string(R.string.label_from_account)));
            return;
        }
        int toPos = toEditor.getSelectedItemPosition();
        if (Spinner.INVALID_POSITION == toPos || toAccountList.get(toPos).getAccount() == null) {
            GUIs.alert(this,
                    i18n.string(R.string.cmsg_field_empty, i18n.string(R.string.label_to_account)));
            return;
        }
        String datestr = dateEditor.getText().toString().trim();
        if ("".equals(datestr)) {
            dateEditor.requestFocus();
            GUIs.alert(this, i18n.string(R.string.cmsg_field_empty, i18n.string(R.string.label_date)));
            return;
        }

        Date date = null;
        try {
            date = dateFormat.parse(datestr);
        } catch (ParseException e) {
            Logger.e(e.getMessage(), e);
            GUIs.errorToast(this, e);
            return;
        }

        String moneystr = moneyEditor.getText().toString();
        if ("".equals(moneystr)) {
            moneyEditor.requestFocus();
            GUIs.alert(this, i18n.string(R.string.cmsg_field_empty, i18n.string(R.string.label_money)));
            return;
        }
        double money = 0;
        try {
            money = Formats.string2Double(moneystr);
        } catch (Exception x) {
            Logger.w(x.getMessage(), x);
        }

        if (money <= 0) {
            GUIs.alert(this, i18n.string(R.string.cmsg_field_zero, i18n.string(R.string.label_money)));
            return;
        }

        String note = noteEditor.getText().toString();

        Account fromAcc = fromAccountList.get(fromPos).getAccount();
        Account toAcc = toAccountList.get(toPos).getAccount();

        if (fromAcc.getId().equals(toAcc.getId())) {
            GUIs.alert(this, i18n.string(R.string.msg_same_from_to));
            return;
        }


        workingDetail.setFrom(fromAcc.getId());
        workingDetail.setTo(toAcc.getId());

        workingDetail.setDate(date);
        workingDetail.setMoney(money);
        workingDetail.setNote(note.trim());
        IDataProvider idp = contexts().getDataProvider();
        if (modeCreate) {

            idp.newDetail(workingDetail);
            setResult(RESULT_OK);

            workingDetail = clone(workingDetail);
            workingDetail.setMoney(0D);
            workingDetail.setNote("");
            moneyEditor.setText("");
            moneyEditor.requestFocus();
            noteEditor.setText("");
            counterCreate++;
            okBtn.setText(i18n.string(R.string.cact_create) + "(" + counterCreate + ")");
            cancelBtn.setVisibility(Button.GONE);
            closeBtn.setVisibility(Button.VISIBLE);
            trackEvent(Contexts.TRACKER_EVT_CREATE);
        } else {

            idp.updateDetail(detail.getId(), workingDetail);

            GUIs.shortToast(this, i18n.string(R.string.msg_detail_updated));
            setResult(RESULT_OK);
            finish();

            trackEvent(Contexts.TRACKER_EVT_UPDATE);
        }
    }

    private void doCancel() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private void doClose() {
        setResult(RESULT_OK);
        GUIs.shortToast(this, i18n().string(R.string.msg_created_detail, counterCreate));
        finish();
    }


    private class SimpleAdapterEx extends SimpleAdapter {

        public SimpleAdapterEx(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
            super(context, data, resource, from, to);
        }
//        
//        @Override
//        public boolean areAllItemsEnabled() {
//            return false;
//        }
//
//        @Override
//        public boolean isEnabled(int position) {
//            //android bug ? not work
//            NamedItem item = (NamedItem)((Map)this.getItem(position)).get(spfrom[0]);
//            PN pn = (PN)item.getValue();
//            return pn.account!=null;
//        }
    }

    private int ddPaddingLeftBase;
    private float ddPaddingIntentBase;
    private boolean ddPaddingBase_set;
    //    private Drawable ddDisabled;
    private Drawable ddSelected;

    private class AccountViewBinder implements SimpleAdapter.ViewBinder {


        public Account getSelectedAccount() {
            return null;
        }

        @Override
        public boolean setViewValue(View view, Object data, String text) {

            I18N i18n = i18n();

            NamedItem item = (NamedItem) data;
            String name = item.getName();
            IndentNode tn = (IndentNode) item.getValue();

            if (!(view instanceof TextView)) {
                return false;
            }
            AccountType at = tn.getType();
            TextView tv = (TextView) view;
            if (!ddPaddingBase_set) {
                ddPaddingBase_set = true;
                ddPaddingIntentBase = 15 * GUIs.getDPRatio(RecordEditorActivity.this);
                ddPaddingLeftBase = tv.getPaddingLeft();
//                ddDisabled = RecordEditorActivity.this.getResources().getDrawable(android.R.color.darker_gray).mutate();
//                ddDisabled.setAlpha(32);
                ddSelected = RecordEditorActivity.this.getResources().getDrawable(android.R.color.darker_gray).mutate();
                ddSelected.setAlpha(128);
            }

            if (Constants.DISPLAY.equals(name)) {
                int tcolor;
                tv.setBackgroundDrawable(null);
                if (AccountType.INCOME == at) {
                    tcolor = RecordEditorActivity.this.getResources().getColor(R.color.income_fgl);
                } else if (AccountType.ASSET == at) {
                    tcolor = RecordEditorActivity.this.getResources().getColor(R.color.asset_fgl);
                } else if (AccountType.EXPENSE == at) {
                    tcolor = RecordEditorActivity.this.getResources().getColor(R.color.expense_fgl);
                } else if (AccountType.LIABILITY == at) {
                    tcolor = RecordEditorActivity.this.getResources().getColor(R.color.liability_fgl);
                } else if (AccountType.OTHER == at) {
                    tcolor = RecordEditorActivity.this.getResources().getColor(R.color.other_fgl);
                } else {
                    tcolor = RecordEditorActivity.this.getResources().getColor(R.color.unknow_fgl);
                }
                tv.setTextColor(tcolor);
                StringBuilder display = new StringBuilder();
                if (tv.getId() == R.id.simple_spinner_dropdown_item_display) {
                    tv.setPadding((int) (ddPaddingLeftBase + tn.getIndent() * ddPaddingIntentBase), tv.getPaddingTop(), tv.getPaddingRight(), tv.getPaddingBottom());
                    if (tn.getAccount() == null) {
//                        tv.setBackgroundDrawable(ddDisabled);
                        tv.setTextColor(tcolor & 0x6FFFFFFF);
                    } else if (tn.getAccount() == getSelectedAccount()) {
                        tv.setBackgroundDrawable(ddSelected);
                    } else {
                        tv.setBackgroundDrawable(null);
                    }

                    if (tn.getIndent() == 0) {
                        display.append(tn.getType().getDisplay(i18n));
                        display.append(" - ");
                    }
                    display.append(tn.getName());
                } else {
                    if (tn.getAccount() == null) {
                        display.append("");
                    } else {
                        display.append(tn.getType().getDisplay(i18n));
                        display.append("-");
                        display.append(tn.getAccount().getName());
                    }
                }
                tv.setText(display.toString());
                return true;
            }
            return false;
        }
    }
}
