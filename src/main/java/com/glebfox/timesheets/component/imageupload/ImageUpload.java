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

package com.glebfox.timesheets.component.imageupload;

import com.glebfox.timesheets.component.icon.GravityUiIcon;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.Svg;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.jmix.core.MessageTools;
import io.jmix.core.Messages;
import io.jmix.core.Resources;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.image.JmixImage;
import io.jmix.flowui.component.upload.FileUploadField;
import io.jmix.flowui.data.EntityValueSource;
import io.jmix.flowui.data.SupportsValueSource;
import io.jmix.flowui.data.ValueSource;
import io.jmix.flowui.data.value.ContainerValueSource;
import io.jmix.flowui.kit.component.button.JmixButton;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class represents a custom image upload component, allowing users to upload image files
 * and display the uploaded image. The component supports integration with data sources
 * and provides an interface for enabling or disabling the upload functionality.
 */
public class ImageUpload extends Composite<VerticalLayout>
        implements SupportsValueSource<byte[]>, HasEnabled {

    private static final String IMAGE_UPLOAD_CLASS_NAME = "image-upload";
    private static final String IMAGE_UPLOAD_PLACEHOLDER_CLASS_NAME = "image-upload-placeholder";
    private static final String IMAGE_HEIGHT = "var(--ts-image-upload-image-height, 10em)";

    private Messages messages;
    private MessageTools messageTools;
    private UiComponents uiComponents;
    private Resources resources;

    private FileUploadField imageUpload;
    private JmixImage<byte[]> image;
    private Svg placeholder;
    private HorizontalLayout uploadWrapper;

    @Autowired
    public void setMessages(Messages messages) {
        this.messages = messages;
    }

    @Autowired
    public void setMessageTools(MessageTools messageTools) {
        this.messageTools = messageTools;
    }

    @Autowired
    public void setUiComponents(UiComponents uiComponents) {
        this.uiComponents = uiComponents;
    }

    @Autowired
    public void setResources(Resources resources) {
        this.resources = resources;
    }

    @Override
    protected VerticalLayout initContent() {
        VerticalLayout imageWrapper = super.initContent();
        imageWrapper.setClassName(IMAGE_UPLOAD_CLASS_NAME);

        //noinspection unchecked
        image = uiComponents.create(JmixImage.class);
        image.setHeight(IMAGE_HEIGHT);
        imageWrapper.add(image);

        placeholder = uiComponents.create(Svg.class);
        String placeholderSvg = resources.getResourceAsString("META-INF/resources/images/user-plus-duotone.svg");
        placeholder.setSvg(placeholderSvg);
        placeholder.setClassName(IMAGE_UPLOAD_PLACEHOLDER_CLASS_NAME);
        placeholder.getElement().getStyle().setHeight(IMAGE_HEIGHT);
        imageWrapper.add(placeholder);

        updateImage(null);

        uploadWrapper = uiComponents.create(HorizontalLayout.class);
        uploadWrapper.setWidthFull();
        imageWrapper.add(uploadWrapper);

        imageUpload = uiComponents.create(FileUploadField.class);
        imageUpload.setUploadIcon(GravityUiIcon.ARROW_UP_FROM_LINE.create());
        uploadWrapper.add(imageUpload);
        uploadWrapper.expand(imageUpload);

        JmixButton uploadClearBtn = uiComponents.create(JmixButton.class);
        uploadClearBtn.setIcon(GravityUiIcon.XMARK.create());
        uploadClearBtn.setTooltipText(messages.getMessage(getClass(), "uploadClearBtn.tooltip"));
        uploadClearBtn.addClickListener(this::onUploadClearBtnClicked);
        uploadWrapper.add(uploadClearBtn);

        return imageWrapper;
    }

    private void onUploadClearBtnClicked(ClickEvent<Button> event) {
        ValueSource<byte[]> valueSource = getValueSource();
        if (valueSource != null) {
            valueSource.setValue(null);
        }
    }

    @Nullable
    @Override
    public ValueSource<byte[]> getValueSource() {
        return imageUpload.getValueSource();
    }

    @Override
    public void setValueSource(@Nullable ValueSource<byte[]> valueSource) {
        imageUpload.setValueSource(valueSource);
        image.setValueSource(valueSource);

        valueSourceChanged(valueSource);
    }

    private void valueSourceChanged(@Nullable ValueSource<byte[]> valueSource) {
        image.setAlt(valueSource instanceof ContainerValueSource
                ? generateAlternateText(((ContainerValueSource<?, ?>) valueSource))
                : null);

        if (valueSource != null) {
            valueSource.addValueChangeListener(event -> updateImage(event.getValue()));
            valueSource.addStateChangeListener(__ -> updateImage(valueSource.getValue()));
            if (valueSource instanceof EntityValueSource<?, ?> entityValueSource) {
                entityValueSource.addInstanceChangeListener(__ -> updateImage(valueSource.getValue()));
            }

            // Since imageUpload is bound to the entity attribute,
            // it has already applied security restriction, so we
            // can just check if this field is visible or in
            // read-only state
            uploadWrapper.setVisible(imageUpload.isVisible() && !imageUpload.isReadOnly());
        } else {
            updateImage(null);
        }
    }

    private String generateAlternateText(ContainerValueSource<?, ?> valueSource) {
        return messageTools.getPropertyCaption(valueSource.getMetaPropertyPath().getMetaProperty());
    }

    private void updateImage(@Nullable byte[] value) {
        image.setVisible(value != null);
        placeholder.setVisible(value == null);
    }
}
