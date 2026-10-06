package io.github.eleninspl.taskmanager.service;

import io.github.eleninspl.taskmanager.model.Category;
import io.github.eleninspl.taskmanager.model.Priority;
import io.github.eleninspl.taskmanager.model.Task;
import io.github.eleninspl.taskmanager.model.enums.TaskStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Which tasks the task list shows, and how they are grouped into sections.
 * This is the search form of the assignment (title, category and priority, in any combination),
 * plus the drawer the user picked in the sidebar.
 */
public final class TaskQuery {

    /** Which drawer is open in the sidebar. */
    public enum Scope { ALL, OVERDUE, NEXT_7_DAYS, CATEGORY }

    /** How the visible tasks are grouped. */
    public enum Grouping { BY_DATE, BY_CATEGORY }

    /**
     * A titled group of tasks.
     *
     * @param title   section heading
     * @param overdue whether this is the overdue section (drawn in the alert color)
     * @param tasks   the tasks, sorted by due date then title
     */
    public record Section(String title, boolean overdue, List<Task> tasks) {}

    private static final Comparator<Task> BY_DUE_DATE = Comparator
            .comparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(t -> t.getTitle().toLowerCase(Locale.ROOT));

    private Scope scope = Scope.ALL;
    private Category category;
    private String titleText = "";
    private Priority priority;
    private Grouping grouping = Grouping.BY_DATE;
    private boolean showCompleted;

    /**
     * Whether a task is overdue: marked Delayed, or past its due date and not completed.
     *
     * @param task the task to check
     * @return true if the task is overdue
     */
    public static boolean isOverdue(Task task) {
        if (task.getStatus() == TaskStatus.COMPLETED) return false;
        return task.getStatus() == TaskStatus.DELAYED
                || (task.getDueDate() != null && task.getDueDate().isBefore(LocalDate.now()));
    }

    /**
     * Whether an unfinished task is due today or within the next six days.
     *
     * @param task the task to check
     * @return true if the task is due within the next 7 days
     */
    public static boolean isDueWithinWeek(Task task) {
        LocalDate today = LocalDate.now();
        return task.getStatus() != TaskStatus.COMPLETED && task.getDueDate() != null
                && !task.getDueDate().isBefore(today) && task.getDueDate().isBefore(today.plusDays(7));
    }

    /**
     * Whether the task passes the drawer and the search criteria (completed tasks included).
     *
     * @param task the task to check
     * @return true if the task belongs in the list
     */
    public boolean matches(Task task) {
        boolean inScope = switch (scope) {
            case ALL -> true;
            case OVERDUE -> isOverdue(task);
            case NEXT_7_DAYS -> isDueWithinWeek(task) || task.getStatus() == TaskStatus.COMPLETED
                    && task.getDueDate() != null && !task.getDueDate().isBefore(LocalDate.now())
                    && task.getDueDate().isBefore(LocalDate.now().plusDays(7));
            case CATEGORY -> category != null && task.getCategory() != null
                    && task.getCategory().getId().equals(category.getId());
        };
        if (!inScope) return false;
        String needle = titleText.trim().toLowerCase(Locale.ROOT);
        if (!needle.isEmpty() && !task.getTitle().toLowerCase(Locale.ROOT).contains(needle)) return false;
        return priority == null
                || (task.getPriority() != null && task.getPriority().getId().equals(priority.getId()));
    }

    /**
     * Filters and groups tasks into sections. Empty sections are left out, and completed
     * tasks form a final section only when completed tasks are shown.
     *
     * @param tasks          all tasks
     * @param categoryOrder  categories in display order, for grouping by category
     * @return the sections to display
     */
    public List<Section> sections(Collection<Task> tasks, List<Category> categoryOrder) {
        List<Task> visible = tasks.stream().filter(this::matches).sorted(BY_DUE_DATE).toList();
        List<Task> open = visible.stream().filter(t -> t.getStatus() != TaskStatus.COMPLETED).toList();
        List<Task> done = visible.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).toList();

        List<Section> sections = new ArrayList<>();
        if (grouping == Grouping.BY_DATE) {
            LocalDate today = LocalDate.now();
            add(sections, "Overdue", true, open, TaskQuery::isOverdue);
            add(sections, "Today", false, open, t -> !isOverdue(t) && today.equals(t.getDueDate()));
            add(sections, "Next 7 days", false, open,
                    t -> !isOverdue(t) && isDueWithinWeek(t) && !today.equals(t.getDueDate()));
            add(sections, "Later", false, open, t -> !isOverdue(t) && !isDueWithinWeek(t));
        } else {
            for (Category c : categoryOrder) {
                add(sections, c.getName(), false, open,
                        t -> t.getCategory() != null && t.getCategory().getId().equals(c.getId()));
            }
        }
        if (showCompleted && !done.isEmpty()) {
            sections.add(new Section("Completed", false, done));
        }
        return sections;
    }

    private static void add(List<Section> sections, String title, boolean overdue, List<Task> from, Predicate<Task> filter) {
        List<Task> tasks = from.stream().filter(filter).toList();
        if (!tasks.isEmpty()) sections.add(new Section(title, overdue, tasks));
    }

    /** @return true when a title or priority filter is active */
    public boolean hasSearchCriteria() {
        return !titleText.isBlank() || priority != null;
    }

    public Scope getScope() { return scope; }
    public Category getCategory() { return category; }
    public Grouping getGrouping() { return grouping; }
    public boolean isShowCompleted() { return showCompleted; }

    /**
     * Opens a drawer.
     *
     * @param scope    the drawer kind
     * @param category the category, when the scope is CATEGORY
     */
    public void setScope(Scope scope, Category category) {
        this.scope = scope;
        this.category = scope == Scope.CATEGORY ? category : null;
    }

    public void setTitleText(String text) { this.titleText = text == null ? "" : text; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public void setGrouping(Grouping grouping) { this.grouping = grouping; }
    public void setShowCompleted(boolean showCompleted) { this.showCompleted = showCompleted; }
}
