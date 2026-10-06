package io.github.eleninspl.taskmanager.storage;

import io.github.eleninspl.taskmanager.model.Category;
import io.github.eleninspl.taskmanager.model.Priority;
import io.github.eleninspl.taskmanager.model.Reminder;
import io.github.eleninspl.taskmanager.model.Task;
import io.github.eleninspl.taskmanager.model.enums.ReminderType;
import io.github.eleninspl.taskmanager.model.enums.TaskStatus;
import io.github.eleninspl.taskmanager.service.CategoryService;
import io.github.eleninspl.taskmanager.service.PriorityService;
import io.github.eleninspl.taskmanager.service.ReminderService;
import io.github.eleninspl.taskmanager.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonStorageTest {

    @TempDir
    Path dataDir;

    @Test
    void missingFilesLoadAsEmpty() {
        JsonStorage storage = new JsonStorage(dataDir.resolve("does-not-exist"));

        assertTrue(storage.loadTasks().isEmpty());
        assertTrue(storage.loadCategories().isEmpty());
        assertTrue(storage.loadPriorities().isEmpty());
        assertTrue(storage.loadReminders().isEmpty());
    }

    @Test
    void roundTripsAllEntities() {
        JsonStorage storage = new JsonStorage(dataDir);
        Category work = new Category("Work");
        Priority high = new Priority("High");
        LocalDate due = LocalDate.of(2030, 1, 15);
        Task task = new Task("Ship release", "Tag and publish", work, high, due, TaskStatus.IN_PROGRESS);
        Reminder reminder = new Reminder(task.getId(), ReminderType.ONE_WEEK_BEFORE, due.minusWeeks(1));

        storage.saveCategories(List.of(work));
        storage.savePriorities(List.of(high));
        storage.saveTasks(List.of(task));
        storage.saveReminders(List.of(reminder));

        Task loaded = storage.loadTasks().iterator().next();
        assertEquals(task.getId(), loaded.getId());
        assertEquals("Ship release", loaded.getTitle());
        assertEquals("Tag and publish", loaded.getDescription());
        assertEquals(due, loaded.getDueDate());
        assertEquals(TaskStatus.IN_PROGRESS, loaded.getStatus());
        assertEquals(work.getId(), loaded.getCategory().getId());
        assertEquals(high.getId(), loaded.getPriority().getId());

        Reminder loadedReminder = storage.loadReminders().iterator().next();
        assertEquals(task.getId(), loadedReminder.getTaskId());
        assertEquals(ReminderType.ONE_WEEK_BEFORE, loadedReminder.getType());
        assertEquals(due.minusWeeks(1), loadedReminder.getReminderDate());

        assertEquals("Work", storage.loadCategories().iterator().next().getName());
        assertEquals("High", storage.loadPriorities().iterator().next().getName());
    }

    @Test
    void saveLeavesNoTemporaryFilesBehind() throws IOException {
        new JsonStorage(dataDir).saveTasks(List.of());

        try (var files = Files.list(dataDir)) {
            assertEquals(List.of("tasks.json"), files.map(p -> p.getFileName().toString()).toList());
        }
    }

    @Test
    void corruptFileIsSetAsideInsteadOfOverwritten() throws IOException {
        Files.writeString(dataDir.resolve("tasks.json"), "{ not valid json");

        assertTrue(new JsonStorage(dataDir).loadTasks().isEmpty());
        assertTrue(Files.exists(dataDir.resolve("tasks.json.corrupt")));
        assertFalse(Files.exists(dataDir.resolve("tasks.json")));
    }

    @Test
    void categoryRenameAfterReloadIsReflectedInTasks() {
        // Session 1: create data and save it
        JsonStorage storage = new JsonStorage(dataDir);
        CategoryService categories = new CategoryService();
        PriorityService priorities = new PriorityService();
        Category work = categories.addCategory("Work");
        Task task = new Task("Plan sprint", "", work, priorities.getDefaultPriority(), LocalDate.of(2030, 1, 1));
        storage.saveCategories(categories.getCategories());
        storage.savePriorities(priorities.getPriorities());
        storage.saveTasks(List.of(task));

        // Session 2: load into fresh services, the way App does
        CategoryService reloadedCategories = new CategoryService();
        PriorityService reloadedPriorities = new PriorityService();
        TaskService reloadedTasks = new TaskService(new ReminderService());
        reloadedCategories.setCategories(storage.loadCategories());
        reloadedPriorities.setPriorities(storage.loadPriorities());
        reloadedTasks.setTasks(storage.loadTasks());
        reloadedTasks.linkReferences(reloadedCategories, reloadedPriorities);

        reloadedCategories.updateCategory(work.getId(), "Office", reloadedTasks);

        assertEquals("Office", reloadedTasks.getTasks().get(0).getCategory().getName());
    }
}
