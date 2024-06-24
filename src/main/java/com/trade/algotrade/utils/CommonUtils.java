package com.trade.algotrade.utils;

import com.trade.algotrade.constants.Constants;
import com.trade.algotrade.constants.ErrorCodeConstants;
import com.trade.algotrade.enitiy.NseHolidayEntity;
import com.trade.algotrade.enums.Segment;
import com.trade.algotrade.exceptions.AlgotradeException;
import com.trade.algotrade.response.ConfigurationResponse;
import com.trade.algotrade.service.AlgoConfigurationService;
import com.trade.algotrade.service.NseService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.DayOfWeek.THURSDAY;
import static java.time.temporal.TemporalAdjusters.lastInMonth;

@Component
public class CommonUtils {

    private static final Logger logger = LoggerFactory.getLogger(CommonUtils.class);
    private static BigDecimal maxPriceDropPointsStatic;
    private static boolean websocketOnFlagStatic;

    public static boolean pauseMockWebsocket;

    @Autowired
    NseService nseService;

    @Autowired
    private AlgoConfigurationService algoConfigurationService;

    @Value("${flag.aftermarketsTest}")
    private boolean offMarketHoursTestFlag;

    @Value("${flag.trailPriceFlag}")
    private boolean trailOffMarketPrice;

    @Value("${flag.maxTrailPoints}")
    private BigDecimal maxTrailPoints;


    @Value("${flag.maxPriceDropPoints}")
    private BigDecimal maxPriceDropPoints;

    public static boolean offMarketHrFlagStatic;

    public static boolean trailOffMarketPriceStatic;

    public static BigDecimal maxTrailPointsStatic;


    @Value("${flag.websocket.onFlag}")
    private boolean websocketOnFlag;

    private String monthlyExpiryDay;

    private String weeklyExpiryDay;

    @PostConstruct
    public void init() {
        offMarketHrFlagStatic = offMarketHoursTestFlag;
        trailOffMarketPriceStatic = trailOffMarketPrice;
        maxTrailPointsStatic = maxTrailPoints;
        maxPriceDropPointsStatic = maxPriceDropPoints;
        websocketOnFlagStatic = websocketOnFlag;
        ConfigurationResponse configurationDetails = algoConfigurationService.getConfigurationDetails();
        Map<String, String> configs = configurationDetails.getConfigurationDetails().getConfigs();
        if (configs == null) {
            throw new AlgotradeException(ErrorCodeConstants.EMPTY_CONFIG_ERROR);
        }
        monthlyExpiryDay = configs.get(Constants.MONTHLY_EXPIRY_DAY);
        weeklyExpiryDay = configs.get(Constants.WEEKLY_EXPIRY_DAY);
    }

    static Map<String, Map<String, String>> tokenMap = new HashMap<String, Map<String, String>>(3);

    public static Map<String, Map<String, String>> getTokenMap() {
        return tokenMap;
    }

    public static void setTokenMap(Map<String, Map<String, String>> tokenMap) {
        CommonUtils.tokenMap = tokenMap;
    }

