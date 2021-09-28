package diff.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.Objects;

public class JsonPaser {
    private static JSONObject getObj(JSONObject obj, String node) {
        try {
            if (node.contains("[")) {
                JSONArray arr = obj.getJSONArray(node.substring(0, node.indexOf("[")));
                for (int i = 0; i < arr.size(); i++) {
                    if ((i + "").equals(node.substring(node.indexOf("["), node.indexOf("]")).replace("[", ""))) {
                        return arr.getJSONObject(i);
                    }
                }
            } else {
                return obj.getJSONObject(node);
            }
        } catch (Exception e) {
            return obj;
        }
        return null;
    }


    public static synchronized String getNodeValue(JSONObject obj, String jsonPath) throws Exception {
        Objects.requireNonNull(obj,"NodeValue 不能为空，检查是否未请求到数据");

        String[] nodes = jsonPath.split("\\.");
        for (int i = 1; i < nodes.length; i++) {
            if (obj != null) {
                obj = getObj(obj, nodes[i]);
            }
            if ((i + 1) == nodes.length) {
                try {
                    return obj.getString(nodes[i]);
                } catch (Exception e) {
                    return "JSONException:" + e.getMessage() + ",NodeString:" + obj.toString();
                }
            }
        }
        return null;
    }


    public static synchronized String getNodeValue(String jsonContent, String jsonPath) throws Exception {
        JSONObject obj = JSON.parseObject(jsonContent);
        return getNodeValue(JSON.parseObject(jsonContent), jsonPath);
    }
}
