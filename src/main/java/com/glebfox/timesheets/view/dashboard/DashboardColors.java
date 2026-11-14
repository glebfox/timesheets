/*
 * Copyright 2026 Gleb Gorelov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.glebfox.timesheets.view.dashboard;

import com.glebfox.timesheets.app.util.WeekStatus;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import io.jmix.chartsflowui.kit.component.model.shared.Color;

import java.util.List;

/**
 * The colors that the dashboard's charts give the statuses of weeks and of time entries, and the marks they draw.
 * <p>
 * A chart draws on a canvas and takes literal colors, not the theme's custom properties. When the user switches
 * the theme, the Charts add-on draws the chart again in ECharts' own light or dark theme, which colors what the
 * dashboard leaves to it, such as the axes' labels and the grid's lines, but the colors set here stay as they are.
 * So each color is one literal for both themes. A status takes the color the app gives the same status, on the
 * calendar or the approval screen: the token's value in the dark theme where the token differs between the themes,
 * as the light theme's darker value would not read on the dark card. The user chose this consistency with the app
 * over a contrast of at least 3:1 (WCAG 1.4.11) against the card's surface, so the colors that fall below it, New's
 * yellow and the half-strength blue of a week in progress, say so. The ratios beside each color are against the
 * light card's surface, about {@code #F6F8FA}, and the dark one, about {@code #1C1F24}: Aura's card surface over
 * the work area's, computed from {@code aura.css}.
 * <p>
 * The colors are written as the project's stylesheets write them, in {@code oklch()}, except a fill of a bar or a
 * slice: ECharts lightens a fill under the pointer through zrender's color parser, which reads only hex,
 * {@code rgb()}, {@code hsl()}, and named colors, and a fill it cannot read vanishes while the pointer is over it.
 * Such a fill is written in hex or {@code rgba()}, with its {@code oklch()} beside it. A line's color, a hatching's,
 * and a label's are drawn as they are, so they take {@code oklch()}.
 */
public final class DashboardColors {

    /**
     * Neutral, a fill: the mid gray between {@code --vaadin-text-color-secondary} of the light theme and of the dark
     * one, which differ, and are each nearly invisible on the other theme's card. {@code oklch(0.599 0.01 264.5)}.
     * 3.72:1 light, 4.17:1 dark.
     */
    private static final Color NEUTRAL = new Color("#7D8086");

    /**
     * Green, a fill: {@code --ts-success-color-dark}, {@code oklch(0.59 0.2 150)} as sRGB shows it, On plan on the
     * calendar and Approved on the approval screen. 3.44:1 light, 4.51:1 dark.
     */
    private static final Color SUCCESS = new Color("#009B31");

    /**
     * Red, a fill: {@code --ts-danger-color-dark}, {@code oklch(0.59 0.2 25)}, the calendar's time short of plan
     * and the approval screen's Rejected. 4.27:1 light, 3.64:1 dark.
     */
    private static final Color DANGER = new Color("#DB373A");

    /**
     * Blue, a fill: {@code --ts-primary-color-dark}, {@code oklch(0.59 0.2 277)}, the accent, which marks Over on
     * the calendar. 4.13:1 light, 3.76:1 dark.
     */
    private static final Color ACCENT = new Color("#6468F0");

    /**
     * The plan's dashed mark, neutral as the calendar's plan figures are: {@link #NEUTRAL}'s color, as a line.
     * 3.72:1 light, 4.17:1 dark.
     */
    public static final Color PLAN = new Color("oklch(0.599 0.01 264.5)");

    /**
     * The fill of a day off, under its hatching: the neutral, mostly transparent, {@code oklch(0.599 0.01 264.5 /
     * 0.18)}. It only repeats the day's label, which names the absence or the holiday, so it needs no contrast of its
     * own.
     */
    public static final Color OFF_DAY = new Color("rgba(125, 128, 134, 0.18)");

    /**
     * The hatching of a day off, over its fill. Decorative, as {@link #OFF_DAY}.
     */
    public static final Color OFF_DAY_HATCH = new Color("oklch(0.599 0.01 264.5 / 0.35)");

