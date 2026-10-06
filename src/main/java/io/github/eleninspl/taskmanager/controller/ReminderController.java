package io.github.eleninspl.taskmanager.controller;

import io.github.eleninspl.taskmanager.controller.dialog.ReminderDialog;
import io.github.eleninspl.taskmanager.model.Reminder;
import io.github.eleninspl.taskmanager.model.Task;
import io.github.eleninspl.taskmanager.service.DateFormats;
import io.github.eleninspl.taskmanager.service.ReminderService;
import io.github.eleninspl.taskmanager.service.TaskQuery;
import io.github.eleninspl.taskmanager.service.TaskService;
import io.github.eleninspl.taskmanager.ui.Dialogs;
import io.github.eleninspl.taskmanager.ui.Icons;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;

/**
 * Lists every active reminder, soonest first, and lets the user add, edit and delete them.
 */
public class ReminderController {

    private final ReminderService reminderService;
    private final TaskService taskService;
    private final FilteredList<Task> remindableTasks;
    private final TableView<Reminder> table = new TableView<>();
    private final Label subtitle = new Label();
    private final VBox view;

    /**
     * @param reminderService reminders
     * @param taskService     tasks the reminders belong to
     */
    public ReminderController(ReminderService reminderService, TaskService taskService) {
        this.reminderService = reminderService;
        this.taskService = taskService;
        // Completed and overdue tasks can't take new reminders
        this.remindableTasks = new FilteredList<>(taskService.getTasks(),
                t -> t.getStatus() != io.github.eleninspl.taskmanager.model.enums.TaskStatus.COMPLETED && !TaskQuery.isOverdue(t));

        Button add = new Button("New reminder", Icons.icon(Icons.PLUS, 14));
        add.getStyleClass().add("primary");
        add.setOnAction(e -> openReminderDialog(null, null));
        Button edit = new Button("Edit");
        edit.setOnAction(e -> edit());
        Button delete = new Button("Delete");
        delete.setOnAction(e -> delete());
        edit.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        delete.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        Node header = ViewHeader.create(new Label("Reminders"), subtitle, edit, delete, add);

        TableColumn<Reminder, String> taskCol = new TableColumn<>("Task");
        taskCol.setCellValueFactory(c -> new SimpleStringProperty(task(c.getValue()).map(Task::getTitle).orElse("")));
        TableColumn<Reminder, String> typeCol = new TableColumn<>("Remind me");
        typeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType().toString()));
        TableColumn<Reminder, LocalDate> dateCol = new TableColumn<>("On");
        dateCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getReminderDate()));
        dateCol.setCellFactory(col -> dateCell());
        TableColumn<Reminder, LocalDate> dueCol = new TableColumn<>("Task due");
        dueCol.setCellValueFactory(c -> new SimpleObjectProperty<>(task(c.getValue()).map(Task::getDueDate).orElse(null)));
        dueCol.setCellFactory(col -> dateCell());
        table.getColumns().setAll(java.util.List.of(taskCol, typeCol, dateCol, dueCol));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        taskCol.setMaxWidth(1f * Integer.MAX_VALUE * 40);
        typeCol.setMaxWidth(1f * Integer.MAX_VALUE * 20);
        dateCol.setMaxWidth(1f * Integer.MAX_VALUE * 20);
        dueCol.setMaxWidth(1f * Integer.MAX_VALUE * 20);

        SortedList<Reminder> sorted = new SortedList<>(reminderService.getReminders(),
                Comparator.comparing(Reminder::getReminderDate));
        table.setItems(sorted);
        table.setRowFactory(tv -> {
            TableRow<Reminder> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !row.isEmpty()) edit();
            });
            return row;
        });
        table.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) edit();
            if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) delete();
        });
        table.setPlaceholder(emptyState());

        StackPane panel = new StackPane(table);
        panel.getStyleClass().add("panel");
        VBox.setVgrow(panel, Priority.ALWAYS);
        view = new VBox(16, header, panel);
        view.setStyle("-fx-padding: 22 20 20 20;");

        InvalidationListener update = obs -> {
            long count = reminderService.getReminders().size();
            subtitle.setText(count == 0 ? "None set" : count == 1 ? "1 reminder" : count + " reminders");
            table.refresh();
        };
        reminderService.getReminders().addListener(update);
        taskService.getTasks().addListener(update);
        update.invalidated(null);
    }

    /** @return the view's root node */
    public Node getView() {
        return view;
    }

    /**
     * Opens the reminder dialog and saves the result.
     *
     * @param existing   reminder to edit, or null to create one
     * @param presetTask task to preselect for a new reminder, or null
     */
    public void openReminderDialog(Reminder existing, Task presetTask) {
        if (existing == null && remindableTasks.isEmpty()) {
            Dialogs.info("No tasks can take a reminder",
                    "Reminders need a task that isn't completed or overdue. Add a task with a future due date first.");
            return;
        }
        new ReminderDialog(remindableTasks, existing, presetTask, reminderService)
                .showAndWait()
                .ifPresent(draft -> {
                    try {
                        if (existing == null) {
                            reminderService.createReminderForTask(draft.task(), draft.type(), draft.specificDate());
                        } else {
                            reminderService.updateReminderForTask(existing.getId(), draft.task(), draft.type(), draft.specificDate());
                        }
                    } catch (IllegalArgumentException ex) {
                        Dialogs.info("The reminder wasn't saved", ex.getMessage());
                    }
                });
    }

    private void edit() {
        Reminder selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) openReminderDialog(selected, null);
    }

    private void delete() {
        Reminder selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        String taskTitle = task(selected).map(Task::getTitle).orElse("this task");
        if (Dialogs.confirm("Delete this reminder?",
                "The " + selected.getType().toString().toLowerCase() + " reminder for “" + taskTitle
                        + "” on " + DateFormats.full(selected.getReminderDate()) + " will be deleted.",
                "Delete reminder")) {
            reminderService.deleteReminder(selected.getId());
        }
    }

    private Optional<Task> task(Reminder reminder) {
        return taskService.getTasks().stream().filter(t -> t.getId().equals(reminder.getTaskId())).findFirst();
    }

    private static javafx.scene.control.TableCell<Reminder, LocalDate> dateCell() {
        javafx.scene.control.TableCell<Reminder, LocalDate> cell = new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setText(empty || date == null ? null : DateFormats.relative(date));
            }
        };
        cell.getStyleClass().add("date-cell");
        return cell;
    }

    private Node emptyState() {
        Label heading = new Label("No reminders");
        heading.getStyleClass().add("empty-title");
        Label body = new Label("Set a reminder a day, a week or a month before a task is due, or on a date you choose.");
        body.getStyleClass().add("empty-body");
        body.setMaxWidth(380);
        Button add = new Button("New reminder");
        add.getStyleClass().add("primary");
        add.setOnAction(e -> openReminderDialog(null, null));
        VBox box = new VBox(heading, body, add);
        box.getStyleClass().add("empty-state");
        VBox.setMargin(add, new javafx.geometry.Insets(8, 0, 0, 0));
        return box;
    }
}
