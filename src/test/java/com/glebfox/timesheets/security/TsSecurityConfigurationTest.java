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

package com.glebfox.timesheets.security;

import com.glebfox.timesheets.entity.user.User;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Requests that a browser sends for an address typed into its address bar, passed through
 * the application's security filters. A request the filters let through reaches the Vaadin
 * servlet, which shows the "Nothing was found" page for an address with no view.
 */
@SpringBootTest
public class TsSecurityConfigurationTest {

    @Autowired
    @Qualifier("springSecurityFilterChain")
    Filter securityFilters;
    @Autowired
    DatabaseUserRepository userRepository;

    @Test
    void signedInUserReachesTheApplicationAtAnUnknownAddress() throws Exception {
        // Vaadin is mapped to the root, so the dispatcher servlet, mapped to "/", receives every address
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/no-such-page");
        request.setServletPath("/no-such-page");
        request.setSession(sessionSignedInAsAdmin());
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain application = new MockFilterChain();

        securityFilters.doFilter(request, response, application);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(application.getRequest()).isNotNull();
    }

    private MockHttpSession sessionSignedInAsAdmin() {
        User admin = userRepository.loadUserByUsername("admin");
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(admin, null, admin.getAuthorities()));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }
}
