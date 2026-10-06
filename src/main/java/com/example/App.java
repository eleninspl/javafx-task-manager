package com.example;

import com.example.model.Task;
import com.example.model.Reminder;
import com.example.model.enums.TaskStatus;
import com.example.service.CategoryService;
import com.example.service.PriorityService;
import com.example.service.ReminderService;
import com.example.service.TaskService;
import com.example.storage.JsonStorage;
import com.example.controller.CategoryController;
import com.example.controller.PriorityController;
import com.example.controller.ReminderController;
import com.example.controller.SearchController;
import com.example.controller.TaskController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class App extends Application {

    // Services and storage
    private CategoryService categoryService;
    private PriorityService priorityService;
    private TaskService taskService;
    private ReminderService reminderService;
    private JsonStorage jsonStorage;

    // Controllers for different parts of the app
    private TaskController taskController;
    private CategoryController categoryController;
    private PriorityController priorityController;
    private ReminderController reminderController;
    private SearchController searchController;

    // UI elements for the summary panel
    private Label totalTasksLabel;
    private Label completedTasksLabel;
    private Label delayedTasksLabel;
    private Label dueIn7DaysLabel;

    // Main content area to swap views
    private BorderPane mainContent;

    @Override
    public void start(Stage primaryStage) {
        initServices();
        loadData();
        checkForDelayedTasks();
        
        // Build UI panels
        HBox summaryPanel = buildSummaryPanel();
        VBox navPanel = buildNavPanel();
        mainContent = new BorderPane();

        // Initialize controllers and set default view
        initControllers();
        mainContent.setCenter(taskController.getView());

        // Wire navigation buttons
        wireNavigation(navPanel);

        // Build overall layout
        BorderPane root = new BorderPane();
        root.setTop(summaryPanel);
        root.setLeft(navPanel);
        root.setCenter(mainContent);

        Scene scene = new Scene(root, 1000, 700);
        primaryStage.setTitle("MediaLab Assistant");
        primaryStage.setScene(scene);
        primaryStage.show();

        updateSummary();
        showTodaysReminders();

        // Save data on close
        primaryStage.setOnCloseRequest(event -> {
            saveData();
            Platform.exit();
        });
    }

    // Initialize services and storage
    private void initServices() {
        jsonStorage = new JsonStorage();
        categoryService = new CategoryService();
        priorityService = new PriorityService();
        reminderService = new ReminderService();
        taskService = new TaskService(reminderService);
    }

    // Load data from JSON files into services
    private void loadData() {
        categoryService.setCategories(jsonStorage.loadCategories());
        priorityService.setPriorities(jsonStorage.loadPriorities());
        taskService.setTasks(jsonStorage.loadTasks());
        reminderService.setReminders(jsonStorage.loadReminders());
    }

    // Save data to JSON files
    private void saveData() {
        jsonStorage.saveCategories(categoryService.getCategories());
        jsonStorage.savePriorities(priorityService.getPriorities());
        jsonStorage.saveTasks(taskService.getTasks());
        jsonStorage.saveReminders(reminderService.getReminders());
    }

    // Check for delayed tasks and alert the user if any exist
    private void checkForDelayedTasks() {
        long delayedCount = taskService.getTasks().stream()
                .filter(task -> task.getStatus() == TaskStatus.DELAYED)
                .count();
        if (delayedCount > 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Delayed Tasks");
            alert.setHeaderText("Attention Required");
            alert.setContentText("You have " + delayedCount + " delayed task(s)!");
            alert.showAndWait();
        }
    }

    // Build the summary panel at the top
    private HBox buildSummaryPanel() {
        HBox summaryPanel = new HBox(20);
        summaryPanel.setPadding(new Insets(10));
        summaryPanel.setAlignment(Pos.CENTER);
        totalTasksLabel = new Label();
        completedTasksLabel = new Label();
        delayedTasksLabel = new Label();
        dueIn7DaysLabel = new Label();
        summaryPanel.getChildren().addAll(totalTasksLabel, completedTasksLabel, delayedTasksLabel, dueIn7DaysLabel);
        return summaryPanel;
    }

    // Build the navigation panel on the left
    private VBox buildNavPanel() {
        VBox navPanel = new VBox(10);
        navPanel.setPadding(new Insets(10));
        navPanel.setStyle("-fx-background-color: #F0F0F0;");
        Button tasksBtn = new Button("Tasks");
        Button categoriesBtn = new Button("Categories");
        Button prioritiesBtn = new Button("Priorities");
        Button remindersBtn = new Button("Reminders");
        Button searchBtn = new Button("Search");
        tasksBtn.setMaxWidth(Double.MAX_VALUE);
        categoriesBtn.setMaxWidth(Double.MAX_VALUE);
        prioritiesBtn.setMaxWidth(Double.MAX_VALUE);
        remindersBtn.setMaxWidth(Double.MAX_VALUE);
        searchBtn.setMaxWidth(Double.MAX_VALUE);
        navPanel.getChildren().addAll(tasksBtn, categoriesBtn, prioritiesBtn, remindersBtn, searchBtn);

        // Wire navigation buttons
        tasksBtn.setOnAction(e -> mainContent.setCenter(taskController.getView()));
        categoriesBtn.setOnAction(e -> mainContent.setCenter(categoryController.getView()));
        prioritiesBtn.setOnAction(e -> mainContent.setCenter(priorityController.getView()));
        remindersBtn.setOnAction(e -> mainContent.setCenter(reminderController.getView()));
        searchBtn.setOnAction(e -> mainContent.setCenter(searchController.getView()));
        return navPanel;
    }

    // Wire navigation buttons (if additional wiring is needed)
    private void wireNavigation(VBox navPanel) {
        // In this example, wiring is done inside buildNavPanel(), so this method may be extended if necessary.
    }

    // Initialize controllers
    private void initControllers() {
        taskController = new TaskController(taskService, categoryService, priorityService, reminderService, this);
        categoryController = new CategoryController(categoryService, taskService, this);
        priorityController = new PriorityController(priorityService, taskService, this);
        reminderController = new ReminderController(reminderService, taskService);
        searchController = new SearchController(taskService, categoryService, priorityService);
    }

    // Update summary labels with current task data
    public void updateSummary() {
        Collection<Task> tasks = taskService.getTasks();
        int total = tasks.size();
        int completed = (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
        int delayed = (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.DELAYED).count();
        int dueIn7Days = (int) tasks.stream()
                .filter(t -> t.getDueDate() != null 
                        && !t.getDueDate().isBefore(LocalDate.now()) 
                        && t.getDueDate().isBefore(LocalDate.now().plusDays(7)))
                .count();

        totalTasksLabel.setText("Total Tasks: " + total);
        completedTasksLabel.setText("Completed: " + completed);
        delayedTasksLabel.setText("Delayed: " + delayed);
        dueIn7DaysLabel.setText("Due in 7 Days: " + dueIn7Days);
    }

    // Show a popup with today's reminders
    private void showTodaysReminders() {
        LocalDate today = LocalDate.now();
        List<Reminder> todaysReminders = reminderService.getReminders().stream()
                .filter(r -> r.getReminderDate().equals(today))
                .collect(Collectors.toList());
        if (!todaysReminders.isEmpty()) {
            StringBuilder message = new StringBuilder();
            message.append("You have ").append(todaysReminders.size()).append(" reminder(s) for today.\n\n");
            for (Reminder r : todaysReminders) {
                Task task = taskService.getTasks().stream()
                        .filter(t -> t.getId().equals(r.getTaskId()))
                        .findFirst().orElse(null);
                if (task != null) {
                    message.append("Task: ").append(task.getTitle()).append("\n");
                    message.append("Due Date: ").append(task.getDueDate() != null ? task.getDueDate().toString() : "N/A").append("\n");
                    message.append("Reminder Type: ").append(r.getType().toString()).append("\n\n");
                }
            }
            Alert alert = new Alert(Alert.AlertType.INFORMATION, message.toString(), ButtonType.OK);
            alert.setTitle("Today's Reminders");
            alert.setHeaderText("Reminders for " + today.toString());
            alert.showAndWait();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
