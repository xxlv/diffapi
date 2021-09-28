package diff;

public class Bootstrap {

    public static void main(String[] args) {
        try {
            String id = System.getenv("diffId");
            String mode = System.getenv("diffMode");
            Diff diff = new Diff();
            Thread finish = new Thread(diff::exit);
            Runtime.getRuntime().addShutdownHook(finish);
            if (mode != null) {
                diff.setMode(mode);
            }
            if (id != null && id.length() > 0) {
                diff.start(id);
            } else {
                diff.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }


}
