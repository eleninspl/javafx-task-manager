package com.example.service;

import com.example.model.Priority;
import com.example.model.Task;
import com.example.model.enums.TaskStatus;
import com.example.model.enums.ReminderType;
import com.example.model.Reminder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TaskService {

    // Observable list for tasks (for UI binding)
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();
    private final ReminderService reminderService;

    public TaskService(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    // Return the observable list of tasks
    public ObservableList<Task> getTasks() {
        return tasks;
    }

    // Load tasks from a collection
    public void setTasks(Collection<Task> loadedTasks) {
        tasks.setAll(loadedTasks);
    }

    // Add a new task to the list
    public Task addTask(Task task) {
        tasks.add(task);
        return task;
    }

    // Update an existing task
    public boolean updateTask(Task updated) {
        for (int i = 0; i < tasks.size(); i++) {
            Task old = tasks.get(i);
            if (old.getId().equals(updated.getId())) {
                boolean dueDateChanged = hasDueDateChanged(old, updated);
                if (dueDateChanged) {
                    // Recalculate reminders if due date changed
                    recalcRemindersForTask(updated);
                } else if (shouldDeleteReminders(old, updated)) {
                    // If due date didn't change but status moved to COMPLETED or DELAYED, delete reminders
                    reminderService.deleteRemindersByTaskId(updated.getId());
                }
                tasks.set(i, updated); // Replace task to trigger UI update
                return true;
            }
        }
        return false;
    }
    
    // Helper: Check if the due date changed between tasks
    private boolean hasDueDateChanged(Task oldTask, Task updatedTask) {
        if (oldTask.getDueDate() == null && updatedTask.getDueDate() != null) {
            return true;
        } else if (oldTask.getDueDate() != null && updatedTask.getDueDate() != null &&
                   !oldTask.getDueDate().equals(updatedTask.getDueDate())) {
            return true;
        }
        return false;
    }
    
    // Helper: Recalculate reminders when a task's due date changes
    private void recalcRemindersForTask(Task updatedTask) {
        ObservableList<Reminder> remList = reminderService.getReminders();
        for (int j = 0; j < remList.size(); j++) {
            Reminder r = remList.get(j);
            if (r.getTaskId().equals(updatedTask.getId())) {
                if (r.getType() != ReminderType.SPECIFIC_DATE) {
                    LocalDate newReminderDate = computeReminderDate(updatedTask.getDueDate(), r.getType());
                    // Remove reminder if new date is not valid
                    if (newReminderDate == null ||
                        !newReminderDate.isBefore(updatedTask.getDueDate()) ||
                        newReminderDate.isBefore(LocalDate.now())) {
                        remList.remove(j);
                        j--;
                    } else {
                        r.setReminderDate(newReminderDate);
                        remList.set(j, r);
                    }
                } else {
                    LocalDate reminderDate = r.getReminderDate();
                    // Remove SPECIFIC_DATE reminder if invalid
                    if (!reminderDate.isBefore(updatedTask.getDueDate()) ||
                        reminderDate.isBefore(LocalDate.now())) {
                        remList.remove(j);
                        j--;
                    } else {
                        remList.set(j, r);
                    }
                }
            }
        }
    }
    
    // Helper: Compute a new reminder date for non-specific reminder types
    private LocalDate computeReminderDate(LocalDate dueDate, ReminderType type) {
        switch (type) {
            case ONE_DAY_BEFORE:
                return dueDate.minusDays(1);
            case ONE_WEEK_BEFORE:
                return dueDate.minusWeeks(1);
            case ONE_MONTH_BEFORE:
                return dueDate.minusMonths(1);
            default:
                return null;
        }
    }
    
    // Helper: Check if reminders should be deleted based on status change
    private boolean shouldDeleteReminders(Task oldTask, Task updatedTask) {
        boolean newStatusTriggered = (updatedTask.getStatus() == TaskStatus.COMPLETED ||
                                      updatedTask.getStatus() == TaskStatus.DELAYED)
                                      && !(oldTask.getStatus() == TaskStatus.COMPLETED ||
                                           oldTask.getStatus() == TaskStatus.DELAYED);
        return newStatusTriggered;
    }
    
    // Delete a task and its associated reminders
    public Task deleteTask(String taskId) {
        Optional<Task> opt = tasks.stream()
                .filter(t -> t.getId().equals(taskId))
                .findFirst();
        if (opt.isPresent()) {
            Task toRemove = opt.get();
            tasks.remove(toRemove);
            reminderService.deleteRemindersByTaskId(taskId);
            return toRemove;
        }
        return null;
    }

    // Reassign tasks using a deleted priority to the default priority
    public void reassignTasksPriority(String deletedPriorityId, Priority defaultPriority) {
        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            if (t.getPriority() != null && t.getPriority().getId().equals(deletedPriorityId)) {
                t.setPriority(defaultPriority);
                tasks.set(i, t);
            }
        }
    }

    // Delete all tasks belonging to a specific category
    public void deleteTasksByCategory(String categoryId) {
        List<Task> tasksToDelete = tasks.stream()
                .filter(t -> t.getCategory() != null && t.getCategory().getId().equals(categoryId))
                .collect(Collectors.toList());
        for (Task t : tasksToDelete) {
            deleteTask(t.getId());
        }
    }

    // Refresh tasks that reference a renamed category
    public void updateTasksForCategory(String categoryId) {
        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            if (t.getCategory() != null && t.getCategory().getId().equals(categoryId)) {
                tasks.set(i, t);
            }
        }
    }

    // Refresh tasks that reference a renamed priority
    public void updateTasksForPriority(String priorityId) {
        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            if (t.getPriority() != null && t.getPriority().getId().equals(priorityId)) {
                tasks.set(i, t);
            }
        }
    }    

    // Check for overdue tasks and update their status to DELAYED
    public void checkOverdueTasks() {
        LocalDate now = LocalDate.now();
        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            if (t.getDueDate() != null && t.getDueDate().isBefore(now)
                    && t.getStatus() != TaskStatus.COMPLETED && t.getStatus() != TaskStatus.DELAYED) {
                Task updatedTask = new Task(t.getId(), t.getTitle(), t.getDescription(), t.getCategory(),
                        t.getPriority(), t.getDueDate(), TaskStatus.DELAYED);
                updateTask(updatedTask);
            }
        }
    }
}
