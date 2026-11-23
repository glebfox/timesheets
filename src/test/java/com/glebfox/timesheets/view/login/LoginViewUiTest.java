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
import com.glebfox.timesheets.demo.DemoDataLoadState;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
import io.jmix.core.Messages;
import io.jmix.flowui.component.image.JmixImage;
import io.jmix.flowui.component.loginform.JmixLoginForm;
import io.jmix.flowui.component.radiobuttongroup.JmixRadioButtonGroup;
import io.jmix.flowui.view.StandardOutcome;
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
class LoginViewUiTest {

    private static final Locale RUSSIAN = Locale.of("ru", "RU");

    @Autowired
    ViewBuilders viewBuilders;

    @Autowired
    Messages messages;

    // The test profile has no demo data load: Mockito's empty Optional tells the view that none is under way
    @MockitoBean
    DemoDataLoadState demoDataLoadState;

    private final AtomicReference<Runnable> loadEnd = new AtomicReference<>();

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
    void showsAStrictlyModalDialogWhileTheDemoDataLoads() {
        startALoad();
        openLoginView();

        Dialog dialog = openedDialog();
        assertThat(dialog.isOpened()).isTrue();
        assertThat(dialog.getModality()).isEqualTo(ModalityMode.STRICT);
        assertThat(UI.getCurrent().hasModalComponent()).isTrue();
        assertThat(dialog.isCloseOnEsc()).isFalse();
        assertThat(dialog.isCloseOnOutsideClick()).isFalse();
        assertThat(dialog.getHeaderTitle()).isEqualTo("Creating demo data");
    }

    @Test
    void stacksTheTextAboveAnEndlessProgressBarWithSpaceBetween() {
        startALoad();
        openLoginView();

        // The dialog's own padding surrounds the stack, and the layout's gap keeps the bar off the text
        VerticalLayout content = (VerticalLayout) openedDialog().getChildren().findFirst().orElseThrow();
        assertThat(content.isPadding()).isFalse();
        assertThat(content.isSpacing()).isTrue();
        List<Component> parts = content.getChildren().toList();
        assertThat(parts).hasSize(2);
        assertThat(parts.get(0).getElement().getText()).isEqualTo("Login opens by itself when the data is ready.");
        assertThat(parts.get(1)).isInstanceOfSatisfying(ProgressBar.class,
                progressBar -> assertThat(progressBar.isIndeterminate()).isTrue());
    }

    @Test
    void namesTheProgressBarForAssistiveTechnology() {
        startALoad();
        openLoginView();

        ProgressBar progressBar = (ProgressBar) ((VerticalLayout) openedDialog().getChildren().findFirst().orElseThrow())
                .getChildren()
                .filter(ProgressBar.class::isInstance)
                .findFirst()
                .orElseThrow();
        assertThat(progressBar.getElement().getAttribute("aria-label")).isEqualTo("Creating demo data");
    }

    @Test
    void leavingThePageStopsWaitingAndClosesTheDialog() {
        startALoad();
        LoginView view = openLoginView();
        Dialog dialog = openedDialog();

        view.close(StandardOutcome.CLOSE);

        assertThat(loadEnd.get()).isNull();
        assertThat(dialog.isOpened()).isFalse();
    }

    @Test
    void theEndOfTheLoadClosesTheDialog() {
        startALoad();
        openLoginView();
        Dialog dialog = openedDialog();

        loadEnd.get().run();

        // The close goes through UI.access, which a UI test only queues: its session's unlock runs nothing
        assertThat(dialog.isOpened()).isTrue();
        VaadinSession session = VaadinSession.getCurrent();
        session.getService().runPendingAccessTasks(session);
        assertThat(dialog.isOpened()).isFalse();
    }

    @Test
    void showsNoDialogWithoutALoadUnderWay() {
        openLoginView();

        assertThat(dialogs()).isEmpty();
        assertThat(UI.getCurrent().hasModalComponent()).isFalse();
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
        assertThat(messages.getMessage(group + "demoDataDialog.title", Locale.ENGLISH))
                .isEqualTo("Creating demo data");
        assertThat(messages.getMessage(group + "demoDataDialog.title", RUSSIAN)).isEqualTo("Создаём демо-данные");
        assertThat(messages.getMessage(group + "demoDataDialog.text", Locale.ENGLISH))
                .isEqualTo("Login opens by itself when the data is ready.");
        assertThat(messages.getMessage(group + "demoDataDialog.text", RUSSIAN))
                .isEqualTo("Вход откроется сам, когда данные будут готовы.");
    }

    private void startALoad() {
        when(demoDataLoadState.whenLoadEnds(any())).thenAnswer(invocation -> {
            loadEnd.set(invocation.getArgument(0));
            return Optional.of((Registration) () -> loadEnd.set(null));
        });
    }

    // An opened dialog is added to the UI, and its modality applied, only before the response to the browser
    private static List<Dialog> dialogs() {
        UI.getCurrent().getInternals().getStateTree().runExecutionsBeforeClientResponse();
        return UI.getCurrent().getChildren()
                .filter(Dialog.class::isInstance)
                .map(Dialog.class::cast)
                .toList();
    }

    // The view opens as a tab of a blank initial view, so the only dialog under the UI is the view's
    private static Dialog openedDialog() {
        assertThat(dialogs()).hasSize(1);
        return dialogs().getFirst();
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
