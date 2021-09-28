package diff;


import java.util.Arrays;
import java.util.List;

public interface RuleApi {

    DiffAssert check(DiffResult left, DiffResult right);

    default String id() {
        return "";
    }

    default String desc() {
        return id();
    }

    default List<DiffSupportMode> support() {
        return Arrays.asList(DiffSupportMode.DIFF, DiffSupportMode.NODE);
    }


}
