package lily.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import lily.exception.LilyException;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link Parser#parseTaskIndex(String)}.
 *
 * <p>This method was chosen because it is a small, pure function: given the same input
 * it always produces the same output, it has no side effects (no I/O, no printing, no
 * shared state), and it has a clearly enumerable set of edge cases around what Java
 * considers a "parsable integer" versus what Lily's command syntax expects a task
 * number to look like. That combination makes it easy to test exhaustively and in
 * isolation, with no mocking or fixtures required.
 */
public class ParserTest {

    @Test
    public void parseTaskIndex_singleDigit_returnsZeroBasedIndex() throws LilyException {
        assertEquals(0, Parser.parseTaskIndex("1"));
    }

    @Test
    public void parseTaskIndex_multiDigit_returnsZeroBasedIndex() throws LilyException {
        assertEquals(4, Parser.parseTaskIndex("5"));
        assertEquals(41, Parser.parseTaskIndex("42"));
    }

    @Test
    public void parseTaskIndex_leadingZeros_parsedAsDecimal() throws LilyException {
        // "007" is a valid decimal integer to Java's parser (not octal), so this must
        // succeed rather than throw.
        assertEquals(6, Parser.parseTaskIndex("007"));
    }

    @Test
    public void parseTaskIndex_explicitPlusSign_parsedSuccessfully() throws LilyException {
        // Integer.parseInt accepts a leading '+' as a valid sign character; this test
        // documents that parseTaskIndex inherits that (slightly surprising) leniency.
        assertEquals(4, Parser.parseTaskIndex("+5"));
    }

    @Test
    public void parseTaskIndex_zero_returnsNegativeOneWithoutThrowing() throws LilyException {
        // parseTaskIndex only validates that the text is a well-formed integer; it does
        // not know about the task list, so it does not reject an out-of-range result
        // such as the one produced by 1-based "0". Range checking is TaskList's job
        // (see TaskList#containsIndex), so this method must return -1 here, not throw.
        assertEquals(-1, Parser.parseTaskIndex("0"));
    }

    @Test
    public void parseTaskIndex_negativeNumber_returnsNegativeIndexWithoutThrowing() throws LilyException {
        // Same reasoning as the zero case: "-5" is a well-formed integer, so no
        // exception is thrown here even though the resulting index is nonsensical as a
        // task position.
        assertEquals(-6, Parser.parseTaskIndex("-5"));
    }

    @Test
    public void parseTaskIndex_nonNumericText_exceptionThrown() {
        LilyException thrown = assertThrows(LilyException.class, () -> Parser.parseTaskIndex("abc"));
        assertEquals("Please provide a valid task number.", thrown.getMessage());
    }

    @Test
    public void parseTaskIndex_emptyString_exceptionThrown() {
        assertThrows(LilyException.class, () -> Parser.parseTaskIndex(""));
    }

    @Test
    public void parseTaskIndex_blankWhitespaceOnly_exceptionThrown() {
        // Integer.parseInt does not trim its input, so a space-only argument is not a
        // parsable integer even though it "looks empty".
        assertThrows(LilyException.class, () -> Parser.parseTaskIndex(" "));
    }

    @Test
    public void parseTaskIndex_surroundingWhitespace_exceptionThrown() {
        // Documents that parseTaskIndex does not trim its argument: " 5 " is rejected
        // even though "5" alone would succeed. Callers are expected to trim first (as
        // Parser.getArguments already does for the text that normally reaches here).
        assertThrows(LilyException.class, () -> Parser.parseTaskIndex(" 5 "));
    }

    @Test
    public void parseTaskIndex_decimalNumber_exceptionThrown() {
        assertThrows(LilyException.class, () -> Parser.parseTaskIndex("1.5"));
    }

    @Test
    public void parseTaskIndex_valueOverflowsInt_exceptionThrown() {
        // Larger than Integer.MAX_VALUE; Integer.parseInt reports this the same way as
        // any other unparsable string (NumberFormatException), which parseTaskIndex
        // converts to the same LilyException as every other invalid case.
        assertThrows(LilyException.class, () -> Parser.parseTaskIndex("99999999999999999999"));
    }

    @Test
    public void parseTaskIndex_nullArgument_exceptionThrown() {
        assertThrows(LilyException.class, () -> Parser.parseTaskIndex(null));
    }
}