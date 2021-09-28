package diff;


import java.util.Map;

public interface PayloadSupplier {


    default Payload get() {
        return null;
    }

    default Payload get(Map<String, Object> metadata) {
        return get();
    }

    String id();

    default String desc() {
        return id();
    }

}
