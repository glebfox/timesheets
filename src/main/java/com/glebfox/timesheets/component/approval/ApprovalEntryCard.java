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

package com.glebfox.timesheets.component.approval;

import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.component.icon.GravityUiIcon;
import com.glebfox.timesheets.data.renderer.BadgeStyle;
import com.glebfox.timesheets.entity.tag.Tag;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.google.common.base.Preconditions;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.shared.Registration;
import io.jmix.core.Messages;
import io.jmix.core.MetadataTools;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A time entry's card on the approval screen: the color of the entry's status, as a stripe along the card's edge on
 * the week board and as a dot before the time in a month's list, the time, the task with
 * its project and, unless the entry waits, a badge of its status, the description, the tags, the rejection reason of
 * a rejected entry, and the buttons for what can be done with it: ✓ for a new or a rejected entry, ✕ for a new, an
 * approved, or a rejected one, and none for a closed one. The description and the reason are rendered from Markdown.
 * <p>
 * An entry that can be rejected may be put in the reject basket, which {@link #setInBasket} tells the card: the card
 * is then marked "in reject basket", and its only button is ↺, which takes the entry out again.
 * <p>
 * The buttons do nothing of their own: whoever shows the card attaches what they do with
 * {@link #addApproveClickListener}, {@link #addRejectClickListener}, and {@link #addUnrejectClickListener}, at any
 * time after the card is created. A button that the card does not show is not in the card. A click from the browser on
 * ✕ or ↺ moves the focus to the button that takes its place, which the card swaps in when {@link #setInBasket} is
 * called after the click.
 * <p>
 * The card's classes are {@code approval-entry-card}, the class of the entry's status ({@code new}, {@code approved},
 * {@code rejected}, or {@code closed}), {@code compact} on the week board, where the description is cut after three
 * lines and shows in full in a tooltip, and {@code in-basket} while the entry is in the reject basket. Its parts have the classes {@code stripe} on the week board,
 * {@code time}, with a {@code dot} in a month's list, {@code content}, which holds {@code task} (with {@code task-name}, {@code project}, and the
 * {@code basket-mark}), {@code description}, {@code tags}, and {@code reason}, and {@code actions}. The card's id is
 * {@code entry-<id>}, and its buttons' are {@code approveEntryBtn-<id>}, {@code rejectEntryBtn-<id>}, and
 * {@code unrejectEntryBtn-<id>}, with the id of the entry.
 * <p>
 * The card is a container so that a lookup of components by id, as Jmix does it, reaches its buttons.
 */
public class ApprovalEntryCard extends Composite<Div> implements HasComponents {

    static final String CLASS_NAME = "approval-entry-card";
    static final String COMPACT_CLASS_NAME = "compact";
    static final String IN_BASKET_CLASS_NAME = "in-basket";

    private static final Set<TimeEntryStatus> APPROVABLE = EnumSet.of(TimeEntryStatus.NEW, TimeEntryStatus.REJECTED);
    private static final Set<TimeEntryStatus> REJECTABLE =
            EnumSet.of(TimeEntryStatus.NEW, TimeEntryStatus.APPROVED, TimeEntryStatus.REJECTED);

    private final TimeEntry entry;
    private final Div task;
    private final Badge basketMark;
    private final Div actions;
    @Nullable
    private final Button approveButton;
    @Nullable
    private final Button rejectButton;
    @Nullable
    private final Button unrejectButton;
    private boolean inBasket;

    /**
     * Creates the card of an entry.
     *
     * @param entry            the entry with its task, the task's project, and its tags
     * @param compact          whether the card is shown on the week board, with the description cut short and in full
     *                         in a tooltip
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

        // The color of the status: a stripe along the edge of a card on the week board, where it reads down a column,
        // and a dot before the time of a card in a month's list, whose cards are rows as wide as the list
        Span statusMark = new Span();
        // The stripe takes its color from the card's status class, and a dot, as everywhere on the screen, from its own
        if (compact) {
            statusMark.addClassName("stripe");
        } else {
            statusMark.addClassNames("dot", ApprovalStatusClassNames.of(status));
        }
        // The status is told by the badge, or by the absence of one for an entry that waits
        statusMark.getElement().setAttribute("aria-hidden", "true");

        Div time = new Div();
        time.addClassName("time");
        if (!compact) {
            time.add(statusMark);
        }
        time.add(new Text(metadataTools.format(entry.getSpentTime())));

        task = createTask(componentSupport, metadataTools);
        basketMark = componentSupport.createBadge(
                messages.getMessage(ApprovalEntryCard.class, "entryCard.inBasket", locale), BadgeStyle.ERROR);
        basketMark.addClassName("basket-mark");

        Div content = new Div(task);
        content.addClassName("content");
        Div description = createMarkdown(entry.getDescription(), "description", content);
        // The week board cuts the description short, so the whole text shows on hover: beside the card, where it covers
        // none of the card's buttons, which are below the text or above it
        if (compact && description != null) {
            Tooltip tooltip = Tooltip.forComponent(description);
            tooltip.setMarkdown(entry.getDescription());
            tooltip.setPosition(Tooltip.TooltipPosition.END_TOP);
        }
        createTags(componentSupport, metadataTools, content);
        if (status == TimeEntryStatus.REJECTED) {
            createMarkdown(entry.getRejectionReason(), "reason", content);
        }

        actions = new Div();
        actions.addClassName("actions");
        approveButton = APPROVABLE.contains(status)
                ? createButton("approveEntryBtn-", GravityUiIcon.CHECK.create(), ButtonVariant.SUCCESS,
                messages.getMessage(ApprovalEntryCard.class, "entryCard.approve", locale))
                : null;
        boolean rejectable = REJECTABLE.contains(status);
        rejectButton = rejectable
                ? createButton("rejectEntryBtn-", GravityUiIcon.XMARK.create(), ButtonVariant.ERROR,
                messages.getMessage(ApprovalEntryCard.class, "entryCard.reject", locale))
                : null;
        // Neutral: it undoes the ✕ and changes nothing saved
        unrejectButton = rejectable
                ? createButton("unrejectEntryBtn-", GravityUiIcon.ARROW_ROTATE_LEFT.create(), null,
                messages.getMessage(ApprovalEntryCard.class, "entryCard.unreject", locale))
                : null;
        // The clicked button leaves the card as the other takes its place, and the focus would fall to the page, so
        // that a keyboard user would have to tab from the top again. Registered before the owner's listeners, so
        // that the button is queued for the focus before the owner swaps the buttons
        focusInsteadOnClick(rejectButton, unrejectButton);
        focusInsteadOnClick(unrejectButton, rejectButton);
        showButtons();

        if (compact) {
            card.add(statusMark);
        }
        card.add(time, content, actions);
    }

    /**
     * Tells whether an entry can be rejected, and so put in the reject basket: a new, an approved, or a rejected one.
     *
     * @param entry the entry
     * @return whether the card of the entry has ✕
     */
    public static boolean canBeRejected(TimeEntry entry) {
        return REJECTABLE.contains(entry.getStatus());
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
     * Tells whether the entry is in the reject basket, which changes the card's mark and buttons.
     *
     * @param inBasket whether the entry is in the reject basket
     * @throws IllegalArgumentException if the entry goes in the basket but cannot be rejected, as a closed entry
     */
    public void setInBasket(boolean inBasket) {
        Preconditions.checkArgument(!inBasket || unrejectButton != null,
                "A %s entry cannot be rejected", entry.getStatus());
        if (inBasket == this.inBasket) {
            return;
        }

        this.inBasket = inBasket;
        getContent().setClassName(IN_BASKET_CLASS_NAME, inBasket);
        if (inBasket) {
            task.add(basketMark);
        } else {
            task.remove(basketMark);
        }
        showButtons();
    }

    /**
     * Adds a listener to the ✓ button. An entry that cannot be approved, an approved or a closed one, has no such
     * button, and the listener is never called. While the entry is in the reject basket, the card does not show it.
     *
     * @param listener the listener to call when ✓ is clicked
     * @return a registration that removes the listener
     */
    public Registration addApproveClickListener(ComponentEventListener<ClickEvent<Button>> listener) {
        return addListenerTo(approveButton, listener);
    }

    /**
     * Adds a listener to the ✕ button, which puts the entry in the reject basket. A closed entry has no such button,
     * and the listener is never called. While the entry is in the reject basket, the card does not show it.
     *
     * @param listener the listener to call when ✕ is clicked
     * @return a registration that removes the listener
     */
    public Registration addRejectClickListener(ComponentEventListener<ClickEvent<Button>> listener) {
        return addListenerTo(rejectButton, listener);
    }

    /**
     * Adds a listener to the ↺ button, which takes the entry out of the reject basket. The card shows it only while
     * the entry is in the basket; a closed entry has no such button, and the listener is never called.
     *
     * @param listener the listener to call when ↺ is clicked
     * @return a registration that removes the listener
     */
    public Registration addUnrejectClickListener(ComponentEventListener<ClickEvent<Button>> listener) {
        return addListenerTo(unrejectButton, listener);
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

        Div taskPart = new Div(taskName, project);
        taskPart.addClassName("task");
        // An entry that waits is the usual case, so only the other statuses name themselves
        if (entry.getStatus() != TimeEntryStatus.NEW) {
            taskPart.add(componentSupport.createTimeEntryStatusRender().createComponent(entry));
        }
        return taskPart;
    }

    private static @Nullable Div createMarkdown(@Nullable String text, String className, Div content) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        Div part = new Div(new Markdown(text));
        part.addClassName(className);
        content.add(part);
        return part;
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

    /**
     * Puts in the actions the buttons for what can be done with the entry now: ↺ alone while it is in the basket,
     * otherwise ✓ and ✕ where they apply.
     */
    private void showButtons() {
        actions.removeAll();
        Stream.of(inBasket ? unrejectButton : approveButton, inBasket ? null : rejectButton)
                .filter(Objects::nonNull)
                .forEach(actions::add);
    }

    /**
     * Makes a click from the browser on a button move the focus to the button that replaces it. A click that the
     * server makes moves no focus. Does nothing for an entry without these buttons, a closed one.
     */
    private static void focusInsteadOnClick(@Nullable Button clicked, @Nullable Button replacement) {
        if (clicked != null && replacement != null) {
            clicked.addClickListener(event -> {
                if (event.isFromClient()) {
                    replacement.focus();
                }
            });
        }
    }

    private Button createButton(String idPrefix, Icon icon, @Nullable ButtonVariant color, String text) {
        Button button = new Button(icon);
        button.setId(idPrefix + entry.getId());
        button.addThemeVariants(ButtonVariant.TERTIARY);
        if (color != null) {
            button.addThemeVariants(color);
        }
        button.setAriaLabel(text);
        button.setTooltipText(text);
        return button;
    }
}
