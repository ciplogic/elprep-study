package commons;

import java.io.IOException;

public interface Thunk {
    void apply() throws IOException;
}
