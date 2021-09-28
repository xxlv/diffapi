package diff;

import static diff.utils.Log.success;
import static diff.utils.Log.warns;

import diff.utils.FileUtils;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.Data;

@Data
public class ReportManager {


    private static final String writeReportPath =
            System.getProperty("user.dir") + File.separator + "reports";

    private static final String reportTpl = writeReportPath + File.separator + "reportTpl.md";

    private static List<DiffReport> reports = new ArrayList<>();

    public static void addReports(List<DiffReport> r) {
        reports.addAll(r);
    }

    private static String getName() {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String name = sdf.format(new Date());
        name = name + "-diff报告";
        return name;
    }

    public static void report(List<DiffReport> reports, long startTime, DiffGlobalConfig config) {
        success("正在生成报告");
        String tpl = FileUtils.readFile(new File(reportTpl));

        String fileName = writeReportPath + File.separator + getName() + ".md";
        Boolean success = FileUtils.writeTo(compile(tpl, reports, startTime, config), fileName);
        if (!success) {
            warns("无法生成报告 [ " + fileName + " ]");
        }
    }

    private static int apiCount(List<DiffReport> reports) {
        return reports.size() * 2;
    }

    private static int successCount(List<DiffReport> reports) {
        int total = 0;
        for (int i = 0; i < reports.size(); i++) {
            total += reports.get(i).successCount();
        }
        return total;
    }

    private static int failCount(List<DiffReport> reports) {
        int total = 0;
        for (int i = 0; i < reports.size(); i++) {
            total += reports.get(i).failCount();
        }
        return total;
    }

    private static String compile(String tpl, List<DiffReport> reports, long startTime,
            DiffGlobalConfig config) {
        StringBuffer loopResult = new StringBuffer();
        int apiCount = apiCount(reports);
        int totalSuccessCount = successCount(reports);
        int totalFailCount = failCount(reports);
        String successRate =
                ((totalSuccessCount * 1.0 / (totalSuccessCount + totalFailCount) * 1.0) * 100)
                        + "%";

        String reportResult = "";
        if (totalFailCount <= 0 && totalSuccessCount > 0) {
            reportResult = "PASS";
        } else {
            reportResult = "FAIL";
        }

        if (totalFailCount == totalSuccessCount && totalFailCount == 0) {
            reportResult = "NO_CHECK";
        }
        String title = "自动生成报告\n";
        if (reports.size() > 0) {
            reports.forEach(report -> {
                String apiName = report.getGroup().getDescription();
                apiName = apiName.split("\r\n")[0];
                int payloadCount = 0;
                if (report.getGroup().getMode() == DiffSupportMode.SAME) {
                    payloadCount = report.getGroup().getSharedPayloads().size();
                } else {
                    payloadCount = report.getGroup().getLeft().getPayloads().size();
                }
                int ruleCount = 0;
                if (report.getGroup().getDiffRule().getRules() != null) {
                    ruleCount = report.getGroup().getDiffRule().getRules().size();
                }
                int successCount = report.successCount();
                int failCount = report.failCount();
                String author = report.getGroup().getAuthor();
                if (author == null) {
                    author = "";
                }
                String result = "FAIL";
                if (failCount <= 0 && successCount > 0) {
                    result = "PASS";
                }

                if (successCount == 0 && failCount == 0) {
                    result = "NO_CHECK";
                }
                loopResult.append("|");
                apiName = apiName.replace("\n", "\t").replace("\r", "");
                loopResult.append(apiName);
                loopResult.append("|");
                loopResult.append(payloadCount);
                loopResult.append("|");
                loopResult.append(ruleCount);
                loopResult.append("|");
                loopResult.append(successCount);
                loopResult.append("|");
                loopResult.append(failCount);
                loopResult.append("|");
                loopResult.append(result);
                loopResult.append("|");
                loopResult.append(author);
                loopResult.append("|");
                loopResult.append(report.getGroup().getMode());
                loopResult.append("|\n");

            });

        }

        tpl = tpl.replace("$projectName",
                config.getProjectConfig() != null ? config.getProjectConfig().getName() : "");
        tpl = tpl.replace("$title", title);
        tpl = tpl.replace("$loopResult", loopResult);
        tpl = tpl.replace("$apiCount", apiCount + "");
        tpl = tpl.replace("$totalSuccessCount", totalSuccessCount + "");
        tpl = tpl.replace("$successRate", successRate + "");
        tpl = tpl.replace("$resultReport", reportResult);
        tpl = tpl.replace("$cost", (System.currentTimeMillis() - startTime) + " ms");

        return tpl;
    }
}