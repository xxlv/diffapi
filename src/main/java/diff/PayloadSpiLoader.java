package diff;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class PayloadSpiLoader {

    private static final AtomicBoolean init = new AtomicBoolean(false);
    private static final Map<String, List<PayloadSupplier>> processorSPIContainer =
            new ConcurrentHashMap<>();

    public static void register() {
        if (!init.get()) {
            ServiceLoader<PayloadSupplier> serviceLoader = ServiceLoader
                    .load(PayloadSupplier.class);
            for (PayloadSupplier spi : serviceLoader) {
                String id = spi.id();
                if (processorSPIContainer.containsKey(id)) {
                    List<PayloadSupplier> spis = new ArrayList<>(processorSPIContainer.get(id));
                    spis.add(spi);
                    processorSPIContainer.put(spi.id(), spis);
                } else {
                    processorSPIContainer.put(id, Collections.singletonList(spi));
                }
            }
            init.compareAndSet(false, true);
        }
    }


    public static List<PayloadSupplier> load(String id) {
        register();
        if (processorSPIContainer.containsKey(id)) {
            return processorSPIContainer.get(id);
        }
        return new ArrayList<>();
    }
}
