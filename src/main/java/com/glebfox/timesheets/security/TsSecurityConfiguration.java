/*
 * Copyright 2024 Gleb Gorelov
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

package com.glebfox.timesheets.security;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import io.jmix.securityflowui.security.FlowuiVaadinWebSecurity;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer.AuthorizedUrl;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The security of the Flow UI, in place of the default configuration from the security-flowui module, which steps
 * aside when an application declares its own subclass of {@link FlowuiVaadinWebSecurity}.
 * <p>
 * You can configure custom API endpoints security by defining {@link SecurityFilterChain} beans in this class.
 * In most cases, custom SecurityFilterChain must be applied first, so the proper
 * {@link org.springframework.core.annotation.Order} should be defined for the bean. The order value from the
 * {@link io.jmix.core.JmixSecurityFilterChainOrder#CUSTOM} is guaranteed to be smaller than any other filter chain
 * order from Jmix.
 * <p>
 * Example:
 *
 * <pre>
 * &#064;Bean
 * &#064;Order(JmixSecurityFilterChainOrder.CUSTOM)
 * SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {
 *     http.securityMatcher("/public/**")
 *             .authorizeHttpRequests(authorize ->
 *                     authorize.anyRequest().permitAll()
 *             );
 *     return http.build();
 * }
 * </pre>
 */
@Configuration
public class TsSecurityConfiguration extends FlowuiVaadinWebSecurity {

    @Override
    protected void configureVaadinSpecifics(HttpSecurity http) {
        http.with(VaadinSecurityConfigurer.vaadin(), configurer -> {
            initLoginView(configurer);
            // Vaadin denies by default every request whose address opens no view, so a signed-in user who types
            // an unknown address gets a 403 from Spring Security instead of the "Nothing was found" page
            // (docs/decisions/0005)
            configurer.anyRequest(AuthorizedUrl::authenticated);
        });
    }
}
