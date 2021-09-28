package diff.compiler;

import diff.Payload;
import java.util.ArrayList;
import java.util.List;

public class PayloadCompileEngine implements CompileEngineAPI<String, Payload> {

    @Override
    public Payload compile(String payload) {
        List<String> origins = new ArrayList<>();
        origins.add(payload);
        return Payload.builder().origin(origins).build();
    }
}
