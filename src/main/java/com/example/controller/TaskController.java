package com.example.controller;

import com.example.model.Category;
import com.example.model.Task;
import com.example.service.CategoryService;
import com.example.service.PriorityService;
import com.example.service.ReminderService;
import com.example.service.TaskService;
import com.example.App;
import com.example.controller.dialog.TaskDialog;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.beans.property.SimpleStringProperty;
import java.util.Optional;
import java.util.stream.Collectors;

public class TaskController {

    private TaskService taskService;           // Handles task business logic
    private CategoryService categoryService;   // Provides category info
    private PriorityService priorityService;   // Provides priority info
    private App app;                           // Reference to main app for summary updates
    private BorderPane view;                   // Main container for task UI
    private TabPane tabPane;                   // TabPane to group tasks by category
    private Button newBtn, editBtn, deleteBtn; // Task operation buttons

    public TaskController(TaskService taskService,
                          CategoryService categoryService,
                          PriorityService priorityService,
                          ReminderService reminderService,
                          App app) {
        this.taskService = taskService;
        this.categoryService = categoryService;
        this.priorityService = priorityService;
        this.app = app;
        createView();
        updateTabs(); // Build initial tabs
        attachListeners();
    }

    public BorderPane getView() {
        return view;
    }

    // Build the overall UI
    private void createView() {
        view = new BorderPane();
        view.setPadding(new Insets(10));
        tabPane = new TabPane();
        view.setCenter(tabPane);
        HBox buttonBox = createButtonBox();
        view.setBottom(buttonBox);
    }

    // Create button box with New, Edit, Delete buttons and attach handlers
    private HBox createButtonBox() {
        newBtn = new Button("New Task");
        editBtn = new Button("Edit Task");
        deleteBtn = new Button("Delete Task");

        newBtn.setOnAction(e -> openTaskDialog(null));
        editBtn.setOnAction(e -> handleEditTask());
        deleteBtn.setOnAction(e -> handleDeleteTask());

        HBox buttonBox = new HBox(10, newBtn, editBtn, deleteBtn);
        buttonBox.setPadding(new Insets(10));
        return buttonBox;
    }

    // Attach listeners for category and task changes
    private void attachListeners() {
        // Rebuild tabs when categories change
        categoryService.getCategories().addListener((ListChangeListener<Category>) change -> updateTabs());
        // Refresh each TableView when tasks change
        taskService.getTasks().addListener((ListChangeListener<Task>) change -> {
            for (Tab tab : tabPane.getTabs()) {
                if (tab.getContent() instanceof TableView<?> tableView) {
                    tableView.refresh();
                }
            }
        });
    }

    // Handle task editing with selection check
    private void handleEditTask() {
        Task selected = getSelectedTask();
        if (selected == null) {
            showAlert("Please select a task to edit.");
            return;
        }
        openTaskDialog(selected);
    }

    // Handle task deletion with selection check and confirmation
    private void handleDeleteTask() {
        Task selected = getSelectedTask();
        if (selected == null) {
            showAlert("Please select a task to delete.");
            return;
        }
        if (confirmAction("Delete Task?", "Are you sure you want to delete this task?")) {
            taskService.deleteTask(selected.getId());
            updateTabs();
            app.updateSummary();
        }
    }

    // Retrieve the currently selected task from the active tab
    private Task getSelectedTask() {
        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
        if (currentTab == null) {
            return null;
        }
        if (currentTab.getContent() instanceof TableView<?> tableView) {
            Object selected = tableView.getSelectionModel().getSelectedItem();
            if (selected instanceof Task task) {
                return task;
            }
        }
        return null;
    }

    // Rebuild the TabPane, grouping tasks by category
    public void updateTabs() {
        tabPane.getTabs().clear();
        for (Category cat : categoryService.getCategories()) {
            ObservableList<Task> tasksInCategory = FXCollections.observableArrayList(
                taskService.getTasks().stream().filter(task -> {
                    if (cat.getName().equalsIgnoreCase("No Category")) {
                        return task.getCategory() == null ||
                               task.getCategory().getName().equalsIgnoreCase("No Category");
                    }
                    return task.getCategory() != null &&
                           task.getCategory().getId().equals(cat.getId());
                }).collect(Collectors.toList())
            );
            if (!tasksInCategory.isEmpty()) {
                Tab tab = new Tab(cat.getName());
                tab.setClosable(false);
                TableView<Task> table = createTaskTable(tasksInCategory);
                tab.setContent(table);
                tabPane.getTabs().add(tab);
            }
        }
    }

    // Create a TableView for tasks with columns for task details
    private TableView<Task> createTaskTable(ObservableList<Task> tasks) {
        TableView<Task> table = new TableView<>(tasks);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Task, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));

        TableColumn<Task, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        TableColumn<Task, String> priorityCol = new TableColumn<>("Priority");
        priorityCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getPriority() != null ?
                    cellData.getValue().getPriority().getName() : "")
        );

        TableColumn<Task, String> dueDateCol = new TableColumn<>("Due Date");
        dueDateCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getDueDate() != null ?
                    cellData.getValue().getDueDate().toString() : "")
        );

        TableColumn<Task, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getStatus().toString())
        );

        table.getColumns().add(titleCol);
        table.getColumns().add(descCol);
        table.getColumns().add(priorityCol);
        table.getColumns().add(dueDateCol);
        table.getColumns().add(statusCol);
        return table;
    }

    // Open the TaskDialog for creating or editing a task
    private void openTaskDialog(Task existingTask) {
        TaskDialog dialog = new TaskDialog(existingTask, categoryService, priorityService);
        dialog.setTitle(existingTask == null ? "New Task" : "Edit Task");
        dialog.showAndWait().ifPresent(resultTask -> {
            if (existingTask == null) {
                taskService.addTask(resultTask);
            } else {
                Task updatedTask = new Task(
                        existingTask.getId(),
                        resultTask.getTitle(),
                        resultTask.getDescription(),
                        resultTask.getCategory(),
                        resultTask.getPriority(),
                        resultTask.getDueDate(),
                        resultTask.getStatus()
                );
                taskService.updateTask(updatedTask);
            }
            updateTabs();
            app.updateSummary();
        });
    }

    // Helper: Show an alert with a message
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.showAndWait();
    }

    // Helper: Show a confirmation dialog and return true if the user confirms
    private boolean confirmAction(String header, String content) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText(header);
        confirm.setContentText(content);
        Optional<ButtonType> result = confirm.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}
