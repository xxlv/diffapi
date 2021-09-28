package diff.executors;

import static diff.utils.Log.errors;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

public class GroovyExecutor implements ExecuteApi {

    @Override
    public boolean execute(String script, ExecuteContext context) {
        Binding binding = new Binding();
        binding.setVariable("$0", context.$0());
        binding.setVariable("$", context.$1());
        binding.setVariable("$1", context.$1());
        binding.setVariable("$2", context.$2());
        binding.setVariable("$diffGroup", context.$diffGroup());
        boolean success = false;
        GroovyShell shell = new GroovyShell(binding);
        try {
            success = (Boolean) shell.evaluate(script);
        } catch (Exception e) {
            errors("无法执行脚本，检查是否规则有误 rule=" + script + " error=" + e.getCause());
        }
        return success;
    }
}
