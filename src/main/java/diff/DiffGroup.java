package diff;

import static diff.utils.Log.errors;
import static diff.utils.Log.infos;
import static diff.utils.Log.warns;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.Data;
import lombok.SneakyThrows;


@Data
public class DiffGroup {

    private static final ExecutorService executors = Executors
            .newFixedThreadPool(Runtime.getRuntime()
                            .availableProcessors(),
                    new ThreadFactoryBuilder().setNameFormat("diffgroup-rule-%d").build());

    private String author;

    private String description;

    private String id;

    private DiffNode left;

    private DiffNode right;

    private DiffRule diffRule;

    private DiffConfig diffConfig;

    private DiffGlobalConfig globalConfig;

    private DiffSupportMode mode;

    private List<Payload> sharedPayloads;

    @SneakyThrows
    public DiffReport apply() {
        if (mode == DiffSupportMode.DIFF) {
            return applyForDiff();
        }
        if (mode == DiffSupportMode.NODE) {
            return applyForNode();
        }
        if (mode == DiffSupportMode.SAME) {
            return applyForSameV2();
        }
        return null;
    }

    // 针对left 或者right 进行不同环境的断言
    public DiffReport applyForSameV2() {
        Objects.requireNonNull(globalConfig, "please use setGlobalConfig");

        diffRule.setDescription(description);
        if (!check(left) && !check(right)) {
            warns("无法构造DiffNode，可能是json配置未正确，不参与断言检查 [" + description + "]");
            return null;
        }
        DiffReport diffReport = new DiffReport();
        List<Map<String, DiffAssert>> result = new ArrayList<>();

        if (check(left)) {
            List<Payload> sharedPayload = getSharedPayloads();
            if (sharedPayload == null) {
                sharedPayload = new ArrayList<>();
            }
            sharedPayload.addAll(left.getPayloads());
            int size = sharedPayload.size();
            for (int i = 0; i < size; i++) {
                Payload pd = sharedPayload.get(i);
                result.addAll(verifyByRule(left, wrapNode(globalConfig.getRightApiAddress(), left),
                        pd,
                        pd));
            }
        }

        if (check(right)) {
            List<Payload> sharedPayload = getSharedPayloads();
            if (sharedPayload == null) {
                sharedPayload = new ArrayList<>();
            }
            sharedPayload.addAll(right.getPayloads());
            int size = sharedPayload.size();
            for (int i = 0; i < size; i++) {
                Payload pd = sharedPayload.get(i);
                result.addAll(verifyByRule(wrapNode(globalConfig.getLeftApiAddress(), right), right,
                        pd,
                        pd));
            }
        }

        diffReport.setGroup(this);
        diffReport.setCheckResults(result);
        finish();
        return diffReport;
    }

    private DiffNode wrapNode(String host, DiffNode node) {
        if (node != null) {

            synchronized (node) {
                try {
                    DiffNode newNode = new DiffNode();
                    DiffApi diffApi = new DiffApi();
                    DiffApi originApi = node.getApi();
                    diffApi.setHost(host);
                    diffApi.setApi(originApi.getApi());
                    diffApi.setExecutors(originApi.getExecutors());
                    diffApi.setMethodVerb(originApi.getMethodVerb());

                    newNode.setName(node.getName());
                    newNode.setPayloads(node.getPayloads());
                    newNode.setId(node.getId());
                    newNode.setToken(node.getToken());
                    newNode.setApi(diffApi);
                    newNode.setTag("Wrap");

                    return newNode;
                } catch (Exception e) {
                    errors("无法构建same节点" + e.getMessage());
                }
            }
        }

        return null;
    }

