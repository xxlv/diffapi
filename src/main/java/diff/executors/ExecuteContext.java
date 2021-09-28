package diff.executors;

import com.alibaba.fastjson.JSONObject;
import diff.DiffGroup;

public class ExecuteContext {

    private String $0;
    private JSONObject $1;
    private JSONObject $2;
    private DiffGroup $diffGroup;

    public ExecuteContext(String $0, JSONObject $1, JSONObject $2, DiffGroup $diffGroup) {
        this.$0 = $0;
        this.$1 = $1;
        this.$2 = $2;
        this.$diffGroup = $diffGroup;
    }

    public String $0() {
        return $0;
    }

    public void set$0(String $0) {
        this.$0 = $0;
    }

    public JSONObject $1() {
        return $1;
    }

    public void set$1(JSONObject $1) {
        this.$1 = $1;
    }

    public JSONObject $2() {
        return $2;
    }

    public void set$2(JSONObject $2) {
        this.$2 = $2;
    }

    public DiffGroup $diffGroup() {
        return $diffGroup;
    }

    public void set$diffGroup(DiffGroup $diffGroup) {
        this.$diffGroup = $diffGroup;
    }
}
