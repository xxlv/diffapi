package diff;

import static diff.utils.Log.errors;
import static diff.utils.Log.infos;

import com.alibaba.fastjson.JSONObject;
import diff.executors.ExecuteApi;
import diff.executors.ExecuteContext;
import diff.executors.GroovyExecutor;
import diff.executors.PythonExecutor;
import diff.utils.JsonPaser;
import diff.utils.MD5Utils;
import diff.utils.StatisticsUtils;
import groovy.lang.GroovyShell;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;


@Data
public class DiffRule {

    private static List<String> container = new ArrayList<>();
    private String description;
    private List<String> rules;
    private List<RuleApi> externalAPIs;

    // fixme
    private static final String pyPrefix = "py:";

    enum Lang {
        Python,
        Groovy
    }

    private static Map<Lang, ExecuteApi> executeApiMap = new ConcurrentHashMap<>();

    static {
        executeApiMap.putIfAbsent(Lang.Python, new PythonExecutor());
        executeApiMap.putIfAbsent(Lang.Groovy, new GroovyExecutor());
    }

    public void attachRule(RuleApi api) {
        if (externalAPIs == null) {
            externalAPIs = new ArrayList<>() {{
                add(api);
            }};
        } else {
            if (!externalAPIs.contains(api)) {
                // maybe
                externalAPIs = new ArrayList<>(externalAPIs);
                externalAPIs.add(api);
            }
        }
    }

    private static final GroovyShell groovyShell = new GroovyShell();

    public Map<String, DiffAssert> verify(DiffGroup diffGroup, DiffResult leftResult,
            DiffResult rightResult) {
        Map<String, DiffAssert> verifyResult = new HashMap<>();
        if (rules != null) {
            for (String rule : rules) {
                verifyResult.put(rule, checkWithScript(rule, leftResult,
                        rightResult, diffGroup));
            }
        }

        if (externalAPIs != null && externalAPIs.size() > 0) {
            infos("启用外部扩展rule 共 " + externalAPIs.size() + " 个" + "[" + description + "]");

            externalAPIs
                    .forEach((e) -> {
                        // 支持的mode
                        if (diffGroup.getMode() != null) {
                            if (!e.support().contains(diffGroup.getMode())) {
                                return;
                            }
                        }
                        verifyResult.put(e.id(), doCheck(e, leftResult,
                                rightResult, diffGroup));
                    });
        }

        return verifyResult;
    }


    private DiffAssert doCheck(RuleApi api, DiffResult left, DiffResult right,
            DiffGroup diffGroup) {
        StatisticsUtils.inc(api.desc() + " CHECK");
        String message = "";
        try {
            DiffAssert diffAssert = api.check(left, right);
            ruleCheckFinish(api.desc(), diffAssert, left, right, diffGroup);
            return diffAssert;
        } catch (Exception e) {
            message = e.getMessage();
        }

        return DiffAssert.builder().success(false).reason("无法检查 " + message).build();
    }

    private void ruleCheckFinish(String rule, DiffAssert diffAssert, DiffResult left,
            DiffResult right, DiffGroup diffGroup) {
        if (!diffAssert.isSuccess()) {
            String cacheFile = MD5Utils.stringToMD5(
                    diffGroup.getLeft().getApi().getApi() + diffGroup.getLeft().getApi()
                            .getHost());

            container.add(left.getPayload());
            errors("" + cacheFile + "[" + diffGroup.getDescription() + "]  " + "Rule:" + rule + "失败"
                    + "\npayload: L= " + left
                    .getPayload() + " R= "
                    + right.getPayload());
        }
    }

    /**
     * 执行脚本
     *
     * @param ruleScript
     * @param diffLeftResult
     * @param diffRightResult
     * @param diffGroup
     * @return
     */
    private DiffAssert checkWithScript(String ruleScript, DiffResult diffLeftResult,
            DiffResult diffRightResult,
            DiffGroup diffGroup) {
        StatisticsUtils.inc(diffGroup.getDescription() + " rule=" + ruleScript);
        JSONObject rightResult = null;
        JSONObject leftResult = null;
        if (diffRightResult != null) {
            rightResult = diffRightResult.getResult();
        }
        if (diffLeftResult != null) {
            leftResult = diffLeftResult.getResult();
        }
        ExecuteApi api = getExecuteApi(diffGroup, ruleScript);
        boolean success = api.execute(ruleScript,
                new ExecuteContext(ruleScript, leftResult, rightResult, diffGroup));

        DiffAssert diffAssert = DiffAssert.builder().success(success)
                .reason(tryGetReason(ruleScript, leftResult, rightResult)).build();
        ruleCheckFinish(ruleScript, diffAssert, diffLeftResult, diffRightResult, diffGroup);
        return diffAssert;
    }

    private ExecuteApi getExecuteApi(DiffGroup diffGroup, String ruleScript) {
        Lang lang = getLang(ruleScript);
        if (executeApiMap.containsKey(lang)) {
            return executeApiMap.get(lang);
        }
        // use default
        return executeApiMap.get(Lang.Groovy);
    }

    private Lang getLang(String ruleScript) {
        if (ruleScript.startsWith(pyPrefix)) {
            return Lang.Python;
        }
        return Lang.Groovy;
    }


    private String tryGetReason(String ruleScript, JSONObject leftResult, JSONObject rightResult) {
        try {
            if (leftResult.containsKey("error")) {
                String message = JsonPaser.getNodeValue(leftResult, "$1.error.message");
                return "L " + message;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (rightResult.containsKey("error")) {
                String message = JsonPaser.getNodeValue(rightResult, "$2.error.message");
                return "R " + message;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "L= " + leftResult + "\n R=" + rightResult + "\n 匹配脚本" + ruleScript + "\n";
    }
}
