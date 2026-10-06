package com.example.model.enums;

// Enum representing the types of reminders that can be set
public enum ReminderType {
    ONE_DAY_BEFORE,    // Reminder one day before the task's due date
    ONE_WEEK_BEFORE,   // Reminder one week before the task's due date
    ONE_MONTH_BEFORE,  // Reminder one month before the task's due date
    SPECIFIC_DATE;     // Reminder on a specific date provided by the user

    @Override
    public String toString() {
        // Return a user-friendly string for each reminder type
        switch (this) {
            case ONE_DAY_BEFORE:
                return "One Day Before";
            case ONE_WEEK_BEFORE:
                return "One Week Before";
            case ONE_MONTH_BEFORE:
                return "One Month Before";
            case SPECIFIC_DATE:
                return "Specific Date";
            default:
                return super.toString();
        }
    }
}
