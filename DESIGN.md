---
name: MediaLab Assistant
description: A deadline-first desktop task manager styled as a tickler file of index cards in a steel drawer.
colors:
  steel-ground: "#ECEFF2"
  drawer-rail: "#E2E6EB"
  card-stock: "#FFFFFF"
  ink: "#1E2430"
  ink-secondary: "#4A5363"
  ink-tertiary: "#565F6E"
  hairline: "#D5DBE2"
  control-edge: "#848E9C"
  rule-blue: "#2F5B9A"
  rule-blue-deep: "#264C82"
  rule-blue-wash: "#E3EBF6"
  overdue-red: "#B42318"
  overdue-wash: "#FBEAE8"
  overdue-card-edge: "#EBC3BE"
  card-edge-hover: "#BCC4CE"
  drawer-hover: "#D8DDE4"
  button-hover: "#F4F6F8"
  button-pressed: "#E9ECF0"
  card-completed: "#F7F8FA"
  stamp-edge: "#B9C1CB"
  row-rule: "#EEF1F4"
typography:
  display:
    fontFamily: "System"
    fontSize: "27px"
    fontWeight: 700
  headline:
    fontFamily: "System"
    fontSize: "17px"
    fontWeight: 700
  title:
    fontFamily: "System"
    fontSize: "14px"
    fontWeight: 700
  body:
    fontFamily: "System"
    fontSize: "13px"
    fontWeight: 400
  meta:
    fontFamily: "System"
    fontSize: "12px"
    fontWeight: 400
  label:
    fontFamily: "System"
    fontSize: "11px"
    fontWeight: 700
  stamp:
    fontFamily: "SF Mono, Menlo, Cascadia Mono, Consolas, JetBrains Mono, DejaVu Sans Mono, Liberation Mono, Ubuntu Mono, Monospaced"
    fontSize: "12px"
    fontWeight: 700
rounded:
  stamp: "3px"
  chip: "4px"
  tooltip: "5px"
  control: "6px"
  card: "7px"
spacing:
  hair: "2px"
  xs: "4px"
  sm: "8px"
  md: "12px"
  lg: "20px"
  xl: "24px"
  empty: "48px"
components:
  button-primary:
    backgroundColor: "{colors.rule-blue}"
    textColor: "{colors.card-stock}"
    rounded: "{rounded.control}"
    padding: "6px 12px"
  button-primary-hover:
    backgroundColor: "{colors.rule-blue-deep}"
  button-secondary:
    backgroundColor: "{colors.card-stock}"
    textColor: "{colors.ink}"
    rounded: "{rounded.control}"
    padding: "6px 12px"
  button-secondary-hover:
    backgroundColor: "{colors.button-hover}"
  button-secondary-pressed:
    backgroundColor: "{colors.button-pressed}"
  button-danger:
    backgroundColor: "{colors.card-stock}"
    textColor: "{colors.overdue-red}"
    rounded: "{rounded.control}"
    padding: "6px 12px"
  segmented-selected:
    backgroundColor: "{colors.rule-blue-wash}"
    textColor: "{colors.rule-blue}"
    padding: "5px 10px"
  input-field:
    backgroundColor: "{colors.card-stock}"
    textColor: "{colors.ink}"
    rounded: "{rounded.control}"
  drawer-item:
    textColor: "{colors.ink}"
    rounded: "{rounded.control}"
    padding: "6px 10px"
  drawer-item-hover:
    backgroundColor: "{colors.drawer-hover}"
  drawer-item-selected:
    backgroundColor: "{colors.card-stock}"
    textColor: "{colors.rule-blue}"
  task-card:
    backgroundColor: "{colors.card-stock}"
    textColor: "{colors.ink}"
    rounded: "{rounded.card}"
    padding: "11px 14px 11px 12px"
  task-card-completed:
    backgroundColor: "{colors.card-completed}"
    textColor: "{colors.ink-tertiary}"
  divider-tab:
    backgroundColor: "{colors.drawer-rail}"
    textColor: "{colors.ink}"
    typography: "{typography.headline}"
    padding: "6px 12px 5px 12px"
  divider-tab-overdue:
    backgroundColor: "{colors.overdue-wash}"
    textColor: "{colors.overdue-red}"
  priority-clip:
    backgroundColor: "{colors.steel-ground}"
    textColor: "{colors.ink-secondary}"
    rounded: "{rounded.chip}"
    padding: "1px 6px"
  status-clip:
    backgroundColor: "{colors.card-stock}"
    textColor: "{colors.ink-secondary}"
    rounded: "{rounded.chip}"
    padding: "1px 6px"
  due-stamp:
    textColor: "{colors.ink-tertiary}"
    typography: "{typography.stamp}"
    rounded: "{rounded.stamp}"
    padding: "2px 7px"
  due-stamp-soon:
    textColor: "{colors.ink}"
  due-stamp-overdue:
    backgroundColor: "{colors.overdue-wash}"
    textColor: "{colors.overdue-red}"
  summary-item:
    textColor: "{colors.ink-secondary}"
    rounded: "{rounded.control}"
    padding: "5px 10px"
  banner:
    backgroundColor: "{colors.rule-blue-wash}"
    textColor: "{colors.ink}"
    rounded: "{rounded.card}"
    padding: "10px 12px 10px 14px"
  tooltip:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.card-stock}"
    rounded: "{rounded.tooltip}"
    padding: "5px 8px"
