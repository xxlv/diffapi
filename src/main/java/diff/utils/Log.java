package diff.utils;

public class Log {


    private static String threadName() {
        return Thread.currentThread().getName();
    }

    public static void infos(final String msg) {
        System.out.println("[INFO] -- " + threadName() + "-- " + msg);
    }

    public static void success(final String msg) {
        System.out.println("[SUCCESS] --" + msg);
    }

    public static void warns(final String msg) {
        System.out.println("[WARN] --" + msg);
    }

    public static void errors(final String msg) {
        System.out.println("[ERROR] --" + msg);
    }


}
