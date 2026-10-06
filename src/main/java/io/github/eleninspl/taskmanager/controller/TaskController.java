package io.github.eleninspl.taskmanager.controller;

import io.github.eleninspl.taskmanager.controller.dialog.TaskDialog;
import io.github.eleninspl.taskmanager.model.Category;
import io.github.eleninspl.taskmanager.model.Priority;
import io.github.eleninspl.taskmanager.model.Task;
import io.github.eleninspl.taskmanager.model.enums.TaskStatus;
import io.github.eleninspl.taskmanager.service.CategoryService;
import io.github.eleninspl.taskmanager.service.DateFormats;
import io.github.eleninspl.taskmanager.service.PriorityService;
import io.github.eleninspl.taskmanager.service.ReminderService;
import io.github.eleninspl.taskmanager.service.TaskQuery;
import io.github.eleninspl.taskmanager.service.TaskService;
import io.github.eleninspl.taskmanager.ui.Dialogs;
import io.github.eleninspl.taskmanager.ui.Icons;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * The task list: tasks shown as cards, grouped by due date or by category, filtered by the
 * drawer chosen in the sidebar and by the search bar (title and priority).
 */
public class TaskController {

    /** A row in the list: either a section heading or a task card. */
    private sealed interface Row permits SectionRow, TaskRow {}
    private record SectionRow(TaskQuery.Section section, boolean first) implements Row {}
    private record TaskRow(Task task) implements Row {}

    private static final Priority NO_FILTER = null;

    private final TaskService taskService;
    private final CategoryService categoryService;
    private final PriorityService priorityService;
    private final ReminderService reminderService;
    private final Consumer<Task> addReminderAction;

    private final TaskQuery query = new TaskQuery();
    private final ObservableList<Row> rows = FXCollections.observableArrayList();
    private final ListView<Row> list = new ListView<>(rows);
    private final Label title = new Label();
    private final Label subtitle = new Label();
    private final TextField searchField = new TextField();
    private final ComboBox<Priority> priorityFilter = new ComboBox<>();
    private final CheckBox showCompleted = new CheckBox("Show completed");
    private final ToggleButton byDate = new ToggleButton("By date");
    private final ToggleButton byCategory = new ToggleButton("By category");
    private final VBox banners = new VBox(8);
    private final StackPane emptyState = new StackPane();
    private final VBox view;

    /**
     * @param taskService       tasks
     * @param categoryService   categories
     * @param priorityService   priority levels
     * @param reminderService   reminders (for counts on the cards)
     * @param addReminderAction opens the reminder dialog for a task
     */
    public TaskController(TaskService taskService, CategoryService categoryService, PriorityService priorityService,
                          ReminderService reminderService, Consumer<Task> addReminderAction) {
        this.taskService = taskService;
        this.categoryService = categoryService;
        this.priorityService = priorityService;
        this.reminderService = reminderService;
        this.addReminderAction = addReminderAction;

        title.getStyleClass().add("view-title");
        subtitle.getStyleClass().add("view-subtitle");
        VBox heading = new VBox(2, title, subtitle);

        banners.managedProperty().bind(javafx.beans.binding.Bindings.isNotEmpty(banners.getChildren()));
        VBox top = new VBox(14, heading, banners, buildFilterBar());
        top.setStyle("-fx-padding: 22 20 14 20;");

        list.getStyleClass().add("card-list");
        list.setCellFactory(lv -> new CardCell());
        list.setPlaceholder(emptyState);
        list.addEventFilter(KeyEvent.KEY_PRESSED, this::handleListKeys);
        // Section headings are not selectable; step over them
        list.getSelectionModel().selectedItemProperty().addListener((obs, old, row) -> {
            if (row instanceof SectionRow) {
                int i = list.getSelectionModel().getSelectedIndex();
                int next = (old != null && rows.indexOf(old) > i) ? i - 1 : i + 1;
                if (next >= 0 && next < rows.size()) list.getSelectionModel().select(next);
                else list.getSelectionModel().clearSelection();
            }
        });
        VBox.setVgrow(list, javafx.scene.layout.Priority.ALWAYS);

        view = new VBox(top, list);

        InvalidationListener refresh = obs -> refresh();
        taskService.getTasks().addListener(refresh);
        categoryService.getCategories().addListener(refresh);
        priorityService.getPriorities().addListener(refresh);
        reminderService.getReminders().addListener(refresh);
        refresh();
    }

    /** @return the view's root node */
    public Node getView() {
        return view;
    }

