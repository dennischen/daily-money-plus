package com.colaorange.dailymoney.core.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.colaorange.commons.util.CalendarHelper;
import com.colaorange.commons.util.Numbers;
import com.colaorange.dailymoney.core.util.Logger;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import static com.colaorange.dailymoney.core.data.DataMeta.*;

/**
 * @author dennis
 */
public class SQLiteDataProvider implements IDataProvider {

    SQLiteDataHelper helper;
    CalendarHelper calHelper;

    public SQLiteDataProvider(SQLiteDataHelper helper, CalendarHelper calHelper) {
        this.helper = helper;
        this.calHelper = calHelper;
    }

    @Override
    public void init() {

    }

    @Override
    public void close() {
        helper.close();
    }

    private String normalizeAccountId(String type, String name) {
        name = name.trim().toLowerCase().replace(' ', '-');
        return type + "-" + name;
    }

    @Override
    public void reset() {
        SQLiteDatabase db = helper.getWritableDatabase();
        helper.onUpgrade(db, -1, db.getVersion());
        detId = 0;
        detId_set = false;
    }

    @Override
    public Account findAccount(String id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(TB_ACC, COL_ACC_ALL, COL_ACC_ID + " = ?", new String[]{id}, null, null, null, "1");
        Account acc = null;
        if (c.moveToNext()) {
            acc = new Account();
            applyCursor(acc, c);
        }
        c.close();
        return acc;
    }

    private void applyCursor(Account acc, Cursor c) {
        int i = 0;
        for (String n : c.getColumnNames()) {
            if (n.equals(COL_ACC_ID)) {
                acc.setId(c.getString(i));
            } else if (n.equals(COL_ACC_NAME)) {
                acc.setName(c.getString(i));
            } else if (n.equals(COL_ACC_TYPE)) {
                acc.setType(c.getString(i));
            } else if (n.equals(COL_ACC_CASHACCOUNT)) {
                //nullable
                acc.setCashAccount(c.getInt(i) == 1);
            } else if (n.equals(COL_ACC_INITVAL)) {
                acc.setInitialValue(c.getDouble(i));
            } else if (n.equals(COL_ACC_PRIORITY)) {
                try {
                    acc.setPriority(c.getInt(i));
                }catch(Exception x){
                    //prevent null
                }
            }
            i++;
        }
    }

    private void applyContextValue(Account acc, ContentValues values) {
        values.put(COL_ACC_ID, acc.getId());
        values.put(COL_ACC_NAME, acc.getName());
        values.put(COL_ACC_TYPE, acc.getType());
        values.put(COL_ACC_CASHACCOUNT, acc.isCashAccount() ? 1 : 0);
        values.put(COL_ACC_INITVAL, acc.getInitialValue());
        values.put(COL_ACC_PRIORITY, acc.getPriority());
    }

    @Override
    public Account findAccount(String type, String name) {
        String id = normalizeAccountId(type, name);
        return findAccount(id);
    }

    @Override
    public List<Account> listAccount(AccountType type) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        if (type == null) {
            c = db.query(TB_ACC, COL_ACC_ALL, null, null, null, null, COL_ACC_ID);
        } else {
            c = db.query(TB_ACC, COL_ACC_ALL, COL_ACC_TYPE + " = ?", new String[]{type.getType()}, null, null,
                    COL_ACC_ID);
        }
        List<Account> result = new ArrayList<Account>();
        Account acc;
        while (c.moveToNext()) {
            acc = new Account();
            applyCursor(acc, c);
            result.add(acc);
        }
        c.close();


        //sort programming
        if (type == null) {
            Collections.sort(result, new Comparator<Account>() {
                @Override
                public int compare(Account o1, Account o2) {
                    if(o1.getType().equals(o2.getType())) {
                        if (o1.getPriority() == o2.getPriority()) {
                            return o1.getId().compareTo(o2.getId());
                        }
                        return o1.getPriority() > o2.getPriority() ? 1 : -1;
                    }
                    return o1.getType().compareTo(o2.getType());
                }
            });
        } else {
            Collections.sort(result, new Comparator<Account>() {
                @Override
                public int compare(Account o1, Account o2) {
                    if (o1.getPriority() == o2.getPriority()) {
                        return o1.getId().compareTo(o2.getId());
                    }
                    return o1.getPriority() > o2.getPriority() ? 1 : -1;
                }
            });
        }

