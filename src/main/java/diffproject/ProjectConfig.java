package diffproject;

import com.alibaba.fastjson.JSONObject;
import java.util.List;
import lombok.Data;

@Data
public class ProjectConfig {

    private String name;
    private String description;
    private List<String> authors;
    private String version;

    public static ProjectConfig getProjectConfig(JSONObject config) {
        ProjectConfig projectConfig = new ProjectConfig();
        if (config != null) {

            if (config.containsKey("name")) {
                projectConfig.setName(config.getString("name"));
            }
            if (config.containsKey("desc")) {
                projectConfig.setDescription(config.getString("desc"));
            }
            if (config.containsKey("version")) {
                projectConfig.setVersion(config.getString("version"));
            }
            if (config.containsKey("authors")) {
                projectConfig.setAuthors(config.getJSONArray("authors").toJavaList(String.class));
            }
        }

        return projectConfig;
    }
}
