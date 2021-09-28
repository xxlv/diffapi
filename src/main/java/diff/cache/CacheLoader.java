package diff.cache;

import static diff.utils.Log.errors;
import static diff.utils.Log.infos;
import static diff.utils.Log.warns;
import static java.io.File.separator;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import diff.utils.FileUtils;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class CacheLoader {

    private static final AtomicBoolean _cacheSet = new AtomicBoolean(false);
    private static final AtomicBoolean init = new AtomicBoolean(false);
    private static final AtomicBoolean isEnableCache = new AtomicBoolean(true);

    private static final String CACHE_PATH =
            System.getProperty("user.dir") + separator + ".diffcache";

    private static String getName(String cacheKey) {
        return CACHE_PATH + separator + cacheKey + ".json";
    }


    private static final Map<String, JSONObject> globalCacheMap = new ConcurrentHashMap<>(10000);

    private static boolean isEnableCache() {
        if (!_cacheSet.get()) {
            String cacheFlag = System.getenv("enableCache");
            if (cacheFlag.equalsIgnoreCase("true")) {
                isEnableCache.set(true);
            } else if (cacheFlag.equalsIgnoreCase("false")) {
                isEnableCache.set(false);
            }
            _cacheSet.set(true);
        }
        return isEnableCache.get();
    }

    public static void preloadCache(String hash) {
        if (isEnableCache()) {
            if (!init.get()) {
                List<File> files = FileUtils.allFiles(CACHE_PATH);
                files.forEach(file -> {
                    try {
                        infos("prepare load " + file.getName());
                        String cacheNamespace = file.getName();
                        cacheNamespace = cacheNamespace.replace(".json", "");
                        if (hash != null) {
                            if (cacheNamespace.equalsIgnoreCase(hash)) {
                                globalCacheMap.putIfAbsent(cacheNamespace,
                                        (JSONObject) JSON.parse(FileUtils.readFile(file)));
                            }
                        } else {
                            String body = FileUtils.readFile(file);
                            if (body != null && body.length() > 2) {
                                globalCacheMap.putIfAbsent(cacheNamespace,
                                        (JSONObject) JSON.parse(body));
                            }
                        }

                    } catch (Exception e) {
                        errors("无法写入globalCacheMap " + e.getMessage());
                    }

                });
                infos("load cache done " + globalCacheMap.size());
                init.set(true);
            }
        }

    }


    public static void flushData() {
        infos("正在准备持久化数据到文件");
        globalCacheMap.forEach((k, v) -> {
            String name = getName(k);
            infos("正在写入缓存文件" + name);
            FileUtils.writeTo(JSON.toJSONString(v), name);
        });
    }

    private static JSONObject loadFromCacheLocalMap(String cacheNamespace, String cacheKey) {
        JSONObject jsonObject = globalCacheMap.get(cacheNamespace);

        if (jsonObject.containsKey(cacheKey)) {
            return (JSONObject) jsonObject.get(cacheKey);
        } else {
            return null;
        }
    }

    public static JSONObject load(String cacheNamespace, String cacheKey) {

        JSONObject result = null;
        if (isEnableCache()) {
            try {
                if (globalCacheMap.containsKey(cacheNamespace)) {
                    result = loadFromCacheLocalMap(cacheNamespace, cacheKey);
                    if (result != null) {
                        infos("Load cache from local map of key " + cacheNamespace + " -- "
                                + cacheKey);
                    }
                }
            } catch (Exception e) {
                warns("无法加载缓存" + e.getMessage());
            }
        }
        return result;
    }


    private static JSONObject buildCache(String cacheKey, JSONObject jsonObject) {
        JSONObject jo = new JSONObject();
        jo.put(cacheKey, jsonObject);
        return jo;
    }

    public static boolean saveCache(String cacheNamespace, String cacheKey, JSONObject jsonObject) {
        try {

            String fileName = getName(cacheNamespace);
            JSONObject cacheJSON = null;

            if (globalCacheMap.size() <= 0) {
                File file = new File(CACHE_PATH);
                if (!file.exists()) {
                    boolean created = file.mkdir();
                    if (!created) {
                        warns("Failed create dir " + file.getAbsolutePath());
                        return false;
                    }
                }
                File cacheFile = new File(fileName);
                if (cacheFile.exists()) {
                    // 存在则读取并更新
                    String cachedData = FileUtils.readFile(cacheFile);
                    if (cachedData != null) {
                        cacheJSON = (JSONObject) JSON.parse(cachedData);
                        if (cacheJSON != null) {
                            cacheJSON.put(cacheKey, jsonObject);
                        }
                    }
                }
            } else {
                if (globalCacheMap.containsKey(cacheNamespace)) {
                    cacheJSON = globalCacheMap.get(cacheNamespace);
                }
            }

            if (cacheJSON == null) {
                cacheJSON = buildCache(cacheKey, jsonObject);
            } else if (!cacheJSON.containsKey(cacheKey)) {
                cacheJSON.put(cacheKey, jsonObject);
            }

            globalCacheMap.putIfAbsent(cacheNamespace, cacheJSON);
            return true;

        } catch (Exception e) {
            warns("无法写入缓存" + e.getMessage());
        }

        return false;
    }

}
