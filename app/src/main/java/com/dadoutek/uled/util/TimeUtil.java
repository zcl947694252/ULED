package com.dadoutek.uled.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 时间工具类
 * Created by Administrator on 2015/7/22.
 */
public class TimeUtil {

    /**
     * 获取当前星期(0-6)
     *
     * @return
     */
    public static int getCurrentDay() {
        try {
            String date = TimeUtil.formatHMills(System.currentTimeMillis(), "yyyyMMdd");
            int day = TimeUtil.dayForWeek(date);
            return day;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static String getWeek2Date(int year, int week) {
        String[] s = getWeekRang(year, week, "MM.dd");
        return s[0] + "-" + s[1];
    }

    public static int getNowSeconds() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.SECOND);
    }

    public static int getTimeYmd(String date1, int type) {
        Date date = null;
        int time = 0;
        try {
            if (date1.contains("-")) {
                date = new SimpleDateFormat("yyyy-MM-dd").parse(date1);
            } else {
                date = new SimpleDateFormat("yyyyMMdd").parse(date1);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar now = Calendar.getInstance();
        now.setTime(date);
        switch (type) {
            case 0:
                time = now.get(Calendar.YEAR);
                return time;
            case 1:
                time = now.get(Calendar.MONTH);
                return time;
            case 2:
                time = now.get(Calendar.DAY_OF_MONTH);
                return time;
        }
        return time;
    }

    public static int getTimeYmd1(String date1, int type) {
        Date date = null;
        int time = 0;
        try {
            date = new SimpleDateFormat("yyyy-MM-dd").parse(date1);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar now = Calendar.getInstance();
        now.setTime(date);
        switch (type) {
            case 0:
                time = now.get(Calendar.YEAR);
                return time;
            case 1:
                time = now.get(Calendar.MONTH);
                return time;
            case 2:
                time = now.get(Calendar.DAY_OF_MONTH);
                return time;
        }
        return time;
    }

    /**
     * 月份时间小于0自动补零
     *
     * @param date1
     * @param type
     * @return
     */
    public static String getTimeYmdadd0(String date1, int type) {
        Date date = null;
        int time = 0;
        try {
            date = new SimpleDateFormat("yyyy-MM-dd").parse(date1);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar now = Calendar.getInstance();
        now.setTime(date);
        switch (type) {
            case 0:
                time = now.get(Calendar.YEAR);
                return time + "";
            case 1:
                time = now.get(Calendar.MONTH) + 1;
                return transAdd0Time(time);
            case 2:
                time = now.get(Calendar.DAY_OF_MONTH);
                return transAdd0Time(time);
            case 3:
                return now.get(Calendar.YEAR) + "-" + (transAdd0Time(now.get(Calendar.MONTH) + 1)) + "-" + transAdd0Time(now.get(Calendar.DAY_OF_MONTH));
        }
        return time + "";
    }


//    /**
//     * 获取当前时间是第多少周
//     * @return
//     */
//    public static int getWeek(String date) {
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
//        Calendar cl = Calendar.getInstance();
//        try {
//            cl.setTime(sdf.parse(date));
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }
//        int week = cl.get(Calendar.WEEK_OF_YEAR);
//        //  System.out.println(week);
//        cl.add(Calendar.DAY_OF_MONTH, -7);
//        return week;
//    }
//
//    public static int getWeekNow() {
//        Calendar c = Calendar.getInstance();
//        int i = c.get(Calendar.WEEK_OF_YEAR);
//        return i;
//    }

    /**
     * 获取当前时间是第多少周
     *
     * @return
     */
    public static int getWeek(String date) {
        SimpleDateFormat sdf;
        if (date.contains("-")) {
            sdf = new SimpleDateFormat("yyyy-MM-dd");
        } else {
            sdf = new SimpleDateFormat("yyyyMMdd");
        }
        Calendar cl = new GregorianCalendar();
        cl.setFirstDayOfWeek(Calendar.SUNDAY);
        try {
            cl.setTime(sdf.parse(date));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        int week = cl.get(Calendar.WEEK_OF_YEAR);
        //  System.out.println(week);
        cl.add(Calendar.DAY_OF_MONTH, -7);
        cl.set(cl.get(Calendar.YEAR), 0, 1);
        if (cl.getMinimalDaysInFirstWeek() == 1) {
//            return week - 1;
            return week;
        }
        return week;
    }

    /**
     * 根据毫秒数获取是该年的第几个星期
     *
     * @param timeHM
     * @return
     */
    public static int getWeekWithDate(long timeHM) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timeHM);
        c.setFirstDayOfWeek(Calendar.SUNDAY);
        c.setTime(c.getTime());
        return c.get(Calendar.WEEK_OF_YEAR);
    }

    /**
     * 一年最多有多少周
     *
     * @param year
     * @return
     */
    public static int getMaxWeekNumOfYear(int year) {
        Calendar c = new GregorianCalendar();
        c.set(year, Calendar.DECEMBER, 31, 23, 59, 59);
        return getWeekOfYear(c.getTime());
    }

    public static int getWeekOfYear(Date date) {
        Calendar c = new GregorianCalendar();
        c.setFirstDayOfWeek(Calendar.SUNDAY);
        c.setMinimalDaysInFirstWeek(7);
        c.setTime(date);
        return c.get(Calendar.WEEK_OF_YEAR);
    }

    /**
     * 获取上一年
     *
     * @param year
     * @return
     */
    public static int getPreYear(int year) {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, year);
            calendar.add(Calendar.YEAR, -1);
            return calendar.get(Calendar.YEAR);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 获取下一年
     *
     * @param year
     * @return
     */
    public static int getNextYear(int year) {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, year);
            calendar.add(Calendar.YEAR, 1);
            return calendar.get(Calendar.YEAR);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 当前周的前一周
     *
     * @return
     */
    public static int getPreWeek(int week, int year) {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.WEEK_OF_YEAR, week);
            calendar.setFirstDayOfWeek(Calendar.SUNDAY);
            calendar.setMinimalDaysInFirstWeek(7);
            calendar.add(Calendar.WEEK_OF_YEAR, -1);
            return calendar.get(Calendar.WEEK_OF_YEAR);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }

    /**
     * 当前周的前一周
     *
     * @return
     */
    public static int getNextWeek(int week, int year) {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.WEEK_OF_YEAR, week);
            calendar.setFirstDayOfWeek(Calendar.SUNDAY);
            calendar.setMinimalDaysInFirstWeek(7);
            calendar.add(Calendar.WEEK_OF_YEAR, 1);
            return calendar.get(Calendar.WEEK_OF_YEAR);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }

