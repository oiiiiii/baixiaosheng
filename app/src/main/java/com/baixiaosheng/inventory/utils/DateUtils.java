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
}