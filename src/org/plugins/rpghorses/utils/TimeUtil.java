package org.plugins.rpghorses.utils;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Created by Rory on 11/21/2016.
 */
public class TimeUtil {

    private static String second = "second";
    private static String seconds = "seconds";
    private static String minute = "minute";
    private static String minutes = "minutes";
    private static String hour = "hour";
    private static String hours = "hours";
    private static String day = "day";
    private static String days = "days";
    private static String week = "week";
    private static String weeks = "weeks";
    private static String month = "month";
    private static String months = "months";
    private static String year = "year";
    private static String years = "years";
    private static String and = "and";

    public static void refreshUnitStrings(FileConfiguration config, String path) {
        TimeUtil.setSecond(config.getString(path + "second", "second"));
        TimeUtil.setSeconds(config.getString(path + "seconds", "seconds"));
        TimeUtil.setMinute(config.getString(path + "minute", "minute"));
        TimeUtil.setMinutes(config.getString(path + "minutes", "minutes"));
        TimeUtil.setHour(config.getString(path + "hour", "hour"));
        TimeUtil.setHours(config.getString(path + "hours", "hours"));
        TimeUtil.setDay(config.getString(path + "day", "day"));
        TimeUtil.setDays(config.getString(path + "days", "days"));
        TimeUtil.setWeek(config.getString(path + "week", "week"));
        TimeUtil.setWeeks(config.getString(path + "weeks", "weeks"));
        TimeUtil.setMonth(config.getString(path + "month", "month"));
        TimeUtil.setMonths(config.getString(path + "months", "months"));
        TimeUtil.setYear(config.getString(path + "year", "year"));
        TimeUtil.setYears(config.getString(path + "years", "years"));
        TimeUtil.setAnd(config.getString(path + "and", "and"));
    }

    public static void setSecond(String second) {
        TimeUtil.second = second;
    }

    public static void setSeconds(String seconds) {
        TimeUtil.seconds = seconds;
    }

    public static void setMinute(String minute) {
        TimeUtil.minute = minute;
    }

    public static void setMinutes(String minutes) {
        TimeUtil.minutes = minutes;
    }

    public static void setHour(String hour) {
        TimeUtil.hour = hour;
    }

    public static void setHours(String hours) {
        TimeUtil.hours = hours;
    }

    public static void setDay(String day) {
        TimeUtil.day = day;
    }

    public static void setDays(String days) {
        TimeUtil.days = days;
    }

    public static void setWeek(String week) {
        TimeUtil.week = week;
    }

    public static void setWeeks(String weeks) {
        TimeUtil.weeks = weeks;
    }

    public static void setMonth(String month) {
        TimeUtil.month = month;
    }

    public static void setMonths(String months) {
        TimeUtil.months = months;
    }

    public static void setYear(String year) {
        TimeUtil.year = year;
    }

    public static void setYears(String years) {
        TimeUtil.years = years;
    }

    public static void setAnd(String and) {
        TimeUtil.and = and;
    }

    public static int getUnitCount(String arg) {
        int count = 0;
        if (arg.length() > 1) {
            if (NumberUtil.isInt(arg.substring(0, arg.length() - 1))) {
                if (arg.endsWith("s")) {
                    count++;
                } else if (arg.endsWith("m")) {
                    count++;
                } else if (arg.endsWith("h")) {
                    count++;
                } else if (arg.endsWith("d")) {
                    count++;
                } else if (arg.endsWith("w")) {
                    count++;
                } else if (arg.endsWith("y")) {
                    count++;
                }

            }
        } else if (arg.length() > 2) {
            if (arg.endsWith("mo") && NumberUtil.isInt(arg.substring(0, arg.length() - 2))) {
                count++;
            }
        }
        return count;
    }

