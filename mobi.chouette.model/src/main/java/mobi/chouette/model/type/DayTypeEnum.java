package mobi.chouette.model.type;

import java.time.DayOfWeek;

/**
 * Day type for timetable
 */
public enum DayTypeEnum {
    /**
     * from monday to friday
     */
    WeekDay,
    /**
     * saturday and sunday
     */
    WeekEnd,
    /**
     * monday
     */
    Monday,
    /**
     * tuesday
     */
    Tuesday,
    /**
     * wednesday
     */
    Wednesday,
    /**
     * thursday
     */
    Thursday,
    /**
     * friday
     */
    Friday,
    /**
     * saturday
     */
    Saturday,
    /**
     * sunday
     */
    Sunday,
    /**
     * school holliday (unused)
     */
    SchoolHolliday,
    /**
     * public holliday (unused)
     */
    PublicHolliday,
    /**
     * market day (unused)
     */
    MarketDay;

    public static DayTypeEnum from(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY:
                return Monday;
            case TUESDAY:
                return Tuesday;
            case WEDNESDAY:
                return Wednesday;
            case THURSDAY:
                return Thursday;
            case FRIDAY:
                return Friday;
            case SATURDAY:
                return Saturday;
            case SUNDAY:
                return Sunday;
            default:
                throw new IllegalArgumentException("Invalid day of week: " + dayOfWeek);
        }
    }

    public static DayTypeEnum from(int dayOfWeek) {
        switch (dayOfWeek) {
            case 1:
                return Monday;
            case 2:
                return Tuesday;
            case 3:
                return Wednesday;
            case 4:
                return Thursday;
            case 5:
                return Friday;
            case 6:
                return Saturday;
            case 7:
                return Sunday;
            default:
                throw new IllegalArgumentException("Invalid day of week: " + dayOfWeek);
        }
    }

    public int buildBitMask() {
        return 1 << this.ordinal();
    }

}
