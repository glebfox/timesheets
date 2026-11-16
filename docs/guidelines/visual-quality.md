# Visual Quality

> Draft. Rules for the look of the project's own UI: the screens and components built from the project's own markup and stylesheets, where the theme decides little and the stylesheet the rest. They come from the design reviews of two screens, the dashboard and the approval screen (November 2026), and are not linked from `CLAUDE.md` yet. A rule is here only if both screens bear it out, or if it is a general principle that a screen merely illustrated; what is true of one screen only, such as how its blocks share a row, belongs in that screen's spec, not here. Theme tokens, colors, and components are in `design-system.md`, and what a screen says to help its user is in `ux.md`; this file is about how a finished screen must look, and how to check that it does.

---

## 1. Hierarchy

- A screen has one element that reads first, and it is the screen's subject or its main action. If nothing reads first, nothing is the answer to "what is this screen for"; if a selected filter or a decoration does, the answer is wrong.
- At most one filled button shows in a view at a time. A layer above the view, a dialog or a bar of a task the user has started, has one of its own. An action repeated down a list or a grid, such as one for each week or each day, is quieter than the one-off action above it: a button without a fill, or an icon.
- Neighboring levels of text differ by at least a quarter in size, or by a step of the theme's scale together with weight or color. One step in size alone is not a level.
- A screen has few levels of text, about four. A size or a weight that differs from its neighbor by a pixel is noise, not one more level.
- A heading of a group is more prominent than the headings and the text of what it groups.
- A block's key value is the largest text in the block.
- The squint test: on a blurred screenshot it is still clear where to look first, second, and third.

## 2. Spacing and grouping

- Three steps, each clearly larger than the one before: inside a block, between blocks, between sections. A block whose inner padding exceeds the gap to its neighbor merges with it.
- Parts that belong together stand closer to each other than to anything else.
- Space groups first, a line second, a box last. A box inside a box needs a reason, and a screen whose every part has the same border has no grouping left.
- A container's padding is not smaller than its corner radius; otherwise text sits against the curve. A part too small for such padding takes a smaller radius: a radius made for a panel turns a compact card into a bubble and a one-line header into a pill.
- Uniformity is in the padding around content, not in the size of the box. Rows of one line and rows of two lines are not made equally tall: they get the same padding, and their height follows.
- The distance from a block's title to its first line of content is the same in every block of the screen.

## 3. Alignment

- A container has one left edge and one right edge for its text, whatever the text sits in: a title, a plain row, a button, a link.
- Blocks stacked in one column share their left and right edges, whatever each is made of: a toolbar, a panel that scrolls, a grid. A container that pads itself, such as a scrolling panel that makes room for focus rings, still ends where the block above it ends.
- Identical parts, such as an avatar, a row, a caption, have one size and one position in every block of the screen. The same value in blocks that stand side by side is on one baseline, whatever else each block holds.
- Parts that end rows of different blocks stacked in one column end on one vertical.
- Numbers that are compared, down a column or across blocks, are set in tabular figures and share an edge.
- An interactive element has two edges: the edge of its text and the edge of its box (its highlight and focus ring). Aligning the text by moving the box outward works only while the box and its focus ring stay clear of the container's border, on every side, the bottom of the last element included. Decide the gap from the box to the border on purpose; it is the same on every side, and the inner radius is the outer radius less that gap.
- Nothing is clipped by accident: a badge, an avatar, an icon, a focus ring, or a color mark is whole in both themes. The usual causes are a component placed inside another component's text slot, and a container that hides its overflow to round its corners.

## 4. Layout

- A row of a grid is filled. A block alone in a row, with the rest empty, is stretched, paired, or moved.
- A part is not squeezed while the part beside it stands empty. Empty columns or rows give up their room, or the structure changes.
- A column of text is at least as wide as its longest ordinary word, and running text gets about 20 characters or more. Narrower, words break in the middle and a description shows two words to a line. A layout that cannot give that width changes its shape, to fewer columns or to a list, rather than squeeze.
- A region that scrolls says so: what it cuts off is visibly cut, not ended at a neat edge, and its scrollbar is within the window, not at the far end of a region taller than the window.
- A wider window never gives less: no part gets smaller, and nothing a narrower window showed is lost, as the window grows.
- An action stands next to what it acts on.

