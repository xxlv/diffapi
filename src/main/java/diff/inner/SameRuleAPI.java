package diff.inner;

import com.alibaba.fastjson.JSON;
import diff.DiffAssert;
import diff.DiffResult;
import diff.DiffSupportMode;
import diff.RuleApi;
import diff.utils.MD5Utils;
import java.util.Collections;
import java.util.List;

/**
 * 简单断言 判断两个对象是否相等
 */
public class SameRuleAPI implements RuleApi {


    @Override
    public DiffAssert check(DiffResult left, DiffResult right) {
        String leftHash = MD5Utils.stringToMD5(JSON.toJSONString(left));
        String rightHash = MD5Utils.stringToMD5(JSON.toJSONString(right));
        String reason = "";
        boolean success = false;
        if (leftHash.equalsIgnoreCase(rightHash)) {
            success = true;
        } else {
            reason = "SameRuleAPI 断言失败 L=" + leftHash + " R=" + rightHash;
        }
        return DiffAssert.builder().reason(reason).success(success).build();
    }

    @Override
    public List<DiffSupportMode> support() {
        return Collections.singletonList(DiffSupportMode.SAME);
    }
}
