package diff;

import diffproject.ProjectConfig;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class DiffGlobalConfig {

    // 配置信息 在payload的suppier 中可以获取到
    // 可以根据自己传递一些环境之类的参数
    private Map<String, Object> metadata;

    private String leftApiAddress;

    private String leftMethod;

    private String leftToken;

    private String rightApiAddress;

    private String rightMethod;

    private String rightToken;

    private boolean debug = true;

    private ProjectConfig projectConfig;

    private List<Map<String, Object>> modeConfigs;
}
