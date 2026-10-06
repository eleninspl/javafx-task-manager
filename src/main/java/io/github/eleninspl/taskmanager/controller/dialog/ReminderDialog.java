package io.github.eleninspl.taskmanager.controller.dialog;

import io.github.eleninspl.taskmanager.model.Reminder;
import io.github.eleninspl.taskmanager.model.Task;
import io.github.eleninspl.taskmanager.model.enums.ReminderType;
import io.github.eleninspl.taskmanager.service.DateFormats;
import io.github.eleninspl.taskmanager.service.ReminderService;
import io.github.eleninspl.taskmanager.ui.Dialogs;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

import java.time.LocalDate;

/**
 * Create or edit a reminder. The resulting date is previewed live, and rule violations
 * (completed task, date after the due date, date in the past) are explained before saving.
 */
public class ReminderDialog extends Dialog<ReminderDialog.Draft> {

    /**
     * What the user chose.
     *
     * @param task         the task to remind about
     * @param type         when to remind
     * @param specificDate the date, for SPECIFIC_DATE reminders; null otherwise
     */
    public record Draft(Task task, ReminderType type, LocalDate specificDate) {}

    private final ComboBox<Task> taskCombo;
    private final ComboBox<ReminderType> typeCombo = new ComboBox<>(FXCollections.observableArrayList(ReminderType.values()));
    private final DatePicker datePicker = new DatePicker();
    private final Label preview = new Label();

    /**
     * @param tasks           tasks that can have reminders (not completed or delayed)
     * @param existing        the reminder to edit, or null to create one
     * @param presetTask      task to preselect for a new reminder, or null
     * @param reminderService validates the choice
     */
    public ReminderDialog(ObservableList<Task> tasks, Reminder existing, Task presetTask, ReminderService reminderService) {
        boolean editing = existing != null;
        setTitle(editing ? "Edit reminder" : "New reminder");
        setHeaderText(editing ? "Edit reminder" : "New reminder");
        ButtonType save = new ButtonType(editing ? "Save changes" : "Add reminder", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, save);

        taskCombo = new ComboBox<>(tasks);
        taskCombo.setPromptText("Choose a task");
        taskCombo.setCellFactory(list -> new TaskCell());
        taskCombo.setButtonCell(new TaskCell());
        typeCombo.setPromptText("Choose when");
        Forms.commitTypedDates(datePicker);
        preview.setWrapText(true);

        VBox form = new VBox(14,
                Forms.field("_Task", taskCombo),
                Forms.row(Forms.field("_Remind me", typeCombo), Forms.field("_On date", datePicker)),
                preview);
        form.setPrefWidth(460);
        getDialogPane().setContent(form);

        if (editing) {
            tasks.stream().filter(t -> t.getId().equals(existing.getTaskId())).findFirst().ifPresent(taskCombo::setValue);
            typeCombo.setValue(existing.getType());
            if (existing.getType() == ReminderType.SPECIFIC_DATE) datePicker.setValue(existing.getReminderDate());
        } else {
            if (presetTask != null && tasks.contains(presetTask)) taskCombo.setValue(presetTask);
            typeCombo.setValue(ReminderType.ONE_DAY_BEFORE);
        }

        Button saveButton = (Button) getDialogPane().lookupButton(save);
        saveButton.getStyleClass().add("primary");
        Runnable validate = () -> {
            boolean specific = typeCombo.getValue() == ReminderType.SPECIFIC_DATE;
            datePicker.setDisable(!specific);
            if (!specific) datePicker.setValue(null);
            String error = reminderService.validationError(taskCombo.getValue(), typeCombo.getValue(), datePicker.getValue());
            saveButton.setDisable(error != null);
            preview.getStyleClass().removeAll("error-text", "hint");
            if (error == null) {
                preview.setText("You'll be reminded on " + DateFormats.full(previewDate()) + ".");
                preview.getStyleClass().add("hint");
            } else {
                preview.setText(error);
                // Missing choices are guidance, rule violations are errors
                boolean incomplete = taskCombo.getValue() == null || (specific && datePicker.getValue() == null);
                preview.getStyleClass().add(incomplete ? "hint" : "error-text");
            }
        };
        taskCombo.valueProperty().addListener((o, a, b) -> validate.run());
        typeCombo.valueProperty().addListener((o, a, b) -> validate.run());
        datePicker.valueProperty().addListener((o, a, b) -> validate.run());
        validate.run();

        setResultConverter(button -> button == save
                ? new Draft(taskCombo.getValue(), typeCombo.getValue(), datePicker.getValue())
                : null);
        Dialogs.prepare(this);
    }

    private LocalDate previewDate() {
        LocalDate due = taskCombo.getValue().getDueDate();
        return switch (typeCombo.getValue()) {
            case ONE_DAY_BEFORE -> due.minusDays(1);
            case ONE_WEEK_BEFORE -> due.minusWeeks(1);
            case ONE_MONTH_BEFORE -> due.minusMonths(1);
            case SPECIFIC_DATE -> datePicker.getValue();
        };
    }

    // Shows "Title · due Fri 9 Oct" so the deadline is visible while choosing
    private static class TaskCell extends ListCell<Task> {
        @Override
        protected void updateItem(Task task, boolean empty) {
            super.updateItem(task, empty);
            setText(empty || task == null ? null
                    : task.getTitle() + "  ·  due " + DateFormats.relative(task.getDueDate()));
        }
    }
}