    /**
     * 指定月的前一月
     *
     * @param month
     * @return
     */
    public static int getPreMonth(int month, int year) {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month - 1);
            calendar.add(Calendar.MONTH, -1);
            return calendar.get(Calendar.MONTH) + 1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }

    /**
     * 指定月的下一月
     *
     * @param month
     * @param year
     * @return
     */
    public static int getNextMonth(int month, int year) {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month - 1);
            calendar.add(Calendar.MONTH, 1);
            return calendar.get(Calendar.MONTH) + 1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }


    /**
     * 传入yyyyMMdd格式的日期返回星期几
     *
     * @param pTime
     * @return
     * @throws Exception
     */
    public static int dayForWeek(String pTime) {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        Date tmpDate = null;
        try {
            tmpDate = format.parse(pTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.SUNDAY);
        cal.setTime(tmpDate);
        int day = cal.get(Calendar.DAY_OF_WEEK) - 1;
        return day;
    }


    /**
     * 根据年和这一年的第几周，和这周的第几天返回日期
     *
     * @param year
     * @param week
     * @param day
     * @return
     */
    public static String getDateForYearAndWeekAndDay(int year, int week, int day) {
        try {
            Calendar cal = Calendar.getInstance();
            cal.clear();
            cal.set(Calendar.YEAR, year);
            cal.setFirstDayOfWeek(Calendar.SUNDAY);
            cal.set(Calendar.WEEK_OF_YEAR, week);
            switch (day) {
                case 0://周日
                    cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                    break;
                case 1://周一
                    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                    break;
                case 2://周二
                    cal.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
                    break;
                case 3://周三
                    cal.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
                    break;
                case 4://周四
                    cal.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
                    break;
                case 5://周五
                    cal.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
                    break;
                case 6://周六
                    cal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
                    break;
            }
            SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyyMMdd");
            return simpleFormat.format(cal.getTime());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 根据传入的第几周,返回这一周的开始和结束日期
     *
     * @param week
     * @return sp[0]开始日期 sp[1]结束日期(格式为20150718)
     */
    public static String[] getWeekRang(int year, int week) {
        return getWeekRang(year, week, "yyyyMMdd");
    }

    public static String[] getWeekRang(int year, int week, String format) {
        String[] sp = new String[2];
        try {
            Calendar cal = Calendar.getInstance();
            cal.clear();
            cal.set(Calendar.YEAR, year);
            cal.setFirstDayOfWeek(Calendar.SUNDAY);
            cal.setMinimalDaysInFirstWeek(7);
//            if(cal.getMinimalDaysInFirstWeek()==1) {
//                week+=1;
//            }
            cal.set(Calendar.WEEK_OF_YEAR, week);
            cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
            SimpleDateFormat simpleFormat = new SimpleDateFormat(format);
            cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);// 每周从周一开始
            cal.setMinimalDaysInFirstWeek(7);
            sp[0] = simpleFormat.format(cal.getTime());
            cal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
            sp[1] = simpleFormat.format(cal.getTime());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sp;
    }

    @SuppressLint("SimpleDateFormat")
    public static String[] getWeekRangEveryday(int year, int week) {
        String[] sp = new String[7];
        try {
            Calendar cal = Calendar.getInstance();
            cal.clear();
            cal.set(Calendar.YEAR, year);
            cal.setFirstDayOfWeek(Calendar.SUNDAY);
            cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);// 每周从周一开始
            cal.setMinimalDaysInFirstWeek(7); // 设置每周最少为7天
            cal.set(Calendar.WEEK_OF_YEAR, week);
            SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyy-MM-dd");
            cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
            sp[0] = simpleFormat.format(cal.getTime());
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            sp[1] = simpleFormat.format(cal.getTime());
            cal.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
            sp[2] = simpleFormat.format(cal.getTime());
            cal.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
            sp[3] = simpleFormat.format(cal.getTime());
            cal.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
            sp[4] = simpleFormat.format(cal.getTime());
            cal.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
            sp[5] = simpleFormat.format(cal.getTime());
            cal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
            sp[6] = simpleFormat.format(cal.getTime());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sp;
    }

    @SuppressLint("SimpleDateFormat")
    public static String[] getWeekRangEveryday(int year, int week, String format) {
        String[] sp = new String[7];
        try {
            Calendar cal = Calendar.getInstance();
            cal.clear();
            cal.set(Calendar.YEAR, year);
            cal.setFirstDayOfWeek(Calendar.SUNDAY);
            cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);// 每周从周一开始
            cal.setMinimalDaysInFirstWeek(7); // 设置每周最少为7天
            cal.set(Calendar.WEEK_OF_YEAR, week);
            SimpleDateFormat simpleFormat = new SimpleDateFormat(format);
            cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
            sp[0] = simpleFormat.format(cal.getTime());
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            sp[1] = simpleFormat.format(cal.getTime());
            cal.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
            sp[2] = simpleFormat.format(cal.getTime());
            cal.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
            sp[3] = simpleFormat.format(cal.getTime());
            cal.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
            sp[4] = simpleFormat.format(cal.getTime());
            cal.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
            sp[5] = simpleFormat.format(cal.getTime());
            cal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
            sp[6] = simpleFormat.format(cal.getTime());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sp;
    }

    /**
     * 获取当前时间是第多少周
     *
     * @return
     */
    public static int getMyCurrentWeek() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.setFirstDayOfWeek(Calendar.SUNDAY); // 设置每周的第一天为星期一
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);// 每周从周一开始
        calendar.setMinimalDaysInFirstWeek(7); // 设置每周最少为7天

        calendar.setTimeInMillis(System.currentTimeMillis());
        Log.d("dateBug", "getMyCurrentWeek: " + calendar.get(Calendar.WEEK_OF_YEAR));
        if (calendar.get(Calendar.WEEK_OF_YEAR) == 53) {
            return 0;
        }
        return calendar.get(Calendar.WEEK_OF_YEAR);
    }

    /**
     * @param date "2016-12-30"
     * @return 53
     */
    public static int getWeekByDate(String date) {
        Calendar calendar = Calendar.getInstance();
        try {
            SimpleDateFormat dateFormat = (SimpleDateFormat) DateFormat.getDateInstance();
            dateFormat.applyPattern("yyyy-MM-dd");
            Date date1 = dateFormat.parse(date);
            calendar.setFirstDayOfWeek(Calendar.SUNDAY); // 设置每周的第一天为星期一
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);// 每周从周一开始
            calendar.setMinimalDaysInFirstWeek(7); // 设置每周最少为7天
            calendar.setTime(date1);

        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (calendar.get(Calendar.WEEK_OF_YEAR) == 53) {
            return 0;
        }
        return calendar.get(Calendar.WEEK_OF_YEAR);
    }

    public static int getNowYear() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        return year;
    }

    public static int getYear(String date) {
        int year = 0;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date d = sdf.parse(date);
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            year = c.get(Calendar.YEAR);
            return year;
        } catch (ParseException e) {
            System.out.println(e.getMessage());
        }
        return year;
    }

    /**
     * 获取当前时间是第多少周
     *
     * @return
     */
    public static int getCurrentWeek() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);// 每周从周一开始
        calendar.setMinimalDaysInFirstWeek(7);
        calendar.setTimeInMillis(System.currentTimeMillis());
        return calendar.get(Calendar.WEEK_OF_YEAR);
    }

    /**
     * 传入当前毫秒数，返回对应的数据
     *
     * @param currentTime
     * @param type        时间类型
     * @return
     */
    public static int getTimeDate(long currentTime, int type) {
        Calendar calendar = Calendar.getInstance();
        return getTimeDate(calendar, currentTime, type);
    }

    /**
     * 获取当天的日期
     *
     * @param format
     */
    public static String getNowDate(String format) {
        Date utilDate = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(utilDate);
    }

    public static int getTimeDate(Calendar calendar, long currentTime, int type) {
        calendar.setTimeInMillis(currentTime);
        switch (type) {
            case Calendar.YEAR://年
                return calendar.get(Calendar.YEAR);
            case Calendar.MONTH://月
                return calendar.get(Calendar.MONTH) + 1;
            case Calendar.DAY_OF_MONTH://日
                return calendar.get(Calendar.DAY_OF_MONTH);
            case Calendar.HOUR_OF_DAY://时(24小时制)
                return calendar.get(Calendar.HOUR_OF_DAY);
            case Calendar.DAY_OF_WEEK://一周的第几天
                return calendar.get(Calendar.DAY_OF_WEEK) - 1;
            case Calendar.HOUR://时(12小时制)
                return calendar.get(Calendar.HOUR);
            case Calendar.MINUTE://分
                return calendar.get(Calendar.MINUTE);
            case Calendar.SECOND://秒
                return calendar.get(Calendar.SECOND);
        }
        return 0;
    }

    /**
     * 将对应毫秒数转换为指定格式的日期
     *
     * @param currentHMills
     * @param format
     * @return
     */
    public static String formatHMills(long currentHMills, String format) {
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
            Date now = new Date(currentHMills);
            return simpleDateFormat.format(now);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 获取当前日期的前一天
     *
     * @param date
     * @return
     */
    public static String getPreDate(String date) {
        return getPreDate(date, "yyyyMMdd");
    }

    public static String getPreDate2(String date) {
        return getPreDate(date, "yyyy-MM-dd");
    }

    public static String getPreDate(String date, String formatType) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(formatType);
        try {
            Date timeDate = dateFormat.parse(date);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(timeDate);
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            Date nextDate = calendar.getTime();
            return dateFormat.format(nextDate);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }


    /**
     * 获取当前日期的下一天
     *
     * @param date
     * @return
     */
    public static String getNextDate(String date) {
        return getNextDate(date, "yyyyMMdd");
    }

    public static String getNextDate2(String date) {
        return getNextDate(date, "yyyy-MM-dd");
    }

    public static String getNextDate(String date, String formatType) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(formatType);
        try {
            Date timeDate = dateFormat.parse(date);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(timeDate);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            Date nextDate = calendar.getTime();
            return dateFormat.format(nextDate);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 根据周范围获取该周每一天的日期(格式为yyyyMMdd)
     *
     * @param weekRange
     * @return
     */
    public static String[] getWeekDate(String[] weekRange) {
        String tempDate = weekRange[0];
        String[] arrays = new String[7];
        arrays[0] = tempDate;
        for (int i = 0; i < 7; i++) {
            tempDate = getNextDate(tempDate);
            arrays[i + 1] = tempDate;
            if (tempDate.equals(weekRange[1])) {
                break;
            }
        }
        return arrays;
    }

    /**
     * 将短时间格式字符串转换为时间 yyyy-MM-dd
     *
     * @param strDate
     * @return
     */
    public static Date strToDate(String strDate) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        ParsePosition pos = new ParsePosition(0);
        Date strtodate = formatter.parse(strDate, pos);
        return strtodate;
    }


    /**
     * 返回该时间间隔有多少分钟
     *
     * @param timeDistance
     * @return
     */
    public static long getDatePoor(long timeDistance) {
        long nd = 1000 * 24 * 60 * 60;
        long nh = 1000 * 60 * 60;
        long nm = 1000 * 60;
        return (timeDistance / nd) * 24 * 60 + (timeDistance % nd / nh) * 60 + (timeDistance % nd % nh / nm);
    }

    /**
     * utc时间格式转换
     *
     * @param dateTime
     * @param type
     * @return
     */
    public static String getUtcFormatedDateTime(long dateTime, int type) {
        long utcTime = dateTime * 1000;
        TimeZone tz = TimeZone.getDefault();
        long localTime = utcTime - tz.getRawOffset();
        SimpleDateFormat localFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        localFormater.setTimeZone(TimeZone.getDefault());
        String localTimeStr = localFormater.format(new Date(localTime));
        return getFormatTime(localTimeStr, type);
    }

    /**
     * 时间格式转换
     *
     * @param dateTime 单位秒
     * @param type
     * @return
     */
    public static String getFormatedDateTime(long dateTime, int type) {
        SimpleDateFormat localFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        localFormater.setTimeZone(TimeZone.getDefault());
        String localTimeStr = localFormater.format(new Date(dateTime * 1000));
        return getFormatTime(localTimeStr, type);
    }

    /**
     * utc转换后获取的类型
     *
     * @param localTime
     * @param type
     * @return
     */
    private static String getFormatTime(String localTime, int type) {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = null;
        try {
            date = formatter.parse(localTime);

        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        switch (type) {
            case 1:                //返回日期
                return calendar.get(Calendar.YEAR) + "-" + transAdd0Time(calendar.get(Calendar.MONTH) + 1) + "-" + transAdd0Time(calendar.get(Calendar.DAY_OF_MONTH));
            case 2:               //返回年
                return calendar.get(Calendar.YEAR) + "";
            case 3:               //返回月
                return transAdd0Time(calendar.get(Calendar.MONTH) + 1);
            case 4:              //返回日
                return transAdd0Time(calendar.get(Calendar.DAY_OF_MONTH));
            case 5:             //返回时间 Hour:minute
                return transAdd0Time(calendar.get(Calendar.HOUR_OF_DAY)) + ":" + transAdd0Time(calendar.get(Calendar.MINUTE));
            case 6:             //返回时
                return transAdd0Time(calendar.get(Calendar.HOUR_OF_DAY));
            case 7:            //返回分
                return transAdd0Time(calendar.get(Calendar.MINUTE));
            case 8:           //返回分钟数
                return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE) + "";
            case 9:           //返回分钟数
                return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE) * 60 + calendar.get(Calendar.SECOND) + "";
        }
        return "";
    }

    //时间格式自动补0
    public static String transAdd0Time(int time) {
        if (time < 10) {
            return 0 + "" + time;
        }
        return time + "";
    }

    /**
     * 分钟对各种时间转换
     *
     * @return
     */
    public static int getMinuteTimeChange(int minute, int type) {
        int time = 0;
        switch (type) {
            case 0:
                time = minute / 60;
                return time;
            case 1:
                time = minute % 60;
                return time;
        }
        return 0;
    }

    /**
     * 分钟对各种时间转换
     *
     * @return
     */
    public static String getMinuteTimeChange(int minute) {
        int hour = getMinuteTimeChange(minute, 0);
        int minutes = getMinuteTimeChange(minute, 1);
        return transAdd0Time(hour) + ":" + transAdd0Time(minutes);
    }

    /**
     * 返回当前的时间，时：分的形式
     */
    public static int getNowHour() {
        int hour1;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        hour1 = hour;
        if (hour < 10) {
            hour1 = +hour;
        }
        return hour1;
    }

    public static int dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
    public static int year = Calendar.getInstance().get(Calendar.YEAR);

    public static String getCurrentDate() {
        SimpleDateFormat format = (SimpleDateFormat) DateFormat.getDateInstance();
        format.applyPattern("yyyy-MM-dd");
        Date date = new Date();
        return format.format(date);
    }

    /**
     * 固定格式的日期
     *
     * @param input 2016-12-11
     * @return 12.11
     */
    public static String getFormatDate2Month(String input) {
        SimpleDateFormat formatOutput = (SimpleDateFormat) DateFormat.getDateInstance();
        SimpleDateFormat formatInput = (SimpleDateFormat) DateFormat.getDateInstance();
        formatInput.applyPattern("yyyy-MM-dd");
        formatOutput.applyPattern("dd.MMM");
        Date date;
        try {
            date = formatInput.parse(input);
            return formatOutput.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 固定格式的日期
     *
     * @param input 12.11
     * @param year  2016
     * @return 11-12-2016
     */
    public static String getFormatMonth2Date(String input, int year) {
        SimpleDateFormat formatOutput = (SimpleDateFormat) DateFormat.getDateInstance();
        SimpleDateFormat formatInput = (SimpleDateFormat) DateFormat.getDateInstance();
        formatInput.applyPattern("dd.MMM");
        formatOutput.applyPattern("dd-MMM");
        Date date;
        try {
            date = formatInput.parse(input);
            //return year + "-" + formatOutput.format(date);
            return formatOutput.format(date) + "-" + year;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 固定格式的日期
     *
     * @param input 12.11
     * @param year  2016
     * @return 2016-12-11
     */
    public static String getFormatMonth3Date(String input, int year) {
        SimpleDateFormat formatOutput = (SimpleDateFormat) DateFormat.getDateInstance();
        SimpleDateFormat formatInput = (SimpleDateFormat) DateFormat.getDateInstance();
        formatInput.applyPattern("dd.MMM");
        formatOutput.applyPattern("MM-dd");
        Date date;
        try {
            date = formatInput.parse(input);
            return year + "-" + formatOutput.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 固定格式的日期
     *
     * @param startDate 2016-11-12
     * @param endDate   2016-11-15
     * @return [11.12, 11.13, 11.14, 11.15]
     */
    public static List<String> getFormatDates(String startDate, String endDate) {
        List<String> list = new ArrayList<>();
        SimpleDateFormat format = (SimpleDateFormat) DateFormat.getDateInstance();
        format.applyPattern("yyyy-MM-dd");
        Date date;
        list.add(getFormatDate2Month(startDate));
        try {
            Calendar calendar = Calendar.getInstance();
            while (!startDate.equals(endDate)) {
                date = format.parse(startDate);
                calendar.setTime(date);
                calendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH) + 1);
                date = calendar.getTime();
                SimpleDateFormat dateFormat = (SimpleDateFormat) DateFormat.getDateInstance();
                dateFormat.applyPattern("dd.MMM");
                startDate = format.format(date);
                list.add(dateFormat.format(date));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * @param string 480
     * @return 08:00
     */
    public static String getFormatTime(String string) {
        String result;
        int time = Integer.parseInt(string);
        result = formatNumber(time / 60) + ":" + formatNumber(time % 60);
        return result;
    }

    private static String formatNumber(int time) {
        if (time < 10) {
            return "0" + time;
        }
        return "" + time;
    }

    /**
     * 根据日前获得今天是星期几 0-6 周日：0 周六：6
     */
    public static String getWeeks(String pTime, Context context) {
        String Week = "";
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        try {

            c.setTime(format.parse(pTime));

        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return String.valueOf(c.get(Calendar.DAY_OF_WEEK) - 1);
    }

    public static String getFormatStringWithUS(String title) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Date date = null; //初始化date
        try {
            date = sdf.parse(title);
            SimpleDateFormat dd = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
            return dd.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getFormatStringWithZH(String title) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
        Date date = null; //初始化date
        try {
            date = sdf.parse(title);
            SimpleDateFormat dd = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            return dd.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getNowTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date curDate = new Date(System.currentTimeMillis());//获取当前时间
        String str = formatter.format(curDate);
        return str;
    }


    public static String formatDate(String str, String s1, String s2) {
        SimpleDateFormat sf1 = new SimpleDateFormat(s1);
        SimpleDateFormat sf2 = new SimpleDateFormat(s2);
        String sfStr = "";
        try {
            sfStr = sf2.format(sf1.parse(str));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return sfStr;
    }
}