    public static boolean isUnitOfTime(String arg) {
        if (arg.length() > 1) {
            if (NumberUtil.isInt(arg.substring(0, arg.length() - 1))) {
                if (arg.endsWith("s")) {
                    return true;
                } else if (arg.endsWith("m")) {
                    return true;
                } else if (arg.endsWith("h")) {
                    return true;
                } else if (arg.endsWith("d")) {
                    return true;
                } else if (arg.endsWith("w")) {
                    return true;
                } else if (arg.endsWith("y")) {
                    return true;
                }

            }
        } else if (arg.length() > 2) {
            if (arg.endsWith("mo") && NumberUtil.isInt(arg.substring(0, arg.length() - 2))) {
                return true;
            }
        }

        int index = 0;
        while (!arg.equals("") && index < arg.length()) {
            char ch = arg.charAt(index);
            switch (ch) {
                case 's':
                    arg = arg.substring(index + 1);
                    break;
                case 'm':
                    if (arg.charAt(index + 1 >= arg.length() ? index : index + 1) == 'o') {
                        arg = arg.substring(index + 2);
                    } else {
                        arg = arg.substring(index + 1);
                    }
                    index = 0;
                    break;
                case 'h':
                    arg = arg.substring(index + 1);
                    index = 0;
                    break;
                case 'd':
                    arg = arg.substring(index + 1);
                    index = 0;
                    break;
                case 'w':
                    arg = arg.substring(index + 1);
                    index = 0;
                    break;
                case 'y':
                    arg = arg.substring(index + 1);
                    index = 0;
                    break;
                default:
                    if (!NumberUtil.isInt("" + ch)) {
                        return false;
                    }
            }
            index++;
        }
        return true;
    }

    public static String formatTime(long seconds) {
        String timeText = "";
        seconds += 1L;
        if (seconds % (60 * 60 * 24 * 365) >= 0) {
            int timecalc = (int) Math.floor(seconds / (60 * 60 * 24 * 365));
            seconds = seconds % (60 * 60 * 24 * 365);
            if (timecalc != 0) {
                if (timecalc == 1) {
                    timeText += timecalc + " " + TimeUtil.year + ", ";
                } else {
                    timeText += timecalc + " " + TimeUtil.years + ", ";
                }
            }
        }

        if (seconds % (30 * 24 * 60 * 60) >= 0) {
            int timecalc = (int) Math.floor(seconds / (30 * 24 * 60 * 60));
            seconds = seconds % (30 * 24 * 60 * 60);
            if (timecalc != 0) {
                if (timecalc == 1) {
                    timeText += timecalc + " " + TimeUtil.month + ", ";
                } else {
                    timeText +=  timecalc + " " + TimeUtil.months + ", ";
                }
            }
        }

        if (seconds % (60 * 60 * 24 * 7) >= 0) {
            int timecalc = (int) Math.floor(seconds / (60 * 60 * 24 * 7));
            seconds = seconds % (60 * 60 * 24 * 7);
            if (timecalc != 0) {
                if (timecalc == 1) {
                    timeText += timecalc + " " + TimeUtil.week + ", ";
                } else {
                    timeText += timecalc + " " + TimeUtil.weeks + ", ";
                }
            }
        }

        if (seconds % (60 * 60 * 24) >= 0) {
            int timecalc = (int) Math.floor(seconds / (60 * 60 * 24));
            seconds = seconds % (60 * 60 * 24);
            if (timecalc != 0) {
                if (timecalc == 1) {
                    timeText += timecalc + " " + TimeUtil.day + ", ";
                } else {
                    timeText += timecalc + " " + TimeUtil.days + ", ";
                }
            }
        }

        if (seconds % (60 * 60) >= 0) {
            int timecalc = (int) Math.floor(seconds / (60 * 60));
            seconds = seconds % (60 * 60);
            if (timecalc != 0) {
                if (timecalc == 1) {
                    timeText += timecalc + " " + TimeUtil.hour + ", ";
                } else {
                    timeText += timecalc + " " + TimeUtil.hours + ", ";
                }
            }
        }

        if (seconds % 60 >= 0) {
            int timecalc = (int) Math.floor(seconds / (60));
            seconds = seconds % (60);
            if (timecalc != 0) {
                if (timecalc == 1) {
                    timeText += timecalc + " " + TimeUtil.minute + ", ";
                } else {
                    timeText += timecalc + " " + TimeUtil.minutes + ", ";
                }
            }
        }

        if (seconds > 0) {
            if (seconds == 1) {
                timeText += seconds + " " + TimeUtil.second + ", ";
            } else {
                timeText += seconds + " " + TimeUtil.seconds + ", ";
            }
        }
        if (timeText.length() > 0) {
            timeText = timeText.substring(0, timeText.length() - 2);
            int lastComma = timeText.lastIndexOf(",");
            if (lastComma != -1) {
                timeText = timeText.substring(0, lastComma) + " " + TimeUtil.and + " " + timeText.substring(lastComma + 2, timeText.length());
            }
        } else {
            timeText = "0 " + TimeUtil.seconds;
        }
        return timeText;
    }

