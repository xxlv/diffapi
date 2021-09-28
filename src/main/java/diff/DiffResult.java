package diff;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DiffResult {

    private transient int order;

    private JSONObject result;

    private String payload;

}
