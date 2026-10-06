package com.example.controller;

import com.example.model.Reminder;
import com.example.model.Task;
import com.example.model.enums.TaskStatus;
import com.example.service.ReminderService;
import com.example.service.TaskService;
import com.example.controller.dialog.ReminderDialog;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.beans.property.SimpleStringProperty;
import java.util.Optional;

public class ReminderController {

    private ReminderService reminderService; // Handles reminder logic
    private TaskService taskService;         // Provides access to tasks
    private BorderPane view;                 // Main container for reminder UI
    private TableView<Reminder> reminderTable; // Table showing reminders
    private Button newBtn, editBtn, deleteBtn; // Buttons for operations

    public ReminderController(ReminderService reminderService, TaskService taskService) {
        this.reminderService = reminderService;
        this.taskService = taskService;
        createView(); // Build the UI
    }

    public BorderPane getView() {
        return view;
    }

    // Build UI components for reminder management
    private void createView() {
        view = new BorderPane();
        view.setPadding(new Insets(10));
        
        createReminderTable();
        createButtonBox();
    }

    // Build and set up the table for reminders
    private void createReminderTable() {
        reminderTable = new TableView<>();
        reminderTable.setItems(reminderService.getReminders());
        reminderTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        reminderTable.getColumns().add(createTaskColumn());
        reminderTable.getColumns().add(createTypeColumn());
        reminderTable.getColumns().add(createDateColumn());
        view.setCenter(reminderTable);
    }

    // Create the "Task" column
    private TableColumn<Reminder, String> createTaskColumn() {
        TableColumn<Reminder, String> taskCol = new TableColumn<>("Task");
        taskCol.setCellValueFactory(cellData -> {
            Task task = taskService.getTasks().stream()
                    .filter(t -> t.getId().equals(cellData.getValue().getTaskId()))
                    .findFirst().orElse(null);
            return new SimpleStringProperty(task == null ? "Unknown" : task.getTitle());
        });
        return taskCol;
    }

    // Create the "Type" column
    private TableColumn<Reminder, String> createTypeColumn() {
        TableColumn<Reminder, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getType().toString())
        );
        return typeCol;
    }

    // Create the "Reminder Date" column
    private TableColumn<Reminder, String> createDateColumn() {
        TableColumn<Reminder, String> dateCol = new TableColumn<>("Reminder Date");
        dateCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getReminderDate() != null ?
                cellData.getValue().getReminderDate().toString() : "Computed by service")
        );
        return dateCol;
    }

    // Build the button box and attach action handlers
    private void createButtonBox() {
        newBtn = new Button("New Reminder");
        editBtn = new Button("Edit Reminder");
        deleteBtn = new Button("Delete Reminder");

        newBtn.setOnAction(e -> openReminderDialog(null));
        editBtn.setOnAction(e -> handleEditReminder());
        deleteBtn.setOnAction(e -> handleDeleteReminder());

        HBox buttonBox = new HBox(10, newBtn, editBtn, deleteBtn);
        buttonBox.setPadding(new Insets(10));
        view.setBottom(buttonBox);
    }

    // Returns the currently selected reminder (or null if none is selected)
    private Reminder getSelectedReminder() {
        return reminderTable.getSelectionModel().getSelectedItem();
    }

    // Handle editing a reminder: show alert if none is selected
    private void handleEditReminder() {
        Reminder selected = getSelectedReminder();
        if (selected == null) {
            showInfoAlert("Please select a reminder to edit.");
            return;
        }
        openReminderDialog(selected);
    }

    // Handle deleting a reminder: show alert if none is selected
    private void handleDeleteReminder() {
        Reminder selected = getSelectedReminder();
        if (selected == null) {
            showInfoAlert("Please select a reminder to delete.");
            return;
        }
        Optional<ButtonType> result = showConfirmation("Delete Reminder", "Are you sure you want to delete this reminder?");
        if (result.isPresent() && result.get() == ButtonType.OK) {
            reminderService.deleteReminder(selected.getId());
        }
    }

    // Open dialog for creating or editing a reminder
    private void openReminderDialog(Reminder existingReminder) {
        // Filter tasks to include only active tasks (not COMPLETED or DELAYED)
        FilteredList<Task> activeTasks = new FilteredList<>(taskService.getTasks(),
                t -> t.getStatus() != TaskStatus.COMPLETED && t.getStatus() != TaskStatus.DELAYED);
        ReminderDialog dialog = new ReminderDialog(activeTasks, existingReminder);
        Optional<Reminder> result = dialog.showAndWait();
        result.ifPresent(partialReminder -> {
            // Find the task associated with the reminder
            Task task = activeTasks.stream()
                    .filter(t -> t.getId().equals(partialReminder.getTaskId()))
                    .findFirst().orElse(null);
            if (task == null) {
                showErrorAlert("Selected task not found.");
                return;
            }
            try {
                reminderService.createReminderForTask(task, partialReminder.getType(), partialReminder.getReminderDate());
            } catch (IllegalArgumentException ex) {
                showErrorAlert(ex.getMessage());
            }
        });
    }

    // Helper: Show an information alert with the provided message
    private void showInfoAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.showAndWait();
    }

    // Helper: Show an error alert with the provided message
    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.showAndWait();
    }

    // Helper: Show a confirmation dialog and return the user's response
    private Optional<ButtonType> showConfirmation(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        return alert.showAndWait();
    }
}
