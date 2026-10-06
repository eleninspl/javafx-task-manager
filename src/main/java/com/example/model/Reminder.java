package com.example.model;

import com.example.model.enums.ReminderType;
import java.time.LocalDate;
import java.util.UUID;

public class Reminder {
    private String id;
    private String taskId;
    private ReminderType type;
    private LocalDate reminderDate;

    // Constructor with generated id
    public Reminder(String taskId, ReminderType type, LocalDate reminderDate) {
        this.id = UUID.randomUUID().toString();
        this.taskId = taskId;
        this.type = type;
        this.reminderDate = reminderDate;
    }

    // Get reminder id
    public String getId() {
        return id;
    }

    // Get associated task id
    public String getTaskId() {
        return taskId;
    }

    // Set associated task id
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    // Get reminder type
    public ReminderType getType() {
        return type;
    }

    // Set reminder type
    public void setType(ReminderType type) {
        this.type = type;
    }

    // Get reminder date
    public LocalDate getReminderDate() {
        return reminderDate;
    }

    // Set reminder date
    public void setReminderDate(LocalDate reminderDate) {
        this.reminderDate = reminderDate;
    }

    // Return a string representation of the reminder
    @Override
    public String toString() {
        return type + " on " + reminderDate;
    }
}