## 5. Color

- One color has one meaning on a screen. A status color is used only for the status and for the action that leads to it, the accent only for what is chosen or can be acted on, and neither for decoration.
- A row has at most one accent-colored element.
- A color that encodes something is explained without hovering: a legend or a label.
- Yellow and red are for what needs action. A state that needs none takes a neutral color or green, and a neutral mark is quiet: lighter than the marks beside it, never the darkest of them.
- Color never tells alone what no text or shape tells, and a part carries at most one color mark of a fact beside the text that names it: a card whose badge names its status may lead with a dot of that color, but does not also carry a line or a stripe in it.
- A color mark is a shape of its own inside the container's padding, a dot, a bar, or a badge, and never one edge of the container painted in the color. A stripe along the edge of a rounded box is clipped by its corners, merges with a focus ring or a selection (`design-system.md`), and reads as a template. A rule beside a block of quoted text, such as a reason given by someone else, is a quote mark, not such a stripe.
- A part that draws its own colors, such as a chart or an SVG drawing, is checked beside the theme's accent in both themes.

## 6. Interactive elements

- A control is the component its role names. A navigation is a link, an action is a button, a row of data is a row. A button is not a container for a layout: its label is one line of text and clips the rest.
- What cannot be clicked does not look like a control: a heading or a value gets no border, fill, and radius of a button, a chip, or a field.
- A control of the project's own has every state the theme's controls have: hover, focus, pressed, selected, disabled, and its look in forced colors (`design-system.md`).
- Making a whole row clickable is a choice with a price, the box and its edges above; a row whose name alone is clickable has no box to align.
- A repeated action in a list is an icon with a tooltip, not a text button in every row (`ux.md`).
- A pointer target is at least 24 by 24 pixels. On a screen that is used by touch it is at least 44 pixels tall; the room comes from the element's own padding, not from a visible box.

## 7. Content

- One format for each kind of value on a screen: dates, durations, counts.
- The year of a date is shown only when it is not the current one. A title that steps through periods is the exception: it always carries its year. A range is folded: "Oct 18 – 24".
- A zero unit is not written: "30m", not "0h 30m".
- Neighboring rows that say the same are merged into one, and a row with nothing to say says nothing: a column of zeros is noise.
- Blocks that stand side by side as equals say the same kind of value first, in the same unit: not a time in one and a count in the next.
- A secondary attribute is a second line or a separate part, not brackets inside the text, and not a second phrase told apart by color alone.
- When a text is cut, what remains is the part that identifies the row. A status and a number are never cut.

## 8. Checking a screen

- Screenshots at the widths the screen is made for, before the work is called done: the widest common one, every width where its layout switches, and the narrowest it supports, a phone where it has a layout for one. Each in both themes and, where the screen's width depends on it, with the navigation drawer open and collapsed (`design-system.md`). Widen the window as well as narrow it.
- Screenshots at the device's own pixels: one scaled down to fit a pane hides a fault of a pixel or two.
- Measure, do not eyeball: the left and right edges of every line of text in a container and of the blocks stacked in a column, the pitch of rows, the baseline of the same value in blocks side by side, and whatever a container clips.
- Check with the keyboard: the focus ring of every focusable element is whole and clear of its container's border.
- Check in forced colors: what only a fill or a color tells still shows (`design-system.md`).
- Check the longest text the screen can get, the other language included, at the narrowest width.
- Check with no data, with one item, with a period only partly filled, and with as much as the screen shows. Demo data rarely gives the first two; make the users that do.
- After a fix, look at what it moved in the neighbors of the part it changed.

## Open

Not settled as general rules. Where a screen decided one of these, the decision is that screen's and is in its spec:

- How blocks of unequal content share a row: equal heights with empty space, or their own heights.
- Whether a wide row keeps its values and its actions next to its text or at the far edge.
- Blocks with no data: where the message stands, how loud it is, and when a block is hidden instead.
- When the items of a list are cards and when they are rows parted by lines.
- Charts: fonts, tooltips, plan marks, the current period, and how a chart uses extra room.
- A palette for categories and statuses that holds across charts, badges, and lists, and whether a status that asks nothing of a screen's user, such as New on the author's own screens, keeps its color there or turns neutral.
