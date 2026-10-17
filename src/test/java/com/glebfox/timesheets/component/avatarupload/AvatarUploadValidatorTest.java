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
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.EnumSet;

import static com.glebfox.timesheets.component.avatarupload.AvatarImageFormatTest.header;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AvatarUploadValidatorTest {

    private final AvatarUploadValidator validator = new AvatarUploadValidator(
            () -> 1000, () -> EnumSet.of(AvatarImageFormat.PNG, AvatarImageFormat.JPEG));

    private final UploadEvent event = mock(UploadEvent.class);

    @Test
    void rejectsDeclaredSizeOverMax() throws IOException {
        when(event.getFileSize()).thenReturn(1001L);

        validator.validateMetadata(event);

        verify(event).reject("FILE_TOO_LARGE");
    }

    @Test
    void acceptsUnknownOrSmallDeclaredSize() throws IOException {
        when(event.getFileSize()).thenReturn(-1L);
        validator.validateMetadata(event);

        when(event.getFileSize()).thenReturn(1000L);
        validator.validateMetadata(event);

        verify(event, never()).reject(anyString());
    }

    @Test
    void headerSizeIsSignatureLength() {
        assertEquals(12, validator.headerSize());
    }

    @Test
    void rejectsNonImageHeader() throws IOException {
        validator.validateHeader(event, header("hello, this is text").asReadOnlyBuffer());

        verify(event).reject("UNSUPPORTED_TYPE");
    }

    @Test
    void rejectsImageOfUnacceptedFormat() throws IOException {
        validator.validateHeader(event, header("GIF89a").asReadOnlyBuffer());

        verify(event).reject("UNSUPPORTED_TYPE");
    }

    @Test
    void acceptsAcceptedFormat() throws IOException {
        validator.validateHeader(event, header(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A).asReadOnlyBuffer());

        verify(event, never()).reject(anyString());
    }

    @Test
    void rejectsActualSizeOverMax() throws IOException {
        UploadContent content = mock(UploadContent.class);

        when(content.size()).thenReturn(1000L);
        validator.validateComplete(event, content);
        verify(event, never()).reject(anyString());

        when(content.size()).thenReturn(1001L);
        validator.validateComplete(event, content);
        verify(event).reject("FILE_TOO_LARGE");
    }
}
