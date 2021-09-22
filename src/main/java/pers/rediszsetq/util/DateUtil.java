package pers.rediszsetq.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class DateUtil {

    /**
     * 获取LocalDateTime毫秒数
     * @param localDateTime
     * @return
     */
    public static  long getMilli(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
    }

    /**
     * 获取东8区当前时间
     * @return
     */
    public static LocalDateTime getNow() {
        return ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toLocalDateTime();
    }
}
