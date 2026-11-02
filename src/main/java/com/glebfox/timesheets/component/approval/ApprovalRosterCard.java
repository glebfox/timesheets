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

import com.glebfox.timesheets.app.util.ApprovalPeriod;
import com.glebfox.timesheets.app.util.ApprovalPerson;
import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.user.User;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Svg;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.Div;
import io.jmix.core.Messages;
import io.jmix.core.security.CurrentAuthentication;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A person's card in the roster of the approval screen: the avatar, the name, the project roles, the status badges,
 * the fingerprint of the period, and the time logged in it. A click on the card, or Enter or Space on it, fires a
 * {@link ClickEvent}; whether the card shows as selected is set through {@link #setSelected}.
 * <p>
 * The card is created by {@code UiComponents}, since it takes the beans it needs from the application context, and
 * shows the person given to {@link #setPerson}.
 * <p>
 * The card's own classes are {@code approval-roster-card} and, while selected, {@code selected}; its parts have the
 * classes {@code avatar}, {@code name}, {@code role}, {@code badges}, {@code fingerprint}, and {@code logged}.
 */
public class ApprovalRosterCard extends Composite<Div>
        implements ClickNotifier<ApprovalRosterCard>, ApplicationContextAware, InitializingBean {

    static final String CLASS_NAME = "approval-roster-card";
    static final String SELECTED_CLASS_NAME = "selected";

    private ApplicationContext applicationContext;
    private ComponentSupport componentSupport;
    private Messages messages;
    private Locale locale;

    /**
     * Creates an empty card.
     */
    public ApprovalRosterCard() {
        Div content = getContent();
        content.addClassName(CLASS_NAME);
        // A card is chosen as a button is: by a click, or by Enter or Space while it has the focus, which click it
        // here. Without preventDefault, Space would also scroll the roster
        content.getElement().setAttribute("role", "button");
        content.getElement().setAttribute("tabindex", "0");
        content.getElement()
                .addEventListener("keydown", event -> ComponentUtil.fireEvent(this, new ClickEvent<>(this)))
                .setFilter("event.key === 'Enter' || event.key === ' '")
                .preventDefault();

        setSelected(false);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        componentSupport = applicationContext.getBean(ComponentSupport.class);
        messages = applicationContext.getBean(Messages.class);
        locale = applicationContext.getBean(CurrentAuthentication.class).getLocale();
    }

    /**
     * Shows a person on the card, in place of what it showed.
     *
     * @param person    the person with their entries in the period
     * @param roleNames the names of the person's roles on the projects of their entries, none for a person who is not
     *                  a participant of them
     * @param period    the period of the entries
     */
    public void setPerson(ApprovalPerson person, List<String> roleNames, ApprovalPeriod period) {
        String name = nameOf(person.user());

        Div content = getContent();
        content.removeAll();

        Avatar avatar = componentSupport.createAvatar(name, person.user().getAvatar(), "avatar");

        Div nameText = new Div(name);
        nameText.addClassName("name");

        Div badges = new Div();
        badges.addClassName("badges");
        ApprovalStatusBadges.of(person.totals(), componentSupport, messages, locale).forEach(badges::add);

        content.add(avatar, nameText, badges);

        if (!roleNames.isEmpty()) {
            Div role = new Div(String.join(", ", roleNames));
            role.addClassName("role");
            content.add(role);
        }

        Map<LocalDate, List<TimeEntry>> entriesByDay = person.entries().stream()
                .collect(Collectors.groupingBy(TimeEntry::getDate));
        Svg fingerprint = new Svg(ApprovalFingerprint.svg(period.days(), entriesByDay));
        // Svg has no class name methods of its own
        fingerprint.getElement().getClassList().add("fingerprint");

        Div logged = new Div(messages.formatMessage(ApprovalRosterCard.class, "rosterCard.logged", locale,
                componentSupport.formatWorkTimeTotal(person.totals().time())));
        logged.addClassName("logged");

        content.add(fingerprint, logged);
    }

    /**
     * Returns the name a person is shown by: the first and the last name, or the username of a user without them.
     *
     * @param user the person's user
     * @return the name of the person
     */
    public static String nameOf(User user) {
        String fullName = String.format("%s %s",
                user.getFirstName() != null ? user.getFirstName() : "",
                user.getLastName() != null ? user.getLastName() : "").trim();
        return fullName.isEmpty() ? user.getUsername() : fullName;
    }

    /**
     * Marks the card as the selected one or not.
     *
     * @param selected whether the card's person is the selected person
     */
    public void setSelected(boolean selected) {
        getContent().setClassName(SELECTED_CLASS_NAME, selected);
        getContent().getElement().setAttribute("aria-pressed", String.valueOf(selected));
    }
}
