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

package com.glebfox.timesheets.component.avatarupload;

import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Image formats that {@link AvatarUpload} accepts, recognized by their file signature.
 * SVG is deliberately absent: it can carry scripts.
 */
public enum AvatarImageFormat {

    PNG("image/png", List.of(".png"), "PNG") {
        @Override
        public boolean matches(ByteBuffer header) {
            return startsWith(header, 0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
        }
    },

    JPEG("image/jpeg", List.of(".jpg", ".jpeg"), "JPEG") {
        @Override
        public boolean matches(ByteBuffer header) {
            return startsWith(header, 0, 0xFF, 0xD8, 0xFF);
        }
    },

    GIF("image/gif", List.of(".gif"), "GIF") {
        @Override
        public boolean matches(ByteBuffer header) {
            return startsWith(header, 0, 'G', 'I', 'F', '8', '7', 'a')
                    || startsWith(header, 0, 'G', 'I', 'F', '8', '9', 'a');
        }
    },

    WEBP("image/webp", List.of(".webp"), "WebP") {
        @Override
        public boolean matches(ByteBuffer header) {
            return startsWith(header, 0, 'R', 'I', 'F', 'F')
                    && startsWith(header, 8, 'W', 'E', 'B', 'P');
        }
    };

    /**
     * The number of leading bytes that is enough to recognize any supported format.
     */
    public static final int SIGNATURE_LENGTH = 12;

    private final String mimeType;
    private final List<String> extensions;
    private final String displayName;

    AvatarImageFormat(String mimeType, List<String> extensions, String displayName) {
        this.mimeType = mimeType;
        this.extensions = extensions;
        this.displayName = displayName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public List<String> getExtensions() {
        return extensions;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns whether the leading bytes of a file carry this format's signature.
     * Reads by absolute index, so the buffer's position is left as it is.
     */
    public abstract boolean matches(ByteBuffer header);

    public static Optional<AvatarImageFormat> detect(ByteBuffer header) {
        return Arrays.stream(values())
                .filter(format -> format.matches(header))
                .findFirst();
    }

    /**
     * Resolves a MIME type, such as {@code image/png}, or a file extension with its dot,
     * such as {@code .png}, ignoring case.
     */
    public static Optional<AvatarImageFormat> fromFileType(@Nullable String fileType) {
        if (fileType == null) {
            return Optional.empty();
        }

        String type = fileType.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(format -> format.mimeType.equals(type) || format.extensions.contains(type))
                .findFirst();
    }

    private static boolean startsWith(ByteBuffer header, int offset, int... signature) {
        if (header.remaining() < offset + signature.length) {
            return false;
        }

        int start = header.position() + offset;
        for (int i = 0; i < signature.length; i++) {
            if ((header.get(start + i) & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
