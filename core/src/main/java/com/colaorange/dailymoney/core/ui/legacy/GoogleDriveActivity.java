package com.colaorange.dailymoney.core.ui.legacy;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.colaorange.commons.util.Files;
import com.colaorange.commons.util.Strings;
import com.colaorange.dailymoney.core.R;
import com.colaorange.dailymoney.core.context.Contexts;
import com.colaorange.dailymoney.core.context.ContextsActivity;
import com.colaorange.dailymoney.core.data.DataBackupRestorer;
import com.colaorange.dailymoney.core.drive.GoogleDriveBackupRestorer;
import com.colaorange.dailymoney.core.drive.GoogleDriveHelper;
import com.colaorange.dailymoney.core.ui.GUIs;
import com.colaorange.dailymoney.core.ui.RegularSpinnerAdapter;
import com.colaorange.dailymoney.core.util.Logger;
import com.colaorange.dailymoney.core.util.Misc;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author dennis
 */
public class GoogleDriveActivity extends ContextsActivity implements OnClickListener {

//    public static final Scope SCOPE_DRIVE_FILE = new Scope("https://www.googleapis.com/auth/drive.file");
//    public static final Scope SCOPE_DRIVE_APPDATA = new Scope("https://www.googleapis.com/auth/drive.appdata");

    /*
    get  API: Drive.API_CONNECTIONLESS is not available on this device.
    com.google.android.gms.common.api.ApiException: 17: API: Drive.API_CONNECTIONLESS is not available on this device.
    when I use this scope
     */
//    private static final Scope SCOPE_DRIVE = new Scope("https://www.googleapis.com/auth/drive");

    private static final int REQUEST_DRIVE_AUTH = 101;
    private static final int REQUEST_PERMISSION = 201;

    GoogleDriveHelper gdHelper;
    GoogleDriveBackupRestorer gdBackupRestorer;

    File workingFolder;

    TextView vAuthInfo;
    Button btnRequestAuth;
    Button btnRequestRevoke;
    Button btnRequestBackup;
    Button btnRequestRestore;
    Button btnRequestClean;

    Spinner vFileList;

