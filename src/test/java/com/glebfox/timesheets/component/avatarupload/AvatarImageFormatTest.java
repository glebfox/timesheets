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

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AvatarImageFormatTest {

    @Test
    void detectsEachSupportedSignature() {
        assertEquals(Optional.of(AvatarImageFormat.PNG),
                AvatarImageFormat.detect(header(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)));
        assertEquals(Optional.of(AvatarImageFormat.JPEG),
                AvatarImageFormat.detect(header(0xFF, 0xD8, 0xFF, 0xE0)));
        assertEquals(Optional.of(AvatarImageFormat.GIF), AvatarImageFormat.detect(header("GIF87a")));
        assertEquals(Optional.of(AvatarImageFormat.GIF), AvatarImageFormat.detect(header("GIF89a")));
        assertEquals(Optional.of(AvatarImageFormat.WEBP), AvatarImageFormat.detect(header("RIFF\u0010\u0000\u0000\u0000WEBP")));
    }

    @Test
    void doesNotDetectTextOrShortInput() {
        assertEquals(Optional.empty(), AvatarImageFormat.detect(header("hello, this is text")));
        assertEquals(Optional.empty(), AvatarImageFormat.detect(ByteBuffer.wrap(new byte[]{(byte) 0x89, 0x50})));
        assertEquals(Optional.empty(), AvatarImageFormat.detect(ByteBuffer.allocate(0)));
        assertEquals(Optional.empty(), AvatarImageFormat.detect(header("RIFF\u0010\u0000\u0000\u0000WAVE")));
    }

    @Test
    void doesNotDetectSvg() {
        assertEquals(Optional.empty(),
                AvatarImageFormat.detect(header("<svg xmlns=\"http://www.w3.org/2000/svg\"/>")));
    }

    @Test
    void detectLeavesBufferPosition() {
        ByteBuffer buffer = header("GIF89a");
        buffer.position(0);

        AvatarImageFormat.detect(buffer);

        assertEquals(0, buffer.position());
    }

    @Test
    void mapsFileTypes() {
        assertEquals(Optional.of(AvatarImageFormat.PNG), AvatarImageFormat.fromFileType("image/png"));
        assertEquals(Optional.of(AvatarImageFormat.JPEG), AvatarImageFormat.fromFileType("IMAGE/JPEG"));
        assertEquals(Optional.of(AvatarImageFormat.JPEG), AvatarImageFormat.fromFileType(".JPG"));
        assertEquals(Optional.of(AvatarImageFormat.JPEG), AvatarImageFormat.fromFileType(".jpeg"));
        assertEquals(Optional.of(AvatarImageFormat.WEBP), AvatarImageFormat.fromFileType(".webp"));

        assertEquals(Optional.empty(), AvatarImageFormat.fromFileType("image/svg+xml"));
        assertEquals(Optional.empty(), AvatarImageFormat.fromFileType(".bmp"));
        assertEquals(Optional.empty(), AvatarImageFormat.fromFileType("image/*"));
        assertEquals(Optional.empty(), AvatarImageFormat.fromFileType("png"));
    }

    static ByteBuffer header(int... bytes) {
        byte[] padded = new byte[AvatarImageFormat.SIGNATURE_LENGTH];
        for (int i = 0; i < bytes.length; i++) {
            padded[i] = (byte) bytes[i];
        }
        return ByteBuffer.wrap(padded);
    }

    static ByteBuffer header(String ascii) {
        byte[] bytes = ascii.getBytes(StandardCharsets.ISO_8859_1);
        return ByteBuffer.wrap(Arrays.copyOf(bytes, Math.max(bytes.length, AvatarImageFormat.SIGNATURE_LENGTH)));
    }
}
