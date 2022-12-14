package com.colaorange.dailymoney.core.ui;

/**
 * @author dennis
 */
public class Constants {

    public static final String ACTION_CREATE_RECORD = "com.colaorange.dailymoney.action.createRecord";

    static final public String PREFS_WORKING_BOOK_ID = "working_book_id";

    static final public String PREFS_HIERARCHICAL_REPORT = "hierarchical_report";
    public static final String PREFS_LAST_BACKUP = "last_backup";

    public static final String PREFS_LAST_FROM_ACCOUNT = "last_from_account";
    public static final String PREFS_LAST_TO_ACCOUNT = "last_to_account";


    public static final int REQUEST_CALCULATOR_CODE = 1;
    public static final int REQUEST_RECORD_EDITOR_CODE = 2;
    public static final int REQUEST_ACCOUNT_EDITOR_CODE = 3;
    public static final int REQUEST_ACCOUNT_RECORD_LIST_CODE = 4;
    public static final int REQUEST_PASSWORD_PROTECTION_CODE = 5;
    public static final int REQUEST_BOOK_EDITOR_CODE = 6;

    public static final int YEAR_LOOK_AFTER = 200;
    public static final int MONTH_LOOK_AFTER = YEAR_LOOK_AFTER * 12;
    public static final int WEEK_LOOK_AFTER = YEAR_LOOK_AFTER * 52;
    public static final int DAY_LOOK_AFTER = YEAR_LOOK_AFTER * 365;


    public static final String LOCAL_URL_PREFIX = "file:///android_asset/";


    public static interface Hint {
        String RECORD_SLIDE = "record-hint1";
        String BALANCE_SLIDE = "balance-hint1";
    }

}
