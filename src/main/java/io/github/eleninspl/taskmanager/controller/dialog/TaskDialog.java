package io.github.eleninspl.taskmanager.controller.dialog;

import io.github.eleninspl.taskmanager.model.Category;
import io.github.eleninspl.taskmanager.model.Priority;
import io.github.eleninspl.taskmanager.model.Task;
import io.github.eleninspl.taskmanager.model.enums.TaskStatus;
import io.github.eleninspl.taskmanager.service.CategoryService;
import io.github.eleninspl.taskmanager.service.PriorityService;
import io.github.eleninspl.taskmanager.ui.Dialogs;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.time.LocalDate;

/**
 * Create or edit a task. Delayed is never offered as a choice: it is assigned automatically
 * when the due date has passed and the task is not completed.
 */
public class TaskDialog extends Dialog<Task> {

    private final TextField titleField = new TextField();
    private final TextArea notesArea = new TextArea();
    private final ComboBox<Category> categoryCombo;
    private final ComboBox<Priority> priorityCombo;
    private final DatePicker dueDatePicker = new DatePicker();
    private final ComboBox<TaskStatus> statusCombo = new ComboBox<>(FXCollections.observableArrayList(
            TaskStatus.OPEN, TaskStatus.IN_PROGRESS, TaskStatus.POSTPONED, TaskStatus.COMPLETED));
    private final Label hint = new Label();
    private final Task existingTask;

    /**
     * @param existingTask    the task to edit, or null to create one
     * @param presetCategory  category to preselect for a new task, or null for "No Category"
     * @param categoryService source of categories
     * @param priorityService source of priorities
     */
    public TaskDialog(Task existingTask, Category presetCategory,
                      CategoryService categoryService, PriorityService priorityService) {
        this.existingTask = existingTask;
        boolean editing = existingTask != null;
        setTitle(editing ? "Edit task" : "New task");
        setHeaderText(editing ? "Edit task" : "New task");

        ButtonType save = new ButtonType(editing ? "Save changes" : "Add task", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, save);

        titleField.setPromptText("What needs doing?");
        notesArea.setPromptText("Optional details");
        notesArea.setPrefRowCount(3);
        notesArea.setWrapText(true);
        categoryCombo = new ComboBox<>(categoryService.getCategories());
        priorityCombo = new ComboBox<>(priorityService.getPriorities());
        Forms.commitTypedDates(dueDatePicker);
        hint.setWrapText(true);
        hint.getStyleClass().add("hint");

        VBox form = new VBox(14,
                Forms.field("_Title", titleField),
                Forms.field("_Notes", notesArea),
                Forms.row(Forms.field("_Category", categoryCombo), Forms.field("_Priority", priorityCombo)),
                Forms.row(Forms.field("_Due date", dueDatePicker), Forms.field("_Status", statusCombo)),
                hint);
        form.setPrefWidth(460);
        getDialogPane().setContent(form);

        if (editing) {
            titleField.setText(existingTask.getTitle());
            notesArea.setText(existingTask.getDescription());
            categoryCombo.setValue(categoryService.resolve(existingTask.getCategory()));
            priorityCombo.setValue(priorityService.resolve(existingTask.getPriority()));
            dueDatePicker.setValue(existingTask.getDueDate());
            if (existingTask.getStatus() == TaskStatus.DELAYED) {
                statusCombo.setPromptText("Delayed (automatic)");
            } else {
                statusCombo.setValue(existingTask.getStatus());
            }
        } else {
            categoryCombo.setValue(presetCategory != null ? presetCategory : categoryService.getNoCategory());
            priorityCombo.setValue(priorityService.getDefaultPriority());
            statusCombo.setValue(TaskStatus.OPEN);
        }

        Button saveButton = (Button) getDialogPane().lookupButton(save);
        saveButton.getStyleClass().add("primary");
        Runnable validate = () -> {
            boolean missing = titleField.getText().isBlank() || dueDatePicker.getValue() == null;
            saveButton.setDisable(missing);
            updateHint(missing);
        };
        titleField.textProperty().addListener((o, a, b) -> validate.run());
        dueDatePicker.valueProperty().addListener((o, a, b) -> validate.run());
        statusCombo.valueProperty().addListener((o, a, b) -> validate.run());
        validate.run();

        setResultConverter(button -> {
            if (button != save) return null;
            LocalDate dueDate = dueDatePicker.getValue();
            TaskStatus status = resolveStatus(dueDate);
            Category category = categoryCombo.getValue() != null ? categoryCombo.getValue() : categoryService.getNoCategory();
            Priority priority = priorityCombo.getValue() != null ? priorityCombo.getValue() : priorityService.getDefaultPriority();
            String title = titleField.getText().trim();
            String notes = notesArea.getText() == null ? "" : notesArea.getText().trim();
            return editing
                    ? new Task(existingTask.getId(), title, notes, category, priority, dueDate, status)
                    : new Task(title, notes, category, priority, dueDate, status);
        });

        Dialogs.prepare(this);
        setOnShown(e -> Platform.runLater(titleField::requestFocus));
    }

    // Completed always wins; otherwise a past due date means Delayed; otherwise the chosen status
    private TaskStatus resolveStatus(LocalDate dueDate) {
        if (statusCombo.getValue() == TaskStatus.COMPLETED) return TaskStatus.COMPLETED;
        if (dueDate.isBefore(LocalDate.now())) return TaskStatus.DELAYED;
        return statusCombo.getValue() != null ? statusCombo.getValue() : TaskStatus.OPEN;
    }

    private void updateHint(boolean missing) {
        hint.getStyleClass().remove("error-text");
        LocalDate due = dueDatePicker.getValue();
        if (missing) {
            hint.setText("Add a title and a due date to save.");
        } else if (due.isBefore(LocalDate.now()) && statusCombo.getValue() != TaskStatus.COMPLETED) {
            hint.setText("This date has already passed, so the task will be marked Delayed.");
            hint.getStyleClass().add("error-text");
        } else if (existingTask != null && existingTask.getStatus() == TaskStatus.DELAYED) {
            hint.setText("With a future due date this task is no longer Delayed.");
        } else if (statusCombo.getValue() == TaskStatus.COMPLETED) {
            hint.setText("Completing a task removes its reminders.");
        } else {
            hint.setText("");
        }
        hint.setManaged(!hint.getText().isEmpty());
    }
}
