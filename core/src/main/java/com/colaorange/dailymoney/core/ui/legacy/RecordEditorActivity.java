package com.colaorange.dailymoney.core.ui.legacy;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.colaorange.calculator2.Calculator;
import com.colaorange.commons.util.CalendarHelper;
import com.colaorange.commons.util.Colors;
import com.colaorange.commons.util.Formats;
import com.colaorange.dailymoney.core.R;
import com.colaorange.dailymoney.core.context.ContextsActivity;
import com.colaorange.dailymoney.core.data.Account;
import com.colaorange.dailymoney.core.data.AccountType;
import com.colaorange.dailymoney.core.data.Record;
import com.colaorange.dailymoney.core.data.IDataProvider;
import com.colaorange.dailymoney.core.ui.Constants;
import com.colaorange.dailymoney.core.ui.RegularSpinnerAdapter;
import com.colaorange.dailymoney.core.ui.legacy.AccountUtil.AccountIndentNode;
import com.colaorange.dailymoney.core.util.GUIs;
import com.colaorange.dailymoney.core.util.I18N;
import com.colaorange.dailymoney.core.util.Logger;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Edit or create a record
 *
 * @author dennis
 */
public class RecordEditorActivity extends ContextsActivity implements android.view.View.OnClickListener {

    /**
     * a boolean to represent the create mode
     */
    public static final String PARAM_MODE_CREATE = "modeCreate";
    /**
     * a specified created date or null, it only usefaul when mode is create and no record arg
     */
    public static final String PARAM_CREATED_DATE = "createdDate";
    /**
     * a specified record for create or update
     */
    public static final String PARAM_RECORD = "record";


    private boolean modeCreate;
    private int counterCreate;
    private Record record;
    private Record workingRecord;

    private DateFormat dateFormat;

    private boolean archived = false;

    private List<AccountIndentNode> fromAccountList;
    private List<AccountIndentNode> toAccountList;

    private RegularSpinnerAdapter<AccountIndentNode> fromAccountAdapter;
    private RegularSpinnerAdapter<AccountIndentNode> toAccountAdapter;


    private Spinner vFromAccount;
    private Spinner vToAccount;

    private EditText vRecordDate;
    private EditText vRecordNote;
    private EditText vRecordMoney;

    private Button btnOk;
    private Button btnCancel;
    private Button btnClose;

    private float nodePaddingBase;

