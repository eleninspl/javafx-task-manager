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

import static org.junit.jupiter.api.Assertions.*;

class ValidationAndEditingTest {

    private static final LocalDate TODAY = LocalDate.now();

    private ReminderService reminderService;
    private TaskService taskService;
    private CategoryService categoryService;
    private PriorityService priorityService;

    @BeforeEach
    void setUp() {
        reminderService = new ReminderService();
        taskService = new TaskService(reminderService);
        categoryService = new CategoryService();
        priorityService = new PriorityService();
    }

    private Task addTask(LocalDate dueDate, TaskStatus status) {
        return taskService.addTask(new Task("Task", "", categoryService.getNoCategory(),
                priorityService.getDefaultPriority(), dueDate, status));
    }

    @Test
    void editingReminderUpdatesItInPlaceInsteadOfAddingAnother() {
        Task task = addTask(TODAY.plusDays(20), TaskStatus.OPEN);
        Reminder reminder = reminderService.createReminderForTask(task, ReminderType.ONE_DAY_BEFORE, null);

        reminderService.updateReminderForTask(reminder.getId(), task, ReminderType.ONE_WEEK_BEFORE, null);

        assertEquals(1, reminderService.getReminders().size());
        Reminder updated = reminderService.getReminders().get(0);
        assertEquals(reminder.getId(), updated.getId());
        assertEquals(ReminderType.ONE_WEEK_BEFORE, updated.getType());
        assertEquals(TODAY.plusDays(13), updated.getReminderDate());
    }

    @Test
    void invalidReminderEditLeavesTheOriginalUntouched() {
        Task task = addTask(TODAY.plusDays(3), TaskStatus.OPEN);
        Reminder reminder = reminderService.createReminderForTask(task, ReminderType.ONE_DAY_BEFORE, null);

        assertThrows(IllegalArgumentException.class,
                () -> reminderService.updateReminderForTask(reminder.getId(), task, ReminderType.ONE_WEEK_BEFORE, null));

        assertEquals(ReminderType.ONE_DAY_BEFORE, reminderService.getReminders().get(0).getType());
    }

    @Test
    void validationErrorExplainsTheProblemWithoutChangingAnything() {
        Task task = addTask(TODAY.plusDays(3), TaskStatus.OPEN);

        assertNull(reminderService.validationError(task, ReminderType.ONE_DAY_BEFORE, null));
        assertNotNull(reminderService.validationError(task, ReminderType.ONE_WEEK_BEFORE, null));
        assertNotNull(reminderService.validationError(task, ReminderType.SPECIFIC_DATE, null));
        assertNotNull(reminderService.validationError(null, ReminderType.ONE_DAY_BEFORE, null));
        assertTrue(reminderService.getReminders().isEmpty());
    }

    @Test
    void categoryNamesMustBeNonBlankAndUnique() {
        Category work = categoryService.addCategory("Work");

        assertNotNull(categoryService.nameError("  ", null));
        assertNotNull(categoryService.nameError("work", null));
        assertNotNull(categoryService.nameError("No Category", null));
        assertNull(categoryService.nameError("Work", work.getId()));
        assertThrows(IllegalArgumentException.class, () -> categoryService.addCategory(""));
        assertThrows(IllegalArgumentException.class, () -> categoryService.addCategory("WORK"));
    }

    @Test
    void categoryNamesAreTrimmed() {
        Category home = categoryService.addCategory("  Home  ");

        assertEquals("Home", home.getName());
    }

    @Test
    void renamingCategoryToAnExistingNameIsRejected() {
        categoryService.addCategory("Work");
        Category home = categoryService.addCategory("Home");

        assertThrows(IllegalArgumentException.class,
                () -> categoryService.updateCategory(home.getId(), "Work", taskService));
        assertEquals("Home", home.getName());
    }

    @Test
    void priorityNamesMustBeNonBlankAndUnique() {
        Priority high = priorityService.addPriority("High");

        assertNotNull(priorityService.nameError("", null));
        assertNotNull(priorityService.nameError("default", null));
        assertNotNull(priorityService.nameError("high", null));
        assertNull(priorityService.nameError("High", high.getId()));
        assertThrows(IllegalArgumentException.class,
                () -> priorityService.updatePriority(high.getId(), " ", taskService));
    }

    @Test
    void completingTaskRemovesRemindersAndReopeningRestoresTheRightStatus() {
        Task future = addTask(TODAY.plusDays(10), TaskStatus.IN_PROGRESS);
        reminderService.createReminderForTask(future, ReminderType.ONE_DAY_BEFORE, null);

        taskService.setCompleted(future, true);
        assertEquals(TaskStatus.COMPLETED, taskService.getTasks().get(0).getStatus());
        assertTrue(reminderService.getReminders().isEmpty());

        taskService.setCompleted(taskService.getTasks().get(0), false);
        assertEquals(TaskStatus.OPEN, taskService.getTasks().get(0).getStatus());
    }

    @Test
    void reopeningOverdueTaskMarksItDelayed() {
        Task overdue = addTask(TODAY.minusDays(1), TaskStatus.COMPLETED);

        taskService.setCompleted(overdue, false);

        assertEquals(TaskStatus.DELAYED, taskService.getTasks().get(0).getStatus());
    }

    @Test
    void dueSoonCountIgnoresCompletedAndOverdueTasks() {
        addTask(TODAY, TaskStatus.OPEN);
        addTask(TODAY.plusDays(6), TaskStatus.IN_PROGRESS);
        addTask(TODAY.plusDays(7), TaskStatus.OPEN);
        addTask(TODAY.plusDays(2), TaskStatus.COMPLETED);
        addTask(TODAY.minusDays(1), TaskStatus.DELAYED);

        assertEquals(2, taskService.countDueWithin(7));
        assertEquals(1, taskService.countWithStatus(TaskStatus.DELAYED));
    }
}
