package com.colaorange.dailymoney.ui.legacy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.colaorange.commons.util.Files;
import com.colaorange.commons.util.Formats;
import com.colaorange.commons.util.GUIs;
import com.colaorange.commons.util.Logger;
import com.colaorange.dailymoney.context.Contexts;
import com.colaorange.dailymoney.context.ContextsActivity;
import com.colaorange.dailymoney.R;
import com.colaorange.dailymoney.data.Account;
import com.colaorange.dailymoney.data.BackupRestorer;
import com.colaorange.dailymoney.data.DataCreator;
import com.colaorange.dailymoney.data.Detail;
import com.colaorange.dailymoney.data.IDataProvider;
import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

/**
 * @author dennis
 */
public class DataMaintenanceActivity extends ContextsActivity implements OnClickListener {

    String csvEncoding;

    File workingFolder;

    static final String APPVER = "appver:";

    int vercode = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.data_maintenance);
        workingFolder = contexts().getWorkingFolder();

        vercode = contexts().getAppVerCode();
        csvEncoding = preference().getCSVEncoding();

        initMembers();


    }

    @Override
    public void onStart() {
        super.onStart();
        refreshUI();
    }

    private void refreshUI() {

        Button requestPermissionBtn = findViewById(R.id.data_maintenance_request_permission);
        //only for 6.0(23+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !contexts().hasWorkingFolderPermission()) {
            requestPermissionBtn.setVisibility(View.VISIBLE);
        } else {
            requestPermissionBtn.setVisibility(View.GONE);
        }

        //working fodler accessibility
        TextView workingFolderText = findViewById(R.id.data_maintenance_workingfolder);
        //test accessable
        if (contexts().hasWorkingFolderPermission()) {
            workingFolderText.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(android.R.drawable.ic_dialog_info), null, null, null);
            workingFolderText.setText(workingFolder.getAbsolutePath());
        } else {
            workingFolderText.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(android.R.drawable.ic_dialog_alert), null, null, null);
            workingFolderText.setText(i18n().string(R.string.msg_working_folder_no_access, workingFolder.getAbsolutePath()));
        }

        TextView lastBackupText = findViewById(R.id.datamain_lastbackup);

        if (preference().getLastBackup() != null) {
            lastBackupText.setText(preference().getLastBackup());
        }
    }

    private void initMembers() {
        findViewById(R.id.data_maintenance_request_permission).setOnClickListener(this);
        findViewById(R.id.data_maintenance_backup).setOnClickListener(this);
        findViewById(R.id.data_maintenance_export_csv).setOnClickListener(this);
        findViewById(R.id.data_maintenance_share_csv).setOnClickListener(this);

        findViewById(R.id.data_maintenance_restore).setOnClickListener(this);
        findViewById(R.id.data_maintenance_import_csv).setOnClickListener(this);

        //TODO move to developer
        findViewById(R.id.data_maintenance_reset).setOnClickListener(this);
        findViewById(R.id.data_maintenance_clear_folder).setOnClickListener(this);
        findViewById(R.id.data_maintenance_create_default).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.data_maintenance_request_permission) {
            doRequestPermission();
        } else if (v.getId() == R.id.data_maintenance_import_csv) {
            doImportCSV();
        } else if (v.getId() == R.id.data_maintenance_export_csv) {
            doExportCSV();
        } else if (v.getId() == R.id.data_maintenance_share_csv) {
            doShareCSV();
        } else if (v.getId() == R.id.data_maintenance_backup) {
            doBackup();
        } else if (v.getId() == R.id.data_maintenance_restore) {
            doRestore();
        } else if (v.getId() == R.id.data_maintenance_reset) {
            doReset();
        } else if (v.getId() == R.id.data_maintenance_create_default) {
            doCreateDefault();
        } else if (v.getId() == R.id.data_maintenance_clear_folder) {
            doClearFolder();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void doRequestPermission() {
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
    }

    private void doBackup() {
        final long now = System.currentTimeMillis();
        final GUIs.IBusyRunnable job = new GUIs.BusyAdapter() {
            BackupRestorer.Result result;

            public void onBusyError(Throwable t) {
                GUIs.error(DataMaintenanceActivity.this, t);
            }

            public void onBusyFinish() {
                if (result.isSuccess()) {
                    String count = "" + (result.getDb() + result.getPref());
                    String msg = i18n().string(R.string.msg_db_backuped, count, workingFolder);
                    preference().setLastBackupTime(now);
                    GUIs.alert(DataMaintenanceActivity.this, msg);
                    refreshUI();
                } else {
                    GUIs.alert(DataMaintenanceActivity.this, result.getErr());
                }
            }

            @Override
            public void run() {
                result = BackupRestorer.backup();
                trackEvent("backup");
            }
        };
        GUIs.doBusy(DataMaintenanceActivity.this, job);
    }

    private void doRestore() {
        final GUIs.IBusyRunnable job = new GUIs.BusyAdapter() {
            BackupRestorer.Result result;
            Long lastBakcup;

            public void onBusyError(Throwable t) {
                GUIs.error(DataMaintenanceActivity.this, t);
            }

            public void onBusyFinish() {
                if (result.isSuccess()) {
                    String count = "" + (result.getDb() + result.getPref());
                    String msg = i18n().string(R.string.msg_db_restored, count, workingFolder);
                    if (lastBakcup != null) {
                        preference().setLastBackupTime(lastBakcup);
                    }
                    GUIs.alert(DataMaintenanceActivity.this, msg);
                } else {
                    GUIs.alert(DataMaintenanceActivity.this, result.getErr());
                }
            }

            @Override
            public void run() {
                lastBakcup = preference().getLastBackupTime();
                result = BackupRestorer.restore();
                trackEvent("restore");
            }
        };


        GUIs.confirm(this, i18n().string(R.string.qmsg_restore_data), new GUIs.OnFinishListener() {
            @Override
            public boolean onFinish(Object data) {
                if (((Integer) data).intValue() == GUIs.OK_BUTTON) {
                    GUIs.doBusy(DataMaintenanceActivity.this, job);
                }
                return true;
            }
        });
    }

    private void doReset() {
        new AlertDialog.Builder(this).setTitle(i18n().string(R.string.qmsg_reset))
                .setItems(R.array.csv_type_options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int which) {
                        final GUIs.IBusyRunnable job = new GUIs.BusyAdapter() {
                            public void onBusyError(Throwable t) {
                                GUIs.error(DataMaintenanceActivity.this, t);
                            }

                            @Override
                            public void run() {
                                try {
                                    _resetDate(which);
                                } catch (Exception e) {
                                    throw new RuntimeException(e.getMessage(), e);
                                }
                            }
                        };
                        GUIs.doBusy(DataMaintenanceActivity.this, job);
                    }
                }).show();
    }

    private void doClearFolder() {
        //TODO move to devlope
        final GUIs.IBusyRunnable job = new GUIs.BusyAdapter() {
            @Override
            public void onBusyFinish() {
                GUIs.alert(DataMaintenanceActivity.this, i18n().string(R.string.msg_folder_cleared, workingFolder));
            }

            @Override
            public void run() {
                for (File f : workingFolder.listFiles()) {
                    String fnm = f.getName().toLowerCase();
                    //don't delete sub folder
                    if (f.isFile()) {
                        f.delete();
                    }
                }
            }
        };

        GUIs.confirm(this, i18n().string(R.string.qmsg_clear_folder, workingFolder), new GUIs.OnFinishListener() {
            @Override
            public boolean onFinish(Object data) {
                if (((Integer) data).intValue() == GUIs.OK_BUTTON) {
                    GUIs.doBusy(DataMaintenanceActivity.this, job);
                }
                return true;
            }
        });

    }

    private void doCreateDefault() {

        final GUIs.IBusyRunnable job = new GUIs.BusyAdapter() {
            @Override
            public void onBusyFinish() {
                GUIs.alert(DataMaintenanceActivity.this, R.string.msg_default_created);
            }

            @Override
            public void run() {
                IDataProvider idp = contexts().getDataProvider();
                new DataCreator(idp, i18n()).createDefaultAccount();
            }
        };

        GUIs.confirm(this, i18n().string(R.string.qmsg_create_default), new GUIs.OnFinishListener() {
            @Override
            public boolean onFinish(Object data) {
                if (((Integer) data).intValue() == GUIs.OK_BUTTON) {
                    GUIs.doBusy(DataMaintenanceActivity.this, job);
                }
                return true;
            }
        });
    }

    private void doExportCSV() {
        final int workingBookId = contexts().getWorkingBookId();
        new AlertDialog.Builder(this).setTitle(i18n().string(R.string.qmsg_export_csv))
                .setItems(R.array.csv_type_options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int which) {
                        final GUIs.IBusyRunnable job = new GUIs.BusyAdapter() {
                            int count = -1;

                            public void onBusyError(Throwable t) {
                                GUIs.error(DataMaintenanceActivity.this, t);
                            }

                            public void onBusyFinish() {
                                if (count >= 0) {
                                    String msg = i18n().string(R.string.msg_csv_exported, Integer.toString(count), workingFolder);
                                    GUIs.alert(DataMaintenanceActivity.this, msg);
                                } else {
                                    GUIs.alert(DataMaintenanceActivity.this, R.string.msg_no_csv);
                                }
                            }

                            @Override
                            public void run() {
                                try {
                                    count = _exportToCSV(which, workingBookId);
                                } catch (Exception e) {
                                    throw new RuntimeException(e.getMessage(), e);
                                }
                                trackEvent("export_csv");
                            }
                        };
                        GUIs.doBusy(DataMaintenanceActivity.this, job);
                    }
                }).show();
    }

    private void doImportCSV() {
        final int workingBookId = contexts().getWorkingBookId();
        new AlertDialog.Builder(this).setTitle(i18n().string(R.string.qmsg_import_csv))
                .setItems(R.array.csv_type_import_options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int which) {
                        final GUIs.IBusyRunnable job = new GUIs.BusyAdapter() {
                            int count = -1;

                            public void onBusyError(Throwable t) {
                                GUIs.error(DataMaintenanceActivity.this, t);
                            }

                            public void onBusyFinish() {
                                if (count >= 0) {
                                    String msg = i18n().string(R.string.msg_csv_imported, Integer.toString(count), workingFolder);
                                    GUIs.alert(DataMaintenanceActivity.this, msg);
                                } else {
                                    GUIs.alert(DataMaintenanceActivity.this, R.string.msg_no_csv);
                                }
                            }

                            @Override
                            public void run() {
                                try {
                                    count = _importFromCSV(which, workingBookId);
                                } catch (Exception e) {
                                    throw new RuntimeException(e.getMessage(), e);
                                }
                                trackEvent("import_csv");
                            }
                        };
                        GUIs.doBusy(DataMaintenanceActivity.this, job);
                    }
                }).show();
    }

    private void doShareCSV() {
        new AlertDialog.Builder(this).setTitle(i18n().string(R.string.qmsg_share_csv))
                .setItems(R.array.csv_type_options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int which) {
                        final GUIs.IBusyRunnable job = new GUIs.BusyAdapter() {
                            List<File> files;

                            public void onBusyError(Throwable t) {
                                GUIs.error(DataMaintenanceActivity.this, t);
                            }

                            public void onBusyFinish() {
                                if (files == null || files.size() < 0) {
                                    GUIs.alert(DataMaintenanceActivity.this, R.string.msg_no_csv);
                                } else {
                                    DateFormat df = preference().getDateFormat();
                                    contexts().shareTextContent(DataMaintenanceActivity.this, i18n().string(R.string.msg_share_csv_title, df.format(new Date())), i18n().string(R.string.msg_share_csv_content), files);
                                }
                            }

                            @Override
                            public void run() {
                                try {
                                    files = _shareCSV(which);
                                } catch (Exception e) {
                                    throw new RuntimeException(e.getMessage(), e);
                                }
                                trackEvent("share_csv");
                            }
                        };
                        GUIs.doBusy(DataMaintenanceActivity.this, job);
                    }
                }).show();
    }


    private File getWorkingFile(String name) {
        File file = new File(workingFolder, name);
        return file;
    }


    private void _resetDate(int mode) {
        if (Contexts.DEBUG) {
            Logger.d("reset date" + mode);
        }
        boolean account = false;
        boolean detail = false;
        switch (mode) {
            case 0:
                account = detail = true;
                break;
            case 1:
                account = true;
                break;
            case 2:
                detail = true;
                break;
        }
        IDataProvider idp = contexts().getDataProvider();
        if (account && detail) {
            idp.reset();
        } else if (account) {
            idp.deleteAllAccount();
        } else if (detail) {
            idp.deleteAllDetail();
        }

    }

    /**
     * running in thread
     **/
    private int _exportToCSV(int mode, int workingBookId) throws IOException {
        if (Contexts.DEBUG) {
            Logger.d("export to csv " + mode);
        }
        boolean account = false;
        boolean detail = false;
        switch (mode) {
            case 0:
                account = detail = true;
                break;
            case 1:
                account = true;
                break;
            case 2:
                detail = true;
                break;
            default:
                return -1;
        }
        IDataProvider idp = contexts().getDataProvider();
        StringWriter sw;
        CsvWriter csvw;
        int count = 0;
        String timestamp = preference().getBackupDateTimeFormat().format(new Date());
        boolean backupWithTime = preference().isBackupWithTimestamp();
        if (detail) {
            sw = new StringWriter();
            csvw = new CsvWriter(sw, ',');
            csvw.writeRecord(new String[]{"id", "from", "to", "date", "value", "note", "archived", APPVER + vercode});
            for (Detail d : idp.listAllDetail()) {
                count++;
                csvw.writeRecord(new String[]{Integer.toString(d.getId()), d.getFrom(), d.getTo(),
                        Formats.normalizeDate2String(d.getDate()), Formats.normalizeDouble2String(d.getMoney()),
                        d.getNote(), d.isArchived() ? "1" : "0"});
            }
            csvw.close();
            String csv = sw.toString();
            File file0 = getWorkingFile("details.csv");
            File file1 = getWorkingFile("details-" + workingBookId + ".csv");

            saveCSVFile(file0, csv, backupWithTime, timestamp);
            saveCSVFile(file1, csv, backupWithTime, timestamp);
        }

        if (account) {
            sw = new StringWriter();
            csvw = new CsvWriter(sw, ',');
            csvw.writeRecord(new String[]{"id", "type", "name", "init", "cash", APPVER + vercode});
            for (Account a : idp.listAccount(null)) {
                count++;
                csvw.writeRecord(new String[]{a.getId(), a.getType(), a.getName(), Formats.normalizeDouble2String(a.getInitialValue()), a.isCashAccount() ? "1" : "0"});
            }
            csvw.close();
            String csv = sw.toString();
            File file0 = getWorkingFile("accounts.csv");
            File file1 = getWorkingFile("accounts-" + workingBookId + ".csv");

            saveCSVFile(file0, csv, backupWithTime, timestamp);
            saveCSVFile(file1, csv, backupWithTime, timestamp);
        }

        return count;
    }

    private void saveCSVFile(File toFile, String csv, boolean backupWithTime, String timestamp) throws IOException {

        Files.saveString(csv, toFile, csvEncoding);

        if(backupWithTime) {
            File withTimeFodler = new File(toFile.getParent(), "csv-with-timestamp");
            if (!withTimeFodler.exists()) {
                withTimeFodler.mkdir();
            }
            Files.copyFileTo(toFile, new File(withTimeFodler, timestamp + "." + toFile.getName()));
        }

        if (Contexts.DEBUG) {
            Logger.d("export to " + toFile.toString());
        }
    }

    private int getAppver(String str) {
        if (str != null && str.startsWith(APPVER)) {
            try {
                return Integer.parseInt(str.substring(APPVER.length()));
            } catch (Exception x) {
                if (Contexts.DEBUG) {
                    Logger.d(x.getMessage());
                }
            }
        }
        return 0;
    }

    /**
     * running in thread
     *
     * @param workingBookId
     **/
    private int _importFromCSV(int mode, int workingBookId) throws Exception {
        if (Contexts.DEBUG) {
            Logger.d("import from csv " + mode);
        }
        boolean account = false;
        boolean detail = false;
        boolean shared = mode >= 3;
        if (shared) mode = mode - 3;
        switch (mode) {
            case 0:
                account = detail = true;
                break;
            case 1:
                account = true;
                break;
            case 2:
                detail = true;
                break;
            default:
                return -1;
        }

        IDataProvider idp = contexts().getDataProvider();
        File details = getWorkingFile(shared ? "details.csv" : "details-" + workingBookId + ".csv");
        File accounts = getWorkingFile(shared ? "accounts.csv" : "accounts-" + workingBookId + ".csv");

        if ((detail && (!details.exists() || !details.canRead())) ||
                (account && (!accounts.exists() || !accounts.canRead()))) {
            return -1;
        }

        CsvReader accountReader = null;
        CsvReader detailReader = null;
        try {
            int count = 0;
            if (account) {
                accountReader = new CsvReader(new InputStreamReader(new FileInputStream(accounts), csvEncoding));
            }
            if (detail) {
                detailReader = new CsvReader(new InputStreamReader(new FileInputStream(details), csvEncoding));
            }

            if ((accountReader != null && !accountReader.readHeaders())) {
                return -1;
            }

            //don't combine with account checker
            if ((detailReader != null && !detailReader.readHeaders())) {
                return -1;
            }

            if (detail) {
                detailReader.setTrimWhitespace(true);
                int appver = getAppver(detailReader.getHeaders()[detailReader.getHeaderCount() - 1]);

                idp.deleteAllDetail();
                while (detailReader.readRecord()) {
                    Detail det = new Detail(detailReader.get("from"), detailReader.get("to"), Formats.normalizeString2Date(detailReader.get("date")), Formats.normalizeString2Double(detailReader.get("value")), detailReader.get("note"));
                    String archived = detailReader.get("archived");
                    if ("1".equals(archived)) {
                        det.setArchived(true);
                    } else if ("0".equals(archived)) {
                        det.setArchived(false);
                    } else {
                        det.setArchived(Boolean.parseBoolean(archived));
                    }

                    idp.newDetailNoCheck(Integer.parseInt(detailReader.get("id")), det);
                    count++;
                }
                detailReader.close();
                detailReader = null;
                if (Contexts.DEBUG) {
                    Logger.d("import from " + details + " ver:" + appver);
                }
            }

            if (account) {
                accountReader.setTrimWhitespace(true);
                int appver = getAppver(accountReader.getHeaders()[accountReader.getHeaderCount() - 1]);
                idp.deleteAllAccount();
                while (accountReader.readRecord()) {
                    Account acc = new Account(accountReader.get("type"), accountReader.get("name"), Formats.normalizeString2Double(accountReader.get("init")));
                    String cash = accountReader.get("cash");
                    acc.setCashAccount("1".equals(cash));

                    idp.newAccountNoCheck(accountReader.get("id"), acc);
                    count++;
                }
                accountReader.close();
                accountReader = null;
                if (Contexts.DEBUG) {
                    Logger.d("import from " + accounts + " ver:" + appver);
                }
            }
            return count;
        } finally {
            if (accountReader != null) {
                accountReader.close();
            }
            if (detailReader != null) {
                detailReader.close();
            }
        }
    }


    /**
     * running in thread
     **/
    private List<File> _shareCSV(int mode) throws Exception {
        if (Contexts.DEBUG) {
            Logger.d("share csv " + mode);
        }
        List<File> files = new ArrayList<File>();
        boolean account = false;
        boolean detail = false;
        switch (mode) {
            case 0:
                account = detail = true;
                break;
            case 1:
                account = true;
                break;
            case 2:
                detail = true;
                break;
            default:
                return files;
        }

        File details = getWorkingFile("details.csv");
        File accounts = getWorkingFile("accounts.csv");

        if ((detail && (!details.exists() || !details.canRead())) ||
                (account && (!accounts.exists() || !accounts.canRead()))) {
            return files;
        }

        if (detail) {
            files.add(details);
        }

        if (account) {
            files.add(accounts);
        }

        return files;

    }
}