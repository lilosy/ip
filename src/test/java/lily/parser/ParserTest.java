package lily.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import lily.exception.LilyException;
import org.junit.jupiter.api.Test;

/**
 * Tests Lily's central command tokenizer and task-index validation.
 *
 * <p>These parser methods are pure functions: given the same input they always produce
 * the same output, have no side effects, and have clearly enumerable command-format
 * edge cases. That makes them valuable to test directly without mocks or fixtures.
 */
public class ParserTest {

    @Test
    public void parseCommand_mixedCaseCommand_normalizesCommandAndPreservesArguments() throws LilyException {
        Parser.ParsedCommand command = Parser.parseCommand("ToDo Read Java Book");

        assertEquals("todo", command.commandWord());
        assertEquals("Read Java Book", command.arguments());
    }

    @Test
    public void parseCommand_commandWithoutArguments_returnsEmptyArgument() throws LilyException {
        Parser.ParsedCommand command = Parser.parseCommand("LIST");

        assertEquals("list", command.commandWord());
        assertEquals("", command.arguments());
    }

    @Test
    public void parseCommand_leadingWhitespace_ignored() throws LilyException {
        Parser.ParsedCommand command = Parser.parseCommand("  list");

        assertEquals("list", command.commandWord());
        assertEquals("", command.arguments());
    }

    @Test
    public void parseCommand_trailingWhitespace_ignored() throws LilyException {
        Parser.ParsedCommand command = Parser.parseCommand("todo read book  ");

        assertEquals("todo", command.commandWord());
        assertEquals("read book", command.arguments());
    }

    @Test
    public void parseCommand_repeatedSpaces_exceptionThrown() {
        assertSpacingError("todo  read book");
    }

    @Test
    public void parseCommand_tabInsteadOfSpace_exceptionThrown() {
        assertSpacingError("todo\tread book");
    }

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
        assertEquals(6, Parser.parseTaskIndex("007"));
    }

    @Test
    public void parseTaskIndex_explicitPlusSign_exceptionThrown() {
        assertInvalidTaskNumber("+5");
    }

    @Test
    public void parseTaskIndex_zero_exceptionThrown() {
        assertInvalidTaskNumber("0");
    }

    @Test
    public void parseTaskIndex_negativeNumber_exceptionThrown() {
        assertInvalidTaskNumber("-5");
    }

    @Test
    public void parseTaskIndex_nonNumericText_exceptionThrown() {
        assertInvalidTaskNumber("abc");
    }

    @Test
    public void parseTaskIndex_emptyString_exceptionThrown() {
        assertInvalidTaskNumber("");
    }

    @Test
    public void parseTaskIndex_blankWhitespaceOnly_exceptionThrown() {
        assertInvalidTaskNumber(" ");
    }

    @Test
    public void parseTaskIndex_surroundingWhitespace_exceptionThrown() {
        assertInvalidTaskNumber(" 5 ");
    }

    @Test
    public void parseTaskIndex_decimalNumber_exceptionThrown() {
        assertInvalidTaskNumber("1.5");
    }

    @Test
    public void parseTaskIndex_valueOverflowsInt_exceptionThrown() {
        LilyException thrown = assertThrows(LilyException.class,
                () -> Parser.parseTaskIndex("99999999999999999999"));
        assertEquals("That task number is too large.", thrown.getMessage());
    }

    @Test
    public void parseTaskIndex_nullArgument_exceptionThrown() {
        assertInvalidTaskNumber(null);
    }

    private static void assertSpacingError(String input) {
        LilyException thrown = assertThrows(LilyException.class, () -> Parser.parseCommand(input));
        assertEquals("Use single spaces between words.", thrown.getMessage());
    }

    private static void assertInvalidTaskNumber(String input) {
        LilyException thrown = assertThrows(LilyException.class, () -> Parser.parseTaskIndex(input));
        assertEquals("Please provide one positive whole-number task number.", thrown.getMessage());
    }
}
