package com.colaorange.dailymoney.core.ui.chart;

import android.os.Bundle;

import com.colaorange.commons.util.CalendarHelper;
import com.colaorange.commons.util.Colors;
import com.colaorange.commons.util.Numbers;
import com.colaorange.dailymoney.core.R;
import com.colaorange.dailymoney.core.context.Contexts;
import com.colaorange.dailymoney.core.context.ContextsActivity;
import com.colaorange.dailymoney.core.context.PeriodMode;
import com.colaorange.dailymoney.core.data.AccountType;
import com.colaorange.dailymoney.core.data.Balance;
import com.colaorange.dailymoney.core.data.BalanceHelper;
import com.colaorange.dailymoney.core.data.IDataProvider;
import com.colaorange.dailymoney.core.ui.GUIs;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * aggregate account chart that compare current and preious period of an account type
 *
 * @author dennis
 */
public class LineFromBeginningAggregateFragment extends PeriodModeChartBaseFragment<LineChart> {

    protected Map<AccountType, Integer> accountTypeTextColorMap;
    private XAxisDateFormatter formatter;

    private DateFormat xAxisFormat;

    private static Set<PeriodMode> supportPeriod = com.colaorange.commons.util.Collections.asSet(PeriodMode.MONTHLY, PeriodMode.YEARLY);

    @Override
    protected void initArgs() {
        super.initArgs();
        Bundle args = getArguments();

        if (!supportPeriod.contains(periodMode)) {
            throw new IllegalStateException("unsupported period " + periodMode);
        }
    }

    @Override
    protected void initMembers() {
        super.initMembers();
        ContextsActivity activity = getContextsActivity();
        accountTypeTextColorMap = activity.getAccountTextColorMap();

        xAxisFormat = Contexts.instance().getPreference().getNonDigitalMonthFormat();

        //x
        XAxis xAxis = vChart.getXAxis();
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(labelTextColor);
        xAxis.setTextSize(labelTextSize - 4);
        if (periodMode == PeriodMode.MONTHLY) {
            xAxis.setGranularity(Numbers.DAY); // only intervals of 1 day
        } else {
            xAxis.setGranularity(Numbers.DAY * 30); // only intervals of 1 month
        }
        xAxis.setValueFormatter(formatter = new XAxisDateFormatter(xAxisFormat));

        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        //y
        YAxis leftAxis = vChart.getAxisLeft();
        YAxis rightAxis = vChart.getAxisRight();

        leftAxis.setTextSize(labelTextSize - 3);

        rightAxis.setEnabled(false);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.chart_line_period_frag;
    }

    @Override
    public void reloadChart() {
        super.reloadChart();
        GUIs.doAsync(getContextsActivity(), new ChartLoading() {
            final Map<AccountType, List<Entry>> entrySeries = new LinkedHashMap<>();

            @Override
            public void run() {

                CalendarHelper calHelper = calendarHelper();

                Date start = calHelper.yearStartDate(baseDate);
                Date end;
                if (periodMode == PeriodMode.MONTHLY) {
                    end = calHelper.monthEndDate(baseDate);
                } else {
                    end = calHelper.yearEndDate(baseDate);
                }

                vChart.getXAxis().setAxisMinimum(start.getTime());
                vChart.getXAxis().setAxisMaximum(end.getTime());


                IDataProvider idp = Contexts.instance().getDataProvider();

                AccountType[] types = AccountType.getSupportedType();

                for (AccountType type : types) {
                    Date localStart = start;
                    List<Entry> entries = new LinkedList<>();
                    while (localStart.getTime() < end.getTime()) {
                        Balance b = BalanceHelper.calculateBalance(type, null, calHelper.monthEndDate(localStart));
                        entries.add(new Entry(localStart.getTime(), (float) b.getMoney()));

                        localStart = calendarHelper().monthAfter(localStart, 1);
                    }
                    entrySeries.put(type, entries);
                }


            }

            private int nextColor(int i) {
                return colorTemplate[i % colorTemplate.length];
            }

            @Override
            public void onAsyncFinish() {
                super.onAsyncFinish();

                List<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
                int i = 0;
                for (AccountType type : entrySeries.keySet()) {
                    List<Entry> list = entrySeries.get(type);
                    LineDataSet set = new LineDataSet(list, ((AccountType) type).getDisplay(i18n));

                    int color = accountTypeTextColorMap.get(type);

                    set.setColors(color);

                    set.setValueTextSize(labelTextSize - 4);
                    set.setValueTextColor(labelTextColor);
                    set.setCircleColor(lightTheme ? Colors.darken(color, 0.3f) : Colors.lighten(color, 0.3f));
                    set.setCircleColorHole(lightTheme ? Colors.darken(color, 0.2f) : Colors.lighten(color, 0.2f));

                    dataSets.add(set);

                    i++;
                }

                /**
                 * java.lang.IndexOutOfBoundsException
                 at java.util.LinkedList.get(LinkedList.java:519)
                 at com.github.mikephil.charting.data.DataSet.getEntryForIndex(DataSet.java:286)
                 at com.github.mikephil.charting.utils.Transformer.generateTransformedValuesLine(Transformer.java:184)
                 at com.github.mikephil.charting.renderer.LineChartRenderer.drawValues(LineChartRenderer.java:547)
                 at com.github.mikephil.charting.charts.BarLineChartBase.onDraw(BarLineChartBase.java:264)
                 at android.view.View.draw(View.java:15114)
                 */
                if (dataSets.size() > 0) {
                    LineData data = new LineData(dataSets);

                    data.setValueFormatter(new MoneyFormatter());
                    vChart.setData(data);
                } else {
                    vChart.setData(null);
                }
                vChart.invalidate(); // refresh
            }
        });
    }

}
