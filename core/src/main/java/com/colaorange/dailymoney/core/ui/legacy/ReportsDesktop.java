package com.colaorange.dailymoney.core.ui.legacy;

import android.app.Activity;
import android.content.Intent;

import com.colaorange.dailymoney.core.util.I18N;
import com.colaorange.dailymoney.core.R;
import com.colaorange.dailymoney.core.context.Contexts;

/**
 * @author dennis
 */
public class ReportsDesktop extends AbstractDesktop {

    public static final String NAME = "report";

    public ReportsDesktop(Activity activity) {
        super(NAME, activity);
    }

    @Override
    protected void init() {
        I18N i18n = Contexts.instance().getI18n();

        label = i18n.string(R.string.desktop_reports);

        Intent intent = null;

        intent = new Intent(activity, BalanceMgntActivity.class);
        intent.putExtra(BalanceMgntActivity.ARG_TOTAL_MODE, false);
        intent.putExtra(BalanceMgntActivity.ARG_MODE, BalanceMgntActivity.MODE_MONTH);
        DesktopItem monthBalance = new DesktopItem(new IntentRun(activity, intent),
                i18n.string(R.string.nav_pg_report_monthly_balance), R.drawable.dtitem_balance_month, true, false, 199);
        addItem(monthBalance);

        intent = new Intent(activity, BalanceMgntActivity.class);
        intent.putExtra(BalanceMgntActivity.ARG_TOTAL_MODE, false);
        intent.putExtra(BalanceMgntActivity.ARG_MODE, BalanceMgntActivity.MODE_YEAR);
        DesktopItem yearBalance = new DesktopItem(new IntentRun(activity, intent),
                i18n.string(R.string.nav_pg_report_yearly_balance), R.drawable.dtitem_balance_year, true, false, 199);
        addItem(yearBalance);

        intent = new Intent(activity, BalanceMgntActivity.class);
        intent.putExtra(BalanceMgntActivity.ARG_TOTAL_MODE, true);
        intent.putExtra(BalanceMgntActivity.ARG_MODE, BalanceMgntActivity.MODE_MONTH);
        DesktopItem totalBalance = new DesktopItem(new IntentRun(activity, intent),
                i18n.string(R.string.nav_pg_report_cumulative_balance), R.drawable.dtitem_balance_cumulative_month, true,false, 197);
        addItem(totalBalance);
    }

}
