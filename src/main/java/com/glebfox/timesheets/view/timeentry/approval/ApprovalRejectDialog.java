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
import com.glebfox.timesheets.app.util.PluralForms;
import com.glebfox.timesheets.app.util.UnsavedInputSupport;
import com.glebfox.timesheets.component.icon.GravityUiIcon;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.google.common.base.Preconditions;
import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.function.SerializableConsumer;
import io.jmix.core.Messages;
import io.jmix.core.MetadataTools;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.markdowneditor.MarkdownEditor;
import io.jmix.flowui.component.sidedialog.SideDialog;
import io.jmix.flowui.component.validation.ValidationErrors;
import io.jmix.flowui.kit.component.sidedialog.SideDialogPosition;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.ViewValidation;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * The side dialog of the approval screen where the entries of the reject basket are rejected with a reason. It opens
 * on the right, modal, so the screen behind it does not react while it is open.
 * <p>
 * It holds the entries of the basket, each with a button that takes it out; the quick reasons, each of which appends
 * its sentence to the reason; the reason itself, required and edited as Markdown; Reject; and Cancel. The dialog does
 * not change the basket or save anything itself: it tells its owner which entry to take out and the reason to reject
 * with, and the owner gives it the entries again with {@link #setEntries}, or closes it. Cancel, Esc, and a
 * click outside the dialog close it, asking first when the reason differs from how the dialog opened. The dialog opens
 * with the focus in the reason, and No to that question puts it back there.
 * <p>
 * The dialog has the class {@code approval-reject-dialog}; its content has the class
 * {@code approval-reject-dialog-content}. The content holds two {@code section}s, each with a {@code section-label}:
 * the {@code entries-section}, whose {@code entries} have a {@code basket-entry} for each entry with its
 * {@code title}, {@code details}, and {@code remove} button, and the {@code quick-reasons-section}, whose
 * {@code quick-reasons} are the buttons; then the {@code reason}. The ids are {@code basketEntry-<id>} and {@code removeBasketEntryBtn-<id>} with the id of the
 * entry, {@code quickReason-<name>} with the name of a quick reason ({@code description}, {@code wrongTask},
 * {@code tooMuchTime}, or {@code duplicate}), {@code reasonField}, {@code rejectBtn}, and {@code cancelBtn}.
 */
final class ApprovalRejectDialog {

    static final String CLASS_NAME = "approval-reject-dialog";

    /**
     * The quick reasons in the order they are shown. Each has the message keys {@code quickReason.<name>.text}, the
     * button's text, and {@code quickReason.<name>.reason}, the sentence it appends.
     */
    private static final List<String> QUICK_REASONS = List.of("description", "wrongTask", "tooMuchTime", "duplicate");

    private final Dialogs dialogs;
    private final UiComponents uiComponents;
    private final ViewValidation viewValidation;
    private final UnsavedInputSupport unsavedInputSupport;
    private final ComponentSupport componentSupport;
    private final Messages messages;
    private final MetadataTools metadataTools;
    private final Locale locale;

    // The dialog open now, if any
    @Nullable
    private Opened opened;

    /**
     * Creates the dialog, closed.
     *
     * @param dialogs             creates the side dialog
     * @param uiComponents        creates the reason's editor
     * @param viewValidation      validates the reason
     * @param unsavedInputSupport asks before a changed reason is discarded
     * @param componentSupport    builds the status badges and formats the days
     * @param messages            gives the texts
     * @param metadataTools       formats the entries' time
     * @param locale              the locale of the texts
     */
    ApprovalRejectDialog(Dialogs dialogs, UiComponents uiComponents, ViewValidation viewValidation,
                         UnsavedInputSupport unsavedInputSupport, ComponentSupport componentSupport,
                         Messages messages, MetadataTools metadataTools, Locale locale) {
        this.dialogs = dialogs;
        this.uiComponents = uiComponents;
        this.viewValidation = viewValidation;
        this.unsavedInputSupport = unsavedInputSupport;
        this.componentSupport = componentSupport;
        this.messages = messages;
        this.metadataTools = metadataTools;
        this.locale = locale;
    }

    /**
     * Opens the dialog.
     *
     * @param owner      the view the dialog is opened from, which asks before a changed reason is discarded
     * @param personName the name of the person whose entries these are
     * @param entries    the entries of the basket, not empty
     * @param reason     the reason to start from, such as one written before a rejection that failed; empty for none
     * @param onRemove   takes an entry out of the basket, when its button in the dialog is clicked
     * @param onReject   rejects the entries of the basket with a reason, never blank, when Reject is clicked; the
     *                   dialog is still open then, and the owner closes it
     * @throws IllegalStateException    if the dialog is open already
     * @throws IllegalArgumentException if there are no entries
     */
    void open(View<?> owner, String personName, List<TimeEntry> entries, String reason,
              SerializableConsumer<TimeEntry> onRemove, SerializableConsumer<String> onReject) {
        Preconditions.checkState(opened == null, "The reject dialog is open already");
        Preconditions.checkArgument(!entries.isEmpty(), "There is nothing to reject");
        opened = new Opened(owner, personName, entries, reason, onRemove, onReject);
    }

    /**
     * Returns the side dialog while it is open.
     *
     * @return the side dialog, {@code null} while the dialog is closed
     */
    @Nullable
    SideDialog getSideDialog() {
        return opened != null ? opened.sideDialog : null;
    }

    /**
     * Shows the entries of the basket again, after it changed. Does nothing while the dialog is closed.
     *
     * @param entries the entries of the basket, not empty
     */
    void setEntries(List<TimeEntry> entries) {
        if (opened != null) {
            opened.setEntries(entries);
        }
    }

    /**
     * Closes the dialog without asking. Does nothing while it is closed.
     */
    void close() {
        if (opened != null) {
            opened.sideDialog.close();
        }
    }

    /**
     * The dialog while it is open.
     */
    private final class Opened {

        private final View<?> owner;
        private final SerializableConsumer<TimeEntry> onRemove;
        private final SerializableConsumer<String> onReject;
        private final String openedReason;
        private final Div entryList;
        private final MarkdownEditor reasonField;
        private final Button rejectButton;
        private final SideDialog sideDialog;

        Opened(View<?> owner, String personName, List<TimeEntry> entries, String reason,
               SerializableConsumer<TimeEntry> onRemove, SerializableConsumer<String> onReject) {
            this.owner = owner;
            this.onRemove = onRemove;
            this.onReject = onReject;
            this.openedReason = reason;

            entryList = new Div();
            entryList.addClassName("entries");

            Div quickReasons = new Div();
            quickReasons.addClassName("quick-reasons");
            QUICK_REASONS.forEach(name -> quickReasons.add(createQuickReasonButton(name)));

            reasonField = createReasonField(reason);

            rejectButton = new Button(GravityUiIcon.XMARK.create(), event -> reject());
            rejectButton.setId("rejectBtn");
            rejectButton.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.ERROR);

            Button cancelButton = new Button(messages.getMessage("actions.Cancel", locale), event -> askBeforeClosing());
            cancelButton.setId("cancelBtn");

            Div content = new Div(
                    createSection("entries", "rejectDialog.entries", entryList),
                    createSection("quick-reasons", "rejectDialog.quickReasons", quickReasons),
                    reasonField);
            content.addClassName(CLASS_NAME + "-content");

            sideDialog = dialogs.createSideDialog()
                    .withHeader(message("rejectDialog.header", personName))
                    .withContentComponents(content)
                    .withFooterComponents(rejectButton, cancelButton)
                    .withSideDialogPosition(SideDialogPosition.RIGHT)
                    .withClassName(CLASS_NAME)
                    // With a listener, Esc and a click outside no longer close the dialog on their own
                    .withCloseActionListener(event -> askBeforeClosing())
                    .withOpenedChangeListener(event -> {
                        if (!event.isOpened()) {
                            opened = null;
                        }
                    })
                    .build();
            // Strict: the screen behind does not take any event while the dialog is open
            sideDialog.setModality(ModalityMode.STRICT);

            setEntries(entries);
            sideDialog.open();
            // On the reason, not on the dialog itself: as a view's focus component is focused, with the autofocus
            // property too, so the field takes the focus once it is rendered, before the dialog's own focus lands
            reasonField.setAutofocus(true);
            reasonField.focus();
        }

        void setEntries(List<TimeEntry> entries) {
            entryList.removeAll();
            // By day, and within a day in the order they were put in the basket
            entries.stream()
                    .sorted(Comparator.comparing(TimeEntry::getDate))
                    .forEach(entry -> entryList.add(createEntry(entry)));
            rejectButton.setText(message(PluralForms.key("rejectBtn.text", entries.size(), locale), entries.size()));
        }

        private Div createSection(String className, String labelKey, Div body) {
            Span label = new Span(message(labelKey));
            label.addClassName("section-label");

            Div section = new Div(label, body);
            section.addClassNames("section", className + "-section");
            return section;
        }

        private Div createEntry(TimeEntry entry) {
            Span task = new Span(message("rejectDialog.entry.title", entry.getTask().getName(),
                    metadataTools.format(entry.getSpentTime())));
            Div title = new Div(task);
            title.addClassName("title");
            // A new entry is the usual case, so only the other statuses name themselves, as on the entry's card
            if (entry.getStatus() != TimeEntryStatus.NEW) {
                title.add(componentSupport.createTimeEntryStatusRender().createComponent(entry));
            }

            String date = message("entriesDay.title", componentSupport.formatDayOfWeekShort(
                    entry.getDate().getDayOfWeek()), entry.getDate().format(
                    DateTimeFormatter.ofPattern(message("entriesDay.date"), locale)));
            Span details = new Span(StringUtils.isBlank(entry.getDescription())
                    ? date
                    : message("rejectDialog.entry.details", date, entry.getDescription()));
            details.addClassName("details");

            String removeText = message("rejectDialog.removeEntry");
            Button remove = new Button(GravityUiIcon.XMARK.create(), event -> onRemove.accept(entry));
            remove.setId("removeBasketEntryBtn-" + entry.getId());
            remove.addClassName("remove");
            remove.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
            remove.setAriaLabel(removeText);
            remove.setTooltipText(removeText);

            Div row = new Div(title, remove, details);
            row.setId("basketEntry-" + entry.getId());
            row.addClassName("basket-entry");
            return row;
        }

        private Button createQuickReasonButton(String name) {
            String sentence = message("quickReason." + name + ".reason");
            Button button = new Button(message("quickReason." + name + ".text"), GravityUiIcon.PLUS.create(),
                    event -> appendReason(sentence));
            button.setId("quickReason-" + name);
            button.addThemeVariants(ButtonVariant.SMALL);
            return button;
        }

        private MarkdownEditor createReasonField(String reason) {
            MarkdownEditor field = uiComponents.create(MarkdownEditor.class);
            field.setId("reasonField");
            field.addClassName("reason");
            field.setLabel(message("reasonField.label"));
            field.setRequiredMessage(message("reasonField.requiredMessage"));
            field.setWidthFull();
            // Jmix checks a field the moment it becomes required (jmix.ui.component.immediate-required-validation-
            // enabled), so an empty reason would show its error before anything was tried. The check passes over a
            // hidden field: made required and given its first value while hidden, the field shows the error only
            // once a change or Reject checks it
            field.setVisible(false);
            field.setRequired(true);
            field.setValue(reason);
            field.setVisible(true);
            return field;
        }

        /**
         * Appends a sentence to the reason as a paragraph of its own after any text written before: Markdown joins
         * lines that only a line break separates.
         */
        private void appendReason(String sentence) {
            String current = StringUtils.stripEnd(Objects.toString(reasonField.getValue(), ""), null);
            reasonField.setValue(current.isEmpty() ? sentence : current + "\n\n" + sentence);
            reasonField.focus();
        }

        private void reject() {
            // Blanks say nothing, so they count as no reason, as the operation refuses them
            if (StringUtils.isBlank(reasonField.getValue())) {
                reasonField.clear();
            }

            ValidationErrors errors = viewValidation.validateUiComponent(reasonField);
            if (!errors.isEmpty()) {
                viewValidation.showValidationErrors(errors);
                viewValidation.focusProblemComponent(errors);
                return;
            }

            onReject.accept(reasonField.getValue());
        }

        private void askBeforeClosing() {
            unsavedInputSupport.askBeforeClosing(sideDialog, owner,
                    !Objects.toString(reasonField.getValue(), "").equals(openedReason), reasonField);
        }
    }

    private String message(String key, Object... params) {
        return params.length == 0
                ? messages.getMessage(ApprovalRejectDialog.class, key, locale)
                : messages.formatMessage(ApprovalRejectDialog.class, key, locale, params);
    }
}
