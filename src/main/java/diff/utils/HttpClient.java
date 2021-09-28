package diff.utils;

import static diff.utils.Log.errors;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.util.concurrent.RateLimiter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.SneakyThrows;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpClient {

    static OkHttpClient client = new OkHttpClient().newBuilder()
            .build();

    static RateLimiter rateLimiter = RateLimiter.create(100);


    @SneakyThrows
    public JSONObject post(String server, String method, String payloadJson, String token) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("api", method);
        payload.put("version", "1.0");
        payload.put("nonce", UUID.randomUUID());
        payload.put("params", JSON.parseObject(payloadJson));
        payload.put("token", token);

        MediaType mediaType = MediaType.parse("application/json;charset=UTF-8");
        RequestBody body = RequestBody.create(mediaType, JSON.toJSONString(payload));
        Request request = new Request.Builder()
                .url(server)
                .method("POST", body)
                .addHeader("sec-ch-ua",
                        "\"Chromium\";v=\"92\", \" Not A;Brand\";v=\"99\", \"Google Chrome\";"
                                + "v=\"92\"")
                .addHeader("sec-ch-ua-mobile", "?0")
                .addHeader("app-platform", "web")
                .addHeader("content-type", "application/json;charset=UTF-8")
                .addHeader("accept", "application/json, text/plain, */*")
                .addHeader("app-id", "autotest")
                .addHeader("user-agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, "
                                + "like Gecko) Chrome/92.0.4515.107 Safari/537.36")
                .addHeader("app-version-name", "0.0.1")
                .addHeader("app-version", "1")
                .addHeader("origin", server)
                .addHeader("sec-fetch-site", "cross-site")
                .addHeader("sec-fetch-mode", "cors")
                .addHeader("sec-fetch-dest", "empty")
                .addHeader("accept-language", "zh,en;q=0.9,zh-CN;q=0.8,zh-TW;q=0.7,da;q=0.6")
                .build();

        try {
            rateLimiter.acquire();
            Response response = client.newCall(request).execute();
            if (response.body() != null) {
                String result = (response.body().string());
                return apiGetWay((JSONObject) JSON.parse(result));
            }
        } catch (Exception e) {
            errors("无法执行client 请求" + method);
        }
        return null;
    }


    public JSONObject apiGetWay(JSONObject jsonObject) {
        if (jsonObject.containsKey("result")) {
            return (JSONObject) jsonObject.get("result");
        }
        return jsonObject;
    }

}
