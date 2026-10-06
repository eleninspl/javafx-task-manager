package io.github.eleninspl.taskmanager.controller;

import io.github.eleninspl.taskmanager.model.Category;
import io.github.eleninspl.taskmanager.model.Task;
import io.github.eleninspl.taskmanager.model.enums.TaskStatus;
import io.github.eleninspl.taskmanager.service.CategoryService;
import io.github.eleninspl.taskmanager.service.ReminderService;
import io.github.eleninspl.taskmanager.service.TaskQuery;
import io.github.eleninspl.taskmanager.service.TaskService;
import io.github.eleninspl.taskmanager.ui.Icons;
import javafx.beans.InvalidationListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * The drawer rail: the task drawers (All, Overdue, Next 7 days, one per category) with their
 * counts, followed by the management screens. Exactly one item is always selected.
 */
public class Sidebar {

    /** Keys for the fixed items; category drawers use {@code "cat:" + id}. */
    public static final String ALL = "all", OVERDUE = "overdue", NEXT_7 = "next7",
            REMINDERS = "reminders", CATEGORIES = "categories", PRIORITIES = "priorities";

    private final TaskService taskService;
    private final CategoryService categoryService;
    private final ReminderService reminderService;
    private final ToggleGroup group = new ToggleGroup();
    private final VBox categoryDrawers = new VBox(2);
    private final VBox fixedDrawers = new VBox(2);
    private final VBox manageItems = new VBox(2);
    private final ScrollPane view;
    private Consumer<String> onSelect = key -> { };
    private String selectedKey = ALL;

    /**
     * @param taskService     tasks, for counts
     * @param categoryService categories, one drawer each
     * @param reminderService reminders, for the count
     * @param newTask         action of the New task button
     */
    public Sidebar(TaskService taskService, CategoryService categoryService, ReminderService reminderService,
                   Runnable newTask) {
        this.taskService = taskService;
        this.categoryService = categoryService;
        this.reminderService = reminderService;

        Button add = new Button("New task", Icons.icon(Icons.PLUS, 14));
        add.getStyleClass().add("primary");
        add.setMaxWidth(Double.MAX_VALUE);
        add.setOnAction(e -> newTask.run());
        add.setTooltip(new javafx.scene.control.Tooltip("New task (" + shortcut() + "N)"));
        VBox.setMargin(add, new javafx.geometry.Insets(0, 0, 12, 0));

        VBox content = new VBox(2, add, fixedDrawers, groupLabel("Categories"), categoryDrawers,
                groupLabel("Manage"), manageItems);
        content.getStyleClass().add("sidebar");

        view = new ScrollPane(content);
        view.setFitToWidth(true);
        view.setFitToHeight(true);
        view.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        view.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        group.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null) {
                // Clicking the selected item again must not leave nothing selected
                if (old != null) old.setSelected(true);
                return;
            }
            String key = (String) now.getUserData();
            if (!key.equals(selectedKey)) {
                selectedKey = key;
                onSelect.accept(key);
            }
        });

        InvalidationListener rebuild = obs -> rebuild();
        taskService.getTasks().addListener(rebuild);
        categoryService.getCategories().addListener(rebuild);
        reminderService.getReminders().addListener(rebuild);
        rebuild();
    }

    /** @return the sidebar's root node */
    public Node getView() {
        return view;
    }

    /** @param onSelect called with the key of the item the user selects */
    public void setOnSelect(Consumer<String> onSelect) {
        this.onSelect = onSelect;
    }

    /**
     * Selects an item programmatically and notifies the listener.
     *
     * @param key item key
     */
    public void select(String key) {
        for (Toggle t : group.getToggles()) {
            if (key.equals(t.getUserData())) {
                selectedKey = null; // force notification even if unchanged
                t.setSelected(true);
                if (selectedKey == null) { // already selected: listener didn't fire
                    selectedKey = key;
                    onSelect.accept(key);
                }
                return;
            }
        }
    }

    /** @return key of the selected item */
    public String getSelectedKey() {
        return selectedKey;
    }

    /**
     * @param key a category drawer key
     * @return the category, or null when the key is not a category drawer
     */
    public Category categoryFor(String key) {
        return key.startsWith("cat:") ? categoryService.getCategoryById(key.substring(4)) : null;
    }

    private void rebuild() {
        group.getToggles().clear();
        long overdue = count(TaskQuery::isOverdue);
        fixedDrawers.getChildren().setAll(
                drawer(ALL, "All tasks", Icons.ALL, count(t -> t.getStatus() != TaskStatus.COMPLETED), false),
                drawer(OVERDUE, "Overdue", Icons.OVERDUE, overdue, overdue > 0),
                drawer(NEXT_7, "Next 7 days", Icons.WEEK, count(TaskQuery::isDueWithinWeek), false));
        categoryDrawers.getChildren().clear();
        for (Category c : categoryService.getCategories()) {
            long open = count(t -> t.getStatus() != TaskStatus.COMPLETED && t.getCategory() != null
                    && t.getCategory().getId().equals(c.getId()));
            categoryDrawers.getChildren().add(drawer("cat:" + c.getId(), c.getName(), Icons.FOLDER, open, false));
        }
        manageItems.getChildren().setAll(
                drawer(REMINDERS, "Reminders", Icons.BELL, reminderService.getReminders().size(), false),
                drawer(CATEGORIES, "Edit categories", Icons.FOLDER, -1, false),
                drawer(PRIORITIES, "Edit priorities", Icons.FLAG, -1, false));

        // Keep the selection; if its category was deleted, fall back to All tasks
        boolean found = false;
        for (Toggle t : group.getToggles()) {
            if (t.getUserData().equals(selectedKey)) {
                t.setSelected(true);
                found = true;
            }
        }
        if (!found) select(ALL);
    }

    private long count(Predicate<Task> predicate) {
        return taskService.getTasks().stream().filter(predicate).count();
    }

    private ToggleButton drawer(String key, String name, String icon, long count, boolean alert) {
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("drawer-name");
        nameLabel.setMinWidth(0);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(10, Icons.icon(icon), nameLabel, spacer);
        row.setAlignment(Pos.CENTER_LEFT);
        if (count >= 0) {
            Label countLabel = new Label(count == 0 ? "" : String.valueOf(count));
            countLabel.getStyleClass().add("drawer-count");
            row.getChildren().add(countLabel);
        }
        ToggleButton button = new ToggleButton();
        button.setGraphic(row);
        button.setMaxWidth(Double.MAX_VALUE);
        row.prefWidthProperty().bind(button.widthProperty().subtract(20));
        button.getStyleClass().setAll("drawer");
        if (alert) button.getStyleClass().add("alert");
        button.setUserData(key);
        button.setToggleGroup(group);
        button.setAccessibleText(count > 0 ? name + ", " + count : name);
        return button;
    }

    private static Label groupLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-label");
        return label;
    }

    static String shortcut() {
        return System.getProperty("os.name", "").toLowerCase().contains("mac") ? "⌘" : "Ctrl+";
    }
}