---

# Design System: MediaLab Assistant

## Overview

**Creative North Star: "The Tickler File"**

Tasks are white index cards filed by due date in a steel-grey drawer. The window is the drawer: a cool grey ground, a slightly darker rail on the left holding the drawers (All, Overdue, Next 7 days, each category), and a long column of cards grouped behind divider tabs that stand up out of the ground (Overdue, Today, Next 7 days, Later, Completed). Each card carries fixed furniture: a check box on the left, a bold title, a note line, a meta line with category and a priority clip, and a monospaced date-due stamp on the right, like a library slip.

The system is calm and dense in the way a desktop work tool should be. Hierarchy comes from size contrast and weight, not from color: one blue marks what you can act on or have selected, and red is kept for lateness and destruction. Depth is almost flat; cards lift by a hairline edge and a barely-there shadow. JavaFX looked-up colors (`tk-*` on `.root`) are the source of truth, and borders are drawn by layering two backgrounds with an inset rather than with stroke borders.

**Key Characteristics:**
- Steel ground, white card stock, ink-dark text; one blue accent, one red for overdue.
- Cards and controls drawn with layered backgrounds: an edge color at inset 0, the fill at inset 1.
- Due dates as uppercase monospaced stamps; the stamp's border weight and color carry urgency.
- Section headings are divider tabs (top corners rounded only) that read as part of the drawer.
- Focus is a 2px ring of rule blue drawn as an outer background layer, shown on keyboard focus only (`:focus-visible`).
- Thin 1.8-stroke line icons on a 24-unit grid, rendered at 16px, colored by the theme.

## Colors

A cool, low-chroma steel palette with a single ink-blue accent and a single brick red, both used sparingly.

### Primary
- **Index-Card Rule Blue** (`rule-blue`): the only accent. The primary button (New task, Save changes), the selected drawer's name and icon, the selected card's 1.5px edge, the selected segment in the group-by control, checked check boxes, and every keyboard focus ring. Also JavaFX's `-fx-accent` and `-fx-focus-color`.
- **Deep Rule Blue** (`rule-blue-deep`): hover state of the primary button only.
- **Rule Blue Wash** (`rule-blue-wash`): selection fill for table rows, plain list rows and context-menu items; the selected segment's fill; the outer halo of the primary button's and check box's focus ring; the background of informational banners (today's reminders).

### Tertiary
- **Overdue Brick Red** (`overdue-red`): overdue and Delayed work (the Overdue tab title and count, the overdue stamp, the "N days overdue" meta line, the nonzero delayed count in the summary bar and drawer), inline error text in dialogs, and destructive actions (danger buttons, danger menu items).
- **Overdue Wash** (`overdue-wash`): the Overdue divider tab and the overdue stamp's fill.
- **Overdue Card Edge** (`overdue-card-edge`): the 1px edge of an overdue card.

### Neutral
- **Steel Ground** (`steel-ground`): the window ground, the card-list sheet, priority clips, summary-item hover.
- **Drawer Rail** (`drawer-rail`): the sidebar and the resting divider tab.
- **Card Stock** (`card-stock`): cards, panels, inputs, buttons, dialogs, the summary bar.
- **Ink** (`ink`): primary text; the tooltip fill.
- **Secondary Ink** (`ink-secondary`): notes, meta lines, subtitles, field labels, captions, default icon stroke.
- **Tertiary Ink** (`ink-tertiary`): counts, hints, prompt text, section and column labels, resting stamps, completed titles.
- **Hairline** (`hairline`): card and panel edges, the summary bar's bottom rule, the status clip edge.
- **Control Edge** (`control-edge`): the 1px edge of every button, input, combo box and check box; darker than the hairline so controls read as controls.
- **State neutrals**: `card-edge-hover` (card edge on hover), `drawer-hover` (drawer row hover), `button-hover` and `button-pressed` (secondary button fills), `card-completed` (completed card fill), `stamp-edge` (resting stamp border), `row-rule` (table and list row dividers).

### Named Rules
**The One Blue Rule.** Rule blue appears only on what is actionable, selected or focused, plus its wash on informational banners. It never decorates a heading, a divider, or static text.

