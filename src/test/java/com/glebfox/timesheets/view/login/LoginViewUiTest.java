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

package com.glebfox.timesheets.view.login;

import com.glebfox.timesheets.TimesheetsApplication;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.server.VaadinSession;
import io.jmix.core.Messages;
import io.jmix.flowui.component.image.JmixImage;
import io.jmix.flowui.component.loginform.JmixLoginForm;
import io.jmix.flowui.component.radiobuttongroup.JmixRadioButtonGroup;
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
class LoginViewUiTest {

    private static final Locale RUSSIAN = Locale.of("ru", "RU");

    @Autowired
    ViewBuilders viewBuilders;

    @Autowired
    Messages messages;

    @Test
    void localeSwitchOffersEveryAvailableLocaleNamedInItsOwnLanguage() {
        JmixRadioButtonGroup<Locale> localeSwitch = component(openLoginView(), "localeSwitch");
        assertThat(localeSwitch.getListDataView().getItems()).containsExactly(Locale.ENGLISH, RUSSIAN);
        assertThat(localeSwitch.getItemLabelGenerator().apply(Locale.ENGLISH)).isEqualTo("English");
        assertThat(localeSwitch.getItemLabelGenerator().apply(RUSSIAN)).isEqualTo("Русский");
    }

    @Test
    void eachSwitchItemCarriesItsLanguage() {
        JmixRadioButtonGroup<Locale> localeSwitch = component(openLoginView(), "localeSwitch");
        Component label = localeSwitch.getItemRenderer().createComponent(RUSSIAN);
        assertThat(label.getElement().getText()).isEqualTo("Русский");
        assertThat(label.getElement().getAttribute("lang")).isEqualTo("ru-RU");
    }

    @Test
    void loginFormHidesItsOwnLocaleList() {
        JmixLoginForm login = component(openLoginView(), "login");
        assertThat(login.isLocalesVisible()).isFalse();
    }

    @Test
    void localeSwitchStartsAtTheFormsLocale() {
        LoginView view = openLoginViewIn(RUSSIAN);
        JmixLoginForm login = component(view, "login");
        JmixRadioButtonGroup<Locale> localeSwitch = component(view, "localeSwitch");
        assertThat(login.getSelectedLocale()).isEqualTo(RUSSIAN);
        assertThat(localeSwitch.getValue()).isEqualTo(RUSSIAN);
    }

    @Test
    void checkingALanguageSelectsItInTheFormAndTheSession() {
        LoginView view = openLoginViewIn(Locale.ENGLISH);
        JmixLoginForm login = component(view, "login");
        JmixRadioButtonGroup<Locale> localeSwitch = component(view, "localeSwitch");

        localeSwitch.setValue(RUSSIAN);

        assertThat(login.getSelectedLocale()).isEqualTo(RUSSIAN);
        assertThat(VaadinSession.getCurrent().getLocale()).isEqualTo(RUSSIAN);
    }

    @Test
    void localeSwitchHasAnAccessibleName() {
        JmixRadioButtonGroup<Locale> localeSwitch = component(openLoginView(), "localeSwitch");
        assertThat(localeSwitch.getAriaLabel()).hasValue("Language");
    }

    @Test
    void logoIsTheBrandLogoNamedTimesheets() {
        JmixImage<?> logo = component(openLoginView(), "logo");
        assertThat(logo.getSrc()).endsWith("assets/brand/logos/logo-dark.svg");
        assertThat(logo.getAlt()).hasValue("Timesheets");
    }

    @Test
    void loginTextsExistInBothLanguages() {
        String group = "com.glebfox.timesheets.view.login/";
        assertThat(messages.getMessage(group + "loginForm.headerTitle", Locale.ENGLISH)).isEqualTo("Log in");
        assertThat(messages.getMessage(group + "loginForm.headerTitle", RUSSIAN)).isEqualTo("Вход");
        assertThat(messages.getMessage(group + "loginForm.submit", Locale.ENGLISH)).isEqualTo("Log in");
        assertThat(messages.getMessage(group + "loginForm.submit", RUSSIAN)).isEqualTo("Войти");
        assertThat(messages.getMessage(group + "localeSwitch.ariaLabel", Locale.ENGLISH)).isEqualTo("Language");
        assertThat(messages.getMessage(group + "localeSwitch.ariaLabel", RUSSIAN)).isEqualTo("Язык");
        assertThat(messages.getMessage(group + "logo.alternateText", Locale.ENGLISH)).isEqualTo("Timesheets");
        assertThat(messages.getMessage(group + "logo.alternateText", RUSSIAN)).isEqualTo("Timesheets");
    }

    private LoginView openLoginView() {
        viewBuilders.view(UiTestUtils.getCurrentView(), LoginView.class).open();
        return UiTestUtils.getCurrentView();
    }

    // Each test gets a fresh session whose locale is the JVM's default, which may be no available locale at all
    private LoginView openLoginViewIn(Locale locale) {
        VaadinSession.getCurrent().setLocale(locale);
        return openLoginView();
    }

    private static <T extends Component> T component(LoginView view, String id) {
        return UiTestUtils.getComponent(view, id);
    }
}
