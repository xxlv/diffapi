package diff;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DiffAssert {

    private boolean success;

    private String reason;


}