    public static long convertToSeconds(String time) {
        time = time.toLowerCase();
        long seconds = 0;
        int index = 0;
        if (time.equals("") || !((time.contains("s") || time.contains("m") || time.contains("h") || time.contains("d") || time.contains("w") || time.contains("y")))) {
            return -1;
        }
        while (!time.equals("")) {
            char ch = time.charAt(index);
            switch (ch) {
                case 's':
                    if (!NumberUtil.isInt(time.substring(0, index))) {
                        return -1;
                    }
                    seconds += Integer.parseInt(time.substring(0, index));
                    if (time.length() > index + 1) {
                        time = time.substring(index + 1);
                        index = 0;
                    } else {
                        return seconds;
                    }
                    break;
                case 'm':
                    if (!NumberUtil.isInt(time.substring(0, index))) {
                        return -1;
                    }
                    if (time.charAt(index + 1 >= time.length() ? index : index + 1) == 'o') {
                        seconds += Integer.parseInt(time.substring(0, index)) * 30 * 24 * 60 * 60;
                        if (time.length() > index + 2) {
                            time = time.substring(index + 2);
                        } else {
                            return seconds;
                        }
                    } else {
                        seconds += Integer.parseInt(time.substring(0, index)) * 60;
                        if (time.length() > index + 1) {
                            time = time.substring(index + 1);
                        } else {
                            return seconds;
                        }
                    }
                    index = 0;
                    break;
                case 'h':
                    if (!NumberUtil.isInt(time.substring(0, index))) {
                        return -1;
                    }
                    seconds += Integer.parseInt(time.substring(0, index)) * 60 * 60;
                    if (time.length() > index + 1) {
                        time = time.substring(index + 1);
                        index = 0;
                    } else {
                        return seconds;
                    }
                    break;
                case 'd':
                    if (!NumberUtil.isInt(time.substring(0, index))) {
                        return -1;
                    }
                    seconds += Integer.parseInt(time.substring(0, index)) * 60 * 60 * 24;
                    if (time.length() > index + 1) {
                        time = time.substring(index + 1);
                        index = 0;
                    } else {
                        return seconds;
                    }
                    break;
                case 'w':
                    if (!NumberUtil.isInt(time.substring(0, index))) {
                        return -1;
                    }
                    seconds += Integer.parseInt(time.substring(0, index)) * 60 * 60 * 24 * 7;
                    if (time.length() > index + 1) {
                        time = time.substring(index + 1);
                        index = 0;
                    } else {
                        return seconds;
                    }
                    break;
                case 'y':
                    if (!NumberUtil.isInt(time.substring(0, index))) {
                        return -1;
                    }
                    seconds += Integer.parseInt(time.substring(0, index)) * 60 * 60 * 24 * 365;
                    if (time.length() > index + 1) {
                        time = time.substring(index + 1);
                        index = 0;
                    } else {
                        return seconds;
                    }
                    break;
            }
            index++;
        }
        return seconds;
    }

}