    public static String getHostIp() {
        InetAddress ip = null;
        String hostname;
        try {
            ip = InetAddress.getLocalHost();
            hostname = ip.getHostName();
            System.out.println("Your current IP address : " + ip);
            System.out.println("Your current Hostname : " + hostname);
            return ip.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return "127.0.0.1";
    }

    public boolean isExpiryOnHoliday(Date comingThursday) {
        logger.debug(" ******[ CommonUtils ]:-[ isExpiryOnHoliday ] Completed  ********");
        final List<NseHolidayEntity> holidays = nseService.getHolidays(Segment.EQ);
        if (!CollectionUtils.isEmpty(holidays)) {
            return holidays.stream().anyMatch(h -> {
                String holidayStringDate = h.getHolidayDate();
                Date holidayDate = DateUtils.convertStringToDate(holidayStringDate, DateUtils.NSE_DATE_FORMAT);
                return DateUtils.isSameDayUsingInstant(comingThursday, holidayDate);
            });
        }
        logger.debug(" ******[ CommonUtils ]:-[ isExpiryOnHoliday ] Completed  ********");
        return false;
    }

    public boolean isTodayNseHoliday() {
        logger.debug(" ******[ CommonUtils ]:-[ isTodayNseHoliday ] Started  ********");
        if (CommonUtils.getOffMarketHoursTestFlag()) {
            return false;
        }
        if (DateUtils.isTodayWeekend()) {
            return true;
        }
        List<NseHolidayEntity> holidays = nseService.getHolidays(Segment.EQ);
        boolean isNseHoliday = holidays.stream().anyMatch(h -> {
            String holidayStringDate = h.getHolidayDate();
            Date holidayDate = DateUtils.convertStringToDate(holidayStringDate, DateUtils.NSE_DATE_FORMAT);
            Date today = new Date();
            return DateUtils.isSameDayUsingInstant(today, holidayDate);
        });
        logger.debug(" ******[ CommonUtils ]:-[ isTodayNseHoliday ] Completed  ********");
        return isNseHoliday || DateUtils.isTodayWeekend();
    }

    public boolean isTodayNseHoliday(LocalDate day) {
        if (DateUtils.isDayWeekend(day)) {
            return true;
        }
        logger.debug(" ******[ CommonUtils ]:-[ isTodayNseHoliday ] Started  ********");
        List<NseHolidayEntity> holidays = nseService.getHolidays(Segment.EQ);
        boolean isNseHoliday = holidays.stream().anyMatch(h -> {
            String holidayStringDate = h.getHolidayDate();
            Date holidayDate = DateUtils.convertStringToDate(holidayStringDate, DateUtils.NSE_DATE_FORMAT);
            Date date = Date.from(day.atStartOfDay(DateUtils.IST_TIME_ZONE).toInstant());
            return DateUtils.isSameDayUsingInstant(date, holidayDate);
        });
        logger.debug(" ******[ CommonUtils ]:-[ isTodayNseHoliday ] Completed  ********");
        return isNseHoliday;
    }

    public String findCurrentExpiryDate() {
        LocalDate currentExpiryDay = getCurrentExpiryDay();
        return DateUtils.convertToString(currentExpiryDay, DateUtils.KOTAK_EXPIRY_DATE_FORMAT);
    }

    public LocalDate getCurrentExpiryDay() {
        LocalDate currentExpiryDate = DateUtils.comingWeeklyExpiryDayDate(Calendar.WEDNESDAY);
        // check this is last week of month then check thursday as expiryDay

//        Calendar ca1 = Calendar.getInstance();
//        int currentWeekOfMonth = ca1.get(Calendar.WEEK_OF_MONTH);
//
//        int maxWeekOfMonth = getMaxWeekOfMonth(currentExpiryDate);
//
//        LocalDate lastThursdayOfMonth = LocalDate.now().with(lastInMonth(THURSDAY));
//        TemporalField monthlyExpiryWeekTemp = WeekFields.of(DayOfWeek.MONDAY, 1).weekOfMonth();
//        int monthlyExpiryWeek = lastThursdayOfMonth.get(monthlyExpiryWeekTemp);
//
//        boolean isMonthlyExpiryWeek = (currentWeekOfMonth == monthlyExpiryWeek && (LocalDate.now().equals(lastThursdayOfMonth)
//                || LocalDate.now().isBefore(lastThursdayOfMonth)))
//                || (LocalDate.now().isBefore(lastThursdayOfMonth) && ChronoUnit.DAYS.between(LocalDate.now(), lastThursdayOfMonth) <= 7) ;
//
//        List<NseHolidayEntity> holidays = nseService.getHolidays(Segment.EQ);
//        if (isMonthlyExpiryWeek) {
//            currentExpiryDate = lastThursdayOfMonth;
//            if (!CollectionUtils.isEmpty(holidays)) {
//                currentExpiryDate = getExpiryDate(currentExpiryDate, holidays, monthlyExpiryDay);
//            }
//        } else {
//            if (!CollectionUtils.isEmpty(holidays)) {
//                currentExpiryDate = getExpiryDate(currentExpiryDate, holidays, weeklyExpiryDay);
//            }
//        }
        List<NseHolidayEntity> holidays = nseService.getHolidays(Segment.EQ);
        if (!CollectionUtils.isEmpty(holidays)) {
            currentExpiryDate = getExpiryDate(currentExpiryDate, holidays, weeklyExpiryDay);
        }
        return currentExpiryDate;
    }

    private static int getMaxWeekOfMonth(LocalDate currentExpiryDate) {
        //Check max week of the current month
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, currentExpiryDate.getYear());
        calendar.set(Calendar.MONDAY, currentExpiryDate.getMonthValue());
        calendar.set(Calendar.DAY_OF_MONTH, currentExpiryDate.getDayOfMonth());
        int maxWeekOfMonth = Math.min(calendar.getActualMaximum(Calendar.WEEK_OF_MONTH), 5);
        return maxWeekOfMonth;
    }