**The Red Is Late Rule.** Red means overdue, error, or destructive. A nonzero Delayed count turns red; a zero count stays ink. Do not use red for priority or emphasis.

## Typography

**Display Font:** the platform UI face (JavaFX default "System")
**Body Font:** the platform UI face
**Label/Mono Font:** first installed of SF Mono, Menlo, Cascadia Mono, Consolas, JetBrains Mono, DejaVu Sans Mono, Liberation Mono, Ubuntu Mono, falling back to Monospaced. JavaFX CSS has no fallback lists, so `ui/Theme.java` resolves the family at startup and injects a generated stylesheet for `.stamp` and `.mono`.

**Character:** a plain native sans that stays out of the way, set against one mechanical monospaced voice for dates, the way a date-due slip is stamped rather than written.

### Hierarchy
- **Display** (bold, 27px): the view title (All tasks, Reminders, a category name). One per view.
- **Headline** (bold, 17px): divider-tab section titles, empty-state titles, dialog headers.
- **Title** (bold, 14px): card titles and the app name. Summary counts are bold 15px.
- **Body** (regular, 13px): the root size; notes, controls, table cells, subtitles.
- **Meta** (regular, 12px): card meta lines, hints, error text, drawer counts, tooltips; field labels are bold 12px in secondary ink.
- **Label** (bold, 11px, sentence case): sidebar group labels (Categories, Manage). Table column headers and clips sit at 11.5px.
- **Stamp** (bold, 12px, monospaced, uppercase content such as SUN 4 OCT, TODAY, DONE): the due-date stamp only.

### Named Rules
**The Stamp Rule.** Monospace is reserved for due dates. Dates in tables, dialogs and meta lines stay in the UI face.

**The Size Not Color Rule.** Hierarchy is built from the 27 / 17 / 14 / 13 / 12 steps and weight; color ink steps (ink, secondary, tertiary) only separate primary from supporting text.

## Layout

A fixed desktop frame. A full-width summary bar on card stock runs across the top (padding 10px 20px, hairline bottom rule): app name left, four counts right, each count a clickable filter. Below it, the sidebar rail (preferred 230px, minimum 200px, padding 16px 10px, 2px between rows) holds the primary New task button, the fixed drawers, the category drawers with counts, and the Manage group. The content area has a 20px gutter (views pad 22px top, 20px sides), a view title with a secondary subtitle, a filter bar 14px below, then the card list.

Cards stack with 6px between them (3px cell padding top and bottom). A section break adds 20px above its divider tab; the first section sits 4px down. The card list carries a 20px gutter left, right and bottom. Dialogs pad 20px 24px for the header, 12px 24px for content, and 12px 24px 20px for the button bar. Empty states center with 48px vertical padding and 8px spacing. At narrower widths the layout keeps its structure; the content column narrows and the filter bar compresses.

## Elevation & Depth

Nearly flat. Depth comes from tone (ground, rail, card stock) and from hairline edges drawn as a background layer. Task cards alone carry a soft ambient shadow so they read as stock lying on the ground; completed cards drop it. Nothing else casts a shadow, and nothing lifts on hover; hover darkens the edge instead.

### Shadow Vocabulary
- **Card rest** (`dropshadow(gaussian, #1E243014, 6, 0, 0, 1)`): task cards only.

### Named Rules
**The Edge Not Lift Rule.** State changes move the edge color (hairline to `card-edge-hover`, to rule blue when selected), never the shadow or position.

## Shapes

Small, consistent corners. Controls and drawer rows round to 6px, cards, panels, banners and context menus to 7px, chips and check boxes to 4px, stamps to 3px. Outer and inner radii are paired so the edge stays even: 6/5 for controls, 7/6 for cards and panels, 4/3 for check boxes and status clips. Divider tabs round only their top corners (7px 7px 0 0) so they read as tabs rising from the card column. Segmented controls round only their outer ends. Icons are open line drawings with round caps and joins.

## Components

### Buttons
- **Shape:** gently rounded (6px outer, 5px inner).
- **Secondary (default):** card-stock fill inside a 1px control-edge ring, ink text, 6px 12px padding. Hover fills `button-hover`, pressed fills `button-pressed`. Disabled drops to 45% opacity.
- **Primary:** solid rule blue, white bold text, white icon, no edge ring; hover deepens to `rule-blue-deep`. One per context (New task in the sidebar, the confirming action in a dialog).
- **Danger:** the secondary button with red text.
- **Quiet:** transparent, 4px padding, hover fills black at about 6% (`#00000010`); used for icon-only actions such as dismissing a banner.
- **Focus:** keyboard focus only. Secondary buttons swap the edge to a 2px rule-blue ring; the primary button gains a 2px rule-blue-wash halo outside its fill (radius 8px).

