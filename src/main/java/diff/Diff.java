package diff;

import static diff.utils.Log.errors;
import static diff.utils.Log.warns;

import com.alibaba.fastjson.JSONObject;
import diff.cache.CacheLoader;
import diff.compiler.CompileEngineAPI;
import diff.compiler.PayloadCompileEngine;
import diff.compiler.RuleCompileEngine;
import diff.utils.FileUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Diff {

    private String mode;

    private static final DiffConfig diffConfig = new DiffConfig();

    private final String checkPointPath =
            System.getProperty("user.dir") + File.separator + "checkpoints";

    private static final CompileEngineAPI<List<String>, DiffRule> ruleCompileEngine =
            new RuleCompileEngine();
    private static final CompileEngineAPI<String, Payload> payloadCompileEngine =
            new PayloadCompileEngine();
    private DiffGlobalConfig diffGlobalConfig = new DiffGlobalConfig();
    private List<DiffGroup> diffGroups = new ArrayList<>();
    private final List<DiffReport> diffReports = new ArrayList<>();

    public void init() {
        init0(null);
    }

    public void setMode(String md) {
        mode = md;
    }

    private void init0(String id) {
        List<JSONObject> jsonConfig = FileUtils.loadJson(checkPointPath);
        RuleSpiLoader.register();
        CacheLoader.preloadCache(id);
        installGlobalConfig(jsonConfig);
        installGroups(jsonConfig);
        check();
    }

    private void check() {
        Objects.requireNonNull(diffGroups);
        diffGroups.forEach(diffGroup -> {
            if (diffGroup.getLeft() != null && diffGroup.getRight() != null) {
                int l = diffGroup.getLeft().countPayloadOrigin();
                int r = diffGroup.getRight().countPayloadOrigin();
                if (l != r) {
                    warns("L=" + l + "  != R =" + r + " " + diffGroup.getDescription());
                }
            }
        });
    }

    public void start() {
        init();
        long startTime = System.currentTimeMillis();
        diffReports.addAll(invokeAll());
        printReports(diffReports);
        //eve
        ReportManager.report(diffReports, startTime, diffGlobalConfig);
    }

    public void exit() {
        try {
            CacheLoader.flushData();
        } catch (Exception e) {
            errors("刷新数据失败" + e.getMessage());
        }
    }

    public void start(String id) {
        init();
        long startTime = System.currentTimeMillis();
        diffReports.addAll(invokeById(id));
        printReports(diffReports);

        ReportManager.report(diffReports, startTime, diffGlobalConfig);
    }


    private List<DiffReport> invokeById(String id) {
        if (mode != null) {
            return diffGroups.stream()
                    .filter((e) -> e.getId().equals(id))
                    .filter(e -> mode.equalsIgnoreCase(e.getMode().name()))
                    .map(DiffGroup::apply)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return diffGroups.stream().filter((e) -> e.getId().equals(id)).map(DiffGroup::apply)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<DiffReport> invokeAll() {
        if (mode != null) {
            return diffGroups.stream().
                    filter(e -> mode.equalsIgnoreCase(e.getMode().name()))
                    .map(DiffGroup::apply)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return diffGroups.stream().map(DiffGroup::apply).filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    private void installGlobalConfig(List<JSONObject> configs) {
        diffGlobalConfig = diffConfig.initGlobalConfig(configs);
    }

    private void installGroups(List<JSONObject> configs) {
        diffGroups = diffConfig
                .initGroups(ruleCompileEngine, payloadCompileEngine, diffGlobalConfig, configs);
    }

    private void printReports(List<DiffReport> diffReports) {
        System.out.println("\n**********************\n *最终报告如下*\n**********************\n");
        for (DiffReport dr : diffReports) {
            System.out.println(dr);
        }
        System.out.println("\n*******************************");
    }

}
