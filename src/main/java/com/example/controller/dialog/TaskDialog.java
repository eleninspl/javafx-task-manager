package com.example.controller.dialog;

import com.example.model.Category;
import com.example.model.Priority;
import com.example.model.Task;
import com.example.model.enums.TaskStatus;
import com.example.service.CategoryService;
import com.example.service.PriorityService;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;
import java.time.LocalDate;

public class TaskDialog extends Dialog<Task> {

    private TextField titleField;      // Field for task title
    private TextField descField;       // Field for task description
    private ComboBox<Category> categoryCombo; // ComboBox for category selection
    private ComboBox<Priority> priorityCombo; // ComboBox for priority selection
    private DatePicker dueDatePicker;  // DatePicker for due date
    private ComboBox<TaskStatus> statusCombo; // ComboBox for task status

    public TaskDialog(Task existingTask, CategoryService categoryService, PriorityService priorityService) {
        // Set dialog title and add OK/Cancel buttons
        setTitle("Task Dialog");
        ButtonType okBtn = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(okBtn, cancelBtn);

        // Create grid layout for input fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        // Initialize fields with prompt text
        titleField = new TextField();
        titleField.setPromptText("Title");

        descField = new TextField();
        descField.setPromptText("Description");

        categoryCombo = new ComboBox<>(categoryService.getCategories());
        categoryCombo.setPromptText("Select Category");

        priorityCombo = new ComboBox<>(priorityService.getPriorities());
        priorityCombo.setPromptText("Select Priority");

        dueDatePicker = new DatePicker();
        dueDatePicker.setPromptText("Due Date");

        // Allow selection of status; DELAYED is computed so it's not available here
        statusCombo = new ComboBox<>(FXCollections.observableArrayList(
            TaskStatus.OPEN, TaskStatus.IN_PROGRESS, TaskStatus.POSTPONED, TaskStatus.COMPLETED
        ));
        statusCombo.setPromptText("Select Status");

        // Add labels and controls to the grid
        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label("Category:"), 0, 2);
        grid.add(categoryCombo, 1, 2);
        grid.add(new Label("Priority:"), 0, 3);
        grid.add(priorityCombo, 1, 3);
        grid.add(new Label("Due Date:"), 0, 4);
        grid.add(dueDatePicker, 1, 4);
        grid.add(new Label("Status:"), 0, 5);
        grid.add(statusCombo, 1, 5);

        getDialogPane().setContent(grid);

        // If editing, pre-fill the fields with existing task data
        if (existingTask != null) {
            titleField.setText(existingTask.getTitle());
            descField.setText(existingTask.getDescription());
            categoryCombo.setValue(existingTask.getCategory());
            priorityCombo.setValue(existingTask.getPriority());
            dueDatePicker.setValue(existingTask.getDueDate());
            // Prepopulate status if it is one of the allowed options
            if (existingTask.getStatus() == TaskStatus.OPEN ||
                existingTask.getStatus() == TaskStatus.IN_PROGRESS ||
                existingTask.getStatus() == TaskStatus.POSTPONED ||
                existingTask.getStatus() == TaskStatus.COMPLETED) {
                statusCombo.setValue(existingTask.getStatus());
            }
        }

        // Disable OK button if title is empty or due date not selected
        Button okButton = (Button) getDialogPane().lookupButton(okBtn);
        okButton.disableProperty().bind(Bindings.createBooleanBinding(() ->
            titleField.getText().trim().isEmpty() || dueDatePicker.getValue() == null,
            titleField.textProperty(), dueDatePicker.valueProperty()
        ));

        // Convert dialog result when OK is clicked; compute task status based on due date
        setResultConverter(new Callback<ButtonType, Task>() {
            @Override
            public Task call(ButtonType b) {
                if (b == okBtn) {
                    LocalDate dueDate = dueDatePicker.getValue();
                    TaskStatus status;
                    // If task is overdue, force status to DELAYED; otherwise, use chosen status or default to OPEN
                    if (statusCombo.getValue() == TaskStatus.COMPLETED) {
                        status = TaskStatus.COMPLETED;
                    }
                    else if (dueDate.isBefore(LocalDate.now())) {
                        status = TaskStatus.DELAYED;
                    } else {
                        status = (statusCombo.getValue() != null) ? statusCombo.getValue() : TaskStatus.OPEN;
                    }
                    // Use default category/priority if not selected
                    Category category = (categoryCombo.getValue() != null)
                        ? categoryCombo.getValue() : categoryService.getNoCategory();
                    Priority priority = (priorityCombo.getValue() != null)
                        ? priorityCombo.getValue() : priorityService.getDefaultPriority();
                    return new Task(
                        titleField.getText().trim(),
                        descField.getText().trim(),
                        category,
                        priority,
                        dueDate,
                        status
                    );
                }
                return null;
            }
        });
    }
}
