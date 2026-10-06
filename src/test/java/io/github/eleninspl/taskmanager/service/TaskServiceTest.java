package io.github.eleninspl.taskmanager.service;

import io.github.eleninspl.taskmanager.model.Category;
import io.github.eleninspl.taskmanager.model.Priority;
import io.github.eleninspl.taskmanager.model.Reminder;
import io.github.eleninspl.taskmanager.model.Task;
import io.github.eleninspl.taskmanager.model.enums.ReminderType;
import io.github.eleninspl.taskmanager.model.enums.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskServiceTest {

    private static final LocalDate TODAY = LocalDate.now();

    private ReminderService reminderService;
    private TaskService taskService;
    private Category work;
    private Priority high;

    @BeforeEach
    void setUp() {
        reminderService = new ReminderService();
        taskService = new TaskService(reminderService);
        work = new Category("Work");
        high = new Priority("High");
    }

    private Task task(String title, LocalDate dueDate, TaskStatus status) {
        return taskService.addTask(new Task(title, "", work, high, dueDate, status));
    }

    private Task findTask(String id) {
        return taskService.getTasks().stream().filter(t -> t.getId().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void overdueOpenTaskBecomesDelayedAndLosesItsReminders() {
        Task overdue = task("Overdue", TODAY.minusDays(2), TaskStatus.OPEN);
        reminderService.addReminder(new Reminder(overdue.getId(), ReminderType.SPECIFIC_DATE, TODAY.minusDays(3)));

        taskService.checkOverdueTasks();

        assertEquals(TaskStatus.DELAYED, findTask(overdue.getId()).getStatus());
        assertTrue(reminderService.getReminders().isEmpty());
    }

    @Test
    void overdueCheckLeavesCompletedAndFutureTasksAlone() {
        Task done = task("Done", TODAY.minusDays(5), TaskStatus.COMPLETED);
        Task future = task("Future", TODAY.plusDays(5), TaskStatus.IN_PROGRESS);

        taskService.checkOverdueTasks();

        assertEquals(TaskStatus.COMPLETED, findTask(done.getId()).getStatus());
        assertEquals(TaskStatus.IN_PROGRESS, findTask(future.getId()).getStatus());
    }

    @Test
    void completingTaskRemovesItsReminders() {
        Task t = task("Report", TODAY.plusDays(10), TaskStatus.OPEN);
        reminderService.createReminderForTask(t, ReminderType.ONE_DAY_BEFORE, null);

        taskService.updateTask(new Task(t.getId(), t.getTitle(), "", work, high, t.getDueDate(), TaskStatus.COMPLETED));

        assertTrue(reminderService.getReminders().isEmpty());
    }

    @Test
    void changingDueDateRecalculatesRelativeReminders() {
        Task t = task("Report", TODAY.plusDays(10), TaskStatus.OPEN);
        reminderService.createReminderForTask(t, ReminderType.ONE_DAY_BEFORE, null);
        LocalDate newDueDate = TODAY.plusDays(20);

        taskService.updateTask(new Task(t.getId(), t.getTitle(), "", work, high, newDueDate, TaskStatus.OPEN));

        assertEquals(newDueDate.minusDays(1), reminderService.getReminders().get(0).getReminderDate());
    }

    @Test
    void movingDueDateEarlierDropsRemindersThatWouldFallInThePast() {
        Task t = task("Report", TODAY.plusDays(30), TaskStatus.OPEN);
        reminderService.createReminderForTask(t, ReminderType.ONE_WEEK_BEFORE, null);

        taskService.updateTask(new Task(t.getId(), t.getTitle(), "", work, high, TODAY.plusDays(3), TaskStatus.OPEN));

        assertTrue(reminderService.getReminders().isEmpty());
    }

    @Test
    void deletingTaskDeletesItsReminders() {
        Task keep = task("Keep", TODAY.plusDays(10), TaskStatus.OPEN);
        Task remove = task("Remove", TODAY.plusDays(10), TaskStatus.OPEN);
        reminderService.createReminderForTask(keep, ReminderType.ONE_DAY_BEFORE, null);
        reminderService.createReminderForTask(remove, ReminderType.ONE_DAY_BEFORE, null);

        taskService.deleteTask(remove.getId());

        assertEquals(List.of(keep), List.copyOf(taskService.getTasks()));
        assertEquals(1, reminderService.getReminders().size());
        assertEquals(keep.getId(), reminderService.getReminders().get(0).getTaskId());
    }

    @Test
    void linkReferencesPointsTasksAtSharedCategoryAndPriorityInstances() {
        CategoryService categories = new CategoryService();
        PriorityService priorities = new PriorityService();
        Category shared = categories.addCategory("Work");
        Priority sharedHigh = priorities.addPriority("High");
        // Copies with the same IDs, as Gson produces when reading tasks.json
        Task loaded = task("Loaded", TODAY.plusDays(3), TaskStatus.OPEN);
        loaded.setCategory(new Category(shared.getId(), "Work"));
        loaded.setPriority(new Priority(sharedHigh.getId(), "High"));

        taskService.linkReferences(categories, priorities);
        categories.updateCategory(shared.getId(), "Office", taskService);

        assertSame(shared, loaded.getCategory());
        assertSame(sharedHigh, loaded.getPriority());
        assertEquals("Office", loaded.getCategory().getName());
    }

    @Test
    void linkReferencesFallsBackToDefaultsForUnknownReferences() {
        CategoryService categories = new CategoryService();
        PriorityService priorities = new PriorityService();
        Task orphan = task("Orphan", TODAY.plusDays(3), TaskStatus.OPEN);
        orphan.setCategory(new Category("missing-id", "Deleted category"));
        orphan.setPriority(null);

        taskService.linkReferences(categories, priorities);

        assertSame(categories.getNoCategory(), orphan.getCategory());
        assertSame(priorities.getDefaultPriority(), orphan.getPriority());
    }
}
