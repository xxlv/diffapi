package diff.compiler;

import diff.DiffRule;

import java.util.List;

public class RuleCompileEngine implements CompileEngineAPI<List<String>, DiffRule> {

    @Override
    public DiffRule compile(List<String> s) {
        // rule 需要在diffGroup中编译
        DiffRule diffRule = new DiffRule();
        diffRule.setRules(s);
        return diffRule;
    }
}
