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
import com.glebfox.timesheets.app.util.PluralForms;
import com.glebfox.timesheets.component.icon.GravityUiIcon;
import com.glebfox.timesheets.data.renderer.BadgeStyle;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.shared.Registration;
import io.jmix.core.Messages;
import io.jmix.core.security.CurrentAuthentication;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * The bar of the reject basket, which floats over the bottom of the selected person's part of the approval screen
 * while the basket has entries: their count, their dates, Clear, and Write a reason. It is hidden while the basket is
 * empty.
 * <p>
 * The bar only shows what it is given with {@link #setEntries}; its buttons do nothing of their own, and whoever shows
 * the bar attaches what they do. It is created by {@code UiComponents}, as a descriptor's {@code component} element
 * creates it, since it takes the beans it needs from the application context.
 * <p>
 * The bar has the class {@code approval-basket-bar}. Its parts are the {@code count} badge, the {@code label}, the
 * {@code separator}, the {@code dates}, and the buttons; the count, the dates, and the buttons have the ids
 * {@code basketCountText}, {@code basketDatesText}, {@code clearBasketBtn}, and {@code writeReasonBtn}.
 * <p>
 * The bar is a container so that a lookup of components by id, as Jmix does it, reaches its parts.
 */
public final class ApprovalBasketBar extends Composite<Div>
        implements HasComponents, ApplicationContextAware, InitializingBean {

    static final String CLASS_NAME = "approval-basket-bar";

    private ApplicationContext applicationContext;
    private Messages messages;
    private Locale locale;
    private ApprovalPeriodTitle titles;

    private Badge count;
    private Span label;
    private Span dates;
    // Before the bar is built, so that listeners can be added at any time
    private final Button clearButton = new Button();
    private final Button writeReasonButton = new Button();

    /**
     * Creates the bar, hidden until it is given entries.
     */
    public ApprovalBasketBar() {
        getContent().addClassName(CLASS_NAME);
        setVisible(false);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        messages = applicationContext.getBean(Messages.class);
        locale = applicationContext.getBean(CurrentAuthentication.class).getLocale();
        titles = new ApprovalPeriodTitle(messages, locale);
        initParts(applicationContext.getBean(ComponentSupport.class));
    }

    private void initParts(ComponentSupport componentSupport) {
        count = componentSupport.createBadge("", BadgeStyle.ERROR);
        count.addThemeVariants(BadgeVariant.FILLED);
        count.setId("basketCountText");
        count.addClassName("count");

        label = new Span();
        label.addClassName("label");

        Span separator = new Span(messages.getMessage(ApprovalBasketBar.class, "entriesDay.separator", locale));
        separator.addClassName("separator");

        dates = new Span();
        dates.setId("basketDatesText");
        dates.addClassName("dates");

        clearButton.setText(messages.getMessage(ApprovalBasketBar.class, "clearBasketBtn.text", locale));
        clearButton.setId("clearBasketBtn");
        clearButton.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);

        writeReasonButton.setText(messages.getMessage(ApprovalBasketBar.class, "writeReasonBtn.text", locale));
        writeReasonButton.setIcon(GravityUiIcon.ARROW_RIGHT.create());
        writeReasonButton.setId("writeReasonBtn");
        writeReasonButton.setIconAfterText(true);
        writeReasonButton.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.ERROR, ButtonVariant.SMALL);

        // The spaces keep the bar's text, such as "2 entries to reject · Oct 5 – 6", in words for assistive technology
        getContent().add(count, new Text(" "), label, new Text(" "),
                separator, new Text(" "), dates, clearButton, writeReasonButton);
    }

    /**
     * Shows the entries of the basket: their count and their dates. With none, the bar is hidden.
     *
     * @param entries the entries in the basket
     */
    public void setEntries(Collection<TimeEntry> entries) {
        setVisible(!entries.isEmpty());
        if (entries.isEmpty()) {
            return;
        }

        int size = entries.size();
        count.setText(String.valueOf(size));
        label.setText(messages.getMessage(ApprovalBasketBar.class,
                PluralForms.key("basketBar.label", size, locale), locale));
        dates.setText(formatDays(entries));
    }

    /**
     * Adds a listener to Clear, which empties the basket.
     *
     * @param listener the listener
     * @return a registration that removes the listener
     */
    public Registration addClearClickListener(ComponentEventListener<ClickEvent<Button>> listener) {
        return clearButton.addClickListener(listener);
    }

    /**
     * Adds a listener to Write a reason, which opens the dialog where the entries are rejected.
     *
     * @param listener the listener
     * @return a registration that removes the listener
     */
    public Registration addWriteReasonClickListener(ComponentEventListener<ClickEvent<Button>> listener) {
        return writeReasonButton.addClickListener(listener);
    }

    /**
     * Writes the days of the entries in order, a run of days in a row as a range, such as "Oct 5 – 7, Oct 14".
     */
    private String formatDays(Collection<TimeEntry> entries) {
        List<LocalDate> days = entries.stream().map(TimeEntry::getDate).distinct().sorted().toList();

        List<String> runs = new ArrayList<>();
        LocalDate first = days.getFirst();
        LocalDate last = first;
        for (LocalDate day : days.subList(1, days.size())) {
            if (!day.equals(last.plusDays(1))) {
                runs.add(titles.formatDates(first, last));
                first = day;
            }
            last = day;
        }
        runs.add(titles.formatDates(first, last));
        return String.join(", ", runs);
    }
}
