// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.minecarts.components;

import com.google.common.collect.Lists;
import org.terasology.engine.entitySystem.Owns;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.network.FieldReplicateType;
import org.terasology.engine.network.Replicate;
import org.terasology.gestalt.entitysystem.component.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CartWheelComponent implements Component<CartWheelComponent> {
    @Owns
    public List<EntityRef> targets = new ArrayList<>();
    @Replicate(FieldReplicateType.SERVER_TO_CLIENT)
    public List<WheelDefinition> wheels = new ArrayList<>();

    @Override
    public void copyFrom(CartWheelComponent other) {
        this.targets = Lists.newArrayList(other.targets);
        this.wheels = other.wheels.stream()
                .map(WheelDefinition::copy)
                .collect(Collectors.toList());
    }
}
