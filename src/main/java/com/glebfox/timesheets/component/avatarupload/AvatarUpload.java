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

import com.glebfox.timesheets.component.icon.GravityUiIcon;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Svg;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.upload.UploadButton;
import com.vaadin.flow.component.upload.UploadDropZone;
import com.vaadin.flow.component.upload.UploadManager;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.TransferContext;
import com.vaadin.flow.server.streams.TransferProgressListener;
import com.vaadin.flow.server.streams.UploadRejectedException;
import io.jmix.core.Messages;
import io.jmix.core.Resources;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.delegate.FieldDelegate;
import io.jmix.flowui.component.upload.handler.InMemoryUploadHandler;
import io.jmix.flowui.data.SupportsValueSource;
import io.jmix.flowui.data.ValueSource;
import org.apache.commons.io.FileUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A field that shows a {@code byte[]} image attribute as a round avatar and lets the user upload or remove
 * it through a menu on the avatar, or drop an image file on it. Uploads are refused with a warning
 * notification, never with an error state, so a rejected file leaves the previous value in place.
 */
public class AvatarUpload extends CustomField<byte[]>
        implements SupportsValueSource<byte[]>, ApplicationContextAware, InitializingBean {

    // Images are stored in the database and loaded with every list that shows them, so phone photos are refused
    public static final int DEFAULT_MAX_FILE_SIZE = 2 * 1024 * 1024;

    private static final String CLASS_NAME = "avatar-upload";
    private static final String UPLOADING_ATTRIBUTE = "uploading";
    private static final String EDITABLE_PLACEHOLDER = "META-INF/resources/images/lucide-user-round-plus.svg";
    private static final String READ_ONLY_PLACEHOLDER = "META-INF/resources/images/lucide-user-round.svg";

    private ApplicationContext applicationContext;
    private Messages messages;
    private Notifications notifications;
    private Resources resources;

    private FieldDelegate<AvatarUpload, byte[], byte[]> fieldDelegate;

    // Read by the upload validator on the request thread
    private volatile int maxFileSize = DEFAULT_MAX_FILE_SIZE;
    private volatile Set<AvatarImageFormat> acceptedFormats =
            Collections.unmodifiableSet(EnumSet.allOf(AvatarImageFormat.class));
    private List<String> acceptedFileTypes = List.of();

    private byte @Nullable [] image;

    private UploadManager uploadManager;
    private UploadButton uploadButton;
    private MenuItem avatarItem;
    private MenuItem uploadItem;
    private MenuItem removeItem;
    private Image imageComponent;
    private Svg editablePlaceholder;
    private Svg readOnlyPlaceholder;
    private Span badge;
    private Span accessibleName;

    public AvatarUpload() {
        super(null, true);
        addClassName(CLASS_NAME);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        messages = applicationContext.getBean(Messages.class);
        notifications = applicationContext.getBean(Notifications.class);
        resources = applicationContext.getBean(Resources.class);

        //noinspection unchecked
        fieldDelegate = applicationContext.getBean(FieldDelegate.class, this);

        initContent();
    }

    private void initContent() {
        uploadManager = new UploadManager(this, createUploadHandler());
        uploadManager.setMaxFiles(1);
        uploadManager.addAllFinishedListener(event -> uploadManager.clearFileList());
        uploadManager.addFileRejectedListener(this::onFileRejected);
        applyUploadRestrictions();

        uploadButton = new UploadButton(uploadManager);
        uploadButton.addClassName(CLASS_NAME + "-button");

        // A menu bar opens its sub-menu below the avatar and from the keyboard; a context menu opens at the pointer
        MenuBar menuBar = new MenuBar();
        menuBar.addClassName(CLASS_NAME + "-menu-bar");
        menuBar.addThemeVariants(MenuBarVariant.TERTIARY);

        avatarItem = menuBar.addItem(createAvatar());

        SubMenu subMenu = avatarItem.getSubMenu();
        uploadItem = subMenu.addItem(getMessage("uploadItem.text"));
        uploadItem.addComponentAsFirst(GravityUiIcon.ARROW_UP_FROM_LINE.create());
        uploadItem.addAttachListener(event -> bindUploadItem());

        removeItem = subMenu.addItem(getMessage("removeItem.text"), event -> handleRemove());
        removeItem.addComponentAsFirst(GravityUiIcon.TRASH_BIN.create());
        removeItem.getElement().getClassList().add(CLASS_NAME + "-remove-item");

        UploadDropZone dropZone = new UploadDropZone(menuBar, uploadManager);
        dropZone.addClassName(CLASS_NAME + "-drop-zone");

        add(dropZone, uploadButton);

        updateState();
    }

    private Component createAvatar() {
        imageComponent = new Image();
        imageComponent.setAlt("");
        imageComponent.addClassName(CLASS_NAME + "-image");

        editablePlaceholder = createPlaceholder(EDITABLE_PLACEHOLDER);
        readOnlyPlaceholder = createPlaceholder(READ_ONLY_PLACEHOLDER);

        badge = new Span(GravityUiIcon.PENCIL.create());
        badge.addClassName(CLASS_NAME + "-badge");

        Div dropHint = new Div(GravityUiIcon.ARROW_UP_FROM_LINE.create(), new Span(getMessage("dropHint.text")));
        dropHint.addClassName(CLASS_NAME + "-drop-hint");

        Div progress = new Div();
        progress.addClassName(CLASS_NAME + "-progress");

        // Names the avatar button for assistive technology; an aria-label on the menu item does not reach it
        accessibleName = new Span(getMessage("menu.ariaLabel"));
        accessibleName.addClassName(CLASS_NAME + "-accessible-name");

        Div avatar = new Div(imageComponent, editablePlaceholder, readOnlyPlaceholder, badge, dropHint, progress,
                accessibleName);
        avatar.addClassName(CLASS_NAME + "-avatar");
        return avatar;
    }

    private Svg createPlaceholder(String resource) {
        Svg placeholder = new Svg();
        placeholder.setSvg(resources.getResourceAsString(resource));
        placeholder.addClassName(CLASS_NAME + "-placeholder");
        return placeholder;
    }

    private InMemoryUploadHandler createUploadHandler() {
        InMemoryUploadHandler handler = applicationContext.getBean(InMemoryUploadHandler.class);
        handler.setUploadSuccessHandler(context -> handleUploadSucceeded(context.data()));
        handler.withValidator(new AvatarUploadValidator(this::getMaxFileSize, this::getAcceptedFormats));
        handler.whenStart(() -> setUploading(true));
        handler.whenComplete(success -> setUploading(false));
        handler.addTransferProgressListener(new TransferProgressListener() {
            @Override
            public void onError(TransferContext context, IOException reason) {
                // Progress listeners of the handler already run inside UI.access()
                setUploading(false);
                handleRejection(toRejection(reason));
            }
        });
        return handler;
    }

    private void bindUploadItem() {
        // The open sub-menu treats the synthetic click on the hidden file input as an outside click and
        // cancels it, so the file picker is opened right after the item's click, still within its user
        // activation. Runs on every attach, since Flow can recreate either client element: the button is
        // looked up at click time, and the flag keeps a surviving item element from getting a second listener.
        uploadItem.getElement().executeJs("""
                this.__avatarUploadButton = $0;
                if (!this.__avatarUploadBound) {
                    this.__avatarUploadBound = true;
                    this.addEventListener('click', () => setTimeout(() => this.__avatarUploadButton.openFilePicker()));
                }""", uploadButton.getElement());
    }

    private void onFileRejected(UploadManager.FileRejectedEvent event) {
        AvatarUploadRejection rejection = switch (event.getReason()) {
            case FILE_TOO_LARGE -> AvatarUploadRejection.FILE_TOO_LARGE;
            case INCORRECT_FILE_TYPE -> AvatarUploadRejection.UNSUPPORTED_TYPE;
            case TOO_MANY_FILES -> AvatarUploadRejection.TOO_MANY_FILES;
            case UNKNOWN -> AvatarUploadRejection.FAILED;
        };
        handleRejection(rejection);
    }

    private static AvatarUploadRejection toRejection(IOException reason) {
        if (reason instanceof UploadRejectedException) {
            return Arrays.stream(AvatarUploadRejection.values())
                    .filter(rejection -> rejection.name().equals(reason.getMessage()))
                    .findFirst()
                    .orElse(AvatarUploadRejection.FAILED);
        }
        return AvatarUploadRejection.FAILED;
    }

    void handleUploadSucceeded(byte[] data) {
        image = data;
        // On a read-only field this restores the previous value through setPresentationValue()
        setModelValue(data, true);
        updatePresentation();
    }

    void handleRemove() {
        image = null;
        setModelValue(null, true);
        updatePresentation();
    }

    void handleRejection(AvatarUploadRejection rejection) {
        String text = switch (rejection) {
            case FILE_TOO_LARGE -> messages.formatMessage(AvatarUpload.class, "rejection.fileTooLarge",
                    FileUtils.byteCountToDisplaySize(getMaxFileSize()));
            case UNSUPPORTED_TYPE -> messages.formatMessage(AvatarUpload.class, "rejection.unsupportedType",
                    getAcceptedFormats().stream()
                            .sorted()
                            .map(AvatarImageFormat::getDisplayName)
                            .collect(Collectors.joining(", ")));
            case TOO_MANY_FILES -> getMessage("rejection.tooManyFiles");
            case FAILED -> getMessage("rejection.failed");
        };

        notifications.create(text)
                .withType(Notifications.Type.WARNING)
                .show();
    }

    public int getMaxFileSize() {
        return maxFileSize;
    }

    /**
     * Sets the maximum size of an uploaded file in bytes. Defaults to {@link #DEFAULT_MAX_FILE_SIZE}.
     */
    public void setMaxFileSize(int maxFileSize) {
        if (maxFileSize <= 0) {
            throw new IllegalArgumentException("Maximum file size must be positive: " + maxFileSize);
        }

        this.maxFileSize = maxFileSize;
        applyUploadRestrictions();
    }

    public List<String> getAcceptedFileTypes() {
        return acceptedFileTypes;
    }

    /**
     * Narrows the accepted images to the given MIME types and extensions, such as {@code image/png} or
     * {@code .jpg}. Each must name one of the {@link AvatarImageFormat formats}; no types means all of them.
     */
    public void setAcceptedFileTypes(String... fileTypes) {
        Set<AvatarImageFormat> formats = EnumSet.noneOf(AvatarImageFormat.class);
        for (String fileType : fileTypes) {
            formats.add(AvatarImageFormat.fromFileType(fileType)
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported image type: " + fileType)));
        }

        acceptedFileTypes = List.of(fileTypes);
        acceptedFormats = Collections.unmodifiableSet(
                formats.isEmpty() ? EnumSet.allOf(AvatarImageFormat.class) : formats);
        applyUploadRestrictions();
    }

    Set<AvatarImageFormat> getAcceptedFormats() {
        return acceptedFormats;
    }

    private void applyUploadRestrictions() {
        if (uploadManager == null) {
            return;
        }

        uploadManager.setMaxFileSize(maxFileSize);
        // MIME types only: the server-side check of the upload manager requires a file to match
        // the accepted MIME types and the accepted extensions at once
        uploadManager.setAcceptedMimeTypes(acceptedFormats.stream()
                .map(AvatarImageFormat::getMimeType)
                .toArray(String[]::new));
    }

    @Nullable
    @Override
    public ValueSource<byte[]> getValueSource() {
        return fieldDelegate.getValueSource();
    }

    @Override
    public void setValueSource(@Nullable ValueSource<byte[]> valueSource) {
        fieldDelegate.setValueSource(valueSource);
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        super.setReadOnly(readOnly);
        updateState();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        updateState();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        updateState();
    }

    @Override
    public void onEnabledStateChanged(boolean enabled) {
        super.onEnabledStateChanged(enabled);
        // Also called when an ancestor is disabled or enabled
        updateState();
    }

    boolean isEditable() {
        return !isReadOnly() && isEnabled();
    }

    private void updateState() {
        if (uploadManager == null) {
            return;
        }

        // The manager is the server-side gate of the upload: when disabled, it refuses uploads
        // and disables the drop zone and the button on the client
        uploadManager.setEnabled(isEditable());
        updatePresentation();
    }

    private void updatePresentation() {
        boolean editable = isEditable();
        boolean hasImage = image != null;

        if (hasImage) {
            byte[] content = image;
            String mimeType = AvatarImageFormat.detect(ByteBuffer.wrap(content))
                    .map(AvatarImageFormat::getMimeType)
                    .orElse(null);
            imageComponent.setSrc(DownloadHandler.fromInputStream(event ->
                    new DownloadResponse(new ByteArrayInputStream(content), "avatar", mimeType, content.length)));
        }

        imageComponent.setVisible(hasImage);
        editablePlaceholder.setVisible(!hasImage && editable);
        readOnlyPlaceholder.setVisible(!hasImage && !editable);
        badge.setVisible(editable);
        // A field that cannot be changed offers no control: the avatar is not focusable and opens nothing
        avatarItem.setEnabled(editable);
        accessibleName.setVisible(editable);
        uploadItem.setVisible(editable);
        removeItem.setVisible(editable && hasImage);
    }

    private void setUploading(boolean uploading) {
        getElement().setAttribute(UPLOADING_ATTRIBUTE, uploading);
    }

    @Override
    protected byte @Nullable [] generateModelValue() {
        return image;
    }

    @Override
    protected void setPresentationValue(byte @Nullable [] newPresentationValue) {
        image = newPresentationValue;
        updatePresentation();
    }

    @Override
    protected boolean valueEquals(byte @Nullable [] value1, byte @Nullable [] value2) {
        return Arrays.equals(value1, value2);
    }

    Component getBadge() {
        return badge;
    }

    Component getAccessibleName() {
        return accessibleName;
    }

    MenuItem getAvatarItem() {
        return avatarItem;
    }

    MenuItem getUploadItem() {
        return uploadItem;
    }

    MenuItem getRemoveItem() {
        return removeItem;
    }

    private String getMessage(String key) {
        return messages.getMessage(AvatarUpload.class, key);
    }
}
