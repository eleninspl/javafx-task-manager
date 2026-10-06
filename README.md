# MediaLab Assistant

A desktop task manager built with Java 17 and JavaFX. It lets you organize tasks by category and priority, schedule reminders, and track deadlines. All data is saved locally as JSON.

I built it for the Multimedia Technology course at the School of Electrical and Computer Engineering, National Technical University of Athens (NTUA).

## Features

- **Tasks**: Create, edit and delete tasks. Each task has a title, description, category, priority, due date and status (Open, In Progress, Postponed, Completed or Delayed).
- **Deadline tracking**: Overdue tasks that are not completed are marked Delayed, and the user is warned at startup.
- **Categories and priorities**: Fully editable. A protected default ("No Category" / "Default") cannot be renamed or removed. Deleting a category also deletes its tasks. Deleting a priority moves its tasks to the default priority.
- **Reminders**: A task can have several reminders: one day, one week or one month before the due date, or on a custom date. Reminder dates are validated against the due date and today's date. When a task's due date changes, its reminders are recalculated. When a task is completed or becomes delayed, its reminders are removed. Reminders due today appear in a dialog at startup.
- **Search**: Filter tasks by any combination of title, category and priority.
- **Dashboard**: The header shows totals for all tasks, completed tasks, delayed tasks and tasks due in the next 7 days. It updates as you make changes.

## Tech stack

| Area        | Technology                          |
|-------------|-------------------------------------|
| Language    | Java 17                             |
| UI          | JavaFX 21 (layouts written in code) |
| Persistence | JSON via Gson 2.10                  |
| Build       | Maven, `javafx-maven-plugin`        |

## Architecture

The code is split into layers so that business rules do not depend on the UI:

```
com.example
├── App.java              Entry point: wires services and controllers, builds the main window
├── model/                Task, Category, Priority, Reminder, plus TaskStatus and ReminderType enums
├── service/              Business rules and in-memory state (CRUD, cascades, reminder validation)
├── controller/           One controller per screen (Tasks, Categories, Priorities, Reminders, Search)
│   └── dialog/           Create and edit dialogs
└── storage/              JsonStorage (Gson) and a LocalDate type adapter
```

- **Services** keep their data in JavaFX `ObservableList`s, so the table views update automatically when the data changes.
- **Cross-entity rules** are handled in the service layer, not the controllers. Examples: deleting a category also deletes its tasks and their reminders, and changing a due date recalculates reminders.
- **Persistence**: Data is loaded once at startup and written back when the window closes. It is stored in four files under `medialab/` in the working directory: `tasks.json`, `categories.json`, `priorities.json` and `reminders.json`.

## Getting started

### Prerequisites

- JDK 17 or later
- Maven 3.8 or later

### Run

```bash
git clone https://github.com/eleninspl/javafx-task-manager.git
cd "javafx-task-manager/Task Managment System"
mvn javafx:run
```

The `medialab/` data directory is created on first exit, in the directory you launched from.

## Documentation

- [`report.pdf`](report.pdf): Project report covering design decisions and assumptions
- [`multimedia_project_guidelines.pdf`](multimedia_project_guidelines.pdf): Original assignment specification

## Design assumptions

- The default category and the default priority always exist and cannot be changed.
- Users cannot set Delayed themselves. The status is assigned automatically from the due date.
- Due dates are dates only, with no time of day.
