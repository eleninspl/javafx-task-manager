# Product

<!-- impeccable:product-schema 1 -->

## Platform

desktop

Native JavaFX 21 desktop application (macOS, Windows, Linux). Not web, iOS or Android: there is no browser surface, and no web detector applies.

## Users

Students juggling coursework deadlines (lab reports, exam study, group projects) alongside personal errands. They open the app at the start of a study session or the start of the day to answer "what needs my attention now?", and add or update tasks as work comes in.

Secondary audience: portfolio reviewers who see the README screenshots and may run the app briefly.

## Product Purpose

MediaLab Assistant (repository: javafx-task-manager) is a local, offline task manager. Users organize tasks by category and priority, set reminders, and track deadlines. Success: a student sees overdue and upcoming work at a glance and can capture or update a task in seconds.

## Positioning

A deadline-first personal task manager that runs entirely on the user's machine. Tasks become Delayed automatically when their due date passes, and reminders are validated and recalculated against the due date. No account and no network: data is plain JSON in the user's home folder.

## Operating Context

- Desktop, mouse and keyboard, usually at a desk during study or work sessions.
- Launched on demand; the startup moment is when overdue tasks and today's reminders surface.
- Originated as the semester project for Multimedia Technology (Τεχνολογία Πολυμέσων), 7th semester, ECE NTUA, 2024–25.

## Capabilities and Constraints

The original assignment's feature set and rules must all be preserved (confirmed). Presentation and layout may change.

- Tasks: title, description, category, priority, due date (date only), status: Open, In Progress, Postponed, Completed, Delayed.
- Delayed is assigned automatically to overdue, non-completed tasks; users cannot choose it.
- Categories are user-managed. "No Category" is built in and cannot be renamed or deleted. Deleting a category deletes its tasks and their reminders.
- Priorities are user-managed free-text levels. "Default" is built in and cannot be renamed or deleted. Deleting a priority moves its tasks to Default.
- Reminders: several per task, either one day, one week or one month before the due date, or on a specific date. A reminder cannot be on a completed task, after the due date, or in the past. Reminders are recalculated when the due date changes and removed when a task becomes Completed or Delayed.
- Search by any combination of title, category and priority.
- Summary of total, completed, delayed, and due within 7 days.
- Storage: JSON files in ~/.medialab-assistant, saved automatically on every change.
- Stack: Java 17, JavaFX 21 (controls only, layouts written in code), Gson, Maven, JUnit 5.

## Brand Commitments

- The product name stays "MediaLab Assistant" (assigned by the course).
- All UI text is English.

## Evidence on Hand

- docs/report.pdf: the original project report, in Greek.
- docs/screenshots/: README screenshots, made from synthetic demo data (no real user data exists).
- No real users, testimonials or usage metrics. Do not invent any.

## Product Principles

1. Deadlines first. What is overdue or due soon is always visible without searching for it.
2. Capture fast. Adding or updating a task should take seconds and be possible from the keyboard.
3. Calm, not alarming. Overdue work is surfaced clearly but without blocking the user or scolding them.
4. Honest automation. When the app changes something on its own (Delayed status, removed reminders), the user can see what happened.
5. Faithful to the spec. Every feature and rule of the original assignment keeps working.
