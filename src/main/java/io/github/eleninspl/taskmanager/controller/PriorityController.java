package io.github.eleninspl.taskmanager.controller;

import io.github.eleninspl.taskmanager.model.Priority;
import io.github.eleninspl.taskmanager.service.PriorityService;
import io.github.eleninspl.taskmanager.service.TaskService;

/**
 * Manage priority levels. Deleting a level moves its tasks to "Default", which is built in.
 */
public class PriorityController extends ManagedListController<Priority> {

    private final PriorityService priorityService;
    private final TaskService taskService;

    /**
     * @param priorityService priority levels
     * @param taskService     tasks, for usage counts and reassignment
     */
    public PriorityController(PriorityService priorityService, TaskService taskService) {
        super("Priorities", "priority", "priorities", priorityService.getPriorities(), taskService.getTasks());
        this.priorityService = priorityService;
        this.taskService = taskService;
        start();
    }

    @Override protected String name(Priority p) { return p.getName(); }
    @Override protected boolean isBuiltIn(Priority p) { return p == priorityService.getDefaultPriority(); }

    @Override
    protected long usage(Priority p) {
        return taskService.getTasks().stream()
                .filter(t -> t.getPriority() != null && t.getPriority().getId().equals(p.getId())).count();
    }

    @Override
    protected long openUsage(Priority p) {
        return taskService.getTasks().stream()
                .filter(t -> t.getStatus() != io.github.eleninspl.taskmanager.model.enums.TaskStatus.COMPLETED)
                .filter(t -> t.getPriority() != null && t.getPriority().getId().equals(p.getId())).count();
    }

    @Override protected String nameError(String name, Priority excluded) {
        return priorityService.nameError(name, excluded == null ? null : excluded.getId());
    }
    @Override protected void create(String name) { priorityService.addPriority(name); }
    @Override protected void rename(Priority p, String name) { priorityService.updatePriority(p.getId(), name, taskService); }
    @Override protected void remove(Priority p) { priorityService.deletePriority(p.getId(), taskService); }

    @Override
    protected String deleteConsequence(Priority p, long usage) {
        return usage == 0 ? "No tasks use this priority."
                : "Its " + tasks(usage) + " will move to “Default”.";
    }

    @Override
    protected String builtInNote() {
        return "“Default” is used when a task has no other priority. Deleting a priority moves its tasks to Default.";
    }
}
