package lily.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import lily.Lily;
import lily.Lily.Response;

/** Controller for Lily's chat-style main window. */
public class MainWindow {
    @FXML private ScrollPane scrollPane;
    @FXML private VBox dialogContainer;
    @FXML private TextField userInput;
    @FXML private Button sendButton;
    private Lily lily;

    /** Keeps the newest response visible. */
    @FXML
    public void initialize() {
        scrollPane.vvalueProperty().bind(dialogContainer.heightProperty());
    }

    /** Supplies the task manager used to process GUI commands. */
    public void setLily(Lily lily) {
        this.lily = lily;
        dialogContainer.getChildren().add(DialogBox.getLilyDialog(lily.getStartupMessage()));
    }

    /** Adds the user's command and Lily's reply to the conversation. */
    @FXML
    private void handleUserInput() {
        String input = userInput.getText();
        if (input.isBlank()) {
            return;
        }
        Response response = lily.getResponseResult(input);
        dialogContainer.getChildren().addAll(DialogBox.getUserDialog(input),
                DialogBox.getLilyDialog(response.message(), response.isError()));
        userInput.clear();
    }
}
