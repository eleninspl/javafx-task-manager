package io.github.eleninspl.taskmanager.service;

import io.github.eleninspl.taskmanager.model.Priority;
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

    // Return a user-facing message explaining why a name can't be used, or null if it is fine.
    // excludeId is the priority being renamed (it may keep its own name).
    public String nameError(String name, String excludeId) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) return "Enter a name.";
        boolean taken = priorities.stream()
                .anyMatch(p -> !p.getId().equals(excludeId) && p.getName() != null && p.getName().equalsIgnoreCase(trimmed));
        return taken ? "There is already a priority called \"" + trimmed + "\"." : null;
    }

    // Add a new priority with a valid, unique name
    public Priority addPriority(String name) {
        String error = nameError(name, null);
        if (error != null) throw new IllegalArgumentException(error);
        Priority newPriority = new Priority(name.trim());
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
            String error = nameError(newName, priorityId);
            if (error != null) throw new IllegalArgumentException(error);
            pri.setName(newName.trim());
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

    // Map a priority loaded from disk to the instance held by this service:
    // match by ID, then by name, and fall back to the default priority
    public Priority resolve(Priority priority) {
        if (priority == null) return getDefaultPriority();
        Priority byId = getPriorityById(priority.getId());
        if (byId != null) return byId;
        return priorities.stream()
                .filter(p -> p.getName() != null && p.getName().equals(priority.getName()))
                .findFirst()
                .orElse(getDefaultPriority());
    }
}