    public DiffReport applyForSame() {
        diffRule.setDescription(description);
        if (!check(left) || !check(right)) {
            warns("无法构造DiffNode，可能是json配置未正确，不参与断言检查 [" + description + "]");
            return null;
        }

        DiffReport diffReport = new DiffReport();

        List<Payload> sharedPayload = getSharedPayloads();
        int size = sharedPayload.size();
        List<Map<String, DiffAssert>> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Payload leftPayload = sharedPayload.get(i);
            Payload rightPayload = sharedPayload.get(i);
            result.addAll(verifyByRule(left, right, leftPayload,
                    rightPayload));
        }
        diffReport.setGroup(this);
        diffReport.setCheckResults(result);
        finish();
        return diffReport;
    }


    public DiffReport applyForDiff() {
        diffRule.setDescription(description);

        if (!check(left) || !check(right)) {
            warns("无法构造DiffNode，可能是json配置未正确，不参与断言检查 [" + description + "]");
            return null;
        }

        DiffReport diffReport = new DiffReport();
        int leftPayloadSize = left.countPayloadOrigin();
        int rightPayloadSize = right.countPayloadOrigin();

        // 仅仅在对比的模式上需要检查
        if (leftPayloadSize != rightPayloadSize || left.getPayloads().size() != right
                .getPayloads()
                .size()) {
            errors(
                    "[" + description + "] 提供的json不满足格式化要求 参数数量不匹配，左侧 " + leftPayloadSize + "("
                            + left.getPayloads().size() + ")" + " 右侧 "
                            + rightPayloadSize + "(" + right.getPayloads().size() + ")"
                            + " left=["
                            + left.getName() + "]  right= [" + right
                            .getName() + "]");

            return null;

        }

        int size = left.getPayloads().size();
        // 将数据累计起来
        List<Map<String, DiffAssert>> result = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            Payload leftPayload = left.getPayloads().get(i);
            Payload rightPayload = right.getPayloads().get(i);
            result.addAll(verifyByRule(left, right, leftPayload,
                    rightPayload));
        }

        diffReport.setGroup(this);
        diffReport.setCheckResults(result);

        finish();
        return diffReport;
    }


    public DiffReport applyForNode() {
        diffRule.setDescription(description);
        if (!check(left)) {
            warns("无法构造DiffNode，可能是json配置未正确，不参与断言检查 [" + description + "]");
            return null;
        }
        right = left; // simple use trick

        DiffReport diffReport = new DiffReport();
        int leftPayloadSize = left.countPayloadOrigin();

        int size = left.getPayloads().size();
        // 将数据累计起来
        List<Map<String, DiffAssert>> result = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            Payload leftPayload = left.getPayloads().get(i);
            Payload rightPayload = right.getPayloads().get(i);
            result.addAll(verifyByRule(left, right, leftPayload,
                    rightPayload));
        }

        diffReport.setGroup(this);
        diffReport.setCheckResults(result);

        finish();
        return diffReport;
    }


    private List<Map<String, DiffAssert>> verifyByRule(DiffNode left, DiffNode right,
            Payload leftPayload, Payload rightPayload) {
        List<Map<String, DiffAssert>> result = new ArrayList<>();
        try {
            Map<String, DiffResult> rightResultMap;
            Map<String, DiffResult> leftResultMap = left.getApi()
                    .request("left", leftPayload, left.getToken());
            // left ==right means in node mode
            if (left == right) {
                rightResultMap = leftResultMap;
            } else {
                if (right != null) {
                    rightResultMap = right.getApi()
                            .request("right", rightPayload, right.getToken());
                } else {
                    rightResultMap = new ConcurrentHashMap<>();
                }

            }
            if (leftResultMap.size() > 0 || rightResultMap.size() > 0) {
                infos("verifyByRule current size" + leftResultMap.size());
                final CountDownLatch downLatch = new CountDownLatch(leftResultMap.size());

                leftResultMap.forEach((k, v) -> {
                    DiffGroup self = this;
                    executors.submit(() -> {

                        result.add(diffRule.verify(self, v, rightResultMap.get(k)));

                        downLatch.countDown();
                    });
                });
                downLatch.await();
                if (leftResultMap.size() != rightResultMap.size()) {
                    errors(description + " 查询的数据 left 和 right 不相等");
                    Map<String, DiffAssert> defaultSizeErrorMap = new HashMap<>();
                    defaultSizeErrorMap.put("_return_size_not_equal",
                            DiffAssert.builder().success(false)
                                    .reason("[" + description + "] leftResultSize= "
                                            + leftResultMap.size() + " rightResultSize= "
                                            + rightResultMap.size()).build());
                    result.add(defaultSizeErrorMap);
                }

            }
        } catch (Exception e) {
            errors("执行异常" + e.getMessage());
        }
        return result;
    }

    private boolean check(DiffNode node) {
        if (node == null) {
            return false;
        }
        if (Objects.isNull(node.getApi()) || node.getApi().getApi().length() <= 0) {
            return false;
        }
        return true;
    }

    private void finish() {
    }


}
