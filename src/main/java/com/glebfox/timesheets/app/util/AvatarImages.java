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

package com.glebfox.timesheets.app.util;

import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import org.jspecify.annotations.NullMarked;

import java.io.ByteArrayInputStream;

/**
 * The image handler of every avatar the application builds from stored image bytes.
 */
@NullMarked
public final class AvatarImages {

    private AvatarImages() {
    }

    /**
     * Creates the handler to pass to {@code Avatar.setImageHandler} for an avatar that shows the given image.
     * <p>
     * Vaadin answers 403 for the image of a disabled avatar. Tabbed mode disables the content of a tab that is not
     * selected, and an avatar rendered just before its tab is disabled, such as one in the project editor's client
     * field or in a grid's avatar cell, has its image requested while disabled. Vaadin serves Image and IFrame sources
     * while disabled, but not Avatar images
     * (<a href="https://github.com/vaadin/flow/issues/22772">vaadin/flow#22772</a>), so the handler allows disabled
     * requests.
     *
     * @param data the image bytes
     * @return an inline handler that serves the image to a disabled avatar too
     */
    public static DownloadHandler handler(byte[] data) {
        // inline() comes first: setImageHandler makes only a predefined handler inline, not the wrapper that
        // allowDisabled() returns
        return DownloadHandler.fromInputStream(downloadEvent ->
                        new DownloadResponse(new ByteArrayInputStream(data), "avatar.png", null, data.length))
                .inline()
                .allowDisabled();
    }
}
