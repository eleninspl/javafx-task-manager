package io.github.eleninspl.taskmanager.controller.dialog;

import io.github.eleninspl.taskmanager.ui.Dialogs;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.function.Function;

/**
 * Asks for a single name (a category or a priority level), with the rule check shown inline.
 */
public class NameDialog extends Dialog<String> {

    /**
     * @param heading     dialog heading, e.g. "Rename category"
     * @param fieldLabel  label of the text field (may contain a mnemonic)
     * @param initial     current name, or empty for a new item
     * @param actionLabel label of the confirming button
     * @param validator   returns an error message for an invalid name, or null when valid
     */
    public NameDialog(String heading, String fieldLabel, String initial, String actionLabel,
                      Function<String, String> validator) {
        setTitle(heading);
        setHeaderText(heading);
        ButtonType ok = new ButtonType(actionLabel, ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ok);

        TextField field = new TextField(initial);
        Label error = new Label();
        error.getStyleClass().add("error-text");
        VBox content = new VBox(8, Forms.field(fieldLabel, field), error);
        content.setPrefWidth(360);
        getDialogPane().setContent(content);

        Button okButton = (Button) getDialogPane().lookupButton(ok);
        okButton.getStyleClass().add("primary");
        Runnable validate = () -> {
            String message = validator.apply(field.getText());
            boolean unchanged = field.getText().trim().equals(initial == null ? "" : initial.trim());
            okButton.setDisable(message != null || unchanged);
            // Don't greet the user with an error for an empty field they haven't typed in yet
            error.setText(message != null && !field.getText().isEmpty() ? message : "");
            error.setManaged(!error.getText().isEmpty());
        };
        field.textProperty().addListener((o, a, b) -> validate.run());
        validate.run();

        setResultConverter(button -> button == ok ? field.getText().trim() : null);
        Dialogs.prepare(this);
        setOnShown(e -> Platform.runLater(() -> {
            field.requestFocus();
            field.selectAll();
        }));
    }
}
