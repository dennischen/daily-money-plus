package com.colaorange.commons.util;


import android.annotation.SuppressLint;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author dennis
 */
public class CalendarHelper {

    private int firstDayOfWeek = 1;//1 SUN, 2 MON..etc
    private int startDayOfMonth = 1;//between 1-28
    private int startMonthOfYear = 0;//0-11
    private int startMonthDayOfYear = 1;//between 1-28


    private TimeZone timeZone;

    public CalendarHelper() {
    }

    public int getFirstDayOfWeek() {
        return firstDayOfWeek;
    }

    public void setFirstDayOfWeek(int firstDayOfWeek) {
        this.firstDayOfWeek = firstDayOfWeek;
    }

    public int getStartDayOfMonth() {
        return startDayOfMonth;
    }

    public void setStartDayOfMonth(int startDayOfMonth) {
        if (startDayOfMonth < 1 || startDayOfMonth > 28) {
            throw new IllegalArgumentException("the value of startDayOfMonth must between 1-28");
        }
        this.startDayOfMonth = startDayOfMonth;
    }

    public int getStartMonthOfYear() {
        return startMonthOfYear;
    }

    public void setStartMonthOfYear(int startMonthOfYear) {
        if (startMonthOfYear < 0 || startMonthOfYear > 11) {
            throw new IllegalArgumentException("the value of startMonthOfYear must between 0-11");
        }
        this.startMonthOfYear = startMonthOfYear;
    }

    public int getStartMonthDayOfYear() {
        return startMonthDayOfYear;
    }

