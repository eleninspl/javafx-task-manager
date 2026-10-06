package io.github.eleninspl.taskmanager.service;

import io.github.eleninspl.taskmanager.model.Category;
import io.github.eleninspl.taskmanager.model.Priority;
import io.github.eleninspl.taskmanager.model.Task;
import io.github.eleninspl.taskmanager.model.enums.ReminderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CategoryAndPriorityServiceTest {

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

    private Task addTask(Category category, Priority priority) {
        return taskService.addTask(new Task("Task", "", category, priority, LocalDate.now().plusDays(10)));
    }

    @Test
    void defaultCategoryCannotBeRenamedOrDeleted() {
        Category noCategory = categoryService.getNoCategory();

        assertFalse(categoryService.updateCategory(noCategory.getId(), "Renamed", taskService));
        assertNull(categoryService.deleteCategory(noCategory.getId(), taskService));
        assertEquals("No Category", noCategory.getName());
        assertTrue(categoryService.getCategories().contains(noCategory));
    }

    @Test
    void deletingCategoryDeletesItsTasksAndTheirReminders() {
        Category work = categoryService.addCategory("Work");
        Category home = categoryService.addCategory("Home");
        Task workTask = addTask(work, priorityService.getDefaultPriority());
        Task homeTask = addTask(home, priorityService.getDefaultPriority());
        reminderService.createReminderForTask(workTask, ReminderType.ONE_DAY_BEFORE, null);

        categoryService.deleteCategory(work.getId(), taskService);

        assertEquals(List.of(homeTask), List.copyOf(taskService.getTasks()));
        assertTrue(reminderService.getReminders().isEmpty());
        assertFalse(categoryService.getCategories().contains(work));
    }

    @Test
    void loadingCategoriesKeepsTheBuiltInDefaultAndSkipsInvalidEntries() {
        categoryService.setCategories(List.of(
                new Category("old-id", "No Category"),
                new Category("blank", "  "),
                new Category("w", "Work")));

        assertEquals(List.of("No Category", "Work"),
                categoryService.getCategories().stream().map(Category::getName).toList());
    }

    @Test
    void defaultPriorityCannotBeRenamedOrDeleted() {
        Priority defaultPriority = priorityService.getDefaultPriority();

        assertFalse(priorityService.updatePriority(defaultPriority.getId(), "Renamed", taskService));
        assertNull(priorityService.deletePriority(defaultPriority.getId(), taskService));
        assertEquals("Default", defaultPriority.getName());
    }

    @Test
    void deletingPriorityMovesItsTasksToTheDefault() {
        Priority high = priorityService.addPriority("High");
        Task task = addTask(categoryService.getNoCategory(), high);

        priorityService.deletePriority(high.getId(), taskService);

        assertSame(priorityService.getDefaultPriority(), task.getPriority());
        assertEquals(1, taskService.getTasks().size());
    }

    @Test
    void renamingPriorityUpdatesTasksThatUseIt() {
        Priority high = priorityService.addPriority("High");
        Task task = addTask(categoryService.getNoCategory(), new Priority(high.getId(), "High"));

        priorityService.updatePriority(high.getId(), "Urgent", taskService);

        assertEquals("Urgent", task.getPriority().getName());
    }

    @Test
    void loadingPrioritiesRecreatesTheDefaultWhenMissing() {
        priorityService.setPriorities(List.of(new Priority("h", "High")));

        Priority defaultPriority = priorityService.getDefaultPriority();
        assertNotNull(defaultPriority);
        assertEquals("Default", defaultPriority.getName());
        assertEquals(2, priorityService.getPriorities().size());
    }
}
