package com.baixiaosheng.inventory.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    /**
     * 格式化时间戳为yyyy-MM-dd HH:mm:ss格式
     * @param timeMillis 时间戳（毫秒）
     * @return 格式化后的日期字符串
     */
    public static String formatTime(long timeMillis) {
        if (timeMillis <= 0) {
            return "无";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timeMillis));
    }

    /**
     * 格式化Date对象为yyyy-MM-dd格式（适配筛选场景）
     * @param date 日期对象
     * @return 格式化后的日期字符串，null返回空串
     */
    public static String formatDateToYmd(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        return sdf.format(date);
    }

    /**
     * 格式化时间戳为yyyy-MM-dd格式（适配物品详情简化展示）
     * @param timeMillis 时间戳（毫秒）
     * @return 格式化后的日期字符串，无效值返回"无"
     */
    public static String formatTimeToYmd(long timeMillis) {
        if (timeMillis <= 0) {
            return "无";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        return sdf.format(new Date(timeMillis));
    }
}