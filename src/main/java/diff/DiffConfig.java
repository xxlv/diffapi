package diff;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import diff.compiler.CompileEngineAPI;
import diff.inner.SameRuleAPI;
import diffproject.ProjectConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DiffConfig {

    // json config keys
    static final String LEFT = "left";
    static final String RIGHT = "right";
    static final String NODE = "node";
    static final String METHOD = "method";
    static final String METADATA = "metadata";
    static final String TOKEN = "token";
    static final String API_ADDRESS = "apiAddress";
    static final String ID = "id";
    static final String DESC = "desc";
    static final String RULES = "rules";
    static final String AUTHOR = "author";
    static final String SCOPE = "scope";
    static final String GLOBAL = "global";
    static final String NAME = "name";
    static final String API = "api";
    static final String PAYLOAD = "payload";
    static final String SHARD_PAYLOADS = "sharedPayloads";
    static final String MODE = "mode";
    static final String PROJECT = "project";
    static final String CONFIGS = "configs";

    DiffGlobalConfig globalConfig;

    @SuppressWarnings("unchecked")
    public DiffGlobalConfig initGlobalConfig(List<JSONObject> configs) {
        DiffGlobalConfig diffGlobalConfig = new DiffGlobalConfig();
        configs.forEach(config -> {
            if (isGlobal(config)) {
                if (config.containsKey(PROJECT)) {
                    diffGlobalConfig.setProjectConfig(
                            ProjectConfig.getProjectConfig((JSONObject) config.get(PROJECT)));
                }
                JSONObject left = (JSONObject) config.get(LEFT);
                JSONObject right = (JSONObject) config.get(RIGHT);
                if (left != null && left.containsKey(API_ADDRESS)) {
                    diffGlobalConfig.setLeftApiAddress((String) left.get(API_ADDRESS));
                }
                if (right != null && right.containsKey(API_ADDRESS)) {
                    diffGlobalConfig.setRightApiAddress((String) right.get(API_ADDRESS));
                }
                if (left != null && left.containsKey(METHOD)) {
                    diffGlobalConfig.setLeftMethod((String) left.get(METHOD));
                }
                if (right != null && right.containsKey(METHOD)) {
                    diffGlobalConfig.setRightMethod((String) right.get(METHOD));
                }

                if (left != null && left.containsKey(TOKEN)) {
                    diffGlobalConfig.setLeftToken((String) left.get(TOKEN));
                }
                if (right != null && right.containsKey(TOKEN)) {
                    diffGlobalConfig.setRightToken((String) right.get(TOKEN));
                }

                if (config.containsKey(METADATA)) {
                    diffGlobalConfig.setMetadata((Map<String, Object>) config.get(METADATA));
                }
                // 加载指定mode下的shared config

                if (config.containsKey(CONFIGS)) {
                    diffGlobalConfig
                            .setModeConfigs((List<Map<String, Object>>) config.get(CONFIGS));
                }
            }

        });

        globalConfig = diffGlobalConfig;

        return diffGlobalConfig;
    }

    public List<DiffGroup> initGroups(CompileEngineAPI<List<String>, DiffRule> ruleCompileEngine,
            CompileEngineAPI<String, Payload> payloadCompileEngine,
            DiffGlobalConfig diffGlobalConfig, List<JSONObject> configs) {
        List<DiffGroup> diffGroups = new ArrayList<>();

        configs.forEach(config -> {
            DiffSupportMode diffSupportMode = getMode(config);
            switch (diffSupportMode) {
                case DIFF:
                    diffGroups.addAll(getDiffGroupOfSupportDiff(ruleCompileEngine,
                            payloadCompileEngine, diffGlobalConfig, config));
                    break;

                case NODE:
                    diffGroups.addAll(getDiffGroupOfSupportNode(ruleCompileEngine,
                            payloadCompileEngine, diffGlobalConfig, config));
                    break;
                case SAME:
                    diffGroups.addAll(getDiffGroupOfSupportSame(ruleCompileEngine,
                            payloadCompileEngine, diffGlobalConfig, config));
                    break;
            }
        });
        return diffGroups;
    }


    /**
     * 支持Same mode
     * <p>
     * 通过该模式，可以快速指定线上和预发环境的数据 比如一次修改，要验证对其他接口是否影响
     * <p>
     * 比如预发和线上的接口A和A' 要验证某次修改对A和A'是否有影响 可以通过该断言
     *
     * @param ruleCompileEngine
     * @param payloadCompileEngine
     * @param diffGlobalConfig
     * @param config
     * @return
     */
    private List<DiffGroup> getDiffGroupOfSupportSame(
            CompileEngineAPI<List<String>, DiffRule> ruleCompileEngine,
            CompileEngineAPI<String, Payload> payloadCompileEngine,
            DiffGlobalConfig diffGlobalConfig, JSONObject config) {

        List<DiffGroup> diffGroups = new ArrayList<>();
        JSONObject left = (JSONObject) config.get(LEFT);
        JSONObject right = (JSONObject) config.get(RIGHT);
        String desc = (String) config.get(DESC);
        if (!isGlobal(config)) {
            DiffGroup df = new DiffGroup();
            String id = System.currentTimeMillis() + "";
            if (config.containsKey(ID)) {
                id = (String) config.get(ID);
            }
            if (config.containsKey(AUTHOR)) {
                df.setAuthor(config.getString(AUTHOR));
            }
            df.setId(id);
            df.setDescription(desc);
            DiffNode leftNode = makeNode(payloadCompileEngine, diffGlobalConfig, left, LEFT);
            if (right != null) {
                DiffNode rightNode = makeNode(payloadCompileEngine, diffGlobalConfig, right, RIGHT);
                df.setRight(rightNode);
            }
            // merge payload
            df.setLeft(leftNode);
            DiffRule diffRule = makeRule(ruleCompileEngine, id, (JSONArray) config.get(RULES));
            diffRule.attachRule(new SameRuleAPI());

            df.setDiffRule(diffRule);
            JSONArray payloads = null;
            if (config.containsKey(SHARD_PAYLOADS)) {
                payloads = (JSONArray) config.get(SHARD_PAYLOADS);
            }
            // try install payloads
            df.setSharedPayloads(loadPayloads(payloads, id, payloadCompileEngine));
            df.setMode(DiffSupportMode.SAME);
            df.setGlobalConfig(diffGlobalConfig);
            diffGroups.add(df);
        }
        return diffGroups;
    }


    private List<DiffGroup> getDiffGroupOfSupportNode(
            CompileEngineAPI<List<String>, DiffRule> ruleCompileEngine,
            CompileEngineAPI<String, Payload> payloadCompileEngine,
            DiffGlobalConfig diffGlobalConfig, JSONObject config) {

        List<DiffGroup> diffGroups = new ArrayList<>();
        JSONObject node = (JSONObject) config.get(NODE);
        if (node == null) {
            node = (JSONObject) config.get(LEFT);
        }
        String desc = (String) config.get(DESC);
        if (!isGlobal(config)) {
            DiffGroup df = new DiffGroup();
            String id = System.currentTimeMillis() + "";
            if (config.containsKey(ID)) {
                id = (String) config.get(ID);
            }
            if (config.containsKey(AUTHOR)) {
                df.setAuthor(config.getString(AUTHOR));
            }
            df.setId(id);
            df.setDescription(desc);
            df.setMode(DiffSupportMode.NODE);
            df.setLeft(makeNode(payloadCompileEngine, diffGlobalConfig, node, LEFT));

            JSONArray rules = (JSONArray) config.get(RULES);
            JSONArray mergedRules = new JSONArray();
            if (rules != null) {
                mergedRules.addAll(rules);
            }
            List<Map<String, Object>> modeConfigs = diffGlobalConfig.getModeConfigs();
            modeConfigs.forEach(v -> {
                if (v.containsKey("mode") && DiffSupportMode.NODE.name()
                        .equalsIgnoreCase((String) v.get("mode"))) {
                    // 获取node 的share 配置
                    mergedRules.addAll((JSONArray) v.get("rules"));
                }
            });
            df.setDiffRule(
                    makeRule(ruleCompileEngine, id, mergedRules));
            df.setGlobalConfig(diffGlobalConfig);

            diffGroups.add(df);
        }
        return diffGroups;
    }

    private List<DiffGroup> getDiffGroupOfSupportDiff(
            CompileEngineAPI<List<String>, DiffRule> ruleCompileEngine,
            CompileEngineAPI<String, Payload> payloadCompileEngine,
            DiffGlobalConfig diffGlobalConfig, JSONObject config) {
        List<DiffGroup> diffGroups = new ArrayList<>();
        JSONObject left = (JSONObject) config.get(LEFT);
        JSONObject right = (JSONObject) config.get(RIGHT);
        String desc = (String) config.get(DESC);
        if (!isGlobal(config)) {
            DiffGroup df = new DiffGroup();
            String id = System.currentTimeMillis() + "";
            if (config.containsKey(ID)) {
                id = (String) config.get(ID);
            }
            if (config.containsKey(AUTHOR)) {
                df.setAuthor(config.getString(AUTHOR));
            }
            df.setId(id);
            df.setDescription(desc);
            df.setLeft(makeNode(payloadCompileEngine, diffGlobalConfig, left, LEFT));
            df.setRight(makeNode(payloadCompileEngine, diffGlobalConfig, right, RIGHT));
            df.setDiffRule(
                    makeRule(ruleCompileEngine, id, (JSONArray) config.get(RULES)));
            df.setMode(DiffSupportMode.DIFF);
            df.setGlobalConfig(diffGlobalConfig);
            diffGroups.add(df);

        }
        return diffGroups;
    }

    private DiffSupportMode getMode(JSONObject config) {
        if (config == null) {
            return DiffSupportMode.UNK;
        }
        if (config.containsKey(MODE)) {
            try {
                return DiffSupportMode.valueOf(config.getString(MODE));
            } catch (Exception e) {
                return DiffSupportMode.UNK;
            }
        }
        if (config.containsKey(LEFT) && config.containsKey(RIGHT)) {
            return DiffSupportMode.DIFF;
        }
        if (config.containsKey(NODE)) {
            return DiffSupportMode.NODE;
        }
        return DiffSupportMode.UNK;
    }

    private boolean isGlobal(JSONObject config) {
        return config.containsKey(SCOPE) && ((String) config.get(SCOPE))
                .equalsIgnoreCase(GLOBAL);
    }

    private DiffNode makeNode(CompileEngineAPI<String, Payload> payloadCompileEngine,
            DiffGlobalConfig diffGlobalConfig, JSONObject object, String d) {
        DiffNode node = new DiffNode();
        String api = (String) object.get(API);
        JSONArray payloads = (JSONArray) object.get(PAYLOAD);
        node.setId(System.currentTimeMillis());
        node.setApi(makeApi(diffGlobalConfig, object, d));

        if (object.containsKey(NAME)) {
            node.setName((String) object.get(NAME));
        } else {
            node.setName("AUTO-" + api);
        }
        if (object.containsKey(TOKEN)) {
            node.setToken((String) object.get(TOKEN));
        } else {
            if (d.equalsIgnoreCase(LEFT)) {
                node.setToken(diffGlobalConfig.getLeftToken());
            } else {
                node.setToken(diffGlobalConfig.getRightToken());
            }
        }
        List<Payload> payloadFromPlaintext;
        String identity = null;
        if (object.containsKey(ID)) {
            identity = object.getString(ID);
        }
        payloadFromPlaintext = loadPayloads(payloads, identity,
                payloadCompileEngine);
        node.setPayloads(payloadFromPlaintext);

        return node;
    }

    private List<Payload> loadPayloads(JSONArray payloads, String identity,
            CompileEngineAPI<String, Payload> payloadCompileEngine) {
        List<Payload> payloadFromPlaintext = new ArrayList<>();
        if (payloads != null) {
            payloadFromPlaintext = payloads.stream()
                    .map((e) -> payloadCompileEngine.compile((String) e))
                    .collect(Collectors.toList());
        }

        if (identity != null) { List<Payload> payloadsFromSPI = loadExternalPayload(identity);
            payloadFromPlaintext.addAll(payloadsFromSPI);
        }
        return payloadFromPlaintext;
    }


    private DiffApi makeApi(DiffGlobalConfig diffGlobalConfig, JSONObject object, String d) {
        DiffApi api = new DiffApi();

        if (object.containsKey(API_ADDRESS)) {
            api.setHost((String) object.get(API_ADDRESS));
        } else {
            if (d.equalsIgnoreCase(LEFT)) {
                api.setHost(diffGlobalConfig.getLeftApiAddress());
            } else {
                api.setHost(diffGlobalConfig.getRightApiAddress());
            }
        }
        if (object.containsKey(METHOD)) {
            api.setMethodVerb((String) object.get(METHOD));
        } else {
            if (d.equalsIgnoreCase(LEFT)) {
                api.setMethodVerb(diffGlobalConfig.getLeftMethod());
            } else {
                api.setMethodVerb(diffGlobalConfig.getRightMethod());
            }
        }
        api.setApi(object.getString(API));
        return api;
    }


    private DiffRule makeRule(CompileEngineAPI<List<String>, DiffRule> ruleCompileEngine, String id,
            JSONArray object) {
        DiffRule rule = new DiffRule();
        if (object != null) {
            List<String> rules = object.stream().map((e) -> (String) e)
                    .collect(Collectors.toList());
            rule = ruleCompileEngine.compile(rules);
            tryInstallExternalRule(id, rule);
        }
        return rule;
    }

    private List<Payload> loadExternalPayload(String id) {
        List<PayloadSupplier> spis = PayloadSpiLoader.load(id);
        List<Payload> payloads = new ArrayList<>();
        assert globalConfig != null;
        spis.forEach(spi -> payloads.add(spi.get(globalConfig.getMetadata())));
        return payloads;
    }

    private void tryInstallExternalRule(String id, DiffRule rule) {
        List<RuleApi> ruleApis = RuleSpiLoader.load(id);
        rule.setExternalAPIs(ruleApis);
    }

}