    //0:usual, 1:last
    private int bookmarkMode = 0;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record_editor);
        initArgs();
        initMembers();
        refreshUI();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.record_editor_menu, menu);
        GUIs.post(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.menu_bookmark).setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        doShowBookmarkModeDlg();
                        return true;
                    }
                });
            }
        });
        return true;
    }

    private void doShowBookmarkModeDlg() {
        new AlertDialog.Builder(this).setTitle(i18n().string(R.string.act_bookmark))
                .setItems(R.array.record_editor_bookmark_options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int which) {
                        doBookmarkMode(bookmarkMode = which);
                    }
                }).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.menu_bookmark) {
            if (++bookmarkMode > 1) {
                bookmarkMode = 0;
            }
            doBookmarkMode(bookmarkMode);
            return true;
        } else if (item.getItemId() == R.id.menu_swap) {
            String from = workingRecord.getFrom();
            workingRecord.setFrom(workingRecord.getTo());
            workingRecord.setTo(from);
            refreshSpinner(false);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void doBookmarkMode(int bookmarkMode) {

        switch (bookmarkMode) {
            case 1:
                workingRecord.setFrom(preference().getLastFromAccount());
                workingRecord.setTo(preference().getLastToAccount());
                break;
            case 0:
            default:
                workingRecord.setFrom("");
                workingRecord.setTo("");
                break;
        }


        refreshSpinner(false);
    }


    /**
     * clone a record without id
     **/
    private Record clone(Record record) {
        Record d = new Record(record.getFrom(), record.getTo(), record.getDate(), record.getMoney(), record.getNote());
        d.setArchived(record.isArchived());
        return d;
    }


    private void initArgs() {
        dateFormat = preference().getDateFormat();

        Bundle bundle = getIntentExtras();
        modeCreate = bundle.getBoolean(PARAM_MODE_CREATE, true);
        Date createdDate = (Date) bundle.get(PARAM_CREATED_DATE);
        record = (Record) bundle.get(PARAM_RECORD);

        if (modeCreate && record == null) {
            record = new Record("", "", createdDate == null ? new Date() : createdDate, 0D, "");
        }

        workingRecord = clone(record);

        if (modeCreate) {
            setTitle(R.string.title_receditor_create);
        } else {
            setTitle(R.string.title_receditor_update);
        }
    }


    private void initMembers() {

        boolean archived = workingRecord.isArchived();


        vRecordDate = findViewById(R.id.record_date);
        vRecordDate.setText(dateFormat.format(workingRecord.getDate()));
        vRecordDate.setEnabled(!archived);

        vRecordMoney = findViewById(R.id.record_money);
        vRecordMoney.setText(workingRecord.getMoney() <= 0 ? "" : Formats.double2String(workingRecord.getMoney()));
        vRecordMoney.setEnabled(!archived);

        vRecordNote = findViewById(R.id.record_note);
        vRecordNote.setText(workingRecord.getNote());

        if (!archived) {
            findViewById(R.id.btn_prev).setOnClickListener(this);
            findViewById(R.id.btn_next).setOnClickListener(this);
            findViewById(R.id.btn_today).setOnClickListener(this);
            findViewById(R.id.btn_datepicker).setOnClickListener(this);
        }
        findViewById(R.id.btn_cal2).setOnClickListener(this);

        btnOk = findViewById(R.id.btn_ok);
        if (modeCreate) {
            btnOk.setCompoundDrawablesWithIntrinsicBounds(resolveThemeAttrResId(R.attr.ic_add), 0, 0, 0);
            btnOk.setText(R.string.act_create);
        } else {
            btnOk.setCompoundDrawablesWithIntrinsicBounds(resolveThemeAttrResId(R.attr.ic_save), 0, 0, 0);
            btnOk.setText(R.string.act_update);
            vRecordMoney.requestFocus();
        }
        btnOk.setOnClickListener(this);

        btnCancel = findViewById(R.id.btn_cancel);
        btnClose = findViewById(R.id.btn_close);

        btnCancel.setOnClickListener(this);
        btnClose.setOnClickListener(this);

        vFromAccount = findViewById(R.id.record_from_account);

        fromAccountList = new LinkedList<>();
        fromAccountAdapter = new RegularSpinnerAdapter<AccountIndentNode>(this, fromAccountList) {

            public boolean isSelected(int position) {
                return vFromAccount.getSelectedItemPosition() == position;
            }

            @Override
            public boolean isEnabled(int position) {
                return getItem(position).getAccount() != null;
            }

            @Override
            public ViewHolder<AccountIndentNode> createViewHolder() {
                return new AccountTypeViewBinder(this);
            }
        };
        vFromAccount.setAdapter(fromAccountAdapter);

        vToAccount = findViewById(R.id.record_to_account);
        toAccountList = new LinkedList<>();
        toAccountAdapter = new RegularSpinnerAdapter<AccountIndentNode>(this, toAccountList) {

            public boolean isSelected(int position) {
                return vToAccount.getSelectedItemPosition() == position;
            }

            @Override
            public boolean isEnabled(int position) {
                return getItem(position).getAccount() != null;
            }

            @Override
            public ViewHolder<AccountIndentNode> createViewHolder() {
                return new AccountTypeViewBinder(this);
            }
        };
        vToAccount.setAdapter(toAccountAdapter);

        vFromAccount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                AccountIndentNode tn = fromAccountList.get(pos);
                if (tn.getAccount() != null) {
                    onFromChanged(tn.getAccount());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        vToAccount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                AccountIndentNode tn = toAccountList.get(pos);
                if (tn.getAccount() != null) {
                    onToChanged(tn.getAccount());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        //10dp
        nodePaddingBase = 10 * getDpRatio();
    }

    private void refreshUI() {
        refreshSpinner(false);
    }

    private void refreshSpinner(boolean toOnly) {
        IDataProvider idp = contexts().getDataProvider();

        if (!toOnly) {
            // initial from
            fromAccountList.clear();
            for (AccountType at : AccountType.getFromType()) {
                List<Account> list = idp.listAccount(at);
                fromAccountList.addAll(AccountUtil.toIndentNode(list));
            }
        }
        String fromId = workingRecord.getFrom();
        String fromType = null;
        int recFromPos, firstFromPos, i;
        recFromPos = firstFromPos = i = -1;

        for (AccountIndentNode node : fromAccountList) {
            i++;
            Account acc = node.getAccount();
            if (acc != null) {
                if (firstFromPos == -1) {
                    firstFromPos = i;
                    fromType = node.getAccount().getType();
                }
                if (acc.getId().equals(fromId)) {
                    recFromPos = i;
                    fromType = node.getAccount().getType();
                    break;
                }
            }
        }


        if (!toOnly) {
            fromAccountAdapter.notifyDataSetChanged();

            if (recFromPos > -1) {
                vFromAccount.setSelection(recFromPos);
            } else if (firstFromPos > -1) {
                vFromAccount.setSelection(firstFromPos);
                workingRecord.setFrom(fromAccountList.get(firstFromPos).getAccount().getId());
            } else {
                vFromAccount.setSelection(Spinner.INVALID_POSITION);
                workingRecord.setFrom("");
            }
        }

        //reset for dynamic item and clear selection
        toAccountList.clear();
        for (AccountType at : AccountType.getToType(fromType)) {
            List<Account> list = idp.listAccount(at);
            toAccountList.addAll(AccountUtil.toIndentNode(list));
        }

        String toId = workingRecord.getTo();
        int recToPos, firstToPos;
        recToPos = firstToPos = i = -1;
        // String toType = null;
        for (AccountIndentNode node : toAccountList) {
            i++;
            Account acc = node.getAccount();
            if (acc != null) {
                if (firstToPos == -1) {
                    firstToPos = i;
                }
                if (acc.getId().equals(toId)) {
                    recToPos = i;
                }

            }
        }

        toAccountAdapter.notifyDataSetChanged();

        if (recToPos > -1) {
            vToAccount.setSelection(recToPos);
        } else if (firstToPos > -1) {
            vToAccount.setSelection(firstToPos);
            workingRecord.setTo(toAccountList.get(firstToPos).getAccount().getId());
        } else {
            vToAccount.setSelection(Spinner.INVALID_POSITION);
            workingRecord.setTo("");
        }


    }


    private void onFromChanged(Account acc) {
        workingRecord.setFrom(acc.getId());
        refreshSpinner(true);
    }

    private void onToChanged(Account acc) {
        workingRecord.setTo(acc.getId());
    }

    private void updateDateEditor(Date d) {
        vRecordDate.setText(dateFormat.format(d));
    }

    @Override
    public void onClick(View v) {
        CalendarHelper cal = calendarHelper();
        if (v.getId() == R.id.btn_ok) {
            doOk();
        } else if (v.getId() == R.id.btn_cancel) {
            doCancel();
        } else if (v.getId() == R.id.btn_close) {
            doClose();
        } else if (v.getId() == R.id.btn_prev) {
            try {
                Date d = dateFormat.parse(vRecordDate.getText().toString());
                updateDateEditor(cal.yesterday(d));
            } catch (ParseException e) {
                Logger.e(e.getMessage(), e);
            }
        } else if (v.getId() == R.id.btn_next) {
            try {
                Date d = dateFormat.parse(vRecordDate.getText().toString());
                updateDateEditor(cal.tomorrow(d));
            } catch (ParseException e) {
                Logger.e(e.getMessage(), e);
            }
        } else if (v.getId() == R.id.btn_today) {
            updateDateEditor(cal.today());
        } else if (v.getId() == R.id.btn_datepicker) {
            try {
                Date d = dateFormat.parse(vRecordDate.getText().toString());
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
        } else if (v.getId() == R.id.btn_cal2) {
            doCalculator2();
        }
    }

    private void doCalculator2() {
        Intent intent = null;
        intent = new Intent(this, Calculator.class);
        intent.putExtra(Calculator.PARAM_NEED_RESULT, true);
        intent.putExtra(Calculator.PARAM_THEME, isLightTheme() ? Calculator.THEME_LIGHT : Calculator.THEME_DARK);

        String start = "";
        try {
            start = Formats.editorTextNumberDecimalToCal2(vRecordMoney.getText().toString());
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
                vRecordMoney.setText(Formats.cal2ToEditorTextNumberDecimal(result));
            } catch (Exception x) {
            }
        }
    }

    private void doOk() {

        I18N i18n = i18n();

        // verify
        int fromPos = vFromAccount.getSelectedItemPosition();
        if (Spinner.INVALID_POSITION == fromPos || fromAccountList.get(fromPos).getAccount() == null) {
            GUIs.alert(this,
                    i18n.string(R.string.msg_field_empty, i18n.string(R.string.label_from_account)));
            return;
        }
        int toPos = vToAccount.getSelectedItemPosition();
        if (Spinner.INVALID_POSITION == toPos || toAccountList.get(toPos).getAccount() == null) {
            GUIs.alert(this,
                    i18n.string(R.string.msg_field_empty, i18n.string(R.string.label_to_account)));
            return;
        }
        String datestr = vRecordDate.getText().toString().trim();
        if ("".equals(datestr)) {
            vRecordDate.requestFocus();
            GUIs.alert(this, i18n.string(R.string.msg_field_empty, i18n.string(R.string.label_date)));
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

        String moneystr = vRecordMoney.getText().toString();
        if ("".equals(moneystr)) {
            vRecordMoney.requestFocus();
            GUIs.alert(this, i18n.string(R.string.msg_field_empty, i18n.string(R.string.label_money)));
            return;
        }
        double money = 0;
        try {
            money = Formats.string2Double(moneystr);
        } catch (Exception x) {
            Logger.w(x.getMessage(), x);
        }

        if (money <= 0) {
            GUIs.alert(this, i18n.string(R.string.msg_field_zero, i18n.string(R.string.label_money)));
            return;
        }

        String note = vRecordNote.getText().toString();

        Account fromAcc = fromAccountList.get(fromPos).getAccount();
        Account toAcc = toAccountList.get(toPos).getAccount();

        if (fromAcc.getId().equals(toAcc.getId())) {
            GUIs.alert(this, i18n.string(R.string.msg_same_from_to));
            return;
        }


        workingRecord.setFrom(fromAcc.getId());
        workingRecord.setTo(toAcc.getId());

        workingRecord.setDate(date);
        workingRecord.setMoney(money);
        workingRecord.setNote(note.trim());
        IDataProvider idp = contexts().getDataProvider();
        if (modeCreate) {

            idp.newRecord(workingRecord);
            setResult(RESULT_OK);

            workingRecord = clone(workingRecord);
            workingRecord.setMoney(0D);
            workingRecord.setNote("");
            vRecordMoney.setText("");
            vRecordMoney.requestFocus();
            vRecordNote.setText("");
            counterCreate++;
            btnOk.setText(i18n.string(R.string.act_create) + "(" + counterCreate + ")");
            btnCancel.setVisibility(Button.GONE);
            btnClose.setVisibility(Button.VISIBLE);
            trackEvent(TE.CREATE_RECORD);
        } else {

            idp.updateRecord(record.getId(), workingRecord);

            GUIs.shortToast(this, i18n.string(R.string.msg_record_updated));
            setResult(RESULT_OK);
            finish();

            trackEvent(TE.UPDDATE_RECORD);
        }

        preference().setLastAccount(workingRecord.getFrom(), workingRecord.getTo());
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


    public class AccountTypeViewBinder extends RegularSpinnerAdapter.ViewHolder<AccountIndentNode> {

        public AccountTypeViewBinder(RegularSpinnerAdapter adapter) {
            super(adapter);
        }

        @Override
        public void bindViewValue(AccountIndentNode item, LinearLayout vlayout, TextView vtext, boolean isDropdown, boolean isSelected) {
            I18N i18n = i18n();

            Map<AccountType, Integer> textColorMap = getAccountTextColorMap();
            Map<AccountType, Integer> bgColorMap = getAccountBgColorMap();

            AccountType at = item.getType();

            int textColor = textColorMap.get(at);


            StringBuilder display = new StringBuilder();

            if (isDropdown) {
                vlayout.setPadding((int) ((1 + item.getIndent()) * nodePaddingBase), vlayout.getPaddingTop(), vlayout.getPaddingRight(), vlayout.getPaddingBottom());

                if (item.getIndent() == 0) {
                    display.append(item.getType().getDisplay(i18n));
                    display.append(" - ");
                }
                display.append(item.getName());

                if (item.getAccount() == null) {
                    textColor = Colors.lighten(textColor, 0.3f);
                } else if (isSelected) {
                    textColor = Colors.darken(textColor, 0.3f);
                }
            } else {
                if (item.getAccount() != null) {
                    display.append(item.getType().getDisplay(i18n));
                    display.append("-");
                    display.append(item.getAccount().getName());
                }
            }
            vtext.setTextColor(textColor);
            vtext.setText(display.toString());
        }
    }
}