    private List<DriveFileInfo> fileList;
    private RegularSpinnerAdapter<DriveFileInfo> fileListAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.google_drive);
        workingFolder = contexts().getWorkingFolder();

        initMembers();
    }

    private void initMembers() {
        vAuthInfo = findViewById(R.id.auth_info);


        (btnRequestAuth = findViewById(R.id.request_auth)).setOnClickListener(this);
        (btnRequestRevoke = findViewById(R.id.request_revoke)).setOnClickListener(this);
        (btnRequestBackup = findViewById(R.id.request_backup)).setOnClickListener(this);
        (btnRequestRestore = findViewById(R.id.request_restore)).setOnClickListener(this);
        (btnRequestClean = findViewById(R.id.request_clean)).setOnClickListener(this);


        vFileList = findViewById(R.id.file_list);

        fileList = new LinkedList<>();
        fileListAdapter = new RegularSpinnerAdapter<DriveFileInfo>(this, fileList) {
            @Override
            public ViewHolder<DriveFileInfo> createViewHolder() {
                return new FileInfoViewBinder(this);
            }

            public boolean isSelected(int position) {
                return vFileList.getSelectedItemPosition() == position;
            }
        };
        vFileList.setAdapter(fileListAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        //only for 6.0(23+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !contexts().hasWorkingFolderPermission()) {
            if (!Contexts.instance().hasWorkingFolderPermission()) {
                doRequestPermission();
            } else {
                doRequestAuth(true);
            }
        } else {
            doRequestAuth(true);
        }


        refreshUI();
    }

    private void refreshUI() {
        if (gdHelper != null) {
            vAuthInfo.setText(i18n().string(R.string.label_signin_as, gdHelper.getGoogleSignInAccount().getDisplayName()));
            btnRequestAuth.setEnabled(false);
            btnRequestRevoke.setEnabled(true);
            btnRequestRestore.setEnabled(true);
            btnRequestBackup.setEnabled(true);
            btnRequestClean.setEnabled(true);
            vFileList.setEnabled(true);

        } else {
            vAuthInfo.setText(i18n().string(R.string.label_not_signin));
            btnRequestAuth.setEnabled(true);
            btnRequestRevoke.setEnabled(false);
            btnRequestRestore.setEnabled(false);
            btnRequestBackup.setEnabled(false);
            btnRequestClean.setEnabled(false);
            vFileList.setEnabled(false);
        }

        refreshFileList();
    }


    private DriveFolder getAppFolder() throws ExecutionException, InterruptedException {
        return gdBackupRestorer.getAppFolder();
    }

    private void refreshFileList() {
        fileList.clear();

        fileList.add(new DriveFileInfo(i18n().string(R.string.label_select_backup_file), null, -1));

        if (gdHelper != null) {
            GUIs.doBusy(this, new GUIs.IBusyRunnable() {

                String fileName;

                @Override
                public void onBusyFinish() {
                    fileListAdapter.notifyDataSetChanged();
                }

                @Override
                public void onBusyError(Throwable t) {
                    fileList.clear();
                    fileListAdapter.notifyDataSetChanged();
                    GUIs.alert(getApplicationContext(), t.getMessage());
                }

                public void run() {
                    try {
                        DriveFolder appFolder = getAppFolder();

                        List<Metadata> files = gdHelper.listChildren(appFolder);
                        for (Metadata data : files) {
                            if (!data.isFolder() && !data.isTrashed()) {
                                String title = data.getTitle();
                                if (title.toLowerCase().endsWith(".zip")) {
                                    DriveFile file = data.getDriveId().asDriveFile();
                                    fileList.add(new DriveFileInfo(title, file, data.getFileSize()));
                                }
                            }
                        }
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                }
            });
        }

        fileListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.request_auth) {
            doRequestAuth(false);
        } else if (v.getId() == R.id.request_revoke) {
            doRevoke();
        } else if (v.getId() == R.id.request_restore) {
            doRestore();
        } else if (v.getId() == R.id.request_backup) {
            doBackup();
        } else if (v.getId() == R.id.request_clean) {
            doClean();
        }
    }

    private void doRequestAuth(final boolean silentOnly) {

        Task<GoogleDriveHelper> task = GoogleDriveHelper.signIn(this);
        task.addOnSuccessListener(new OnSuccessListener<GoogleDriveHelper>() {
            @Override
            public void onSuccess(GoogleDriveHelper helper) {
                GoogleDriveActivity.this.gdHelper = helper;
                gdBackupRestorer = new GoogleDriveBackupRestorer(helper);
                Logger.i("Login in success as {}" + helper.getGoogleSignInAccount().getDisplayName());
                doRequestSync();
            }
        }).addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        if (!silentOnly) {
                            startActivityForResult(GoogleDriveHelper.getSignInIntent(GoogleDriveActivity.this), REQUEST_DRIVE_AUTH);
                        }
                    }
                });
    }

    private void doRequestSync() {
        if (gdHelper != null) {
            GUIs.doBusy(this, new GUIs.BusyAdapter() {
                @Override
                public void onBusyFinish() {
                    refreshUI();
                }

                public void onBusyError(Throwable t) {
                    Logger.e(t.getMessage(), t);
                    refreshUI();
                }

                @Override
                public void run() {
                    gdHelper.requestSync();
                }
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent data) {
        if (requestCode == REQUEST_DRIVE_AUTH) {
            if (resultCode == RESULT_OK) {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                try {
                    gdHelper = new GoogleDriveHelper(this, task.getResult(ApiException.class));
                    gdBackupRestorer = new GoogleDriveBackupRestorer(gdHelper);
                    Logger.i("Sign in success on activity result");
                    doRequestSync();
                } catch (ApiException e) {
                    // The ApiException status code indicates the detailed failure reason.
                    // Please refer to the GoogleSignInStatusCodes class reference for more information.
                    Logger.w("signInResult:failed status code=" + e.getStatusCode());
                    GUIs.alert(this, i18n().string(R.string.msg_signin_fail, "status code " + e.getStatusCode()));
                }
            } else {
                GUIs.alert(this, i18n().string(R.string.msg_signin_fail, "result code " + resultCode));
            }

        }
        return;
    }

    private void doRevoke() {
        //gsiClient.signOut()
        GoogleDriveHelper.revokeAccess(this).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void v) {
                gdHelper = null;
                gdBackupRestorer = null;
                refreshUI();
            }
        }).addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        gdHelper = null;
                        gdBackupRestorer = null;
                        refreshUI();
                    }
                });
    }

    private void doRestore() {

        int idx = vFileList.getSelectedItemPosition();
        if (idx == -1 || fileList.get(idx).file == null) {
            GUIs.shortToast(this, i18n().string(R.string.label_select_backup_file));
            return;
        }

        final DriveFileInfo info = fileList.get(idx);

        final GUIs.IBusyRunnable job = new GUIs.IBusyRunnable() {

            class Result{
                int count;
                String fileName;
                String err;
            }

            Result result;
            Long lastBakcup;

            @Override
            public void onBusyFinish() {
                if (result.err == null) {
                    String count = Integer.toString(result.count);
                    String msg = i18n().string(R.string.msg_db_restored, count, result.fileName);
                    if (lastBakcup != null) {
                        preference().setLastBackupTime(lastBakcup);
                    }
                    //theme, templates is possible changed
                    GUIs.alert(GoogleDriveActivity.this, msg, new GUIs.OnFinishListener() {
                        @Override
                        public boolean onFinish(int which, Object data) {
                            if (which == GUIs.OK_BUTTON) {
                                restartAppColdly();
                            }
                            return true;
                        }
                    });
                } else {
                    GUIs.alert(GoogleDriveActivity.this, result.err);
                }

            }

            @Override
            public void onBusyError(Throwable t) {
                GUIs.alert(getApplicationContext(), t.getMessage());
            }

            public void run() {
                result = new Result();
                result.fileName = info.title;

                GoogleDriveBackupRestorer.RestoreResult restoreResult = gdBackupRestorer.restore(info.file);
                if(!restoreResult.isSuccess()){
                    result.err = restoreResult.getErr();
                    return;
                }

                lastBakcup = preference().getLastBackupTime();

                DataBackupRestorer.Result dbrResult = new DataBackupRestorer().restore(restoreResult.getFolder());

                //clean drive restore folder
                Files.deepClean(restoreResult.getFolder());
                restoreResult.getFolder().delete();

                if (!dbrResult.isSuccess()) {
                    result.err = dbrResult.getErr();
                    return;
                }
                result.count = dbrResult.getDb() + dbrResult.getPref();

                trackEvent(TE.DRIVE_RESTORE);
            }
        };

        GUIs.confirm(this, i18n().string(R.string.qmsg_restore_data), new GUIs.OnFinishListener() {
            @Override
            public boolean onFinish(int which, Object data) {
                if (which == GUIs.OK_BUTTON) {
                    GUIs.doBusy(GoogleDriveActivity.this, job);
                }
                return true;
            }
        });

    }

    private void doBackup() {
        final GUIs.IBusyRunnable job = new GUIs.IBusyRunnable() {
            class Result{
                int count;
                String fileName;
                String err;
            }

            Result result;

            @Override
            public void onBusyFinish() {
                if (result.err == null) {
                    String msg = i18n().string(R.string.msg_db_backuped, result.count, result.fileName);
                    preference().setLastBackupTime(System.currentTimeMillis());
                    GUIs.alert(GoogleDriveActivity.this, msg);

                    refreshFileList();
                } else {
                    GUIs.alert(GoogleDriveActivity.this, result.err);
                }
            }

            @Override
            public void onBusyError(Throwable t) {
                GUIs.alert(GoogleDriveActivity.this, t.getMessage());
            }

            public void run() {
                result = new Result();

                DataBackupRestorer.Result dbrResult = new DataBackupRestorer().backup();
                if (!dbrResult.isSuccess()) {
                    result.err = dbrResult.getErr();
                    return;
                }
                File lastFolder = dbrResult.getLastFolder();

                GoogleDriveBackupRestorer.BackupResult backupResult = gdBackupRestorer.backup(lastFolder);

                result.count = backupResult.getCount();
                result.fileName = backupResult.getFileName();

                trackEvent(TE.DRIVE_BACKUP);
            }
        };

        GUIs.confirm(this, i18n().string(R.string.qmsg_backup_data_to_drive), new GUIs.OnFinishListener() {
            @Override
            public boolean onFinish(int which, Object data) {
                if (which == GUIs.OK_BUTTON) {
                    GUIs.doBusy(GoogleDriveActivity.this, job);
                }
                return true;
            }
        });
    }

    private void doClean() {
        final GUIs.IBusyRunnable job = new GUIs.IBusyRunnable() {

            class Result {
                int count;
                String err;
            }

            Result result;

            @Override
            public void onBusyFinish() {
                if (result.err == null) {
                    String msg = i18n().string(R.string.msg_drive_cleaned, result.count);
                    GUIs.alert(GoogleDriveActivity.this, msg);
                    refreshFileList();
                } else {
                    GUIs.alert(GoogleDriveActivity.this, result.err);
                }


            }

            @Override
            public void onBusyError(Throwable t) {
                GUIs.alert(GoogleDriveActivity.this, t.getMessage());
            }

            public void run() {
                result = new Result();
                try {
                    DriveFolder appFolder = getAppFolder();
                    for (Metadata data : gdHelper.listChildren(appFolder)) {
                        if (!data.isFolder() && !data.isTrashed()
                                && data.getCreatedDate().equals(data.getModifiedDate())) {
                            gdHelper.deleteFile(data.getDriveId().asDriveFile());
                            result.count++;
                        }
                    }
                    trackEvent(TE.DRIVE_CLEAN);
                } catch (Exception e) {
                    Logger.e(e.getMessage(), e);
                    result.err = e.getMessage();
                }
            }
        };

        GUIs.confirm(this, i18n().string(R.string.qmsg_clean_drive_data), new GUIs.OnFinishListener() {
            @Override
            public boolean onFinish(int which, Object data) {
                if (which == GUIs.OK_BUTTON) {
                    GUIs.doBusy(GoogleDriveActivity.this, job);
                }
                return true;
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION) {
            if (Misc.isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE, permissions, grantResults)) {
                recreate();
            } else {
                doRequestAuth(true);
                refreshUI();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void doRequestPermission() {
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
    }


    static class DriveFileInfo {
        final String title;
        final DriveFile file;
        final long size;

        public DriveFileInfo(String title, DriveFile file, long size) {
            this.title = title;
            this.file = file;
            this.size = size;
        }
    }

    public class FileInfoViewBinder extends RegularSpinnerAdapter.ViewHolder<DriveFileInfo> {

        public FileInfoViewBinder(RegularSpinnerAdapter adapter) {
            super(adapter);
        }

        @Override
        public void bindViewValue(DriveFileInfo item, LinearLayout vlayout, TextView vtext, boolean isDropdown, boolean isSelected) {

            if (item.file != null) {
                vtext.setText(Strings.format("{} ({})", item.title, Strings.getShortDigitalUnitString(item.size)));
            } else {
                vtext.setText(item.title);
            }
        }
    }
}
