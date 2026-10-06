package com.example.service;

import com.example.model.Priority;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.Collection;
import java.util.Optional;

public class PriorityService {

    // Observable list of priorities
    private final ObservableList<Priority> priorities = FXCollections.observableArrayList();
    private static final String DEFAULT_PRIORITY_NAME = "Default";
    private String defaultPriorityId;

    public PriorityService() {
        // Create default priority and add it to the list
        Priority defaultPriority = new Priority(DEFAULT_PRIORITY_NAME);
        defaultPriorityId = defaultPriority.getId();
        priorities.add(defaultPriority);
    }

    // Return all priorities as an observable list
    public ObservableList<Priority> getPriorities() {
        return priorities;
    }

    // Load priorities from a collection; ensure default exists
    public void setPriorities(Collection<Priority> loadedPriorities) {
        priorities.clear();
        defaultPriorityId = null;
        for (Priority p : loadedPriorities) {
            if (p == null) continue;
            priorities.add(p);
            // Track the default priority if found
            if (p.getName() != null && p.getName().equals(DEFAULT_PRIORITY_NAME)) {
                defaultPriorityId = p.getId();
            }
        }
        // If default was not found, create it
        if (defaultPriorityId == null) {
            Priority defaultPriority = new Priority(DEFAULT_PRIORITY_NAME);
            defaultPriorityId = defaultPriority.getId();
            priorities.add(defaultPriority);
        }
    }

    // Add a new priority to the list
    public Priority addPriority(String name) {
        Priority newPriority = new Priority(name);
        priorities.add(newPriority);
        return newPriority;
    }

    // Update a priority's name; cannot update the default; then update tasks that use this priority
    public boolean updatePriority(String priorityId, String newName, TaskService taskService) {
        if (priorityId.equals(defaultPriorityId)) {
            return false;
        }
        Optional<Priority> opt = priorities.stream()
                .filter(p -> p.getId().equals(priorityId))
                .findFirst();
        if (opt.isPresent()) {
            Priority pri = opt.get();
            pri.setName(newName);
            int index = priorities.indexOf(pri);
            if (index != -1) {
                priorities.set(index, pri);
            }
            // Update each task that uses this priority
            taskService.getTasks().forEach(t -> {
                if (t.getPriority() != null && t.getPriority().getId().equals(priorityId)) {
                    t.setPriority(pri);
                }
            });
            taskService.updateTasksForPriority(priorityId);
            return true;
        }
        return false;
    }
    
    // Delete a priority; cannot delete default; reassign tasks to default if deleted
    public Priority deletePriority(String priorityId, TaskService taskService) {
        if (priorityId.equals(defaultPriorityId)) {
            return null;
        }
        Optional<Priority> opt = priorities.stream()
                .filter(p -> p.getId().equals(priorityId))
                .findFirst();
        if (opt.isPresent()) {
            Priority toRemove = opt.get();
            priorities.remove(toRemove);
            taskService.reassignTasksPriority(priorityId, getDefaultPriority());
            return toRemove;
        }
        return null;
    }

    // Return the default priority object
    public Priority getDefaultPriority() {
        return priorities.stream()
                .filter(p -> p.getId().equals(defaultPriorityId))
                .findFirst()
                .orElse(null);
    }

    // Get a priority by its ID
    public Priority getPriorityById(String id) {
        return priorities.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}
