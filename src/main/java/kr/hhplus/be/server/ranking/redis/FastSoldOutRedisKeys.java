package kr.hhplus.be.server.ranking.redis;

public final class FastSoldOutRedisKeys {

    private FastSoldOutRedisKeys() {
    }

    public static String soldCount(Long scheduleId) {
        return "sold:count:" + scheduleId;
    }

    public static String soldTotal(Long scheduleId) {
        return "sold:total:" + scheduleId;
    }

    public static String soldOpenAt(Long scheduleId) {
        return "sold:openAt:" + scheduleId;
    }

    public static String soldOutAt(Long scheduleId) {
        return "sold:outAt:" + scheduleId;
    }

    public static String rankDay(String yyyyMMdd) {
        return "rank:fastSoldOut:day:" + yyyyMMdd;
    }
}
