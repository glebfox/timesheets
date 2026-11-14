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

package com.glebfox.timesheets.theme;

import com.vaadin.flow.internal.CssBundler;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A relative {@code url()} in the theme must reach a file the application serves, both as development serves the
 * stylesheets, each from its own folder, and as a production build serves them, inlined into {@code styles.css}.
 * Either way it stays under the servlet context's root, so it holds under a context path.
 */
class ThemeStylesheetUrlsTest {

    private static final Path RESOURCES = Path.of("src/main/resources/META-INF/resources").toAbsolutePath();
    private static final Path THEME = RESOURCES.resolve("themes/timesheets-aura");

    private static final Pattern URL = Pattern.compile("url\\(\\s*(['\"]?)([^'\")]+)\\1\\s*\\)");
    private static final Pattern NOT_RELATIVE = Pattern.compile("^(data:|[a-z]+://|/|#).*|.*\\.css$");

    @Test
    void everyUrlOfTheProductionBundleReachesAServedFile() throws IOException {
        // As Vaadin's production build does (TaskProcessStylesheetCss): it inlines the imports into styles.css,
        // which is then served from the theme's folder, and rewrites only the url()s that point inside that folder
        String bundled = CssBundler.inlineImportsForStaticResourcesRelative(
                THEME.toFile(), THEME.resolve("styles.css").toFile(), null);

        assertThat(relativeUrls(bundled)).allSatisfy(url -> assertServed(THEME, url));
    }

    @Test
    void everyUrlOfEachStylesheetReachesAServedFile() throws IOException {
        List<Path> stylesheets;
        try (Stream<Path> files = Files.walk(THEME)) {
            stylesheets = files.filter(file -> file.toString().endsWith(".css")).toList();
        }

        for (Path stylesheet : stylesheets) {
            assertThat(relativeUrls(Files.readString(stylesheet)))
                    .allSatisfy(url -> assertServed(stylesheet.getParent(), url));
        }
    }

    @Test
    void loginLogoMaskIsTheBrandsMonochromeLogo() throws IOException {
        // The theme keeps a copy of its own for the url() above; a changed logo must reach both
        assertThat(Files.readAllBytes(THEME.resolve("view/login-view-logo-mask.svg")))
                .isEqualTo(Files.readAllBytes(RESOURCES.resolve("assets/brand/logos/logo-mono-white.svg")));
    }

    private static List<String> relativeUrls(String css) {
        Matcher matcher = URL.matcher(css);
        return matcher.results()
                .map(result -> result.group(2).trim())
                .filter(url -> !NOT_RELATIVE.matcher(url).matches())
                .toList();
    }

    private static void assertServed(Path folder, String url) {
        Path target = folder.resolve(url).normalize();
        assertThat(target)
                .as("url('%s') from %s", url, RESOURCES.relativize(folder))
                .startsWithRaw(RESOURCES)
                .exists();
    }
}