    public void setStartMonthDayOfYear(int startMonthDayOfYear) {
        if (startMonthDayOfYear < 1 || startMonthDayOfYear > 28) {
            throw new IllegalArgumentException("the value of startMonthDayOfYear must between 1-28");
        }
        this.startMonthDayOfYear = startMonthDayOfYear;
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    @SuppressLint("WrongConstant")
    public Calendar calendar(Date d) {
        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(firstDayOfWeek);
        if (timeZone != null) {
            cal.setTimeZone(timeZone);
        }
        cal.setTime(d);
        return cal;
    }

    public Date tomorrow(Date d) {
        Calendar cal = calendar(d);
        cal.add(Calendar.DATE, 1);
        return cal.getTime();
    }

    public Date dateAfter(Date d, int i) {
        Calendar cal = calendar(d);
        cal.add(Calendar.DATE, i);
        return cal.getTime();
    }

    public Date dateBefore(Date d, int i) {
        Calendar cal = calendar(d);
        cal.add(Calendar.DATE, -i);
        return cal.getTime();
    }

    public Date yearAfter(Date d, int i) {
        Calendar cal = calendar(d);
        cal.add(Calendar.YEAR, i);
        return cal.getTime();
    }

    public Date yearBefore(Date d, int i) {
        Calendar cal = calendar(d);
        cal.add(Calendar.YEAR, -i);
        return cal.getTime();
    }

    public Date monthAfter(Date d, int i) {
        Calendar cal = calendar(d);
        cal.add(Calendar.MONTH, i);
        return cal.getTime();
    }

    public Date monthBefore(Date d, int i) {
        Calendar cal = calendar(d);
        cal.add(Calendar.MONTH, -i);
        return cal.getTime();
    }

    public Date today() {
        return new Date();
    }

    public Date yesterday(Date d) {
        Calendar cal = calendar(d);
        cal.add(Calendar.DATE, -1);
        return cal.getTime();
    }

    public Date toDayStart(Date d) {
        return toDayStart(calendar(d));
    }

    public Date toDayMiddle(Date d) {
        return toDayMiddle(calendar(d));
    }

    public Date toDayEnd(Date d) {
        return toDayEnd(calendar(d));
    }

    public Date toDayStart(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public Date toDayMiddle(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 12);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public Date toDayEnd(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    public int weekOfMonth(Date d) {
        Calendar cal = calendar(d);
        return cal.get(Calendar.WEEK_OF_MONTH);
    }

    public int weekOfYear(Date d) {
        Calendar cal = calendar(d);
        return cal.get(Calendar.WEEK_OF_YEAR);
    }

    public int dayOfMonth(Date d) {
        Calendar cal = calendar(d);
        return cal.get(Calendar.DAY_OF_MONTH);
    }

    public int dayOfWeek(Date d) {
        Calendar cal = calendar(d);
        return cal.get(Calendar.DAY_OF_WEEK);
    }

    public int dayOfYear(Date d) {
        Calendar cal = calendar(d);
        return cal.get(Calendar.DAY_OF_YEAR);
    }

    public Date monthStartDate(Date d) {
        if (startDayOfMonth == 1) {
            return absMonthStartDate(d);
        }

        Calendar cal = calendar(d);
        int cd = cal.get(Calendar.DAY_OF_MONTH);
        if (cd < startDayOfMonth) {
            cal.add(Calendar.MONTH, -1);
        }
        cal.set(Calendar.DAY_OF_MONTH, startDayOfMonth);


        return toDayStart(cal);
    }

    public Date monthEndDate(Date d) {
        if (startDayOfMonth == 1) {
            return absMonthEndDate(d);
        }
        d = monthStartDate(d);
        Calendar cal = calendar(d);

        cal.add(Calendar.MONTH, 1);//add 1 month
        cal.add(Calendar.DAY_OF_MONTH, -1);//minus 1 day
        return toDayEnd(cal);
    }

    public Date absMonthStartDate(Date d) {
        Calendar cal = calendar(d);
        cal.set(Calendar.DAY_OF_MONTH, startDayOfMonth);
        return toDayStart(cal);
    }

    public Date absMonthEndDate(Date d) {
        Calendar cal = calendar(d);
        int last = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        cal.set(Calendar.DAY_OF_MONTH, last);
        return toDayEnd(cal);
    }

    public Date yearStartDate(Date d) {
        if (startMonthOfYear == 0 && startMonthDayOfYear == 1) {
            return absYearStartDate(d);
        }
        Calendar cal = calendar(d);

        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        if (month < startMonthOfYear || (month == startMonthOfYear && day < startMonthDayOfYear)) {
            cal.add(Calendar.YEAR, -1);
        }
        cal.set(Calendar.MONTH, startMonthOfYear);
        cal.set(Calendar.DAY_OF_MONTH, startMonthDayOfYear);

        return toDayStart(cal);
    }

    public Date yearEndDate(Date d) {
        if (startMonthOfYear == 0 && startMonthDayOfYear == 1) {
            return absYearEndDate(d);
        }
        d = yearStartDate(d);
        Calendar cal = calendar(d);
        cal.add(Calendar.YEAR, 1);//add 1 year
        cal.add(Calendar.DAY_OF_YEAR, -1);//minus 1 day
        return toDayEnd(cal);
    }

    public Date absYearStartDate(Date d) {
        Calendar cal = calendar(d);
        cal.set(Calendar.DAY_OF_YEAR, 1);
        return toDayStart(cal);
    }

    public Date absYearEndDate(Date d) {
        Calendar cal = calendar(d);
        int last = cal.getActualMaximum(Calendar.DAY_OF_YEAR);
        cal.set(Calendar.DAY_OF_YEAR, last);
        return toDayEnd(cal);
    }

    public Date weekStartDate(Date d) {
        Calendar cal = calendar(d);
        cal.set(Calendar.DAY_OF_WEEK, firstDayOfWeek);
        //in android or java?, there is a bug in day of week(or my bug?), this is a workaround.
        Date fd = toDayStart(cal);
        if (fd.after(d)) {
            return dateBefore(fd, 7);
        } else {
            return fd;
        }
    }

    public Date weekEndDate(Date d) {
        Calendar cal = calendar(d);
//        int last = cal.getActualMaximum(Calendar.DAY_OF_WEEK);
        int last = (firstDayOfWeek == 1) ? 7 : firstDayOfWeek - 1;
        cal.set(Calendar.DAY_OF_WEEK, last);
        return toDayEnd(cal);
    }


    public boolean isSameYear(Date d1, Date d2) {
        Calendar cal1 = calendar(d1);
        Calendar cal2 = calendar(d2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR);
    }

    public boolean isPastYear(Date base, Date d2) {
        return yearStartDate(base).after(d2);
    }

    public boolean isFutureYear(Date base, Date d2) {
        return yearEndDate(base).before(d2);
    }


    public boolean isSameMonth(Date d1, Date d2) {
        Calendar cal1 = calendar(d1);
        Calendar cal2 = calendar(d2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH);
    }

    public boolean isPastMonth(Date base, Date d2) {
        return absMonthStartDate(base).after(d2);
    }

    public boolean isFutureMonth(Date base, Date d2) {
        return absMonthEndDate(base).before(d2);
    }

    public boolean isSameWeek(Date d1, Date d2) {
        Calendar cal1 = calendar(d1);
        Calendar cal2 = calendar(d2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR);
    }

    public boolean isPastWeek(Date base, Date d2) {
        return weekStartDate(base).after(d2);
    }

    public boolean isFutureWeek(Date base, Date d2) {
        return weekEndDate(base).before(d2);
    }


    public boolean isSameDay(Date base, Date d2) {
        Calendar cal1 = calendar(base);
        Calendar cal2 = calendar(d2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    public boolean isYesterday(Date base, Date date2) {
        Calendar cal1 = calendar(base);
        Calendar cal2 = calendar(date2);
        int y1 = cal1.get(Calendar.YEAR);
        int y2 = cal2.get(Calendar.YEAR);
        int d1 = cal1.get(Calendar.DAY_OF_YEAR);
        int d2 = cal2.get(Calendar.DAY_OF_YEAR);
        return (y1 == y2 && d1 == d2 + 1) || (y1 == y2 + 1 && d1 == 1 && d2 == 365);
    }
    public boolean isTomorrow(Date base, Date date2) {
        Calendar cal1 = calendar(base);
        Calendar cal2 = calendar(date2);
        int y1 = cal1.get(Calendar.YEAR);
        int y2 = cal2.get(Calendar.YEAR);
        int d1 = cal1.get(Calendar.DAY_OF_YEAR);
        int d2 = cal2.get(Calendar.DAY_OF_YEAR);
        return (y1 == y2 && d1 == d2 - 1) || (y1 == y2 - 1 && d1 == 365 && d2 == 1);
    }

    public boolean isPastDay(Date base, Date d2) {
        return toDayStart(base).after(d2);
    }

    public boolean isFutureDay(Date base, Date d2) {
        return toDayEnd(base).before(d2);
    }

    static public final TimeZone UTC0 = TimeZone.getTimeZone("UTC0");
    static public final TimeZone GMT0 = TimeZone.getTimeZone("GMT+0:00");


    public static DateFormat getRRC1123Format() {
        return new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'",
                new DateFormatSymbols(Locale.ENGLISH));
    }

    public static String getRFC1123(Date date) {
        return getRRC1123Format().format(date);
    }

    public static Date parseRFC1123(String str) throws Exception {
        return getRRC1123Format().parse(str);
    }
}
