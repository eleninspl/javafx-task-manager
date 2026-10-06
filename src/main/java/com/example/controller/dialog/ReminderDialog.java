package com.example.controller.dialog;

import com.example.model.Reminder;
import com.example.model.enums.ReminderType;
import com.example.model.Task;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;
import java.time.LocalDate;

public class ReminderDialog extends Dialog<Reminder> {

    private ComboBox<Task> taskCombo;            // ComboBox for selecting a task
    private ComboBox<ReminderType> typeCombo;      // ComboBox for selecting reminder type
    private DatePicker specificDatePicker;         // DatePicker for a specific date

    public ReminderDialog(ObservableList<Task> tasks, Reminder existingReminder) {
        // Set dialog title based on create or edit mode
        setTitle(existingReminder == null ? "New Reminder" : "Edit Reminder");

        // Add OK and Cancel buttons
        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType);

        // Create grid layout for fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        // Initialize ComboBox for tasks and set prompt text
        taskCombo = new ComboBox<>(tasks);
        taskCombo.setPromptText("Select Task");

        // Initialize ComboBox for reminder type and populate with types
        typeCombo = new ComboBox<>(FXCollections.observableArrayList(
            ReminderType.ONE_DAY_BEFORE,
            ReminderType.ONE_WEEK_BEFORE,
            ReminderType.ONE_MONTH_BEFORE,
            ReminderType.SPECIFIC_DATE
        ));
        typeCombo.setPromptText("Select Reminder Type");

        // Initialize DatePicker for specific date, disable by default
        specificDatePicker = new DatePicker();
        specificDatePicker.setPromptText("Specific Date");
        specificDatePicker.setDisable(true);

        // Add listener to enable/disable DatePicker based on type selection
        typeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == ReminderType.SPECIFIC_DATE) {
                specificDatePicker.setDisable(false);
            } else {
                specificDatePicker.setDisable(true);
                specificDatePicker.setValue(null);
            }
        });

        // Add labels and controls to the grid
        grid.add(new Label("Task:"), 0, 0);
        grid.add(taskCombo, 1, 0);
        grid.add(new Label("Reminder Type:"), 0, 1);
        grid.add(typeCombo, 1, 1);
        grid.add(new Label("Specific Date:"), 0, 2);
        grid.add(specificDatePicker, 1, 2);
        getDialogPane().setContent(grid);

        // Pre-fill fields if editing an existing reminder
        if (existingReminder != null) {
            for (Task t : tasks) {
                if (t.getId().equals(existingReminder.getTaskId())) {
                    taskCombo.setValue(t);
                    break;
                }
            }
            typeCombo.setValue(existingReminder.getType());
            specificDatePicker.setValue(existingReminder.getReminderDate());
            // Enable DatePicker if type is SPECIFIC_DATE
            specificDatePicker.setDisable(existingReminder.getType() != ReminderType.SPECIFIC_DATE);
        }

        // Disable OK button if required fields are missing
        Node okButton = getDialogPane().lookupButton(okButtonType);
        okButton.disableProperty().bind(
            taskCombo.valueProperty().isNull()
            .or(typeCombo.valueProperty().isNull())
            .or(
                typeCombo.valueProperty().isEqualTo(ReminderType.SPECIFIC_DATE)
                .and(specificDatePicker.valueProperty().isNull())
            )
        );

        // When OK is clicked, create a new Reminder object
        // If the type is not SPECIFIC_DATE, reminderDate will be null (and will be computed by the service)
        setResultConverter(new Callback<ButtonType, Reminder>() {
            @Override
            public Reminder call(ButtonType b) {
                if (b == okButtonType) {
                    Task selectedTask = taskCombo.getValue();
                    ReminderType type = typeCombo.getValue();
                    LocalDate date = (type == ReminderType.SPECIFIC_DATE) ? specificDatePicker.getValue() : null;
                    return new Reminder(selectedTask.getId(), type, date);
                }
                return null;
            }
        });
    }
}
