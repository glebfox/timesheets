/*
 * Copyright 2023 Gleb Gorelov
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

import com.glebfox.timesheets.demo.DemoDataLoadState;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.AbstractLogin.LoginEvent;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import io.jmix.core.CoreProperties;
import io.jmix.core.MessageTools;
import io.jmix.core.security.AccessDeniedException;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.loginform.JmixLoginForm;
import io.jmix.flowui.component.radiobuttongroup.JmixRadioButtonGroup;
import io.jmix.flowui.kit.component.ComponentUtils;
import io.jmix.flowui.kit.component.loginform.JmixLoginI18n;
import io.jmix.flowui.view.*;
import io.jmix.securityflowui.authentication.AuthDetails;
import io.jmix.securityflowui.authentication.LoginViewSupport;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

@Route(value = "login")
@ViewController("ts_LoginView")
@ViewDescriptor("login-view.xml")
public class LoginView extends StandardView implements LocaleChangeObserver {

    private static final Logger log = LoggerFactory.getLogger(LoginView.class);

    @Autowired
    private CoreProperties coreProperties;

    @Autowired
    private LoginViewSupport loginViewSupport;

    @ViewComponent
    private MessageBundle messageBundle;

    @Autowired
    private MessageTools messageTools;

    @Autowired
    private UiComponents uiComponents;

    // Only a dev start has one
    @Autowired
    private ObjectProvider<DemoDataLoadState> demoDataLoadState;

    @ViewComponent
    private JmixLoginForm login;

    @ViewComponent
    private JmixRadioButtonGroup<Locale> localeSwitch;

    @Value("${ui.login.defaultUsername:}")
    private String defaultUsername;

    @Value("${ui.login.defaultPassword:}")
    private String defaultPassword;

    // Set while the page waits for the demo data
    private @Nullable Runnable stopWaitingForDemoData;

    @Subscribe
    public void onInit(final InitEvent event) {
        initLocales();
        initDefaultCredentials();
    }

    private void initLocales() {
        LinkedHashMap<Locale, String> locales = coreProperties.getAvailableLocales().stream()
                .collect(Collectors.toMap(Function.identity(), messageTools::getLocaleDisplayName, (s1, s2) -> s1,
                        LinkedHashMap::new));

        // The form keeps its items although its list is hidden: setSelectedLocale accepts only a locale among them
        ComponentUtils.setItemsMap(login, locales);
        ComponentUtils.setItemsMap(localeSwitch, locales);
        // Set after the items: setItemsMap sets an item label generator, which replaces the renderer with a text one
        localeSwitch.setRenderer(new ComponentRenderer<>(locale -> {
            Span label = new Span(locales.get(locale));
            label.getElement().setAttribute("lang", locale.toLanguageTag());
            return label;
        }));

        login.setSelectedLocale(VaadinSession.getCurrent().getLocale());
        localeSwitch.setValue(login.getSelectedLocale());
    }

    private void initDefaultCredentials() {
        if (StringUtils.isNotBlank(defaultUsername)) {
            login.setUsername(defaultUsername);
        }

        if (StringUtils.isNotBlank(defaultPassword)) {
            login.setPassword(defaultPassword);
        }
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        demoDataLoadState.ifAvailable(this::waitForDemoData);
    }

    // Jmix's UiEventPublisher cannot end the wait: it reaches only the sessions whose security context holds a
    // signed-in user (SessionHolder.getActiveSessionsForUsernames), and a login page's session has none
    private void waitForDemoData(DemoDataLoadState loadState) {
        Dialog dialog = createDemoDataDialog();
        UI ui = UI.getCurrent();
        loadState.whenLoadEnds(() -> ui.access(dialog::close)).ifPresent(registration -> {
            dialog.open();
            stopWaitingForDemoData = () -> {
                registration.remove();
                dialog.close();
            };
        });
    }

    // The dialog belongs to the UI, not to the view, so it would outlive the view. Not named onDetach, which would
    // override the View method that removes the view's application listeners.
    @Subscribe
    public void onViewDetach(final DetachEvent event) {
        if (stopWaitingForDemoData != null) {
            stopWaitingForDemoData.run();
            stopWaitingForDemoData = null;
        }
    }

    private Dialog createDemoDataDialog() {
        // A Vaadin dialog, not one of Jmix's Dialogs: the message dialog always has an OK button, the option dialog's
        // builder cannot keep Esc or a click outside from closing it, and the background task dialog runs a task of
        // its own, where this page only waits for the start's
        Dialog dialog = uiComponents.create(Dialog.class);
        // A Vaadin 25 dialog is ModalityMode.VISUAL by default, which blocks the page only in the browser.
        // STRICT makes the server ignore the events of everything outside the dialog, the login form's included.
        dialog.setModality(ModalityMode.STRICT);
        dialog.setCloseOnEsc(false);
        dialog.setCloseOnOutsideClick(false);
        dialog.setHeaderTitle(messageBundle.getMessage("demoDataDialog.title"));

        ProgressBar progressBar = uiComponents.create(ProgressBar.class);
        progressBar.setIndeterminate(true);
        // A progress bar needs a name of its own, and Vaadin's has no setter for it
        progressBar.getElement().setAttribute("aria-label", messageBundle.getMessage("demoDataDialog.title"));
        dialog.add(progressBar);
        return dialog;
    }

    @Subscribe("localeSwitch")
    public void onLocaleSwitchComponentValueChange(
            final ComponentValueChangeEvent<JmixRadioButtonGroup<Locale>, Locale> event) {
        // The form ignores the locale it already has, so the value set when the view opens changes nothing
        if (event.getValue() != null) {
            login.setSelectedLocale(event.getValue());
        }
    }

    @Subscribe("login")
    public void onLogin(final LoginEvent event) {
        try {
            loginViewSupport.authenticate(
                    AuthDetails.of(event.getUsername(), event.getPassword())
                            .withLocale(login.getSelectedLocale())
                            .withRememberMe(login.isRememberMe())
            );
        } catch (final BadCredentialsException | DisabledException | LockedException | AccessDeniedException e) {
            log.warn("Login failed for user '{}': {}", event.getUsername(), e.toString());
            event.getSource().setError(true);
        }
    }

    @Override
    public void localeChange(final LocaleChangeEvent event) {
        UI.getCurrent().getPage().setTitle(messageBundle.getMessage("LoginView.title"));

        final JmixLoginI18n loginI18n = JmixLoginI18n.createDefault();

        final JmixLoginI18n.JmixForm form = new JmixLoginI18n.JmixForm();
        form.setTitle(messageBundle.getMessage("loginForm.headerTitle"));
        form.setUsername(messageBundle.getMessage("loginForm.username"));
        form.setPassword(messageBundle.getMessage("loginForm.password"));
        form.setSubmit(messageBundle.getMessage("loginForm.submit"));
        form.setForgotPassword(messageBundle.getMessage("loginForm.forgotPassword"));
        form.setRememberMe(messageBundle.getMessage("loginForm.rememberMe"));
        loginI18n.setForm(form);

        final LoginI18n.ErrorMessage errorMessage = new LoginI18n.ErrorMessage();
        errorMessage.setTitle(messageBundle.getMessage("loginForm.errorTitle"));
        errorMessage.setMessage(messageBundle.getMessage("loginForm.badCredentials"));
        errorMessage.setUsername(messageBundle.getMessage("loginForm.errorUsername"));
        errorMessage.setPassword(messageBundle.getMessage("loginForm.errorPassword"));
        loginI18n.setErrorMessage(errorMessage);

        login.setI18n(loginI18n);

        // The descriptor's text is resolved once, when the view is created
        localeSwitch.setAriaLabel(messageBundle.getMessage("localeSwitch.ariaLabel"));
    }
}
