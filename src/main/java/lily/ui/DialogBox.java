package lily.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/** A labelled chat message aligned for either the user or Lily. */
public class DialogBox extends HBox {
    private DialogBox(String text, Pos alignment, String style) {
        setAlignment(alignment);
        setStyle("-fx-padding: 10; " + style);
        Label message = new Label(text);
        message.setWrapText(true);
        message.setMaxWidth(300);
        message.setStyle("-fx-padding: 8; -fx-background-radius: 8; -fx-background-color: "
                + (alignment == Pos.CENTER_RIGHT ? "#d6ecff" : "#f1e5ff") + ";");
        getChildren().add(message);
    }

    /** Creates a right-aligned user message. */
    public static DialogBox getUserDialog(String text) {
        return new DialogBox(text, Pos.CENTER_RIGHT, "");
    }

    /** Creates a left-aligned Lily reply. */
    public static DialogBox getLilyDialog(String text) {
        return new DialogBox(text, Pos.CENTER_LEFT, "");
    }
}
