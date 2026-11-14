# Visual Quality

> Draft. These rules come from the design review of one screen, the dashboard (November 2026), and are not linked from `CLAUDE.md` yet: they are to be tried on a few more screens first, to keep what holds everywhere and drop what was the dashboard's own. What is true of one screen only, such as how its blocks share a row, belongs in that screen's spec, not here. Theme tokens, colors, and components are in `design-system.md`; this file is about how a finished screen must look, and how to check that it does.

---

## 1. Hierarchy

- A screen has one element that reads first. If nothing does, nothing is the answer to "what is this screen for".
- Neighboring levels of text differ by at least a quarter in size, or clearly in weight and color. A two-pixel step between a page title and a block title is not a level.
- A heading of a group is more prominent than the headings inside the group.
- A block's key figure is the largest text in the block.
- The squint test: on a blurred screenshot it is still clear where to look first, second, and third.

## 2. Spacing

- Three steps, each clearly larger than the one before: inside a block, between blocks, between sections. A block whose inner padding exceeds the gap to its neighbor merges with it.
- A container's padding is not smaller than its corner radius; otherwise text sits against the curve.
- Uniformity is in the padding around content, not in the size of the box. Rows of one line and rows of two lines are not made equally tall: they get the same padding, and their height follows.
- The distance from a block's title to its first line of content is the same in every block of the screen.

## 3. Alignment

- A container has one left edge and one right edge for its text, whatever the text sits in: a title, a plain row, a button, a link.
- Identical parts, such as an avatar, a row, a caption, have one size and one position in every block of the screen.
- Parts that end rows of different blocks stacked in one column end on one vertical.
- An interactive element has two edges: the edge of its text and the edge of its box (its highlight and focus ring). Aligning the text by moving the box outward works only while the box and its focus ring stay clear of the container's border, on every side, the bottom of the last element included. Decide the gap from the box to the border on purpose; it is the same on every side, and the inner radius is the outer radius less that gap.
- Nothing is clipped by its container: a badge, an avatar, an icon, or a focus ring is whole in both themes. A component placed inside another component's text slot is the usual cause.

## 4. Layout

- A row of a grid is filled. A block alone in a row, with the rest empty, is stretched, paired, or moved.
- An action stands next to what it acts on.

## 5. Color

- One color has one meaning on a screen. A status color is used only for the status, the accent only for what can be acted on.
- A row has at most one accent-colored element.
- A color that encodes something is explained without hovering: a legend or a label.
- A neutral state, such as New or Draft, takes a neutral color. Yellow and red are for what needs action.
- A part that draws its own colors, such as a chart, is checked beside the theme's accent in both themes.

## 6. Interactive elements

- A control is the component its role names. A navigation is a link, an action is a button, a row of data is a row. A button is not a container for a layout: its label is one line of text and clips the rest.
- Making a whole row clickable is a choice with a price, the box and its edges above; a row whose name alone is clickable has no box to align.
- A repeated action in a list is an icon with a tooltip, not a text button in every row.
- A touch target is at least 44 pixels tall; the room comes from the element's own padding, not from a visible box.

## 7. Content

- One date format on a screen. The year is shown only when it is not the current one. A range is folded: "Oct 18 – 24".
- Neighboring rows that say the same are merged into one.
- A zero unit is not written: "30m", not "0h 30m".
- A secondary attribute is a second line or a separate part, not brackets inside the text, and not a second phrase told apart by color alone.
- When a text is cut, what remains is the part that identifies the row.

## 8. Checking a screen

- Screenshots at three widths, a desktop, a width where the layout switches, and a phone, in both themes, before the work is called done.
- Measure, do not eyeball: the left and right edges of every line of text in a container, the pitch of rows, and whatever a container clips.
- Check with the keyboard: the focus ring of every focusable element is whole and clear of its container's border.
- Check the longest text the screen can get, the other language included, at the narrowest width.
- Check with no data, with one item, and with as much as the screen shows. Demo data rarely gives the first two; make the users that do.
- After a fix, look at what it moved in the neighbors of the part it changed.

## Open

Not settled as general rules. Where the dashboard decided one of these, the decision is the dashboard's and is in its spec:

- How blocks of unequal content share a row: equal heights with empty space, or their own heights.
- Whether a wide row keeps its values next to its text or at the far edge.
- Blocks with no data: where the message stands, how loud it is, and when a block is hidden instead.
- Charts: fonts, tooltips, plan marks, the current period, and how a chart uses extra room.
- A palette for categories and statuses that holds across charts, badges, and lists.
