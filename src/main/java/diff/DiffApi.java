package diff;

import static diff.utils.Log.errors;
import static diff.utils.Log.infos;
import static diff.utils.Log.warns;

import com.alibaba.fastjson.JSONObject;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import diff.cache.CacheLoader;
import diff.utils.HttpClient;
import diff.utils.MD5Utils;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor // 为了支持对象拷贝
public class DiffApi {

    private static final String defaultPayload = "{}";

    private static final HttpClient CLIENT = new HttpClient();

    private String api;

    private String host;

    private String methodVerb;
    // 启用则不发送http
    // debug情况下用
    private static final boolean ENABLE_MOCK = false;


    ExecutorService executors = Executors.newFixedThreadPool(Runtime.getRuntime()
                    .availableProcessors() * 2,
            new ThreadFactoryBuilder().setNameFormat("diffapi-thread-%d").build());


    public Map<String, DiffResult> request(String direction, Payload payload, String token)
            throws Exception {
        return request(payload, token);
    }

    public Map<String, DiffResult> request(Payload payload, String token) throws Exception {
        return request(payload, token, true);
    }

    public Map<String, DiffResult> request(Payload payload, String token,
            boolean enableCache) throws Exception {
        Objects.requireNonNull(api, "api not found");
        Objects.requireNonNull(host, "host not found");
        Objects.requireNonNull(methodVerb, "methodVerb not found");
        // 跟踪payload
        Map<String, Future<DiffResult>> futureMap = new ConcurrentHashMap<>();
        if (methodVerb.equalsIgnoreCase("post")) {
            List<String> payloads = payload.getOrigin();
            AtomicInteger order = new AtomicInteger(0);

            payloads.forEach(p -> {
                futureMap.putIfAbsent(order.getAndIncrement() + "",
                        executors.submit(() -> {
                            JSONObject jsonObject = null;
                            try {
                                String pd = p != null ? p : defaultPayload;
                                infos("current process using payload " + pd);
                                String name = Thread.currentThread().getName();
                                String cacheKey = MD5Utils.stringToMD5(api + host + pd);
                                String cacheNamespace = MD5Utils.stringToMD5(api + host);
                                if (enableCache) {
                                    jsonObject = CacheLoader.load(cacheNamespace, cacheKey);

                                    if (jsonObject != null) {
                                        infos("[T=" + name + "]正在使用本地缓存数据 [" + cacheKey + "]" + api
                                                + "[" + host + "]"
                                                + " Using payload = " + (pd));
                                    }
                                }
                                if (null == jsonObject) {
                                    if (ENABLE_MOCK) {
                                        infos("正在mock数据");
                                        jsonObject = new JSONObject();
                                    } else {
                                        infos("[T=" + name + "]正在发起请求数据 " + api + "[" + host
                                                + "]"
                                                + " Using payload = " + (pd));
                                        jsonObject = CLIENT
                                                .post(host, api, pd,
                                                        token);
                                        CacheLoader.saveCache(cacheNamespace, cacheKey, jsonObject);
                                    }
                                }
                            } catch (Exception e) {
                                warns("无法请求" + api + " payload " + p);
                            }
                            return new DiffResult(order.get(), jsonObject, p);
                        }));
            });

            Map<String, DiffResult> resultMap = new ConcurrentHashMap<>();
            futureMap.keySet().forEach(k -> {
                try {
                    Future<DiffResult> value = futureMap.get(k);
                    resultMap.putIfAbsent(k, value.get());
                } catch (Exception e) {
                    errors("异步获取API结果失败" + e.getCause());
                }
            });

            return resultMap;
        }
        return null;
    }

}
