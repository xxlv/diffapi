package diff.utils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 简单的 rule 执行统计
 */
public class StatisticsUtils {

    private static final AtomicInteger count = new AtomicInteger(0);

    public static void inc() {
        count.getAndIncrement();
        Log.infos("当前处理进度 [" + getCount() + "]");
    }


    public static void inc(String desc) {
        count.getAndIncrement();
        Log.infos("当前正在执行 【" + desc + "】 [" + getCount() + "]");
    }

    public static int getCount() {
        return count.get();
    }

}
