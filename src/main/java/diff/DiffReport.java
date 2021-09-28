package diff;

import static java.lang.Integer.sum;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Data;

@Data
public class DiffReport {

    private DiffGroup group;
    private List<Map<String, DiffAssert>> checkResults;

    @Override
    public String toString() {
        StringBuilder r = new StringBuilder();
        if (group.getMode() == DiffSupportMode.DIFF) {
            r.append("【接口对比断言】");
        }
        if (group.getMode() == DiffSupportMode.NODE) {
            r.append("【接口节点断言】");
        }
        r.append("报告 【").append(group.getDescription()).append("】 ");
        if (failCount() <= 0 && successCount() > 0) {
            r.append(" **SUCCESS** ");
        }
        if (checkResults != null && checkResults.size() > 0) {
            r.append("结果[").append(sum(successCount(), failCount())).append("] 条");
            r.append("成功[").append(successCount()).append("] 条 ");
            r.append("失败[").append(failCount()).append("] 条 \n");
            for (int i = 0; i < checkResults.size(); i++) {
                Map<String, DiffAssert> re = checkResults.get(i);
                r.append("*** Payload  第").append(i + 1).append("轮 \n");
                re.forEach((k, v) -> {
                    if (v == null) {
                        v = DiffAssert.builder().success(false).reason("断言为空").build();
                    }
                    if (!v.isSuccess()) {
                        r.append("**** 规则 [").append(k).append("]").append(" 结果 ")
                                .append((v.isSuccess()) ? "成功" : "失败").append(" 原因 ")
                                .append(v.getReason()).append("\n");
                    }
                });
            }

            return r.toString();
        }

        r.append("MISS [无法找到有效的报告]");
        return r.toString();
    }

    public int successCount() {
        if (count().size() <= 0) {
            return 0;
        }
        return count().get(0);
    }

    public int failCount() {
        if (count().size() <= 1) {
            return 0;
        }
        return count().get(1);
    }

    private List<Integer> count() {
        AtomicInteger successCnt = new AtomicInteger();
        AtomicInteger failCnt = new AtomicInteger();
        for (Map<String, DiffAssert> re : checkResults) {
            if (re == null) {
                continue;
            }
            re.forEach((k, v) -> {
                if (v != null) {
                    if (v.isSuccess()) {
                        successCnt.incrementAndGet();
                    } else {
                        failCnt.incrementAndGet();
                    }
                }
            });
        }
        return Arrays.asList(successCnt.get(), failCnt.get());
    }
}