    /**
     * The time a person of the team spent on projects where the current user does not approve them: a plain total,
     * in the neutral, as Closed is, and told apart from Closed by the stripes of
     * {@link #OTHER_PROJECTS_STRIPE} over it. 3.72:1 light, 4.17:1 dark.
     */
    public static final Color OTHER_PROJECTS = NEUTRAL;

    /**
     * The stripes over {@link #OTHER_PROJECTS}: light enough to show on the neutral in both themes. Decorative: the
     * tooltip names the segment.
     */
    public static final Color OTHER_PROJECTS_STRIPE = new Color("oklch(1 0 0 / 0.45)");

    /**
     * The line of an axis: the neutral, partly transparent, so it comes out about as Aura's border color,
     * {@code --vaadin-border-color}, over the card in both themes. Decorative: the labels beside it carry the values.
     */
    public static final Color AXIS_LINE = new Color("oklch(0.599 0.01 264.5 / 0.4)");

    /**
     * A chart's background, so the card's surface shows through in either theme.
     */
    public static final Color TRANSPARENT = new Color("transparent");

    /**
     * The colors of the projects in Time by project, fills, a slot each by the project's size, the largest first, so
     * the largest project is always blue; picked to tell apart and to sit well together. The ratios are against the
     * light and the dark card's surface, and each stays at 3:1 or more against the extremes too.
     */
    private static final List<Color> PROJECTS = List.of(
            // oklch(0.578 0.17 268.4). 4.19:1 light, 3.70:1 dark
            new Color("#5070DD"),
            // oklch(0.627 0.163 127.6). 3.18:1 light, 4.87:1 dark
            new Color("#6C9900"),
            // oklch(0.579 0.239 352.6). 4.62:1 light, 3.36:1 dark
            new Color("#D80087"),
            // oklch(0.551 0.121 56.1). 4.73:1 light, 3.28:1 dark; 3.09:1 against the dark card's solid surface
            new Color("#A65C1C"),
            // oklch(0.631 0.123 331.6). 3.48:1 light, 4.46:1 dark
            new Color("#B46CAB"),
            // oklch(0.643 0.216 36.8). 3.44:1 light, 4.52:1 dark
            new Color("#F34800"));

    /**
     * The projects that Time by project counts together as Other: the neutral. 3.72:1 light, 4.17:1 dark.
     */
    public static final Color OTHER_PROJECT_SLICE = NEUTRAL;

    private DashboardColors() {
    }

    /**
     * Returns the color of a project in Time by project by its place among the projects, the largest first.
     *
     * @param slot the project's place, from zero
     * @return the color; the slots repeat beyond the last
     */
    public static Color ofProject(int slot) {
        return PROJECTS.get(slot % PROJECTS.size());
    }

    /**
     * Returns the color of a week's status, as the calendar's week summary shows it.
     *
     * @param status the status
     * @return the color
     */
    public static Color of(WeekStatus status) {
        return switch (status) {
            case OVER -> ACCENT;
            case ON_PLAN -> SUCCESS;
            // The red of time short of plan on the calendar, which marks a short day, so a shortfall stands out; the
            // calendar's week summary draws a short week in --aura-orange, but the user chose the red
            case SHORT -> DANGER;
            // The accent at half strength, as the calendar's week summary draws a week in progress or upcoming:
            // oklch(0.59 0.2 277 / 0.5). Over the card 1.91:1 light, 1.90:1 dark: below 3:1, the user's choice
            case IN_PROGRESS, UPCOMING -> new Color("rgba(100, 104, 240, 0.5)");
            // The calendar gives No workdays no color of its own; such a week has no time to draw anyway
            case NO_WORKDAYS -> NEUTRAL;
        };
    }

    /**
     * Returns the color of a time entry's status, as the approval screen's status bar shows it.
     *
     * @param status the status
     * @return the color
     */
    public static Color of(TimeEntryStatus status) {
        return switch (status) {
            // --ts-warning-color, oklch(0.89 0.2 98), the same in both themes: the yellow of New on the calendar and
            // the approval screen. 1.29:1 light, 11.99:1 dark: below 3:1 on the light card, the user's choice; the
            // legend and the tooltip name the segment
            case NEW -> new Color("#FEDA00");
            case APPROVED -> SUCCESS;
            case REJECTED -> DANGER;
            // The neutral of Closed on the approval screen
            case CLOSED -> NEUTRAL;
        };
    }
}
