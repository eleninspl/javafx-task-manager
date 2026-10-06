package io.github.eleninspl.taskmanager;

import io.github.eleninspl.taskmanager.controller.CategoryController;
import io.github.eleninspl.taskmanager.controller.PriorityController;
import io.github.eleninspl.taskmanager.controller.ReminderController;
import io.github.eleninspl.taskmanager.controller.Sidebar;
import io.github.eleninspl.taskmanager.controller.TaskController;
import io.github.eleninspl.taskmanager.model.Reminder;
import io.github.eleninspl.taskmanager.model.Task;
import io.github.eleninspl.taskmanager.model.enums.TaskStatus;
import io.github.eleninspl.taskmanager.service.CategoryService;
import io.github.eleninspl.taskmanager.service.DateFormats;
import io.github.eleninspl.taskmanager.service.PriorityService;
import io.github.eleninspl.taskmanager.service.ReminderService;
import io.github.eleninspl.taskmanager.service.TaskQuery;
import io.github.eleninspl.taskmanager.service.TaskService;
import io.github.eleninspl.taskmanager.storage.JsonStorage;
import io.github.eleninspl.taskmanager.ui.Dialogs;
import io.github.eleninspl.taskmanager.ui.Icons;
import io.github.eleninspl.taskmanager.ui.Theme;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MediaLab Assistant. The window has two parts, as the assignment requires: a summary bar on top
 * (total, completed, delayed, due within 7 days) and below it the sidebar with the current view.
 */
public class App extends Application {

    private static final String WINDOW_TITLE = "MediaLab Assistant";

    // Services and storage
    private CategoryService categoryService;
    private PriorityService priorityService;
    private TaskService taskService;
    private ReminderService reminderService;
    private JsonStorage jsonStorage;
    private boolean savePending;

    // Views
    private Sidebar sidebar;
    private TaskController taskController;
    private ReminderController reminderController;
    private CategoryController categoryController;
    private PriorityController priorityController;
    private final BorderPane content = new BorderPane();

    // Summary bar
    private final SummaryItem totalItem = new SummaryItem("tasks", "task");
    private final SummaryItem completedItem = new SummaryItem("completed", "completed");
    private final SummaryItem delayedItem = new SummaryItem("delayed", "delayed");
    private final SummaryItem dueSoonItem = new SummaryItem("due in 7 days", "due in 7 days");

    @Override
    public void start(Stage stage) {
        initServices();
        loadData();
        enableAutoSave();
        taskService.checkOverdueTasks();

        Dialogs.setOwner(stage);
        initViews();

        BorderPane body = new BorderPane();
        body.setLeft(sidebar.getView());
        body.setCenter(content);
        BorderPane root = new BorderPane();
        root.setTop(buildSummaryBar());
        root.setCenter(body);

        Scene scene = new Scene(root, 1120, 740);
        Theme.apply(scene);
        installShortcuts(scene);
        stage.setTitle(WINDOW_TITLE);
        stage.setMinWidth(900);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> saveData());
        stage.show();

