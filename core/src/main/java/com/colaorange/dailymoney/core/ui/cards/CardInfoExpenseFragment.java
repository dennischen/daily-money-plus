package com.colaorange.dailymoney.core.ui.cards;

import android.view.View;
import android.widget.TextView;

import com.colaorange.commons.util.CalendarHelper;
import com.colaorange.commons.util.Var;
import com.colaorange.dailymoney.core.R;
import com.colaorange.dailymoney.core.context.Contexts;
import com.colaorange.dailymoney.core.context.EventQueue;
import com.colaorange.dailymoney.core.data.Account;
import com.colaorange.dailymoney.core.data.AccountType;
import com.colaorange.dailymoney.core.data.BalanceHelper;
import com.colaorange.dailymoney.core.data.IDataProvider;
import com.colaorange.dailymoney.core.ui.GUIs;

import java.util.Date;
import java.util.List;

/**
 * @author dennis
 */
public class CardInfoExpenseFragment extends CardBaseFragment implements EventQueue.EventListener {

    private TextView vInfoWeeklyExpense;
    private TextView vInfoMonthlyExpense;
    private TextView vInfoCumulativeCash;

    @Override
    protected void initMembers() {
        super.initMembers();
        vInfoWeeklyExpense = rootView.findViewById(R.id.card_info_weekly_expense);
        vInfoMonthlyExpense = rootView.findViewById(R.id.card_info_monthly_expense);
        vInfoCumulativeCash = rootView.findViewById(R.id.card_info_cash);

    }

    @Override
    protected int getLayoutResId() {
        return R.layout.card_info_expense_frag;
    }

    @Override
    protected boolean doReloadContent() {

        GUIs.doAsync(getContextsActivity(), new GUIs.AsyncAdapter() {

            Var<Double> varWE = new Var<>();
            Var<Double> varME = new Var<>();
            Var<Double> varC = new Var<>();

            @Override
            public void run() {

                CalendarHelper calHelper = calendarHelper();

                Date now = new Date();
                Date start = calHelper.weekStartDate(now);
                Date end = calHelper.weekEndDate(now);

                double weeklyExpense = BalanceHelper.calculateBalance(AccountType.EXPENSE, start, end).getMoney();

                start = calHelper.monthStartDate(now);
                end = calHelper.monthEndDate(now);
                double monthlyExpense = BalanceHelper.calculateBalance(AccountType.EXPENSE, start, end).getMoney();

                IDataProvider idp = Contexts.instance().getDataProvider();
                List<Account> acl = idp.listAccount(AccountType.ASSET);
                double cash = 0;
                for (Account ac : acl) {
                    if (ac.isCashAccount()) {
                        cash += BalanceHelper.calculateBalance(ac, null, calHelper.toDayEnd(now)).getMoney();
                    }
                }

                varWE.value = weeklyExpense;
                varME.value = monthlyExpense;
                varC.value = cash;
            }

            @Override
            public void onAsyncFinish() {
                setNoData(false);
                vInfoWeeklyExpense.setText(contexts().toFormattedMoneyString(varWE.value));
                vInfoMonthlyExpense.setText(contexts().toFormattedMoneyString(varME.value));
                vInfoCumulativeCash.setText(contexts().toFormattedMoneyString(varC.value));
            }
        });

        return true;
    }
}