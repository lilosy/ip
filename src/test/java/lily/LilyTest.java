package lily;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests Lily's GUI-facing command-processing method. */
class LilyTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void getResponse_todoThenList_includesAddedTask() {
        Lily lily = new Lily(temporaryDirectory.resolve("lily.txt").toString());

        String addResponse = lily.getResponse("todo read GUI tutorial");
        String listResponse = lily.getResponse("list");

        assertTrue(addResponse.contains("I've added this task"));
        assertTrue(listResponse.contains("read GUI tutorial"));
    }
}
