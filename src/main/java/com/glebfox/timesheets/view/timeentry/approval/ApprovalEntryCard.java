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

package com.glebfox.timesheets.view.timeentry.approval;

import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.component.icon.GravityUiIcon;
import com.glebfox.timesheets.data.renderer.BadgeStyle;
import com.glebfox.timesheets.entity.tag.Tag;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.shared.Registration;
import io.jmix.core.Messages;
import io.jmix.core.MetadataTools;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A time entry's card on the approval screen: a stripe in the color of the entry's status, the time, the task with
 * its project and, unless the entry waits, a badge of its status, the description, the tags, the rejection reason of
 * a rejected entry, and the buttons for what can be done with it: ✓ for a new or a rejected entry, ✕ for a new, an
 * approved, or a rejected one, and none for a closed one. The description and the reason are rendered from Markdown.
 * <p>
 * The buttons do nothing of their own: whoever shows the card attaches what they do with
 * {@link #addApproveClickListener} and {@link #addRejectClickListener}, at any time after the card is created.
 * <p>
 * The card's classes are {@code approval-entry-card}, the class of the entry's status ({@code new}, {@code approved},
 * {@code rejected}, or {@code closed}), and {@code compact} on the week board, where the description is cut after
 * three lines. Its parts have the classes {@code stripe}, {@code time}, {@code content}, which holds {@code task}
 * (with {@code task-name} and {@code project}), {@code description}, {@code tags}, and {@code reason}, and
 * {@code actions}. The card's id is {@code entry-<id>}, and its buttons' are {@code approveEntryBtn-<id>} and
 * {@code rejectEntryBtn-<id>}, with the id of the entry.
 * <p>
 * The card is a container so that a lookup of components by id, as Jmix does it, reaches its buttons.
 */
public class ApprovalEntryCard extends Composite<Div> implements HasComponents {

    static final String CLASS_NAME = "approval-entry-card";
    static final String COMPACT_CLASS_NAME = "compact";

    private static final Set<TimeEntryStatus> APPROVABLE = EnumSet.of(TimeEntryStatus.NEW, TimeEntryStatus.REJECTED);
    private static final Set<TimeEntryStatus> REJECTABLE =
            EnumSet.of(TimeEntryStatus.NEW, TimeEntryStatus.APPROVED, TimeEntryStatus.REJECTED);

    private final TimeEntry entry;
    @Nullable
    private final Button approveButton;
    @Nullable
    private final Button rejectButton;

    /**
     * Creates the card of an entry.
     *
     * @param entry            the entry with its task, the task's project, and its tags
     * @param compact          whether the card is shown on the week board, with the description cut short
     * @param componentSupport builds the badges
     * @param messages         gives the texts
     * @param metadataTools    formats the time, the status, and the project
     * @param locale           the locale of the texts
     */
    public ApprovalEntryCard(TimeEntry entry, boolean compact, ComponentSupport componentSupport, Messages messages,
                             MetadataTools metadataTools, Locale locale) {
        this.entry = entry;
        TimeEntryStatus status = entry.getStatus();

        Div card = getContent();
        card.setId("entry-" + entry.getId());
        card.addClassNames(CLASS_NAME, ApprovalStatusClassNames.of(status));
        card.setClassName(COMPACT_CLASS_NAME, compact);

        Span stripe = new Span();
        stripe.addClassName("stripe");
        // The status is told by the badge, or by the absence of one for an entry that waits
        stripe.getElement().setAttribute("aria-hidden", "true");

        Div time = new Div(metadataTools.format(entry.getSpentTime()));
        time.addClassName("time");

        Div content = new Div(createTask(componentSupport, metadataTools));
        content.addClassName("content");
        createMarkdown(entry.getDescription(), "description", content);
        createTags(componentSupport, metadataTools, content);
        if (status == TimeEntryStatus.REJECTED) {
            createMarkdown(entry.getRejectionReason(), "reason", content);
        }

        Div actions = new Div();
        actions.addClassName("actions");
        approveButton = APPROVABLE.contains(status)
                ? createButton("approveEntryBtn-", GravityUiIcon.CHECK.create(), ButtonVariant.SUCCESS,
                messages.getMessage(ApprovalEntryCard.class, "entryCard.approve", locale), actions)
                : null;
        rejectButton = REJECTABLE.contains(status)
                ? createButton("rejectEntryBtn-", GravityUiIcon.XMARK.create(), ButtonVariant.ERROR,
                messages.getMessage(ApprovalEntryCard.class, "entryCard.reject", locale), actions)
                : null;

        card.add(stripe, time, content, actions);
    }

    /**
     * Returns the entry the card shows.
     *
     * @return the entry
     */
    public TimeEntry getEntry() {
        return entry;
    }

    /**
     * Adds a listener to the ✓ button. An entry that cannot be approved, an approved or a closed one, has no such
     * button, and the listener is never called.
     *
     * @param listener the listener to call when ✓ is clicked
     * @return a registration that removes the listener
     */
    public Registration addApproveClickListener(ComponentEventListener<ClickEvent<Button>> listener) {
        return addListenerTo(approveButton, listener);
    }

    /**
     * Adds a listener to the ✕ button. A closed entry has no such button, and the listener is never called.
     *
     * @param listener the listener to call when ✕ is clicked
     * @return a registration that removes the listener
     */
    public Registration addRejectClickListener(ComponentEventListener<ClickEvent<Button>> listener) {
        return addListenerTo(rejectButton, listener);
    }

    private static Registration addListenerTo(@Nullable Button button,
                                              ComponentEventListener<ClickEvent<Button>> listener) {
        return button != null ? button.addClickListener(listener) : () -> {
        };
    }

    private Div createTask(ComponentSupport componentSupport, MetadataTools metadataTools) {
        Span taskName = new Span(entry.getTask().getName());
        taskName.addClassName("task-name");

        Span project = new Span(metadataTools.getInstanceName(entry.getTask().getProject()));
        project.addClassName("project");

        Div task = new Div(taskName, project);
        task.addClassName("task");
        // An entry that waits is the usual case, so only the other statuses name themselves
        if (entry.getStatus() != TimeEntryStatus.NEW) {
            task.add(componentSupport.createTimeEntryStatusRender().createComponent(entry));
        }
        return task;
    }

    private static void createMarkdown(@Nullable String text, String className, Div content) {
        if (StringUtils.isNotBlank(text)) {
            Div part = new Div(new Markdown(text));
            part.addClassName(className);
            content.add(part);
        }
    }

    private void createTags(ComponentSupport componentSupport, MetadataTools metadataTools, Div content) {
        List<Tag> tags = entry.getTags();
        if (tags == null || tags.isEmpty()) {
            return;
        }

        Div tagsPart = new Div();
        tagsPart.addClassName("tags");
        tags.stream()
                .sorted(Comparator.comparing(Tag::getName, String.CASE_INSENSITIVE_ORDER))
                .forEach(tag -> {
                    Badge badge = componentSupport.createBadge(tag.getName(), BadgeStyle.NEUTRAL);
                    // The name alone is short; the group it belongs to is told on hover
                    badge.getElement().setAttribute("title", metadataTools.getInstanceName(tag));
                    tagsPart.add(badge);
                });
        content.add(tagsPart);
    }

    private Button createButton(String idPrefix, Icon icon, ButtonVariant color, String text, Div actions) {
        Button button = new Button(icon);
        button.setId(idPrefix + entry.getId());
        button.addThemeVariants(ButtonVariant.TERTIARY, color);
        button.setAriaLabel(text);
        button.setTooltipText(text);
        actions.add(button);
        return button;
    }
}
