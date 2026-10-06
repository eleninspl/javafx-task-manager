package com.example.model;

import com.example.model.enums.TaskStatus;
import java.time.LocalDate;
import java.util.UUID;

public class Task {
    private String id;
    private String title;
    private String description;
    private Category category;
    private Priority priority;
    private LocalDate dueDate;
    private TaskStatus status;

    // Constructor with specified status
    public Task(String title, String description, Category category, Priority priority, LocalDate dueDate, TaskStatus status) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        this.category = category;
        this.priority = priority;
        this.dueDate = dueDate;
        this.status = status;
    }

    // Constructor with default status (OPEN)
    public Task(String title, String description, Category category, Priority priority, LocalDate dueDate) {
        this(title, description, category, priority, dueDate, TaskStatus.OPEN);
    }

    // Constructor with provided id and status
    public Task(String id, String title, String description, Category category, Priority priority, LocalDate dueDate, TaskStatus status) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.priority = priority;
        this.dueDate = dueDate;
        this.status = status;
    }

    // Get task id
    public String getId() {
        return id;
    }

    // Get task title
    public String getTitle() {
        return title;
    }

    // Set task title
    public void setTitle(String title) {
        this.title = title;
    }

    // Get task description
    public String getDescription() {
        return description;
    }

    // Set task description
    public void setDescription(String description) {
        this.description = description;
    }

    // Get task category
    public Category getCategory() {
        return category;
    }

    // Set task category
    public void setCategory(Category category) {
        this.category = category;
    }

    // Get task priority
    public Priority getPriority() {
        return priority;
    }

    // Set task priority
    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    // Get task due date
    public LocalDate getDueDate() {
        return dueDate;
    }

    // Set task due date
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    // Get task status
    public TaskStatus getStatus() {
        return status;
    }

    // Set task status
    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    // Return task title as string
    @Override
    public String toString() {
        return title;
    }
}