### Segmented control (group by)
- **Style:** two toggle buttons sharing an edge, 5px 10px padding, outer ends rounded.
- **Selected:** rule-blue edge, rule-blue-wash fill, rule-blue bold text.

### Chips (clips)
- **Priority clip:** steel-ground fill, secondary ink, 4px radius, 1px 6px padding, 11.5px. Neutral by design: priority is never colored.
- **Status clip:** card-stock fill with a hairline edge, same type; shown only for non-default states such as In Progress.

### Cards / Containers
- **Task card:** card stock with a 1px hairline edge (7px outer, 6px inner), padding 11px 14px 11px 12px, 12px between check box, text and stamp, card-rest shadow. Hover darkens the edge; selected swaps it for a 1.5px rule-blue edge; overdue uses `overdue-card-edge`; completed uses the `card-completed` fill, no shadow, tertiary non-bold struck-through title.
- **Panel:** the container for management tables and lists: card stock, hairline edge, 7px radius, no shadow.
- **Banner:** rule-blue-wash fill, 7px radius, 10px 12px 10px 14px padding, rule-blue icon, quiet dismiss button. Informational only (today's reminders).

### Inputs / Fields
- **Style:** card-stock fill in a 1px control-edge ring, 6px radius; prompt text in tertiary ink. The search field indents text 30px for its leading icon. The date picker's arrow is a drawn calendar glyph in secondary ink.
- **Focus:** the ring thickens to 2px rule blue.
- **Check box:** 4px-radius box with a 1.5px control-edge ring; checked fills solid rule blue with a white mark; keyboard focus adds a 3px rule-blue-wash halo.
- **Error:** 12px red text under the fields, inside the dialog, while the input is kept.

### Navigation
- **Drawer rows:** transparent at rest, 6px radius, 6px 10px padding, 16px line icon in secondary ink, name, right-aligned count in tertiary ink at 12px. Hover fills `drawer-hover`. Selected becomes a card-stock tab with the name bold in rule blue and the icon stroked blue. An alert drawer (Overdue with items) shows its count bold red. Keyboard focus draws a 2px rule-blue ring.
- **Group labels:** bold 11px tertiary ink, sentence case, 16px above and 6px below.
- **Summary items:** bold 15px count plus secondary caption; hover fills steel ground; alert items turn both red.

### Due-date stamp (signature)
The library date-due stamp: bold 12px monospaced uppercase text in a 1px `stamp-edge` border, 3px radius, 2px 7px padding, never shrinking. Urgency changes the stamp, not the card: **soon** (due within 7 days) darkens to ink text and a 1.5px secondary-ink border; **overdue** goes red on overdue wash with a translucent red border (`#B4231899`); **done** fades to tertiary text and a hairline border. A tooltip gives the full date.

### Divider tab (signature)
Section headings for the card list: drawer-rail fill, top corners rounded 7px, 6px 12px 5px 12px padding, bold 17px title plus a 12px tertiary count. The Overdue tab uses the overdue wash with red title and count.

### Tables and plain lists
Card-stock rows 40px high with a `row-rule` divider; column headers 34px, bold 11.5px tertiary ink, hairline bottom rule; selection fills rule-blue wash. Date cells use secondary ink. No zebra striping.

### Dialogs, tooltips, menus
Dialogs are plain card stock with a bold 17px header and the primary action at the right of the button bar. Tooltips are ink with white 12px text, 5px radius. Context menus are hairline-edged card stock, 7px radius, focused item in rule-blue wash; destructive items in red.

## Do's and Don'ts

### Do:
- **Do** take every color from the `tk-*` looked-up colors on `.root`; add a new one there before using a new value.
- **Do** draw edges as layered backgrounds (edge color at inset 0, fill at inset 1) with paired radii (6/5, 7/6, 4/3).
- **Do** show focus only on `:focus-visible`, as a 2px rule-blue ring (or a rule-blue-wash halo on solid blue elements).
- **Do** express deadline urgency through the stamp and the divider tab: overdue is red, soon is a heavier ink stamp, done is faded.
- **Do** keep priority neutral: a grey clip, never a color scale.
- **Do** use 16px line icons from `ui/Icons.java`, stroked by the `icon` class.

### Don't:
- **Don't** use rule blue on static text, headings or decoration; it marks only action, selection, focus and the info banner.
- **Don't** use red for anything except overdue or Delayed work, errors and destructive actions.
- **Don't** add shadows beyond the card-rest shadow, or lift elements on hover.
- **Don't** use monospace outside the due-date stamp.
- **Don't** stripe table rows or return to a data-table-plus-CRUD-buttons layout for the task list; tasks are cards.
