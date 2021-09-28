package diff.compiler;

public interface CompileEngineAPI<R, T> {
    T compile(R r);
}
