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
import '@vaadin/icon/vaadin-iconset.js';

// The project's own icons, ts:<id>, used by the main menu and by the grids' empty states. Each icon draws its own
// outline: the wrapper sets no fill and takes the stroke width from --ts-icon-stroke-width, and the icon is one
// path, so a translucent color paints it once.
const template = document.createElement('template');

template.innerHTML = `<vaadin-iconset name="ts" size="16">
  <svg><defs>
    <g id="ts:timesheets" style="fill: none; stroke: var(--vaadin-icon-color, currentColor); stroke-width: var(--ts-icon-stroke-width); stroke-linecap: round; stroke-linejoin: round"><path d="M2.75 9a5.25 5.25 0 1 0 10.5 0a5.25 5.25 0 1 0 -10.5 0M6.5 1.75h3M8 1.75v2M8 9V6.5M12.5 4.5l.75-.75"/></g>
    <g id="ts:calendar" style="fill: none; stroke: var(--vaadin-icon-color, currentColor); stroke-width: var(--ts-icon-stroke-width); stroke-linecap: round; stroke-linejoin: round"><path d="M3.75 2.75h8.5a2 2 0 0 1 2 2v7.5a2 2 0 0 1 -2 2h-8.5a2 2 0 0 1 -2 -2v-7.5a2 2 0 0 1 2 -2zM5 1.75v2.5M11 1.75v2.5M1.75 6.75h12.5"/></g>
    <g id="ts:my-time-entries" style="fill: none; stroke: var(--vaadin-icon-color, currentColor); stroke-width: var(--ts-icon-stroke-width); stroke-linecap: round; stroke-linejoin: round"><path d="M1.75 8a6.25 6.25 0 1 0 12.5 0a6.25 6.25 0 1 0 -12.5 0M8 4.75V8l2.25 1.5"/></g>
    <g id="ts:my-tasks" style="fill: none; stroke: var(--vaadin-icon-color, currentColor); stroke-width: var(--ts-icon-stroke-width); stroke-linecap: round; stroke-linejoin: round"><path d="M5.75 2.75h-1a2 2 0 0 0-2 2v7.5a2 2 0 0 0 2 2h6.5a2 2 0 0 0 2-2v-7.5a2 2 0 0 0-2-2h-1M6.75 1.75h2.5a1 1 0 0 1 1 1v0.5a1 1 0 0 1 -1 1h-2.5a1 1 0 0 1 -1 -1v-0.5a1 1 0 0 1 1 -1zM5.5 8h5M5.5 11h3"/></g>
    <g id="ts:project-management" style="fill: none; stroke: var(--vaadin-icon-color, currentColor); stroke-width: var(--ts-icon-stroke-width); stroke-linecap: round; stroke-linejoin: round"><path d="M3.75 1.75h8.5a2 2 0 0 1 2 2v8.5a2 2 0 0 1 -2 2h-8.5a2 2 0 0 1 -2 -2v-8.5a2 2 0 0 1 2 -2zM5.25 5v6M8 5v3M10.75 5v4.5"/></g>
    <g id="ts:clients" style="fill: none; stroke: var(--vaadin-icon-color, currentColor); stroke-width: var(--ts-icon-stroke-width); stroke-linecap: round; stroke-linejoin: round"><path d="M1.75 14.25h12.5M3.25 14.25V3.75a2 2 0 0 1 2-2h3.5a2 2 0 0 1 2 2v10.5M10.75 7.25h1.5a1.5 1.5 0 0 1 1.5 1.5v5.5M5.75 5h0M8.25 5h0M5.75 8h0M8.25 8h0M7 14.25v-2.5"/></g>
    <g id="ts:projects" style="fill: none; stroke: var(--vaadin-icon-color, currentColor); stroke-width: var(--ts-icon-stroke-width); stroke-linecap: round; stroke-linejoin: round"><path d="M3.75 4.75h8.5a2 2 0 0 1 2 2v5.5a2 2 0 0 1 -2 2h-8.5a2 2 0 0 1 -2 -2v-5.5a2 2 0 0 1 2 -2zM5.75 4.75v-1.5a1.5 1.5 0 0 1 1.5-1.5h1.5a1.5 1.5 0 0 1 1.5 1.5v1.5M1.75 9h12.5"/></g>
    <g id="ts:time-entries" style="fill: none; stroke: var(--vaadin-icon-color, currentColor); stroke-width: var(--ts-icon-stroke-width); stroke-linecap: round; stroke-linejoin: round"><path d="M1.75 2.75h12.5M1.75 7.5h4M1.75 12.25h4M7.75 10.5a3.25 3.25 0 1 0 6.5 0a3.25 3.25 0 1 0 -6.5 0M11 9V10.5h1.25"/></g>
    <g id="ts:approve-time-entries" style="fill: none; stroke: var(--vaadin-icon-color, currentColor); stroke-width: var(--ts-icon-stroke-width); stroke-linecap: round; stroke-linejoin: round"><path d="M1.75 8a6.25 6.25 0 1 0 12.5 0a6.25 6.25 0 1 0 -12.5 0M5.25 8.25l1.75 1.75 3.75-3.75"/></g>
    <g id="ts:settings" style="fill: none; stroke: var(--vaadin-icon-color, currentColor); stroke-width: var(--ts-icon-stroke-width); stroke-linecap: round; stroke-linejoin: round"><path d="M1.75 4.75h6.5M11.75 4.75h2.5M1.75 11.25h2.5M7.75 11.25h6.5M8.25 4.75a1.75 1.75 0 1 0 3.5 0a1.75 1.75 0 1 0 -3.5 0M4.25 11.25a1.75 1.75 0 1 0 3.5 0a1.75 1.75 0 1 0 -3.5 0"/></g>
    <g id="ts:task-types" style="fill: none; stroke: var(--vaadin-icon-color, currentColor); stroke-width: var(--ts-icon-stroke-width); stroke-linecap: round; stroke-linejoin: round"><path d="M1.75 4.25Q1.75 3.25 2.75 3.25L6.5 3.25Q6.75 3.25 6.93 3.43L10.04 6.54Q10.75 7.25 10.04 7.96L6.46 11.54Q5.75 12.25 5.04 11.54L1.93 8.43Q1.75 8.25 1.75 8ZM4.5 6h0M10.25 3.25L13.54 6.54Q14.25 7.25 13.54 7.96L9.25 12.25"/></g>
    <g id="ts:project-roles" style="fill: none; stroke: var(--vaadin-icon-color, currentColor); stroke-width: var(--ts-icon-stroke-width); stroke-linecap: round; stroke-linejoin: round"><path d="M3 4.5a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0 -5 0M1.75 14.25v-1a3.75 3.75 0 0 1 3.75-3.75M7.75 8.75Q7.75 7.75 8.75 7.75L11 7.75Q11.25 7.75 11.43 7.93L13.54 10.04Q14.25 10.75 13.54 11.46L11.46 13.54Q10.75 14.25 10.04 13.54L7.93 11.43Q7.75 11.25 7.75 11Z"/></g>
    <g id="ts:work-schedules" style="fill: none; stroke: var(--vaadin-icon-color, currentColor); stroke-width: var(--ts-icon-stroke-width); stroke-linecap: round; stroke-linejoin: round"><path d="M13.5 5.25v-.5a2 2 0 0 0-2-2H3.75a2 2 0 0 0-2 2v7.5a2 2 0 0 0 2 2h2M5 1.75v2.5M10.25 1.75v2.5M1.75 6.75h4M6.75 10.5a3.75 3.75 0 1 0 7.5 0a3.75 3.75 0 1 0 -7.5 0M10.5 8.75v1.75l1.25.75"/></g>
    <g id="ts:holiday-calendars" style="fill: none; stroke: var(--vaadin-icon-color, currentColor); stroke-width: var(--ts-icon-stroke-width); stroke-linecap: round; stroke-linejoin: round"><path d="M13.5 5.25v-.5a2 2 0 0 0-2-2H3.75a2 2 0 0 0-2 2v7.5a2 2 0 0 0 2 2h2M5 1.75v2.5M10.25 1.75v2.5M1.75 6.75h4M10.5 14 7.25 10.75a2 2 0 0 1 3.25-2.25 2 2 0 0 1 3.25 2.25z"/></g>
    <g id="ts:absence-types" style="fill: none; stroke: var(--vaadin-icon-color, currentColor); stroke-width: var(--ts-icon-stroke-width); stroke-linecap: round; stroke-linejoin: round"><path d="M1.75 8a6.25 6.25 0 0 1 12.5 0zM8 8v5a1.25 1.25 0 0 0 2.5 0"/></g>
    <g id="ts:administration" style="fill: none; stroke: var(--vaadin-icon-color, currentColor); stroke-width: var(--ts-icon-stroke-width); stroke-linecap: round; stroke-linejoin: round"><path d="M3.75 2.75h8.5a2 2 0 0 1 2 2v6.5a2 2 0 0 1 -2 2h-8.5a2 2 0 0 1 -2 -2v-6.5a2 2 0 0 1 2 -2zM4.5 6a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0M3.75 11.25a2 2 0 0 1 4 0M10.25 5.75h1.5M10.25 9.5h1.5"/></g>
    <g id="ts:users" style="fill: none; stroke: var(--vaadin-icon-color, currentColor); stroke-width: var(--ts-icon-stroke-width); stroke-linecap: round; stroke-linejoin: round"><path d="M3.5 5a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0 -5 0M1.75 14.25v-.75a3.75 3.75 0 0 1 3.75-3.75h1a3.75 3.75 0 0 1 3.75 3.75v.75M10.5 2.75a2.25 2.25 0 0 1 0 4.5M12 9.75a3.25 3.25 0 0 1 2.25 3.1v1.4"/></g>
    <g id="ts:absences" style="fill: none; stroke: var(--vaadin-icon-color, currentColor); stroke-width: var(--ts-icon-stroke-width); stroke-linecap: round; stroke-linejoin: round"><path d="M13.5 5.25v-.5a2 2 0 0 0-2-2H3.75a2 2 0 0 0-2 2v7.5a2 2 0 0 0 2 2h2M5 1.75v2.5M10.25 1.75v2.5M1.75 6.75h4M6.75 10.5a3.75 3.75 0 0 1 7.5 0zM10.5 10.5v2.5a1.25 1.25 0 0 0 2.5 0"/></g>
    <g id="ts:security" style="fill: none; stroke: var(--vaadin-icon-color, currentColor); stroke-width: var(--ts-icon-stroke-width); stroke-linecap: round; stroke-linejoin: round"><path d="M8 1.75l5.25 2v4c0 3.25-2.25 5.5-5.25 6.5-3-1-5.25-3.25-5.25-6.5v-4z"/></g>
    <g id="ts:resource-roles" style="fill: none; stroke: var(--vaadin-icon-color, currentColor); stroke-width: var(--ts-icon-stroke-width); stroke-linecap: round; stroke-linejoin: round"><path d="M8 1.75l5.25 2v4c0 3.25-2.25 5.5-5.25 6.5-3-1-5.25-3.25-5.25-6.5v-4zM5.5 8.25l1.75 1.75 3.25-3.5"/></g>
    <g id="ts:row-level-roles" style="fill: none; stroke: var(--vaadin-icon-color, currentColor); stroke-width: var(--ts-icon-stroke-width); stroke-linecap: round; stroke-linejoin: round"><path d="M1.75 2.75h12.5M1.75 7.5h4M1.75 12.25h4M11.25 6.75l3 1.1v2.4c0 1.8-1.25 3.05-3 3.5-1.75-.45-3-1.7-3-3.5v-2.4z"/></g>
    <g id="ts:data-tools" style="fill: none; stroke: var(--vaadin-icon-color, currentColor); stroke-width: var(--ts-icon-stroke-width); stroke-linecap: round; stroke-linejoin: round"><path d="M2.75 3.75a5.25 2 0 1 0 10.5 0a5.25 2 0 1 0 -10.5 0M2.75 3.75v8.5c0 1.1 2.35 2 5.25 2s5.25-.9 5.25-2v-8.5M2.75 8c0 1.1 2.35 2 5.25 2s5.25-.9 5.25-2"/></g>
    <g id="ts:entity-inspector" style="fill: none; stroke: var(--vaadin-icon-color, currentColor); stroke-width: var(--ts-icon-stroke-width); stroke-linecap: round; stroke-linejoin: round"><path d="M1.75 2.75h12.5M1.75 7.5h4M1.75 12.25h4M7.5 9.75a2.75 2.75 0 1 0 5.5 0a2.75 2.75 0 1 0 -5.5 0M12.25 11.75l2 2"/></g>
    <g id="ts:pessimistic-locking" style="fill: none; stroke: var(--vaadin-icon-color, currentColor); stroke-width: var(--ts-icon-stroke-width); stroke-linecap: round; stroke-linejoin: round"><path d="M4.75 7.25h6.5a2 2 0 0 1 2 2v3a2 2 0 0 1 -2 2h-6.5a2 2 0 0 1 -2 -2v-3a2 2 0 0 1 2 -2zM5.25 7.25V5a2.75 2.75 0 0 1 5.5 0v2.25M8 10.25v1.5"/></g>
    <g id="ts:locks" style="fill: none; stroke: var(--vaadin-icon-color, currentColor); stroke-width: var(--ts-icon-stroke-width); stroke-linecap: round; stroke-linejoin: round"><path d="M1.75 2.75h12.5M1.75 7.5h4M1.75 12.25h4M9.25 10.25h4a1 1 0 0 1 1 1v1.5a1 1 0 0 1 -1 1h-4a1 1 0 0 1 -1 -1v-1.5a1 1 0 0 1 1 -1zM9.75 10.25V9a1.5 1.5 0 0 1 3 0v1.25"/></g>
    <g id="ts:tag" style="fill: none; stroke: var(--vaadin-icon-color, currentColor); stroke-width: var(--ts-icon-stroke-width); stroke-linecap: round; stroke-linejoin: round"><path d="M5.75 4.25h6.5a2 2 0 0 1 2 2v3.5a2 2 0 0 1 -2 2h-6.5Q5.5 11.75 5.32 11.57L2.46 8.71Q1.75 8 2.46 7.29L5.32 4.43Q5.5 4.25 5.75 4.25zM5.25 8h0"/></g>
    <g id="ts:tag-group" style="fill: none; stroke: var(--vaadin-icon-color, currentColor); stroke-width: var(--ts-icon-stroke-width); stroke-linecap: round; stroke-linejoin: round"><path d="M5.5 5.75h4.25a2 2 0 0 1 2 2v3a2 2 0 0 1 -2 2H5.5Q5.25 12.75 5.07 12.57L2.46 9.96Q1.75 9.25 2.46 8.54L5.07 5.93Q5.25 5.75 5.5 5.75zM5.25 9.25h0M7.75 3.25h4.5a2 2 0 0 1 2 2v3"/></g>
  </defs></svg>
</vaadin-iconset>`;

document.head.appendChild(template.content);
