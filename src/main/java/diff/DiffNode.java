package diff;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DiffNode {

    private Long id;
    private String name;
    private DiffApi api;
    private String token;
    private List<Payload> payloads;
    private String tag;

    public int countPayloadOrigin() {
        int sum = 0;
        if (payloads == null || payloads.size() <= 0) {
            return sum;
        }
        for (int i = 0; i < payloads.size(); i++) {
            if (payloads.get(i).getOrigin() != null) {
                sum += payloads.get(i).getOrigin().size();
            }
        }
        return sum;
    }
}
