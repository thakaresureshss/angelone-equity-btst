package com.trade.algotrade.utils;

import com.trade.algotrade.exceptions.KotakTradeDateException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

@Component
public class DateUtils {

    public static ZoneId IST_TIME_ZONE = ZoneId.of("Asia/Kolkata");

    private static final Logger logger = LoggerFactory.getLogger(DateUtils.class);
    public static final String DATE_TIME_FORMAT = "dd-MM-yyyy hh:mm:ss";
    public static final String NSE_DATE_FORMAT = "dd-MMM-yyyy";

    public static final String ALGO_TRADE_REQUEST_DATE_TO_LOCALDATETIME = "dd-MM-yyyy HH:mm:ss";
    public static final String KOTAK_EXPIRY_DATE_FORMAT = "ddMMMyy";

    public static final String ANGEL_REQUEST_FORMAT = "yyyy-MM-dd hh:mm";

    public static final String KOTAK_ORDER_RESPONSE_FORMAT = "MMM dd yyyy hh:mm:ss:SSSa";
    // Don't change this add new date format if required

    public static final String TIME_BLOCK_DEFAULT = " 00:00:00";

    public static DateTimeFormatter candleDateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);

    public static LocalDateTime getCurrentDateTimeIst() {
        ZonedDateTime zonedDateTime = ZonedDateTime.now(IST_TIME_ZONE);
        return zonedDateTime.toLocalDateTime();
    }

    public static LocalDateTime getTodayIst() {
        LocalDateTime localDateTime = LocalDate.now(IST_TIME_ZONE).atTime(9, 15, 0, 0);
        return localDateTime;
    }

    public static DateTimeFormatter getCandledatetimeformatter() {
        return candleDateTimeFormatter;
    }

    public static boolean isSameDayUsingInstant(Date date, Date date1) {
        if (date == null || date1 == null) {
            return false;
        }
        return removeTimeFromDate(date).equals(removeTimeFromDate(date1));
    }

    public static boolean isSameDayLocalDate(LocalDate date, LocalDate date1) {
        if (date == null || date1 == null) {
            return false;
        }
        return date.isEqual(date1);

    }

    private static Date removeTimeFromDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        date = calendar.getTime();
        return date;
    }

    public static final Date convertStringToDate(String inputStringDate, String fromFormat) {
        if (StringUtils.isEmpty(inputStringDate)) {
            return null;
        }
        SimpleDateFormat dateFormatter = new SimpleDateFormat(fromFormat, Locale.ENGLISH);
        try {
            return dateFormatter.parse(inputStringDate);
        } catch (ParseException e) {
            logger.error("Exception > Date {} , Format {} , Exception {} ", inputStringDate, fromFormat, e);
        }
        return null;
    }

    public static LocalDate comingWeeklyExpiryDayDate(int expiryDay) {
        Calendar calendar = Calendar.getInstance();
        int currentDay = calendar.get(Calendar.DAY_OF_WEEK);
        int dayDifference = expiryDay - currentDay;
        if (dayDifference > 0) {
            calendar.add(Calendar.DAY_OF_YEAR, dayDifference);
        }
        if (dayDifference < 0) {
            dayDifference += 7;
            calendar.add(Calendar.DAY_OF_YEAR, dayDifference);
        }

        return convertToLocalDate(calendar.getTime());

    }

    public static LocalDate comingMonthlyExpiryDayDate() {
        Calendar calendar = Calendar.getInstance();
        int currentDay = calendar.get(Calendar.DAY_OF_WEEK);
        int dayDifference = Calendar.THURSDAY - currentDay;
        if (dayDifference == 0) {
            calendar.add(Calendar.DAY_OF_YEAR, dayDifference);
        }
        if (dayDifference < 0) {
            dayDifference += 7;
            calendar.add(Calendar.DAY_OF_YEAR, dayDifference);
        }

        return convertToLocalDate(calendar.getTime());

    }

    public static Calendar getLastThursdayOfMonth(Calendar cal, int offset) {
        int dayofweek;
        cal.set(Calendar.MONTH, cal.get(Calendar.MONTH) + offset);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        dayofweek = cal.get(Calendar.DAY_OF_WEEK);
        if (dayofweek < Calendar.THURSDAY)
            cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH) - 7
                    + Calendar.THURSDAY - dayofweek);
        else
            cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH)
                    + Calendar.THURSDAY - dayofweek);

        return cal;
    }

    public static LocalDate convertToLocalDate(Date dateToConvert) {
        if (dateToConvert == null) {
            throw new KotakTradeDateException();
        }
        return dateToConvert.toInstant().atZone(IST_TIME_ZONE).toLocalDate();
    }

    public static String convertToString(LocalDate localDate, String format) {
        return localDate.format(DateTimeFormatter.ofPattern(format));
    }


    public static boolean isTodayWeekend() {
        DayOfWeek day = DayOfWeek.of(LocalDate.now().get(ChronoField.DAY_OF_WEEK));
        return day == DayOfWeek.SUNDAY || day == DayOfWeek.SATURDAY;
    }

    public static boolean isDayWeekend(LocalDate inputDate) {
        DayOfWeek day = DayOfWeek.of(inputDate.get(ChronoField.DAY_OF_WEEK));
        return day == DayOfWeek.SUNDAY || day == DayOfWeek.SATURDAY;
    }

    public static LocalDateTime convertStringToLocalDateTime(String dateTime, String format) {
        logger.debug("Converting string to date format for date string {}", dateTime);
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            return LocalDateTime.parse(dateTime, formatter);
        } catch (Exception e) {
            logger.error("Exception > Date {} , Format {} , Exception {} ", dateTime, format, e);
        }
        return null;
    }

    public static String epochTimeToString(Long epochTime) {
        LocalDateTime localDate = Instant.ofEpochMilli(epochTime).atZone(ZoneId.systemDefault()).toLocalDateTime();
        return localDate.format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
    }

    public static boolean isTradingSessionTime() {
        LocalDateTime marketOpenTime = getMarketOpenDateTime();
        LocalDateTime marketCloseTime = getMarketCloseDateTime();
        LocalDateTime now = LocalDateTime.now(IST_TIME_ZONE);
        return (now.isAfter(marketOpenTime)
                && now.isBefore(marketCloseTime));
    }

    public static boolean isIntradaySquareOffTime() {
        LocalDateTime preFifteenMinMarketClose = getSquareoffStartDateTime();
        LocalDateTime marketCloseTime = getMarketOpenDateTime();
        LocalDateTime now = LocalDateTime.now(IST_TIME_ZONE);
        return (now.isAfter(preFifteenMinMarketClose)
                && now.isBefore(marketCloseTime));
    }


    public static boolean isBetweenPreMarketCloseAndMarketOpenTime() {
        LocalDateTime preMarketCloseTime = getMarketCloseDateTime();
        LocalDateTime marketOpenTime = getMarketOpenDateTime();
        LocalDateTime now = LocalDateTime.now(IST_TIME_ZONE);
        return (now.isAfter(preMarketCloseTime)
                && now.isBefore(marketOpenTime));
    }

    public static LocalDateTime getMarketOpenDateTime() {
        if (CommonUtils.getOffMarketHoursTestFlag()) {
            return LocalDate.now(IST_TIME_ZONE).atTime(5, 15, 0, 0);
        }
        return LocalDate.now(IST_TIME_ZONE).atTime(9, 0, 0, 0);
    }

    public static LocalDateTime getPostFiveMinuteMarketOpenTime() {
        return LocalDate.now(IST_TIME_ZONE).atTime(9, 20, 0, 0);
    }

    private static LocalDateTime getSquareoffStartDateTime() {
        return LocalDate.now(IST_TIME_ZONE).atTime(15, 15, 0, 0);
    }


    public static LocalDateTime getMarketCloseDateTime() {
        return LocalDate.now(IST_TIME_ZONE).atTime(15, 30, 0, 0);
    }

    public static LocalDateTime marketSecondHalfStartDateTime() {
        return LocalDate.now(IST_TIME_ZONE).atTime(12, 30);
    }

    public static boolean isBigCandleStopTime() {
        LocalDateTime marketOpenTime = LocalDate.now(IST_TIME_ZONE).atTime(15, 15);
        LocalDateTime now = LocalDateTime.now(IST_TIME_ZONE);
        return now.isAfter(marketOpenTime);
    }

    public static boolean isTimeBetween(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now(IST_TIME_ZONE);
        return now.isAfter(startTime) && now.isBefore(endTime);
    }

    public static boolean isExpiryAfternoonTime() {
        LocalDateTime expiryAfternoonTime = LocalDate.now(IST_TIME_ZONE).atTime(13, 00, 0, 0);
        LocalDateTime now = LocalDateTime.now(IST_TIME_ZONE);
        return now.isAfter(expiryAfternoonTime);
    }

    public static boolean isBeforeTenAm() {
        LocalDateTime tenAmTime = LocalDate.now(IST_TIME_ZONE).atTime(10, 00, 0, 0);
        LocalDateTime now = LocalDateTime.now(IST_TIME_ZONE);
        return now.isBefore(tenAmTime);
    }

    public static boolean isBeforeTime(String time) {
        // format HH:MM:SS E.g. 14:20:10
        String[] times = time.split(":");
        LocalDateTime tenAmTime = LocalDate.now(IST_TIME_ZONE).atTime(Integer.valueOf(times[0]), Integer.valueOf(times[1]), Integer.valueOf(times[2]), 0);
        LocalDateTime now = LocalDateTime.now(IST_TIME_ZONE);
        return now.isBefore(tenAmTime);
    }

    public static boolean isAfterTime(String time) {
        String[] times = time.split(":");
        LocalDateTime expiryAfternoonTime = LocalDate.now(IST_TIME_ZONE).atTime(Integer.valueOf(times[0]), Integer.valueOf(times[1]), Integer.valueOf(times[2]), 0);
        LocalDateTime now = LocalDateTime.now(IST_TIME_ZONE);
        return now.isAfter(expiryAfternoonTime);
    }

    public static boolean getDateInFormat(LocalDate date, String format) {
        LocalDateTime expiryAfternoonTime = LocalDate.now(IST_TIME_ZONE).atTime(15, 15, 0, 0);
        LocalDateTime now = LocalDateTime.now(IST_TIME_ZONE);
        return now.isAfter(expiryAfternoonTime);
    }

    public static LocalDateTime getDateTime(LocalDate localDate, String time) {
        // format HH:MM:SS E.g. 14:20:10
        String[] times = time.split(":");
        return localDate.now(IST_TIME_ZONE).atTime(Integer.valueOf(times[0]), Integer.valueOf(times[1]), Integer.valueOf(times[2]), 0);
    }

    public static LocalDateTime getMarketOpenDateTime(String time) {
        String[] times = time.split(":");
        return LocalDate.now(IST_TIME_ZONE).atTime(Integer.valueOf(times[0]), Integer.valueOf(times[1]), Integer.valueOf(times[2]), 0);
    }

    public static boolean isEndOrStartOfCandle() {
        //TODO Currently checking for 5 minutes candle, it can be referred from config table.
        int minute = LocalDateTime.now().getMinute();
        return minute % 5 == 0 || minute % 5 == 4;
    }
}
