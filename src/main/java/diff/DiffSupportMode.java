package diff;

public enum DiffSupportMode {
    SAME,   // 完全相同的参数断言 如left 和 right 就是环境不同
    DIFF,   // 节点对比断言
    NODE,   // 节点数据断言
    UNK     // 未知
}
