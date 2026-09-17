package lily.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/** A labelled chat message aligned for either the user or Lily. */
public class DialogBox extends HBox {
    private DialogBox(String text, Pos alignment, boolean isLily) {
        setAlignment(alignment);
        getStyleClass().add(isLily ? "lily-dialog" : "user-dialog");

        if (isLily) {
            Label avatar = new Label("✿");
            avatar.getStyleClass().add("lily-avatar");
            getChildren().add(avatar);
        }

        Label message = new Label(text);
        message.setWrapText(true);
        message.setMaxWidth(300);
        message.getStyleClass().add(isLily ? "lily-message" : "user-message");
        getChildren().add(message);
    }

    /** Creates a right-aligned user message. */
    public static DialogBox getUserDialog(String text) {
        return new DialogBox(text, Pos.CENTER_RIGHT, false);
    }

    /** Creates a left-aligned Lily reply. */
    public static DialogBox getLilyDialog(String text) {
        return getLilyDialog(text, false);
    }

    /** Creates a left-aligned Lily reply, using the error style when appropriate. */
    public static DialogBox getLilyDialog(String text, boolean isError) {
        DialogBox dialog = new DialogBox(text, Pos.CENTER_LEFT, true);
        if (isError) {
            dialog.getChildren().getLast().getStyleClass().add("error-message");
        }
        return dialog;
    }
}