        updateSummary();
        taskService.getTasks().addListener((ListChangeListener<Task>) c -> updateSummary());
        taskController.focusList();
        showTodaysReminders();
        // The assignment asks for a popup about delayed tasks at startup; show it over the window
        Platform.runLater(this::showDelayedTasksPopup);
    }

    // ---------------------------------------------------------------- data

    private void initServices() {
        jsonStorage = new JsonStorage();
        categoryService = new CategoryService();
        priorityService = new PriorityService();
        reminderService = new ReminderService();
        taskService = new TaskService(reminderService);
    }

    private void loadData() {
        categoryService.setCategories(jsonStorage.loadCategories());
        priorityService.setPriorities(jsonStorage.loadPriorities());
        taskService.setTasks(jsonStorage.loadTasks());
        reminderService.setReminders(jsonStorage.loadReminders());
        taskService.linkReferences(categoryService, priorityService);
    }

    private void saveData() {
        jsonStorage.saveCategories(categoryService.getCategories());
        jsonStorage.savePriorities(priorityService.getPriorities());
        jsonStorage.saveTasks(taskService.getTasks());
        jsonStorage.saveReminders(reminderService.getReminders());
    }

    // Save whenever any list changes. Changes made in the same UI event (such as a
    // category delete that cascades to its tasks and reminders) are batched into one save.
    private void enableAutoSave() {
        ListChangeListener<Object> scheduleSave = change -> {
            if (savePending) return;
            savePending = true;
            Platform.runLater(() -> {
                savePending = false;
                saveData();
            });
        };
        categoryService.getCategories().addListener(scheduleSave);
        priorityService.getPriorities().addListener(scheduleSave);
        taskService.getTasks().addListener(scheduleSave);
        reminderService.getReminders().addListener(scheduleSave);
    }

    // ---------------------------------------------------------------- layout

    private void initViews() {
        reminderController = new ReminderController(reminderService, taskService);
        taskController = new TaskController(taskService, categoryService, priorityService, reminderService,
                task -> reminderController.openReminderDialog(null, task));
        categoryController = new CategoryController(categoryService, taskService);
        priorityController = new PriorityController(priorityService, taskService);
        sidebar = new Sidebar(taskService, categoryService, reminderService, this::newTask);
        sidebar.setOnSelect(this::show);
        show(Sidebar.ALL);
    }

    // Switch the content area to the view for a sidebar key
    private void show(String key) {
        switch (key) {
            case Sidebar.REMINDERS -> content.setCenter(reminderController.getView());
            case Sidebar.CATEGORIES -> content.setCenter(categoryController.getView());
            case Sidebar.PRIORITIES -> content.setCenter(priorityController.getView());
            default -> {
                content.setCenter(taskController.getView());
                switch (key) {
                    case Sidebar.OVERDUE -> taskController.showScope(TaskQuery.Scope.OVERDUE, null);
                    case Sidebar.NEXT_7 -> taskController.showScope(TaskQuery.Scope.NEXT_7_DAYS, null);
                    case Sidebar.ALL -> taskController.showScope(TaskQuery.Scope.ALL, null);
                    default -> taskController.showScope(TaskQuery.Scope.CATEGORY, sidebar.categoryFor(key));
                }
            }
        }
    }

    private boolean onTaskView() {
        return content.getCenter() == taskController.getView();
    }

    private void newTask() {
        if (!onTaskView()) sidebar.select(Sidebar.ALL);
        taskController.newTask();
    }

    private void installShortcuts(Scene scene) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN), this::newTask);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN), () -> {
            if (!onTaskView()) sidebar.select(Sidebar.ALL);
            taskController.focusSearch();
        });
    }

    private Node buildSummaryBar() {
        Label name = new Label(WINDOW_TITLE);
        name.getStyleClass().add("app-name");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Each count opens its view and hands focus to the list, so the count doesn't look like an active filter
        totalItem.button.setOnAction(e -> openFromSummary(Sidebar.ALL));
        completedItem.button.setOnAction(e -> {
            openFromSummary(Sidebar.ALL);
            taskController.revealCompleted();
        });
        delayedItem.button.setOnAction(e -> openFromSummary(Sidebar.OVERDUE));
        dueSoonItem.button.setOnAction(e -> openFromSummary(Sidebar.NEXT_7));

        HBox bar = new HBox(name, spacer, totalItem.button, completedItem.button, delayedItem.button, dueSoonItem.button);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("summary-bar");
        return bar;
    }

    private void openFromSummary(String key) {
        sidebar.select(key);
        taskController.focusList();
    }

    /** Refreshes the four counts in the summary bar. */
    public void updateSummary() {
        totalItem.set(taskService.getTasks().size(), false);
        completedItem.set(taskService.countWithStatus(TaskStatus.COMPLETED), false);
        long delayed = taskService.countWithStatus(TaskStatus.DELAYED);
        delayedItem.set(delayed, delayed > 0);
        dueSoonItem.set(taskService.countDueWithin(7), false);
    }

    // One count in the summary bar; clicking it opens the matching drawer
    private static final class SummaryItem {
        final Button button = new Button();
        final Label count = new Label();
        final Label caption = new Label();
        final String plural;
        final String singular;

        SummaryItem(String plural, String singular) {
            this.plural = plural;
            this.singular = singular;
            count.getStyleClass().add("count");
            caption.getStyleClass().add("caption");
            HBox box = new HBox(6, count, caption);
            box.setAlignment(Pos.BASELINE_LEFT);
            button.setGraphic(box);
            button.getStyleClass().setAll("summary-item");
        }

        void set(long n, boolean alert) {
            count.setText(String.valueOf(n));
            caption.setText(n == 1 ? singular : plural);
            button.setAccessibleText(n + " " + caption.getText());
            button.getStyleClass().remove("alert");
            if (alert) button.getStyleClass().add("alert");
        }
    }

    // ---------------------------------------------------------------- startup notices

    // Popup required by the assignment: how many tasks are delayed, which ones, and a way to review them
    private void showDelayedTasksPopup() {
        List<Task> delayed = taskService.getTasks().stream()
                .filter(t -> t.getStatus() == TaskStatus.DELAYED)
                .sorted((a, b) -> a.getDueDate().compareTo(b.getDueDate()))
                .collect(Collectors.toList());
        if (delayed.isEmpty()) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        String heading = delayed.size() == 1 ? "1 task is overdue" : delayed.size() + " tasks are overdue";
        dialog.setTitle(heading);
        dialog.setHeaderText(heading);
        VBox list = new VBox(6);
        delayed.stream().limit(5).forEach(t -> {
            Label title = new Label(t.getTitle());
            title.setStyle("-fx-font-weight: bold;");
            Label due = new Label("was due " + DateFormats.relative(t.getDueDate()));
            due.getStyleClass().add("error-text");
            HBox row = new HBox(8, title, due);
            row.setAlignment(Pos.BASELINE_LEFT);
            list.getChildren().add(row);
        });
        if (delayed.size() > 5) {
            Label more = new Label("and " + (delayed.size() - 5) + " more");
            more.getStyleClass().add("hint");
            list.getChildren().add(more);
        }
        Label explain = new Label(delayed.size() == 1
                ? "It has been marked Delayed. Give it a new due date or mark it completed."
                : "They have been marked Delayed. Give them new due dates or mark them completed.");
        explain.getStyleClass().add("text-secondary");
        explain.setWrapText(true);
        VBox box = new VBox(14, list, explain);
        box.setPrefWidth(420);
        dialog.getDialogPane().setContent(box);

        ButtonType review = new ButtonType("Review overdue", ButtonBar.ButtonData.OK_DONE);
        ButtonType later = new ButtonType("Later", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(later, review);
        Dialogs.prepare(dialog);
        dialog.getDialogPane().lookupButton(review).getStyleClass().add("primary");
        dialog.showAndWait().filter(b -> b == review).ifPresent(b -> sidebar.select(Sidebar.OVERDUE));
    }

    // Today's reminders appear as a banner above the task list instead of interrupting
    private void showTodaysReminders() {
        LocalDate today = LocalDate.now();
        List<String> titles = reminderService.getReminders().stream()
                .filter(r -> today.equals(r.getReminderDate()))
                .map(Reminder::getTaskId)
                .distinct()
                .map(id -> taskService.getTasks().stream().filter(t -> t.getId().equals(id)).findFirst().orElse(null))
                .filter(t -> t != null)
                .map(t -> t.getTitle() + " (due " + DateFormats.relative(t.getDueDate()) + ")")
                .collect(Collectors.toList());
        if (titles.isEmpty()) return;
        String lead = titles.size() == 1 ? "Reminder for today: " : "Reminders for today: ";
        taskController.showBanner(lead + String.join(", ", titles), Icons.BELL);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
