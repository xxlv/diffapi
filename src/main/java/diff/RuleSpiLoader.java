package diff;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class RuleSpiLoader {

    private static final AtomicBoolean init = new AtomicBoolean(false);
    private static final Map<String, List<RuleApi>> processorSPIContainer = new ConcurrentHashMap<>();

    public static void register() {
        if (!init.get()) {
            ServiceLoader<RuleApi> serviceLoader = ServiceLoader
                    .load(RuleApi.class);
            for (RuleApi spi : serviceLoader) {
                String id = spi.id();
                if (processorSPIContainer.containsKey(id)) {
                    List<RuleApi> ruleApis = processorSPIContainer.get(id);
                    ruleApis.add(spi);
                    processorSPIContainer.put(spi.id(), ruleApis);
                } else {
                    processorSPIContainer.put(id, Collections.singletonList(spi));
                }
            }
            init.compareAndSet(false, true);
        }
    }


    public static List<RuleApi> load(String id) {
        register();

        if (processorSPIContainer.containsKey(id)) {
            return processorSPIContainer.get(id);
        }
        return new ArrayList<>();
    }
}
