package io.github.eleninspl.taskmanager.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Window;

import java.util.Optional;

/**
 * Consistently styled, window-owned dialogs.
 */
public final class Dialogs {

    private static Window owner;

    private Dialogs() {}

    /**
     * Sets the main window, which owns every dialog so they open centred over it.
     *
     * @param window the main application window
     */
    public static void setOwner(Window window) {
        owner = window;
    }

    /**
     * Prepares a dialog: owner, theme, and no OS icon in the header.
     *
     * @param dialog the dialog to prepare
     * @param <T>    the dialog result type
     * @return the same dialog
     */
    public static <T> Dialog<T> prepare(Dialog<T> dialog) {
        if (owner != null && dialog.getOwner() == null) {
            dialog.initOwner(owner);
        }
        dialog.setGraphic(null);
        Theme.apply(dialog.getDialogPane());
        return dialog;
    }

    /**
     * Asks the user to confirm a destructive action.
     *
     * @param title        short question, e.g. "Delete this task?"
     * @param message      what will happen, in plain words
     * @param actionLabel  label of the confirming button, e.g. "Delete task"
     * @return true if the user confirmed
     */
    public static boolean confirm(String title, String message, String actionLabel) {
        ButtonType action = new ButtonType(actionLabel, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.NONE, message, cancel, action);
        alert.setTitle(title);
        alert.setHeaderText(title);
        prepare(alert);
        alert.getDialogPane().lookupButton(action).getStyleClass().addAll("primary");
        // Make Cancel the default for Enter, so a stray keypress never deletes anything
        ((javafx.scene.control.Button) alert.getDialogPane().lookupButton(action)).setDefaultButton(false);
        ((javafx.scene.control.Button) alert.getDialogPane().lookupButton(cancel)).setDefaultButton(true);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == action;
    }

    /**
     * Shows a short explanation when something could not be done.
     *
     * @param title   short summary of the problem
     * @param message what happened and what to do instead
     */
    public static void info(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.NONE, message, new ButtonType("OK", ButtonBar.ButtonData.OK_DONE));
        alert.setTitle(title);
        alert.setHeaderText(title);
        prepare(alert);
        alert.showAndWait();
    }
}
