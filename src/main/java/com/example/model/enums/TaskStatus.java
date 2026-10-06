package com.example.model.enums;

// Enum representing the possible statuses of a task
public enum TaskStatus {
    OPEN,           // Task is open
    IN_PROGRESS,    // Task is in progress
    POSTPONED,      // Task has been postponed
    COMPLETED,      // Task is completed
    DELAYED;        // Task is delayed (past due date and not completed)

    @Override
    public String toString() {
        // Return a user-friendly string for each task status
        switch (this) {
            case OPEN:
                return "Open";
            case IN_PROGRESS:
                return "In Progress";
            case POSTPONED:
                return "Postponed";
            case COMPLETED:
                return "Completed";
            case DELAYED:
                return "Delayed";
            default:
                return super.toString();
        }
    }
}
