package io.github.eleninspl.taskmanager.controller;

import io.github.eleninspl.taskmanager.model.Category;
import io.github.eleninspl.taskmanager.service.CategoryService;
import io.github.eleninspl.taskmanager.service.TaskService;

/**
 * Manage categories. Deleting a category deletes its tasks and their reminders;
 * "No Category" is built in.
 */
public class CategoryController extends ManagedListController<Category> {

    private final CategoryService categoryService;
    private final TaskService taskService;

    /**
     * @param categoryService categories
     * @param taskService     tasks, for usage counts and cascading deletes
     */
    public CategoryController(CategoryService categoryService, TaskService taskService) {
        super("Categories", "category", "categories", categoryService.getCategories(), taskService.getTasks());
        this.categoryService = categoryService;
        this.taskService = taskService;
        start();
    }

    @Override protected String name(Category c) { return c.getName(); }
    @Override protected boolean isBuiltIn(Category c) { return c == categoryService.getNoCategory(); }

    @Override
    protected long usage(Category c) {
        return taskService.getTasks().stream()
                .filter(t -> t.getCategory() != null && t.getCategory().getId().equals(c.getId())).count();
    }

    @Override protected String nameError(String name, Category excluded) {
        return categoryService.nameError(name, excluded == null ? null : excluded.getId());
    }
    @Override protected void create(String name) { categoryService.addCategory(name); }
    @Override protected void rename(Category c, String name) { categoryService.updateCategory(c.getId(), name, taskService); }
    @Override protected void remove(Category c) { categoryService.deleteCategory(c.getId(), taskService); }

    @Override
    protected String deleteConsequence(Category c, long usage) {
        return usage == 0 ? "The category is empty, so no tasks are affected."
                : "Its " + tasks(usage) + " and their reminders will be deleted too. This can't be undone.";
    }

    @Override
    protected String builtInNote() {
        return "“No Category” holds tasks without a category. Deleting a category also deletes its tasks.";
    }
}