    private LocalDate getExpiryDate(LocalDate comingExpiryDate, List<NseHolidayEntity> holidays, String expiryDay) {
        boolean isExpiryOnHolday;
        List<NseHolidayEntity> holidayOnExpiry = holidays.stream()
                .filter(h -> h.getHolidayDay().equalsIgnoreCase(expiryDay)).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(holidayOnExpiry)) {
            do {
                final LocalDate currentThursday = comingExpiryDate;
                isExpiryOnHolday = holidayOnExpiry.stream()
                        .anyMatch(h -> DateUtils.isSameDayLocalDate(currentThursday, DateUtils.convertToLocalDate(
                                DateUtils.convertStringToDate(h.getHolidayDate(), DateUtils.NSE_DATE_FORMAT))));
                if (isExpiryOnHolday) {
                    //TODO: if after decreasing the day weekend comes.. skip weekend
                    comingExpiryDate = comingExpiryDate.minusDays(1L);
                }
            } while (isExpiryOnHolday);
        }
        return comingExpiryDate;
    }

    public Boolean nextDayOfWeeklyExpiryIsHoliday() {

        LocalDate comingThursdayDate = DateUtils.comingWeeklyExpiryDayDate(Calendar.WEDNESDAY);
        List<NseHolidayEntity> holidays = nseService.getHolidays(Segment.EQ);
        if (!CollectionUtils.isEmpty(holidays)) {
            comingThursdayDate = getExpiryDate(comingThursdayDate, holidays, weeklyExpiryDay);
        }

        LocalDate nextDayOfExpiry = getExpiryDate(comingThursdayDate, holidays, weeklyExpiryDay).plusDays(1);
        return holidays.stream().anyMatch(h -> DateUtils.isSameDayLocalDate(nextDayOfExpiry, DateUtils
                .convertToLocalDate(DateUtils.convertStringToDate(h.getHolidayDate(), DateUtils.NSE_DATE_FORMAT))));
    }

    public Boolean nextDayOfMonthlyExpiryIsHoliday() {
        LocalDate comingThursdayDate = DateUtils.comingWeeklyExpiryDayDate(Calendar.WEDNESDAY);
        List<NseHolidayEntity> holidays = nseService.getHolidays(Segment.EQ);
        if (!CollectionUtils.isEmpty(holidays)) {
            comingThursdayDate = getExpiryDate(comingThursdayDate, holidays, monthlyExpiryDay);
        }

        LocalDate nextDayOfExpiry = getExpiryDate(comingThursdayDate, holidays, monthlyExpiryDay).plusDays(1);
        return holidays.stream().anyMatch(h -> DateUtils.isSameDayLocalDate(nextDayOfExpiry, DateUtils
                .convertToLocalDate(DateUtils.convertStringToDate(h.getHolidayDate(), DateUtils.NSE_DATE_FORMAT))));
    }

    public Boolean isTodayExpiryDay() {
        LocalDate currentExpiryDate = getCurrentExpiryDay();
        return LocalDate.now().equals(currentExpiryDate);
    }

    public static boolean getOffMarketHoursTestFlag() {
        return offMarketHrFlagStatic;
    }

    public static boolean getTrailPriceFlag() {
        return trailOffMarketPriceStatic;
    }

    public static BigDecimal getMaximumTrailPoints() {
        return maxTrailPointsStatic;
    }

    public static BigDecimal getMaximumPriceDropPoints() {
        return maxPriceDropPointsStatic;
    }

    public static boolean getWebsocketOnFlag() {
        return websocketOnFlagStatic;
    }

    public static BigDecimal roundPriceValueInMultipleOfTickSize(BigDecimal value, BigDecimal increment,
                                                                 RoundingMode roundingMode) {
        if (increment.signum() == 0) {
            return value;
        } else {
            BigDecimal divided = value.divide(increment, 0, roundingMode);
            BigDecimal result = divided.multiply(increment);
            return result;
        }
    }
    @Cacheable(value = "configs",key = "#configKey")
    public String getConfigValue(String configKey) {
        ConfigurationResponse configurationDetails = algoConfigurationService.getConfigurationDetails();
        if (configurationDetails == null || configurationDetails.getConfigurationDetails() == null) {
            throw new AlgotradeException(ErrorCodeConstants.EMPTY_CONFIG_ERROR);
        }
        Map<String, String> configs = configurationDetails.getConfigurationDetails().getConfigs();
        if (configs == null) {
            throw new AlgotradeException(ErrorCodeConstants.EMPTY_CONFIG_ERROR);
        }
        return configs.get(configKey);
    }

    public static void pause(int intervalInMillis){
        long Time0 = System.currentTimeMillis();
        long Time1;
        long runTime = 0;
        while (runTime < intervalInMillis) {
            Time1 = System.currentTimeMillis();
            runTime = Time1 - Time0;
        }
    }

}
