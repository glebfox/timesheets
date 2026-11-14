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
 * So each color is one literal for both themes, picked close to the token the calendar or the approval screen uses
 * for the same status, and with a contrast of at least 3:1 (WCAG 1.4.11) against the card's surface in both
 * themes. The ratios beside each color are against the light
 * card's surface, about {@code #F6F8FA}, and the dark one, about {@code #1C1F24}: Aura's card surface over the
 * work area's, computed from {@code aura.css}. Against the extremes, pure white and the dark card's solid surface
 * {@code #21242A}, each stays at 3:1 or more as well.
 */
public final class DashboardColors {

    /**
     * Neutral: the mid gray of {@code --vaadin-text-color-secondary}. 3.72:1 light, 4.17:1 dark.
     */
    private static final Color NEUTRAL = new Color("#7D8086");

    /**
     * Green: {@code --ts-success-color} (On plan on the calendar, Approved on the approval screen). 3.90:1 light,
     * 3.98:1 dark.
     */
    private static final Color SUCCESS = new Color("#00903F");

    /**
     * The plan's dashed mark, neutral as the calendar's plan figures are. 3.72:1 light, 4.17:1 dark.
     */
    public static final Color PLAN = NEUTRAL;

    /**
     * The fill of a day off, under its hatching: the neutral, mostly transparent. It only repeats the day's label,
     * which names the absence or the holiday, so it needs no contrast of its own.
     */
    public static final Color OFF_DAY = new Color("rgba(125, 128, 134, 0.18)");

    /**
     * The hatching of a day off, over its fill. Decorative, as {@link #OFF_DAY}.
     */
    public static final Color OFF_DAY_HATCH = new Color("rgba(125, 128, 134, 0.35)");

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
    public static final Color OTHER_PROJECTS_STRIPE = new Color("rgba(255, 255, 255, 0.45)");

    /**
     * The line of an axis: the neutral, partly transparent, so it comes out about as Aura's border color,
     * {@code --vaadin-border-color}, over the card in both themes. Decorative: the labels beside it carry the values.
     */
    public static final Color AXIS_LINE = new Color("rgba(125, 128, 134, 0.4)");

    /**
     * A chart's background, so the card's surface shows through in either theme.
     */
    public static final Color TRANSPARENT = new Color("transparent");

    /**
     * The colors of the projects in Time by project, a slot each by the project's size, the largest first, so the
     * largest project is always blue; picked to tell apart and to sit well together. The ratios are against the
     * light and the dark card's surface, and each stays at 3:1 or more against the extremes too.
     */
    private static final List<Color> PROJECTS = List.of(
            // 4.19:1 light, 3.70:1 dark
            new Color("#5070DD"),
            // 3.18:1 light, 4.87:1 dark
            new Color("#6C9900"),
            // 4.62:1 light, 3.36:1 dark
            new Color("#D80087"),
            // 4.73:1 light, 3.28:1 dark; 3.09:1 against the dark card's solid surface
            new Color("#A65C1C"),
            // 3.48:1 light, 4.46:1 dark
            new Color("#B46CAB"),
            // 3.44:1 light, 4.52:1 dark
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
            // --ts-primary-color, the accent of Over on the calendar. 4.51:1 light, 3.44:1 dark
            case OVER -> new Color("#4A6BD7");
            case ON_PLAN -> SUCCESS;
            // --aura-orange, Short on the calendar. 3.39:1 light, 4.58:1 dark
            case SHORT -> new Color("#BC7800");
            // The accent at half strength, In progress and Upcoming on the calendar, as a solid color. 3.30:1 light,
            // 4.71:1 dark
            case IN_PROGRESS, UPCOMING -> new Color("#6889C6");
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
            // --ts-warning-color, a yellow too light to read on the light surface, darkened. 3.29:1 light,
            // 4.72:1 dark
            case NEW -> new Color("#A28700");
            case APPROVED -> SUCCESS;
            // --ts-danger-color. 4.44:1 light, 3.49:1 dark
            case REJECTED -> new Color("#D33A3C");
            // --vaadin-text-color-secondary, the neutral of Closed on the approval screen
            case CLOSED -> NEUTRAL;
        };
    }
}
