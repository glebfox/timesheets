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

import com.vaadin.flow.server.streams.UploadContent;
import com.vaadin.flow.server.streams.UploadEvent;
import com.vaadin.flow.server.streams.UploadValidator;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Server-side checks of an {@link AvatarUpload} upload: the declared size, the file signature,
 * and the received size. Runs on the request thread, so it only rejects, with the name of an
 * {@link AvatarUploadRejection} as the message, and never touches the UI.
 */
class AvatarUploadValidator implements UploadValidator {

    private final IntSupplier maxFileSize;
    private final Supplier<Set<AvatarImageFormat>> acceptedFormats;

    AvatarUploadValidator(IntSupplier maxFileSize, Supplier<Set<AvatarImageFormat>> acceptedFormats) {
        this.maxFileSize = maxFileSize;
        this.acceptedFormats = acceptedFormats;
    }

    @Override
    public void validateMetadata(UploadEvent event) {
        // Vaadin does not bound the size of an XHR upload, so refuse an oversized one before reading it
        if (event.getFileSize() > maxFileSize.getAsInt()) {
            event.reject(AvatarUploadRejection.FILE_TOO_LARGE.name());
        }
    }

    @Override
    public void validateHeader(UploadEvent event, ByteBuffer header) {
        boolean accepted = AvatarImageFormat.detect(header)
                .filter(format -> acceptedFormats.get().contains(format))
                .isPresent();
        if (!accepted) {
            event.reject(AvatarUploadRejection.UNSUPPORTED_TYPE.name());
        }
    }

    @Override
    public void validateComplete(UploadEvent event, UploadContent content) {
        // The declared size can be missing or wrong, so check what was actually received
        if (content.size() > maxFileSize.getAsInt()) {
            event.reject(AvatarUploadRejection.FILE_TOO_LARGE.name());
        }
    }

    @Override
    public int headerSize() {
        return AvatarImageFormat.SIGNATURE_LENGTH;
    }
}
