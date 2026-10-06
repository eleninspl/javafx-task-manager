package com.example.service;

import com.example.model.Reminder;
import com.example.model.enums.ReminderType;
import com.example.model.Task;
import com.example.model.enums.TaskStatus;
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
        // Check that we are not setting a reminder for a completed task
        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot set a reminder for a completed task.");
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
                    throw new IllegalArgumentException("A specific date must be provided for SPECIFIC_DATE reminder.");
                }
                reminderDate = specificDate;
                break;
            default:
                throw new IllegalArgumentException("Invalid reminder type.");
        }
        // Ensure the reminder date is on or before the due date
        if (reminderDate.isAfter(dueDate)) {
            throw new IllegalArgumentException("Reminder date must be on or before the task's due date.");
        }
        // Ensure the reminder date is not in the past
        if (reminderDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Reminder date is in the past.");
        }
        // Create the reminder and add it to the list
        Reminder reminder = new Reminder(task.getId(), type, reminderDate);
        addReminder(reminder);
        return reminder;
    }
}