        return result;
    }

    @Override
    public void newAccount(Account account) throws DuplicateKeyException {
        String id = normalizeAccountId(account.getType(), account.getName());
        newAccount(id, account);
    }

    public String toAccountId(Account account) {
        String id = normalizeAccountId(account.getType(), account.getName());
        return id;
    }

    public synchronized void newAccount(String id, Account account) throws DuplicateKeyException {
        if (findAccount(id) != null) {
            throw new DuplicateKeyException("duplicate account id " + id);
        }
        newAccountNoCheck(id, account);
    }

    @Override
    public void newAccountNoCheck(String id, Account account) {
        Logger.d("new account {}", id);

        account.setId(id);
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        applyContextValue(account, cv);
        db.insertOrThrow(TB_ACC, null, cv);
    }

    @Override
    public boolean updateAccount(String id, Account account) {
        Account acc = findAccount(id);
        if (acc == null) {
            return false;
        }

        // reset id, id is following the name;
        String newid = normalizeAccountId(account.getType(), account.getName());
        account.setId(newid);

        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        applyContextValue(account, cv);

        // use old id to update
        int r = db.update(TB_ACC, cv, COL_ACC_ID + " = ?", new String[]{id});


        if (r > 0) {
            //update the refereted detail id
            cv = new ContentValues();
            cv.put(COL_DET_FROM, newid);
            cv.put(COL_DET_FROM_TYPE, account.getType());
            db.update(TB_DET, cv, COL_DET_FROM + " = ?", new String[]{id});

            cv = new ContentValues();
            cv.put(COL_DET_TO, newid);
            cv.put(COL_DET_TO_TYPE, account.getType());
            db.update(TB_DET, cv, COL_DET_TO + " = ?", new String[]{id});
        }


        return r > 0;
    }

    @Override
    public boolean deleteAccount(String id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int r = db.delete(TB_ACC, COL_ACC_ID + " = ?", new String[]{id});
        return r > 0;
    }

    /**
     * detail impl.
     */

    private void applyCursor(Record det, Cursor c) {
        int i = 0;
        for (String n : c.getColumnNames()) {
            if (n.equals(COL_DET_ID)) {
                det.setId(c.getInt(i));
            } else if (n.equals(COL_DET_FROM)) {
                det.setFrom(c.getString(i));
            } else if (n.equals(COL_DET_TO)) {
                det.setTo(c.getString(i));
            } else if (n.equals(COL_DET_DATE)) {
                det.setDate(new Date(c.getLong(i)));
            } else if (n.equals(COL_DET_MONEY)) {
                det.setMoney(c.getDouble(i));
            } else if (n.equals(COL_DET_ARCHIVED)) {
                det.setArchived((c.getInt(i) == 1));
            } else if (n.equals(COL_DET_NOTE)) {
                det.setNote(c.getString(i));
            }
            i++;
        }
    }

    private void applyContextValue(Record record, ContentValues values) {
        values.put(COL_DET_ID, record.getId());
        values.put(COL_DET_FROM, record.getFrom());
        values.put(COL_DET_FROM_TYPE, record.getFromType());
        values.put(COL_DET_TO, record.getTo());
        values.put(COL_DET_TO_TYPE, record.getToType());
        values.put(COL_DET_DATE, record.getDate().getTime());
        values.put(COL_DET_MONEY, record.getMoney());
        values.put(COL_DET_ARCHIVED, record.isArchived() ? 1 : 0);
        values.put(COL_DET_NOTE, record.getNote());
    }

    @Override
    public Record findRecord(int id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(TB_DET, COL_DET_ALL, COL_DET_ID + " = " + id, null, null, null, null, "1");
        Record det = null;
        if (c.moveToNext()) {
            det = new Record();
            applyCursor(det, c);
        }
        c.close();
        return det;
    }

    static int detId = 0;
    static boolean detId_set;

    public synchronized int nextRecordId() {
        if (!detId_set) {
            SQLiteDatabase db = helper.getReadableDatabase();
            Cursor c = db.rawQuery("SELECT MAX(" + DataMeta.COL_DET_ID + ") FROM " + DataMeta.TB_DET, null);
            if (c.moveToNext()) {
                detId = c.getInt(0);
            }
            detId_set = true;
            c.close();
        }
        return ++detId;
    }

    @Override
    public void newRecord(Record record) {
        int id = nextRecordId();
        try {
            newRecord(id, record);
        } catch (DuplicateKeyException e) {
            Logger.e(e.getMessage(), e);
        }
    }

    public void newRecord(int id, Record record) throws DuplicateKeyException {
        if (findRecord(id) != null) {
            throw new DuplicateKeyException("duplicate record id " + id);
        }
        newRecordNoCheck(id, record);
    }

    @Override
    public void newRecordNoCheck(int id, Record record) {
        Logger.d("new record {}, {}", id, record.getNote());

        first = null;
        record.setId(id);
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        applyContextValue(record, cv);
        db.insertOrThrow(TB_DET, null, cv);
    }

    @Override
    public boolean updateRecord(int id, Record record) {
        Record det = findRecord(id);
        if (det == null) {
            return false;
        }
        first = null;
        //set id, record might have a dirty id from copy or zero
        record.setId(id);
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        applyContextValue(record, cv);

        //use old id to update
        int r = db.update(TB_DET, cv, COL_DET_ID + " = " + id, null);
        return r > 0;
    }

    @Override
    public boolean deleteRecord(int id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        first = null;
        int r = db.delete(TB_DET, COL_DET_ID + " = " + id, null);
        return r > 0;
    }

    @Override
    public List<Record> listAllRecord() {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        c = db.query(TB_DET, COL_DET_ALL, null, null, null, null, DET_ORDERBY);
        List<Record> result = new ArrayList<Record>();
        Record det;
        while (c.moveToNext()) {
            det = new Record();
            applyCursor(det, c);
            result.add(det);
        }
        c.close();
        return result;
    }


    static final String DET_ORDERBY = COL_DET_DATE + " DESC," + COL_DET_ID + " DESC";

    @Override
    public List<Record> listRecord(Date start, Date end, int max) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        StringBuilder where = new StringBuilder();
        where.append(" 1=1 ");
        if (start != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + ">=" + start.getTime());
        }
        if (end != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + "<=" + end.getTime());
        }

        c = db.query(TB_DET, COL_DET_ALL, where.length() == 0 ? null : where.toString(), null, null, null, DET_ORDERBY, max > 0 ? Integer.toString(max) : null);
        List<Record> result = new ArrayList<Record>();
        Record det;
        while (c.moveToNext()) {
            det = new Record();
            applyCursor(det, c);
            result.add(det);
        }
        c.close();
        return result;
    }

    @Override
    public List<Record> searchRecord(SearchCondition condition, int max) {

        String fromAccountId = condition.getFromAccountId();
        String toAccountId = condition.getToAccountId();
        Double fromMoney = condition.getFromMoney();
        Double toMoney = condition.getToMoney();
        Date fromDate = condition.getFromDate();
        Date toDate = condition.getToDate();
        String note = condition.getNote();
        List<String> args = new ArrayList<String>();

        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = null;
        StringBuilder where = new StringBuilder();
        where.append(" 1=1 ");
        if (fromAccountId != null) {
            String nestedId = fromAccountId + ".%";
            where.append(" AND (");
            where.append(COL_DET_FROM + " = ? OR ");
            where.append(COL_DET_FROM + " LIKE ? ");
            where.append(")");
            args.add(fromAccountId);
            args.add(nestedId);
        }
        if (toAccountId != null) {
            String nestedId = toAccountId + ".%";
            where.append(" AND (");
            where.append(COL_DET_TO + " = ? OR ");
            where.append(COL_DET_TO + " LIKE ? ");
            where.append(")");
            args.add(toAccountId);
            args.add(nestedId);
        }
        if (fromDate != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + ">=" + fromDate.getTime());
        }
        if (toDate != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + "<=" + toDate.getTime());
        }
        if (fromMoney != null) {
            where.append(" AND ");
            where.append(COL_DET_MONEY + ">=" + Numbers.format(fromMoney, "#0.##"));
        }
        if (toMoney != null) {
            where.append(" AND ");
            where.append(COL_DET_MONEY + "<=" + Numbers.format(toMoney, "#0.##"));
        }
        if (note != null) {
            where.append(" AND ");
            where.append(COL_DET_NOTE + " like ?");
            args.add("%" + note + "%");
        }

        String[] whereArg = null;
        if (args.size() > 0) {
            whereArg = args.toArray(new String[args.size()]);
        }

        c = db.query(TB_DET, COL_DET_ALL, where.length() == 0 ? null : where.toString(), whereArg, null, null, DET_ORDERBY, max > 0 ? Integer.toString(max) : null);
        List<Record> result = new ArrayList<Record>();
        Record det;
        while (c.moveToNext()) {
            det = new Record();
            applyCursor(det, c);
            result.add(det);
        }
        c.close();
        return result;
    }

    @Override
    public List<Record> listRecord(Account account, int mode, Date start, Date end, int max) {
        return listRecord(account.getId(), mode, start, end, max);
    }

    @Override
    public List<Record> listRecord(String accountId, int mode, Date start, Date end, int max) {
        SQLiteDatabase db = helper.getReadableDatabase();
        StringBuilder where = new StringBuilder();
        List<String> args = new ArrayList<String>();
        String nestedId = accountId + ".%";
        where.append(" 1=1 ");
        if (mode == LIST_RECORD_MODE_FROM) {
            where.append(" AND (");
            where.append(COL_DET_FROM + " = ? OR ");
            where.append(COL_DET_FROM + " LIKE ? ");
            where.append(")");
            args.add(accountId);
            args.add(nestedId);
        } else if (mode == LIST_RECORD_MODE_TO) {
            where.append(" AND (");
            where.append(COL_DET_TO + " = ? OR ");
            where.append(COL_DET_TO + " LIKE ? ");
            where.append(")");
            args.add(accountId);
            args.add(nestedId);
        } else if (mode == LIST_RECORD_MODE_BOTH) {
            where.append(" AND (");
            where.append(COL_DET_FROM + " = ? OR ");
            where.append(COL_DET_FROM + " LIKE ? OR ");
            where.append(COL_DET_TO + " = ? OR ");
            where.append(COL_DET_TO + " LIKE ? ");
            where.append(")");
            args.add(accountId);
            args.add(nestedId);
            args.add(accountId);
            args.add(nestedId);
        }

        if (start != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + ">=" + start.getTime());
        }
        if (end != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + "<=" + end.getTime());
        }
        String[] whereArg = null;
        if (args.size() > 0) {
            whereArg = args.toArray(new String[args.size()]);
        }
        Cursor c = null;
        c = db.query(TB_DET, COL_DET_ALL, where.length() == 0 ? null : where.toString(), whereArg, null, null, DET_ORDERBY, max > 0 ? Integer.toString(max) : null);
        List<Record> result = new ArrayList<Record>();
        Record det;
        while (c.moveToNext()) {
            det = new Record();
            applyCursor(det, c);
            result.add(det);
        }
        c.close();
        return result;
    }

    @Override
    public List<Record> listRecord(AccountType type, int mode, Date start, Date end, int max) {
        SQLiteDatabase db = helper.getReadableDatabase();

        StringBuilder where = new StringBuilder();
        where.append(" 1=1 ");
        if (mode == LIST_RECORD_MODE_FROM) {
            where.append(" AND ");
            where.append(COL_DET_FROM_TYPE + "= '" + type.getType() + "'");
        } else if (mode == LIST_RECORD_MODE_TO) {
            where.append(" AND ");
            where.append(COL_DET_TO_TYPE + "= '" + type.getType() + "'");
        } else if (mode == LIST_RECORD_MODE_BOTH) {
            where.append(" AND (");
            where.append(COL_DET_FROM_TYPE + "= '" + type.getType() + "' OR ");
            where.append(COL_DET_TO_TYPE + "= '" + type.getType() + "')");
        }

        if (start != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + ">=" + start.getTime());
        }
        if (end != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + "<=" + end.getTime());
        }

        Cursor c = null;
        c = db.query(TB_DET, COL_DET_ALL, where.length() == 0 ? null : where.toString(), null, null, null, DET_ORDERBY, max > 0 ? Integer.toString(max) : null);
        List<Record> result = new ArrayList<Record>();
        Record det;
        while (c.moveToNext()) {
            det = new Record();
            applyCursor(det, c);
            result.add(det);
        }
        c.close();
        return result;
    }

    @Override
    public int countRecord(Date start, Date end) {
        SQLiteDatabase db = helper.getReadableDatabase();

        StringBuilder query = new StringBuilder();

        StringBuilder where = new StringBuilder();
        where.append(" 1=1 ");
        if (start != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + ">=" + start.getTime());
        }
        if (end != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + "<=" + end.getTime());
        }

        query.append("SELECT COUNT(").append(COL_DET_ID).append(") FROM ").append(TB_DET);

        if (where.length() > 0) {
            query.append(" WHERE ").append(where);
        }


        Cursor c = db.rawQuery(query.toString(), null);

        int i = 0;
        if (c.moveToNext()) {
            i = c.getInt(0);
        }

        c.close();
        return i;
    }

    @Override
    public int countRecord(Account account, int mode, Date start, Date end) {
        return countRecord(account.getId(), mode, start, end);
    }

    @Override
    public int countRecord(String accountId, int mode, Date start, Date end) {
        SQLiteDatabase db = helper.getReadableDatabase();
        String nestedId = accountId + ".%";
        StringBuilder query = new StringBuilder();
        List<String> args = new ArrayList<String>();
        StringBuilder where = new StringBuilder();
        where.append(" 1=1 ");
        if (mode == LIST_RECORD_MODE_FROM) {
            where.append(" AND (");
            where.append(COL_DET_FROM + " = ? OR ");
            where.append(COL_DET_FROM + " LIKE ? ");
            where.append(")");
            args.add(accountId);
            args.add(nestedId);
        } else if (mode == LIST_RECORD_MODE_TO) {
            where.append(" AND (");
            where.append(COL_DET_TO + " = ? OR ");
            where.append(COL_DET_TO + " LIKE ? ");
            where.append(")");
            args.add(accountId);
            args.add(nestedId);
        } else if (mode == LIST_RECORD_MODE_BOTH) {
            where.append(" AND (");
            where.append(COL_DET_FROM + " = ? OR ");
            where.append(COL_DET_FROM + " LIKE ? OR ");
            where.append(COL_DET_TO + " = ? OR ");
            where.append(COL_DET_TO + " LIKE ? ");
            where.append(")");
            args.add(accountId);
            args.add(nestedId);
            args.add(accountId);
            args.add(nestedId);
        }


        if (start != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + ">=" + start.getTime());
        }
        if (end != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + "<=" + end.getTime());
        }

        query.append("SELECT COUNT(").append(COL_DET_ID).append(") FROM ").append(TB_DET);

        if (where.length() > 0) {
            query.append(" WHERE ").append(where);
        }

        String[] wherearg = null;
        if (args.size() > 0) {
            wherearg = args.toArray(wherearg = new String[args.size()]);
        }

        Cursor c = db.rawQuery(query.toString(), wherearg);

        int i = 0;
        if (c.moveToNext()) {
            i = c.getInt(0);
        }

        c.close();
        return i;
    }

    @Override
    public int countRecord(AccountType type, int mode, Date start, Date end) {
        SQLiteDatabase db = helper.getReadableDatabase();

        StringBuilder query = new StringBuilder();

        StringBuilder where = new StringBuilder();
        where.append(" 1=1 ");

        if (mode == LIST_RECORD_MODE_FROM) {
            where.append(" AND ");
            where.append(COL_DET_FROM_TYPE + "= '" + type.getType() + "'");
        } else if (mode == LIST_RECORD_MODE_TO) {
            where.append(" AND ");
            where.append(COL_DET_TO_TYPE + "= '" + type.getType() + "'");
        } else if (mode == LIST_RECORD_MODE_BOTH) {
            where.append(" AND (");
            where.append(COL_DET_FROM_TYPE + "= '" + type.getType() + "' OR ");
            where.append(COL_DET_TO_TYPE + "= '" + type.getType() + "')");
        }

        if (start != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + ">=" + start.getTime());
        }
        if (end != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + "<=" + end.getTime());
        }

        query.append("SELECT COUNT(").append(COL_DET_ID).append(") FROM ").append(TB_DET);

        if (where.length() > 0) {
            query.append(" WHERE ").append(where);
        }


        Cursor c = db.rawQuery(query.toString(), null);

        int i = 0;
        if (c.moveToNext()) {
            i = c.getInt(0);
        }

        c.close();
        return i;
    }

    @Override
    public double sumFrom(AccountType type, Date start, Date end) {
        SQLiteDatabase db = helper.getReadableDatabase();

        StringBuilder query = new StringBuilder();

        StringBuilder where = new StringBuilder();
        where.append(" WHERE ").append(COL_DET_FROM_TYPE).append(" = '").append(type.type).append("'");
        if (start != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + ">=" + start.getTime());
        }
        if (end != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + "<=" + end.getTime());
        }

        query.append("SELECT SUM(").append(COL_DET_MONEY).append(") FROM ").append(TB_DET).append(where);

        Cursor c = db.rawQuery(query.toString(), null);

        double r = 0D;
        if (c.moveToNext()) {
            r = c.getDouble(0);
        }

        c.close();
        return r;
    }

    @Override
    public double sumFrom(Account acc, Date start, Date end) {
        SQLiteDatabase db = helper.getReadableDatabase();

        StringBuilder query = new StringBuilder();
        List<String> args = new ArrayList<String>();
        StringBuilder where = new StringBuilder();
        where.append(" WHERE ").append(COL_DET_FROM).append(" = ? ");
        args.add(acc.getId());
        if (start != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + ">=" + start.getTime());
        }
        if (end != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + "<=" + end.getTime());
        }

        query.append("SELECT SUM(").append(COL_DET_MONEY).append(") FROM ").append(TB_DET).append(where);

        String[] wherearg = null;
        if (args.size() > 0) {
            wherearg = args.toArray(wherearg = new String[args.size()]);
        }

        Cursor c = db.rawQuery(query.toString(), wherearg);

        double r = 0D;
        if (c.moveToNext()) {
            r = c.getDouble(0);
        }

        c.close();
        return r;
    }

    @Override
    public double sumTo(AccountType type, Date start, Date end) {
        SQLiteDatabase db = helper.getReadableDatabase();

        StringBuilder query = new StringBuilder();

        StringBuilder where = new StringBuilder();
        where.append(" WHERE ").append(COL_DET_TO_TYPE).append(" = '").append(type.type).append("'");
        if (start != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + ">=" + start.getTime());
        }
        if (end != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + "<=" + end.getTime());
        }

        query.append("SELECT SUM(").append(COL_DET_MONEY).append(") FROM ").append(TB_DET).append(where);

        Cursor c = db.rawQuery(query.toString(), null);

        double r = 0D;
        if (c.moveToNext()) {
            r = c.getDouble(0);
        }

        c.close();
        return r;
    }

    @Override
    public double sumTo(Account acc, Date start, Date end) {
        SQLiteDatabase db = helper.getReadableDatabase();

        StringBuilder query = new StringBuilder();
        List<String> args = new ArrayList<String>();
        StringBuilder where = new StringBuilder();
        where.append(" WHERE ").append(COL_DET_TO).append(" = ?");
        args.add(acc.getId());
        if (start != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + ">=" + start.getTime());
        }
        if (end != null) {
            where.append(" AND ");
            where.append(COL_DET_DATE + "<=" + end.getTime());
        }

        query.append("SELECT SUM(").append(COL_DET_MONEY).append(") FROM ").append(TB_DET).append(where);
        String[] wherearg = null;
        if (args.size() > 0) {
            wherearg = args.toArray(wherearg = new String[args.size()]);
        }

        Cursor c = db.rawQuery(query.toString(), wherearg);

        double r = 0D;
        if (c.moveToNext()) {
            r = c.getDouble(0);
        }

        c.close();
        return r;
    }

    @Override
    public void deleteAllAccount() {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(TB_ACC, null, null);
        return;


    }

    @Override
    public void deleteAllRecord() {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(TB_DET, null, null);
        detId = 0;
        detId_set = false;
        first = null;
        return;


    }


    Record first = null;

    @Override
    public Record getFirstRecord() {
        if (first != null) return first;
        SQLiteDatabase db = helper.getReadableDatabase();
        StringBuilder where = new StringBuilder();
        where.append(" 1=1 ");
        Cursor c = null;
        c = db.query(TB_DET, COL_DET_ALL, where.length() == 0 ? null : where.toString(), null, null, null, COL_DET_DATE, Integer.toString(1));
        first = null;
        if (c.moveToNext()) {
            first = new Record();
            applyCursor(first, c);
        }
        c.close();
        return first;
    }

    @Override
    public double sumInitialValue(AccountType type) {
        SQLiteDatabase db = helper.getReadableDatabase();

        StringBuilder query = new StringBuilder();

        StringBuilder where = new StringBuilder();
        where.append(" WHERE ").append(COL_ACC_TYPE).append(" = '").append(type.type).append("'");

        query.append("SELECT SUM(").append(COL_ACC_INITVAL).append(") FROM ").append(TB_ACC).append(where);

        Cursor c = db.rawQuery(query.toString(), null);

        double r = 0D;
        if (c.moveToNext()) {
            r = c.getDouble(0);
        }

        c.close();
        return r;
    }

}
