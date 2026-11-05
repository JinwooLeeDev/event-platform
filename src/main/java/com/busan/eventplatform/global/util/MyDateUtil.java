package com.busan.eventplatform.global.util;

import org.apache.commons.lang3.time.DateFormatUtils;

import java.sql.Timestamp;
import java.util.Date;

public class MyDateUtil {
    public static String timestampFormat(Timestamp timestamp) {
        Date currentDate = new Date(timestamp.getTime()); // 현재 시간 추출.
        return DateFormatUtils.format(currentDate , "yyyy-MM-dd HH : mm"); // 초 필요하다면 : ss
    }

}