    /**
     * Opens a drawer chosen in the sidebar or the summary bar.
     *
     * @param scope    which drawer
     * @param category the category, when scope is CATEGORY
     */
    public void showScope(TaskQuery.Scope scope, Category category) {
        query.setScope(scope, category);
        refresh();
    }

    /** Shows completed tasks in the current drawer. */
    public void revealCompleted() {
        showCompleted.setSelected(true);
    }

    /** Moves keyboard focus to the task list. */
    public void focusList() {
        list.requestFocus();
    }

    /** Moves keyboard focus to the search field. */
    public void focusSearch() {
        searchField.requestFocus();
        searchField.selectAll();
    }

    /** Opens the new-task dialog, preselecting the open category drawer if there is one. */
    public void newTask() {
        new TaskDialog(null, query.getCategory(), categoryService, priorityService)
                .showAndWait()
                .ifPresent(task -> {
                    taskService.addTask(task);
                    select(task.getId());
                });
    }

    /**
     * Shows a dismissible banner above the list.
     *
     * @param message banner text
     * @param icon    icon path from {@link Icons}
     */
    public void showBanner(String message, String icon) {
        Label text = new Label(message);
        text.setWrapText(true);
        HBox.setHgrow(text, javafx.scene.layout.Priority.ALWAYS);
        text.setMaxWidth(Double.MAX_VALUE);
        Button dismiss = new Button();
        dismiss.setGraphic(Icons.icon(Icons.CLOSE, 14));
        dismiss.getStyleClass().add("quiet");
        dismiss.setTooltip(new Tooltip("Dismiss"));
        dismiss.setAccessibleText("Dismiss");
        HBox banner = new HBox(Icons.icon(icon), text, dismiss);
        banner.getStyleClass().add("banner");
        dismiss.setOnAction(e -> banners.getChildren().remove(banner));
        banners.getChildren().add(banner);
    }

    // ---------------------------------------------------------------- filter bar

