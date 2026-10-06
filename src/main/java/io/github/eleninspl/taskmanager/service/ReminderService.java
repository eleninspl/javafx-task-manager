package io.github.eleninspl.taskmanager.service;

import io.github.eleninspl.taskmanager.model.Reminder;
import io.github.eleninspl.taskmanager.model.enums.ReminderType;
import io.github.eleninspl.taskmanager.model.Task;
import io.github.eleninspl.taskmanager.model.enums.TaskStatus;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

public class ReminderService {

    // Observable list for reminders (for UI binding)
    private final ObservableList<Reminder> reminders = FXCollections.observableArrayList();

    // Return the observable list of reminders
    public ObservableList<Reminder> getReminders() {
        return reminders;
    }

    // Load reminders from a collection into our list
    public void setReminders(Collection<Reminder> loadedReminders) {
        reminders.setAll(loadedReminders);
    }

    // Add a reminder to the list and return it
    public Reminder addReminder(Reminder reminder) {
        reminders.add(reminder);
        return reminder;
    }

    // Update an existing reminder by replacing it in the list
    public boolean updateReminder(Reminder updatedReminder) {
        for (int i = 0; i < reminders.size(); i++) {
            if (reminders.get(i).getId().equals(updatedReminder.getId())) {
                reminders.set(i, updatedReminder);
                return true;
            }
        }
        return false;
    }

    // Delete a reminder by its ID; returns the deleted reminder or null if not found
    public Reminder deleteReminder(String reminderId) {
        Optional<Reminder> opt = reminders.stream()
                .filter(r -> r.getId().equals(reminderId))
                .findFirst();
        if (opt.isPresent()) {
            Reminder rem = opt.get();
            reminders.remove(rem);
            return rem;
        }
        return null;
    }

    // Delete all reminders linked to a given task ID
    public void deleteRemindersByTaskId(String taskId) {
        reminders.removeIf(r -> r.getTaskId().equals(taskId));
    }

    // Create a new reminder for a task after validating inputs and computing the reminder date
    public Reminder createReminderForTask(Task task, ReminderType type, LocalDate specificDate) {
        LocalDate reminderDate = resolveReminderDate(task, type, specificDate);
        Reminder reminder = new Reminder(task.getId(), type, reminderDate);
        addReminder(reminder);
        return reminder;
    }

    // Change an existing reminder (task, type or date) in place, with the same validation as creating one
    public Reminder updateReminderForTask(String reminderId, Task task, ReminderType type, LocalDate specificDate) {
        LocalDate reminderDate = resolveReminderDate(task, type, specificDate);
        for (int i = 0; i < reminders.size(); i++) {
            Reminder r = reminders.get(i);
            if (r.getId().equals(reminderId)) {
                r.setTaskId(task.getId());
                r.setType(type);
                r.setReminderDate(reminderDate);
                reminders.set(i, r); // Replace to notify listeners
                return r;
            }
        }
        throw new IllegalArgumentException("This reminder no longer exists.");
    }

    // Return a user-facing message explaining why this reminder cannot be saved, or null if it is valid
    public String validationError(Task task, ReminderType type, LocalDate specificDate) {
        try {
            resolveReminderDate(task, type, specificDate);
            return null;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    // Compute the reminder date for a type and check it against the task's due date and today
    private LocalDate resolveReminderDate(Task task, ReminderType type, LocalDate specificDate) {
        if (task == null) {
            throw new IllegalArgumentException("Choose a task.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Choose when to be reminded.");
        }
        // Check that we are not setting a reminder for a completed task
        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new IllegalArgumentException("Completed tasks can't have reminders.");
        }
        LocalDate dueDate = task.getDueDate();
        LocalDate reminderDate;
        // Calculate reminder date based on type
        switch (type) {
            case ONE_DAY_BEFORE:
                reminderDate = dueDate.minusDays(1);
                break;
            case ONE_WEEK_BEFORE:
                reminderDate = dueDate.minusWeeks(1);
                break;
            case ONE_MONTH_BEFORE:
                reminderDate = dueDate.minusMonths(1);
                break;
            case SPECIFIC_DATE:
                if (specificDate == null) {
                    throw new IllegalArgumentException("Pick the date for this reminder.");
                }
                reminderDate = specificDate;
                break;
            default:
                throw new IllegalArgumentException("Invalid reminder type.");
        }
        // Ensure the reminder date is on or before the due date
        if (reminderDate.isAfter(dueDate)) {
            throw new IllegalArgumentException("The reminder must be on or before the due date ("
                    + DateFormats.full(dueDate) + ").");
        }
        // Ensure the reminder date is not in the past
        if (reminderDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("That reminder would fall on " + DateFormats.full(reminderDate)
                    + ", which has already passed.");
        }
        return reminderDate;
    }
}
