package io.github.eleninspl.taskmanager.service;

import io.github.eleninspl.taskmanager.model.Category;
import io.github.eleninspl.taskmanager.model.Priority;
import io.github.eleninspl.taskmanager.model.Reminder;
import io.github.eleninspl.taskmanager.model.Task;
import io.github.eleninspl.taskmanager.model.enums.ReminderType;
import io.github.eleninspl.taskmanager.model.enums.TaskStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReminderServiceTest {

    private static final LocalDate TODAY = LocalDate.now();

    private final ReminderService reminderService = new ReminderService();

    private static Task taskDue(LocalDate dueDate, TaskStatus status) {
        return new Task("Task", "", new Category("Work"), new Priority("High"), dueDate, status);
    }

    @ParameterizedTest
    @CsvSource({
            "ONE_DAY_BEFORE,   59",
            "ONE_WEEK_BEFORE,  53",
    })
    void computesReminderDateRelativeToDueDate(ReminderType type, int expectedDaysFromToday) {
        Task task = taskDue(TODAY.plusDays(60), TaskStatus.OPEN);

        Reminder reminder = reminderService.createReminderForTask(task, type, null);

        assertEquals(TODAY.plusDays(expectedDaysFromToday), reminder.getReminderDate());
        assertEquals(task.getId(), reminder.getTaskId());
        assertEquals(1, reminderService.getReminders().size());
    }

    @Test
    void oneMonthBeforeUsesCalendarMonths() {
        LocalDate dueDate = TODAY.plusDays(60);

        Reminder reminder = reminderService.createReminderForTask(taskDue(dueDate, TaskStatus.OPEN),
                ReminderType.ONE_MONTH_BEFORE, null);

        assertEquals(dueDate.minusMonths(1), reminder.getReminderDate());
    }

    @Test
    void specificDateIsUsedAsGiven() {
        LocalDate date = TODAY.plusDays(4);

        Reminder reminder = reminderService.createReminderForTask(taskDue(TODAY.plusDays(10), TaskStatus.OPEN),
                ReminderType.SPECIFIC_DATE, date);

        assertEquals(date, reminder.getReminderDate());
    }

    @Test
    void rejectsReminderForCompletedTask() {
        Task done = taskDue(TODAY.plusDays(10), TaskStatus.COMPLETED);

        assertThrows(IllegalArgumentException.class,
                () -> reminderService.createReminderForTask(done, ReminderType.ONE_DAY_BEFORE, null));
        assertTrue(reminderService.getReminders().isEmpty());
    }

    @Test
    void rejectsReminderThatWouldFallInThePast() {
        Task dueSoon = taskDue(TODAY.plusDays(3), TaskStatus.OPEN);

        assertThrows(IllegalArgumentException.class,
                () -> reminderService.createReminderForTask(dueSoon, ReminderType.ONE_WEEK_BEFORE, null));
    }

    @Test
    void rejectsSpecificDateAfterDueDate() {
        Task task = taskDue(TODAY.plusDays(3), TaskStatus.OPEN);

        assertThrows(IllegalArgumentException.class,
                () -> reminderService.createReminderForTask(task, ReminderType.SPECIFIC_DATE, TODAY.plusDays(4)));
    }

    @Test
    void specificDateReminderRequiresADate() {
        Task task = taskDue(TODAY.plusDays(3), TaskStatus.OPEN);

        assertThrows(IllegalArgumentException.class,
                () -> reminderService.createReminderForTask(task, ReminderType.SPECIFIC_DATE, null));
    }
}
