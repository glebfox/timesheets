# 0019 The dashboard's charts come from the Jmix Charts add-on

Status: accepted

## Context

The dashboard needs bar charts, a horizontal bar chart, and a donut. The approval screen draws its own month grid and week board ([decision 0008](0008-the-approval-screen-draws-its-own-month-grid-and-week-board.md)), so the charts could have been drawn by the project too.

Alternatives considered and rejected:

- **Charts drawn by the project, in HTML and CSS or SVG.** More code of the project's own for drawing that a library already does; custom charts can come later if ever needed.

## Decision

The free Jmix Charts add-on, `io.jmix.charts:jmix-charts-flowui-starter`, which draws Apache ECharts on a canvas. Decided by the user (issue #69).

## Consequences

- Colors in a chart's options are literals, not the theme's custom properties ([decision 0021](0021-the-dashboards-charts-color-statuses-as-the-app-does-in-one-color-for-both-themes.md)).
- What the add-on has no API for, such as options by the chart's width and where a tooltip is drawn, goes in as ECharts' native JSON, one per chart.
- A chart draws at the height the stylesheet gives it, not at its content's, and its tooltip is drawn on the page's body so the chart can clip its canvas.
- A UI test reads a chart's options on the server; what reaches the browser it sees only by reporting the chart ready (`docs/guidelines/testing.md`).
