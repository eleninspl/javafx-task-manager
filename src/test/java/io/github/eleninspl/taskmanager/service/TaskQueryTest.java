package io.github.eleninspl.taskmanager.service;

import io.github.eleninspl.taskmanager.model.Category;
import io.github.eleninspl.taskmanager.model.Priority;
import io.github.eleninspl.taskmanager.model.Task;
import io.github.eleninspl.taskmanager.model.enums.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskQueryTest {

    private static final LocalDate TODAY = LocalDate.now();

    private final Category work = new Category("Work");
    private final Category home = new Category("Home");
    private final Priority high = new Priority("High");
    private final Priority low = new Priority("Low");

    private final Task overdue = new Task("Lab report", "", work, high, TODAY.minusDays(2), TaskStatus.DELAYED);
    private final Task today = new Task("Review PRs", "", work, low, TODAY, TaskStatus.OPEN);
    private final Task soon = new Task("Paper", "", home, high, TODAY.plusDays(3), TaskStatus.IN_PROGRESS);
    private final Task later = new Task("Passport", "", home, low, TODAY.plusDays(30), TaskStatus.OPEN);
    private final Task done = new Task("Dentist", "", home, low, TODAY.plusDays(1), TaskStatus.COMPLETED);
    private final List<Task> all = List.of(later, done, soon, today, overdue);

    private static List<String> titles(List<TaskQuery.Section> sections) {
        return sections.stream().map(TaskQuery.Section::title).toList();
    }

    @Test
    void groupsByDueDateInDeadlineOrderAndHidesCompletedByDefault() {
        List<TaskQuery.Section> sections = new TaskQuery().sections(all, List.of(work, home));

        assertEquals(List.of("Overdue", "Today", "Next 7 days", "Later"), titles(sections));
        assertTrue(sections.get(0).overdue());
        assertEquals(List.of(overdue), sections.get(0).tasks());
    }

    @Test
    void completedTasksFormAFinalSectionWhenShown() {
        TaskQuery query = new TaskQuery();
        query.setShowCompleted(true);

        List<TaskQuery.Section> sections = query.sections(all, List.of(work, home));

        assertEquals("Completed", sections.get(sections.size() - 1).title());
        assertEquals(List.of(done), sections.get(sections.size() - 1).tasks());
    }

    @Test
    void groupsByCategoryInCategoryOrder() {
        TaskQuery query = new TaskQuery();
        query.setGrouping(TaskQuery.Grouping.BY_CATEGORY);

        List<TaskQuery.Section> sections = query.sections(all, List.of(home, work));

        assertEquals(List.of("Home", "Work"), titles(sections));
        assertEquals(List.of(soon, later), sections.get(0).tasks());
    }

    @Test
    void combinesTitleCategoryAndPriorityCriteria() {
        TaskQuery query = new TaskQuery();
        query.setScope(TaskQuery.Scope.CATEGORY, work);
        query.setPriority(high);
        query.setTitleText("lab");

        assertTrue(query.matches(overdue));
        assertFalse(query.matches(today));
        assertFalse(query.matches(soon));
        assertTrue(query.hasSearchCriteria());
    }

    @Test
    void overdueAndNextSevenDaysDrawers() {
        TaskQuery query = new TaskQuery();
        query.setScope(TaskQuery.Scope.OVERDUE, null);
        assertEquals(List.of(overdue), all.stream().filter(query::matches).toList());

        query.setScope(TaskQuery.Scope.NEXT_7_DAYS, null);
        assertTrue(query.matches(today));
        assertTrue(query.matches(soon));
        assertFalse(query.matches(later));
        assertFalse(query.matches(overdue));
    }

    @Test
    void openTaskPastItsDueDateCountsAsOverdueEvenBeforeTheStartupCheck() {
        Task stale = new Task("Stale", "", work, low, TODAY.minusDays(1), TaskStatus.OPEN);

        assertTrue(TaskQuery.isOverdue(stale));
        assertFalse(TaskQuery.isOverdue(done));
    }
}
