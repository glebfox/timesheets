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

package com.glebfox.timesheets.component.daterange;

import com.glebfox.timesheets.app.util.DateRange;
import com.google.common.base.Preconditions;
import com.vaadin.flow.shared.Registration;
import io.jmix.core.AccessManager;
import io.jmix.core.entity.EntityValues;
import io.jmix.core.metamodel.model.MetaProperty;
import io.jmix.flowui.accesscontext.UiEntityAttributeContext;
import io.jmix.flowui.data.BindingState;
import io.jmix.flowui.data.ValueSource;
import io.jmix.flowui.kit.event.EventBus;
import io.jmix.flowui.model.InstanceContainer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * The value of a {@link DateRangeField} held in two attributes of an instance container's item, its first and its last
 * day, as Jmix's {@code ContainerValueSource} holds a field's value in one.
 * <p>
 * A last day equal to the first one reads as an empty end, so a one-day range stays one day when its start moves;
 * an empty end is written as the first day. Changes that {@link #setValue} makes to the item fire no value change:
 * the field already shows the range, and reading it back would empty an end that the user entered equal to the start.
 * <p>
 * The framework checks attribute permissions and the required flag only for a value source of one attribute, so this
 * source is read-only when the user may not modify either attribute, and the field takes its required mark from
 * {@link #isStartMandatory()}.
 *
 * @param <E> the type of the container's item
 */
@NullMarked
public class DateRangeValueSource<E> implements ValueSource<DateRange>, ApplicationContextAware {

    private final InstanceContainer<E> container;
    private final MetaProperty startMetaProperty;
    private final MetaProperty endMetaProperty;

    private final EventBus events = new EventBus();

    private @Nullable AccessManager accessManager;

    // Set while setValue writes the item
    private boolean writing;

    /**
     * @param container     the container whose item holds the range
     * @param startProperty the item's attribute of the first day, a {@link LocalDate}
     * @param endProperty   the item's attribute of the last day, a {@link LocalDate}
     */
    public DateRangeValueSource(InstanceContainer<E> container, String startProperty, String endProperty) {
        this.container = Objects.requireNonNull(container);
        this.startMetaProperty = getDateProperty(container, startProperty);
        this.endMetaProperty = getDateProperty(container, endProperty);

        container.addItemChangeListener(this::onItemChange);
        container.addItemPropertyChangeListener(this::onItemPropertyChange);
    }

    private static MetaProperty getDateProperty(InstanceContainer<?> container, String property) {
        MetaProperty metaProperty = container.getEntityMetaClass().getProperty(property);
        Preconditions.checkArgument(LocalDate.class.equals(metaProperty.getJavaType()),
                "Property '%s' of %s is not a LocalDate", property, container.getEntityMetaClass().getName());
        return metaProperty;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        accessManager = applicationContext.getBean(AccessManager.class);
    }

    public InstanceContainer<E> getContainer() {
        return container;
    }

    public String getStartProperty() {
        return startMetaProperty.getName();
    }

    public String getEndProperty() {
        return endMetaProperty.getName();
    }

    /**
     * @return {@code true} if the attribute of the first day is mandatory
     */
    public boolean isStartMandatory() {
        return startMetaProperty.isMandatory();
    }

    @Override
    public @Nullable DateRange getValue() {
        return toRange(container.getItemOrNull());
    }

    @Override
    public void setValue(@Nullable DateRange range) {
        E item = container.getItemOrNull();
        if (item == null) {
            return;
        }

        writing = true;
        try {
            EntityValues.setValue(item, getStartProperty(), range != null ? range.start() : null);
            EntityValues.setValue(item, getEndProperty(), range != null ? range.endOrStart() : null);
        } finally {
            writing = false;
        }
    }

    @Override
    public boolean isReadOnly() {
        return startMetaProperty.isReadOnly()
                || endMetaProperty.isReadOnly()
                || isModifyDenied(startMetaProperty)
                || isModifyDenied(endMetaProperty);
    }

    private boolean isModifyDenied(MetaProperty metaProperty) {
        // Set by the binding before it asks
        AccessManager manager = Objects.requireNonNull(accessManager, "The value source is not bound to a component");

        UiEntityAttributeContext context =
                new UiEntityAttributeContext(container.getEntityMetaClass(), metaProperty.getName());
        manager.applyRegisteredConstraints(context);
        return !context.canModify();
    }

    @Override
    public Class<DateRange> getType() {
        return DateRange.class;
    }

    @Override
    public BindingState getState() {
        return stateOf(container.getItemOrNull());
    }

    @Override
    public Registration addStateChangeListener(Consumer<StateChangeEvent> listener) {
        return events.addListener(StateChangeEvent.class, listener);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Registration addValueChangeListener(Consumer<ValueChangeEvent<DateRange>> listener) {
        return events.addListener(ValueChangeEvent.class, (Consumer) listener);
    }

    private void onItemChange(InstanceContainer.ItemChangeEvent<E> event) {
        BindingState state = stateOf(event.getItem());
        if (state != stateOf(event.getPrevItem())) {
            events.fireEvent(new StateChangeEvent(this, state));
        }

        // The binding listens for an item change only on a value source of one attribute, so it reads the new item's
        // days on this value change
        events.fireEvent(new ValueChangeEvent<>(this, toRange(event.getPrevItem()), toRange(event.getItem())));
    }

    private void onItemPropertyChange(InstanceContainer.ItemPropertyChangeEvent<E> event) {
        if (writing) {
            return;
        }

        E item = event.getItem();
        LocalDate prevValue = (LocalDate) event.getPrevValue();
        DateRange prevRange;
        if (event.getProperty().equals(getStartProperty())) {
            prevRange = toRange(prevValue, EntityValues.getValue(item, getEndProperty()));
        } else if (event.getProperty().equals(getEndProperty())) {
            prevRange = toRange(EntityValues.getValue(item, getStartProperty()), prevValue);
        } else {
            return;
        }

        events.fireEvent(new ValueChangeEvent<>(this, prevRange, toRange(item)));
    }

    private @Nullable DateRange toRange(@Nullable E item) {
        return item != null
                ? toRange(EntityValues.getValue(item, getStartProperty()), EntityValues.getValue(item, getEndProperty()))
                : null;
    }

    private static @Nullable DateRange toRange(@Nullable LocalDate start, @Nullable LocalDate end) {
        return DateRange.of(start, Objects.equals(end, start) ? null : end);
    }

    private static BindingState stateOf(@Nullable Object item) {
        return item != null ? BindingState.ACTIVE : BindingState.INACTIVE;
    }
}
