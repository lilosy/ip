package lily;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
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

        assertTrue(addResponse.contains("Planted it on your list"));
        assertTrue(listResponse.contains("read GUI tutorial"));
    }

    @Test
    void getResponse_schedule_ordersAndGroupsDatedTasksWithOriginalNumbers() {
        Lily lily = new Lily(temporaryDirectory.resolve("lily.txt").toString());
        lily.getResponse("todo undated task");
        lily.getResponse("event morning meeting /from 2026-09-16 0900 /to 2026-09-16 1000");
        lily.getResponse("deadline submit report /by 2026-09-16");
        lily.getResponse("deadline call client /by 2026-09-16 1400");
        lily.getResponse("mark 4");

        String response = lily.getResponse("schedule 2026-09-16");

        assertEquals("Here is your schedule for Sep 16 2026:\n"
                + "Not completed:\n"
                + "3. [D][ ] submit report (by: Sep 16 2026)\n"
                + "2. [E][ ] morning meeting (from: Sep 16 2026, 9:00AM to: Sep 16 2026, 10:00AM)\n"
                + "Completed:\n"
                + "4. [D][X] call client (by: Sep 16 2026, 2:00PM)", response);
    }

    @Test
    void getResponse_scheduleMultiDayEvent_includesBothEndpointDates() {
        Lily lily = new Lily(temporaryDirectory.resolve("lily.txt").toString());
        lily.getResponse("todo keeps event task number at two");
        lily.getResponse("event conference /from 2026-09-16 /to 2026-09-18");

        assertTrue(lily.getResponse("schedule 2026-09-16").contains("2. [E][ ] conference"));
        assertTrue(lily.getResponse("schedule 2026-09-17").contains("2. [E][ ] conference"));
        assertTrue(lily.getResponse("schedule 2026-09-18").contains("2. [E][ ] conference"));
    }

    @Test
    void getResponse_scheduleEntriesAtSameTime_usesOriginalTaskNumberAsTieBreaker() {
        Lily lily = new Lily(temporaryDirectory.resolve("lily.txt").toString());
        lily.getResponse("deadline first /by 2026-09-16 0900");
        lily.getResponse("deadline second /by 2026-09-16 0900");

        String response = lily.getResponse("schedule 2026-09-16");

        assertTrue(response.indexOf("1. [D][ ] first") < response.indexOf("2. [D][ ] second"));
    }

    @Test
    void getResponse_scheduleWithNoMatches_returnsDatedEmptyMessage() {
        Lily lily = new Lily(temporaryDirectory.resolve("lily.txt").toString());

        assertEquals("There are no scheduled tasks for Sep 16 2026.",
                lily.getResponse("schedule 2026-09-16"));
    }

    @Test
    void getResponse_invalidScheduleDate_returnsDateError() {
        Lily lily = new Lily(temporaryDirectory.resolve("lily.txt").toString());

        assertEquals("I couldn't understand the date '2026-02-30'. "
                        + "Try formats like: 2019-10-15 or 2/12/2019.",
                lily.getResponse("schedule 2026-02-30"));
    }

    @Test
    void getResponse_schedule_doesNotRewriteSaveFile() throws IOException {
        Path saveFile = temporaryDirectory.resolve("lily.txt");
        Lily lily = new Lily(saveFile.toString());
        lily.getResponse("deadline submit report /by 2026-09-16");
        String savedBeforeSchedule = Files.readString(saveFile);

        lily.getResponse("schedule 2026-09-16");

        assertEquals(savedBeforeSchedule, Files.readString(saveFile));
    }
}