    private Node buildFilterBar() {
        searchField.setPromptText("Search titles");
        searchField.getStyleClass().add("search-field");
        searchField.setPrefWidth(260);
        searchField.setMinWidth(120);
        searchField.setAccessibleText("Search task titles");
        searchField.textProperty().addListener((o, a, text) -> {
            query.setTitleText(text);
            refresh();
        });
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) searchField.clear();
            if (e.getCode() == KeyCode.DOWN || e.getCode() == KeyCode.ENTER) {
                list.requestFocus();
                selectFirstTask();
            }
        });
        Node searchIcon = Icons.icon(Icons.SEARCH, 15);
        StackPane search = new StackPane(searchField, searchIcon);
        StackPane.setAlignment(searchIcon, Pos.CENTER_LEFT);
        searchIcon.setTranslateX(10);

        priorityFilter.setItems(priorityOptions());
        priorityService.getPriorities().addListener((InvalidationListener) obs -> {
            Priority selected = priorityFilter.getValue();
            priorityFilter.setItems(priorityOptions());
            priorityFilter.setValue(priorityService.getPriorities().contains(selected) ? selected : NO_FILTER);
        });
        priorityFilter.setPromptText("Any priority");
        priorityFilter.setButtonCell(new PriorityCell());
        priorityFilter.setCellFactory(lv -> new PriorityCell());
        priorityFilter.setAccessibleText("Filter by priority");
        priorityFilter.valueProperty().addListener((o, a, p) -> {
            query.setPriority(p);
            refresh();
        });

        ToggleGroup grouping = new ToggleGroup();
        byDate.setToggleGroup(grouping);
        byCategory.setToggleGroup(grouping);
        byDate.getStyleClass().add("left-pill");
        byCategory.getStyleClass().add("right-pill");
        byDate.setSelected(true);
        grouping.selectedToggleProperty().addListener((o, old, now) -> {
            if (now == null) {
                old.setSelected(true); // one grouping is always active
                return;
            }
            query.setGrouping(now == byDate ? TaskQuery.Grouping.BY_DATE : TaskQuery.Grouping.BY_CATEGORY);
            refresh();
        });
        HBox segmented = new HBox(byDate, byCategory);
        segmented.getStyleClass().add("segmented");

        showCompleted.selectedProperty().addListener((o, a, show) -> {
            query.setShowCompleted(show);
            refresh();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        search.setMaxWidth(320);
        search.setMinWidth(120);
        HBox.setHgrow(search, javafx.scene.layout.Priority.SOMETIMES);
        priorityFilter.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        segmented.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        showCompleted.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        HBox bar = new HBox(10, search, priorityFilter, segmented, spacer, showCompleted);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private ObservableList<Priority> priorityOptions() {
        ObservableList<Priority> options = FXCollections.observableArrayList();
        options.add(NO_FILTER);
        options.addAll(priorityService.getPriorities());
        return options;
    }

    private static class PriorityCell extends ListCell<Priority> {
        @Override
        protected void updateItem(Priority p, boolean empty) {
            super.updateItem(p, empty);
            setText(empty ? null : p == null ? "Any priority" : p.getName());
        }
    }

    // ---------------------------------------------------------------- list content

    private void refresh() {
        String selectedId = list.getSelectionModel().getSelectedItem() instanceof TaskRow r ? r.task().getId() : null;

        List<TaskQuery.Section> sections = query.sections(taskService.getTasks(), categoryService.getCategories());
        List<Row> next = new ArrayList<>();
        for (TaskQuery.Section s : sections) {
            next.add(new SectionRow(s, next.isEmpty()));
            s.tasks().forEach(t -> next.add(new TaskRow(t)));
        }
        rows.setAll(next);
        updateHeading();
        updateEmptyState();
        if (selectedId != null) select(selectedId);
        // Rows have different heights; without an anchor the list can open mid-way, so start at the top
        if (list.getSelectionModel().isEmpty() && !rows.isEmpty()) list.scrollTo(0);
    }

    private void updateHeading() {
        title.setText(switch (query.getScope()) {
            case ALL -> "All tasks";
            case OVERDUE -> "Overdue";
            case NEXT_7_DAYS -> "Next 7 days";
            case CATEGORY -> query.getCategory().getName();
        });
        TaskQuery scopeOnly = new TaskQuery();
        scopeOnly.setScope(query.getScope(), query.getCategory());
        long open = taskService.getTasks().stream().filter(scopeOnly::matches)
                .filter(t -> t.getStatus() != TaskStatus.COMPLETED).count();
        long done = taskService.getTasks().stream().filter(scopeOnly::matches)
                .filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
        subtitle.setText(open + " open" + (done > 0 ? " · " + done + " completed" : ""));
    }

    private void updateEmptyState() {
        VBox box = new VBox();
        box.getStyleClass().add("empty-state");
        Label heading = new Label();
        heading.getStyleClass().add("empty-title");
        Label body = new Label();
        body.getStyleClass().add("empty-body");
        body.setMaxWidth(420);
        box.getChildren().addAll(heading, body);

        boolean noTasksAtAll = taskService.getTasks().isEmpty();
        if (noTasksAtAll) {
            heading.setText("No tasks yet");
            body.setText("Add a task with a due date, and it will be filed here by when it's due.");
            box.getChildren().add(action("New task", true, this::newTask));
        } else if (query.hasSearchCriteria()) {
            heading.setText("No tasks match your search");
            body.setText("Try a different title, or search with any priority.");
            box.getChildren().add(action("Clear search", false, () -> {
                searchField.clear();
                priorityFilter.setValue(NO_FILTER);
            }));
        } else if (query.getScope() == TaskQuery.Scope.OVERDUE) {
            heading.setText("Nothing overdue");
            body.setText("Every unfinished task is still within its due date.");
        } else if (query.getScope() == TaskQuery.Scope.NEXT_7_DAYS) {
            heading.setText("Nothing due in the next 7 days");
            body.setText("Tasks due from today up to a week ahead appear here.");
        } else if (query.getScope() == TaskQuery.Scope.CATEGORY && !hasAnyInScope()) {
            boolean builtIn = query.getCategory() == categoryService.getNoCategory();
            heading.setText(builtIn ? "No uncategorized tasks" : "No tasks in " + query.getCategory().getName() + " yet");
            body.setText(builtIn ? "Tasks without a category are filed here."
                    : "Tasks you file under this category appear here.");
            box.getChildren().add(action(builtIn ? "New task" : "New task in " + query.getCategory().getName(),
                    true, this::newTask));
        } else {
            heading.setText("All done here");
            body.setText("Every task in this view is completed.");
            box.getChildren().add(action("Show completed", false, () -> showCompleted.setSelected(true)));
        }
        emptyState.getChildren().setAll(box);
    }

    private boolean hasAnyInScope() {
        TaskQuery scopeOnly = new TaskQuery();
        scopeOnly.setScope(query.getScope(), query.getCategory());
        return taskService.getTasks().stream().anyMatch(scopeOnly::matches);
    }

    private static Button action(String label, boolean primary, Runnable run) {
        Button b = new Button(label);
        if (primary) b.getStyleClass().add("primary");
        b.setOnAction(e -> run.run());
        VBox.setMargin(b, new javafx.geometry.Insets(8, 0, 0, 0));
        return b;
    }

    private void select(String taskId) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i) instanceof TaskRow r && r.task().getId().equals(taskId)) {
                list.getSelectionModel().select(i);
                list.scrollTo(Math.max(0, i - 1));
                return;
            }
        }
    }

    private void selectFirstTask() {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i) instanceof TaskRow) {
                list.getSelectionModel().select(i);
                return;
            }
        }
    }

    // ---------------------------------------------------------------- actions

    private Task selectedTask() {
        return list.getSelectionModel().getSelectedItem() instanceof TaskRow r ? r.task() : null;
    }

    private void handleListKeys(KeyEvent e) {
        Task task = selectedTask();
        if (task == null) return;
        switch (e.getCode()) {
            case ENTER -> { edit(task); e.consume(); }
            case SPACE -> { toggleCompleted(task); e.consume(); }
            case DELETE, BACK_SPACE -> { delete(task); e.consume(); }
            default -> { }
        }
    }

    private void edit(Task task) {
        new TaskDialog(task, null, categoryService, priorityService)
                .showAndWait()
                .ifPresent(updated -> {
                    taskService.updateTask(updated);
                    select(updated.getId());
                });
    }

    private void toggleCompleted(Task task) {
        taskService.setCompleted(task, task.getStatus() != TaskStatus.COMPLETED);
        select(task.getId());
    }

    private void delete(Task task) {
        long reminders = reminderCount(task);
        String detail = reminders == 0 ? "The task will be deleted."
                : "The task and its " + (reminders == 1 ? "reminder" : reminders + " reminders") + " will be deleted.";
        if (Dialogs.confirm("Delete “" + task.getTitle() + "”?", detail + " This can't be undone.", "Delete task")) {
            taskService.deleteTask(task.getId());
        }
    }

    private long reminderCount(Task task) {
        return reminderService.getReminders().stream().filter(r -> r.getTaskId().equals(task.getId())).count();
    }

    private ContextMenu contextMenu(Task task) {
        boolean completed = task.getStatus() == TaskStatus.COMPLETED;
        MenuItem edit = new MenuItem("Edit…");
        edit.setOnAction(e -> edit(task));
        MenuItem toggle = new MenuItem(completed ? "Mark as not completed" : "Mark as completed");
        toggle.setOnAction(e -> toggleCompleted(task));
        MenuItem remind = new MenuItem("Add reminder…");
        remind.setDisable(completed || TaskQuery.isOverdue(task));
        remind.setOnAction(e -> addReminderAction.accept(task));
        MenuItem delete = new MenuItem("Delete…");
        delete.getStyleClass().add("danger");
        delete.setOnAction(e -> delete(task));
        return new ContextMenu(edit, toggle, remind, new SeparatorMenuItem(), delete);
    }

    // ---------------------------------------------------------------- cards

    private class CardCell extends ListCell<Row> {

        CardCell() {
            setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && getItem() instanceof TaskRow r) {
                    edit(r.task());
                }
            });
        }

        @Override
        protected void updateItem(Row row, boolean empty) {
            super.updateItem(row, empty);
            getStyleClass().removeAll("section-cell", "first-section");
            setText(null);
            setContextMenu(null);
            setMouseTransparent(false);
            setAccessibleText(null);
            if (empty || row == null) {
                setGraphic(null);
            } else if (row instanceof SectionRow s) {
                getStyleClass().add("section-cell");
                if (s.first()) getStyleClass().add("first-section");
                setGraphic(sectionHeading(s.section()));
                setMouseTransparent(true);
                setFocusTraversable(false);
            } else if (row instanceof TaskRow t) {
                setGraphic(card(t.task()));
                setContextMenu(contextMenu(t.task()));
                setAccessibleText(describe(t.task()));
            }
        }

        private Node sectionHeading(TaskQuery.Section section) {
            Label name = new Label(section.title());
            name.getStyleClass().add("section-title");
            Label count = new Label(String.valueOf(section.tasks().size()));
            count.getStyleClass().add("section-count");
            HBox tab = new HBox(name, count);
            tab.getStyleClass().add("divider-tab");
            if (section.overdue()) {
                tab.getStyleClass().add("overdue");
                name.getStyleClass().add("overdue");
            }
            tab.setAlignment(Pos.BASELINE_LEFT);
            tab.setMaxWidth(Region.USE_PREF_SIZE);
            HBox row = new HBox(tab);
            row.setStyle("-fx-padding: 0 0 0 10;");
            return row;
        }

        private Node card(Task task) {
            boolean completed = task.getStatus() == TaskStatus.COMPLETED;
            boolean overdue = TaskQuery.isOverdue(task);

            CheckBox check = new CheckBox();
            check.setSelected(completed);
            check.setAccessibleText(completed ? "Mark as not completed" : "Mark as completed");
            check.setTooltip(new Tooltip(completed ? "Mark as not completed" : "Mark as completed"));
            check.setOnAction(e -> taskService.setCompleted(task, check.isSelected()));

            Label taskTitle = new Label(task.getTitle());
            taskTitle.getStyleClass().add("card-title");
            VBox text = new VBox(3, taskTitle);
            if (task.getDescription() != null && !task.getDescription().isBlank()) {
                Label note = new Label(task.getDescription());
                note.getStyleClass().add("card-note");
                text.getChildren().add(note);
            }
            HBox meta = metaLine(task, overdue);
            if (!meta.getChildren().isEmpty()) text.getChildren().add(meta);
            HBox.setHgrow(text, javafx.scene.layout.Priority.ALWAYS);
            text.setMinWidth(0);

            Label stamp = new Label(stampText(task, completed));
            stamp.getStyleClass().add("stamp");
            if (completed) stamp.getStyleClass().add("done");
            else if (overdue) stamp.getStyleClass().add("overdue");
            else if (TaskQuery.isDueWithinWeek(task)) stamp.getStyleClass().add("soon");
            stamp.setMinWidth(Region.USE_PREF_SIZE);
            stamp.setTooltip(new Tooltip("Due " + DateFormats.full(task.getDueDate())));

            HBox card = new HBox(check, text, stamp);
            card.setAlignment(Pos.CENTER_LEFT);
            card.getStyleClass().add("card");
            if (completed) card.getStyleClass().add("completed");
            else if (overdue) card.getStyleClass().add("overdue");
            return card;
        }

        private HBox metaLine(Task task, boolean overdue) {
            HBox meta = new HBox(10);
            meta.setAlignment(Pos.CENTER_LEFT);
            if (overdue) {
                long days = ChronoUnit.DAYS.between(task.getDueDate(), LocalDate.now());
                Label late = new Label(DateFormats.days(days) + " overdue · marked Delayed automatically");
                late.getStyleClass().add("meta-strong");
                meta.getChildren().add(late);
            }
            if (task.getStatus() == TaskStatus.IN_PROGRESS || task.getStatus() == TaskStatus.POSTPONED) {
                meta.getChildren().add(clip(task.getStatus().toString(), true));
            }
            boolean showCategory = query.getScope() != TaskQuery.Scope.CATEGORY
                    && query.getGrouping() != TaskQuery.Grouping.BY_CATEGORY;
            if (showCategory && task.getCategory() != null && task.getCategory() != categoryService.getNoCategory()) {
                Label category = new Label(task.getCategory().getName(), Icons.icon(Icons.FOLDER, 13));
                category.getStyleClass().add("meta");
                category.setGraphicTextGap(5);
                meta.getChildren().add(category);
            }
            if (task.getPriority() != null && task.getPriority() != priorityService.getDefaultPriority()) {
                meta.getChildren().add(clip(task.getPriority().getName(), false));
            }
            long reminders = reminderCount(task);
            if (reminders > 0) {
                Label bell = new Label(String.valueOf(reminders), Icons.icon(Icons.BELL, 13));
                bell.getStyleClass().add("meta");
                bell.setGraphicTextGap(4);
                bell.setTooltip(new Tooltip(reminders == 1 ? "1 reminder" : reminders + " reminders"));
                meta.getChildren().add(bell);
            }
            return meta;
        }

        private Label clip(String text, boolean status) {
            Label clip = new Label(text);
            clip.getStyleClass().add("clip");
            if (status) clip.getStyleClass().add("status");
            return clip;
        }

        private String stampText(Task task, boolean completed) {
            if (completed) return "DONE";
            return DateFormats.relative(task.getDueDate()).toUpperCase(Locale.ENGLISH);
        }

        private String describe(Task task) {
            StringBuilder sb = new StringBuilder(task.getTitle());
            sb.append(", due ").append(DateFormats.full(task.getDueDate()));
            sb.append(", ").append(task.getStatus());
            if (task.getPriority() != null) sb.append(", priority ").append(task.getPriority().getName());
            if (task.getCategory() != null) sb.append(", category ").append(task.getCategory().getName());
            return sb.toString();
        }
    }
}
