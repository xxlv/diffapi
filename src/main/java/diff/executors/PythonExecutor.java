package diff.executors;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Joiner;
import diff.utils.Log;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.python.util.PythonInterpreter;

public class PythonExecutor implements ExecuteApi {

    final String paramPrefix = "$";
    final String globalParams = "_vars";
    final String pythonResultKey = "result";
    private static final Boolean debug = true;
    List<String> preImport = Arrays.asList("import time", "import json", "import sys");

    @Override
    public boolean execute(String script, ExecuteContext context) {
        // fixme dont hardcode
        String prefix = "py:";
        if (script.startsWith(prefix)) {
            script = script.substring(prefix.length());
        }
        script = "\n" + script;
        try {
            PythonInterpreter interpreter = new PythonInterpreter();
            String space = "    ";
            String preImportPackage = Joiner.on("\n").join(preImport);
            String pre = preImportPackage + "\nif _vars:\n" + space
                    + "_vars=json.loads(_vars)\n" + space
                    + "result=False\n";
            if (debug) {
                pre += space
                        + "print('[Python-DEBUG] Current Execute Python script : {}'.format"
                        + "(_vars['$0']))\n";
            }
            script = space + script;
            interpreter.set(globalParams, params(context));
            script = script.replaceAll("\n", "\n" + space);

            interpreter.exec(pre + script);

            return interpreter.get(pythonResultKey, Boolean.class);
        } catch (Exception e) {
            Log.errors("无法执行python " + e.getCause());
        }

        return false;
    }

    private String params(ExecuteContext context) {
        Map<String, Object> params = new ConcurrentHashMap<>();
        params.putIfAbsent(getParam("0"), context.$0());
        params.putIfAbsent(getParam("1"), context.$1());
        params.putIfAbsent(getParam("2"), context.$2());
        params.putIfAbsent(getParam("diffGroup"), context.$diffGroup());
        return asString(params);

    }

    private String getParam(String key) {
        return paramPrefix + key;
    }

    private String asString(Object object) {
        return JSON.toJSONString(object);
    }
}
