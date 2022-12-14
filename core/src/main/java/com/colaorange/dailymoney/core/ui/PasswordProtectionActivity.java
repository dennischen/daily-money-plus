package com.colaorange.dailymoney.core.ui;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.colaorange.dailymoney.core.context.Preference;
import com.colaorange.dailymoney.core.context.ContextsActivity;
import com.colaorange.dailymoney.core.R;

/**
 * @author dennis
 */
public class PasswordProtectionActivity extends ContextsActivity implements OnClickListener {

    @Override
    public boolean isDialogTheme(){
        return true;
    }
    @Override
    public boolean isNoActionBarTheme() {
        return false;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.password_protection);
        findViewById(R.id.pwd_protection_ok).setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.pwd_protection_ok) {
            doPasswordOk();
        }
    }

    private void doPasswordOk() {
        String pwdHash = preference().getPasswordHash();
        String pwd = ((TextView) findViewById(R.id.pwd_protection_text)).getText().toString();
        if (Preference.passwordMD5(pwd).equals(pwdHash)) {
            setResult(RESULT_OK);
            finish();
        } else {
            GUIs.shortToast(this, R.string.msg_wrong_password);
            ((TextView) findViewById(R.id.pwd_protection_text)).setText("");
        }
    }
}
