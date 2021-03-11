// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.minecarts.components;

import org.terasology.engine.entitySystem.Component;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.network.FieldReplicateType;
import org.terasology.engine.network.Replicate;

import java.util.ArrayList;
import java.util.List;

public class CartWheelComponent implements Component {
    public List<EntityRef> targets = new ArrayList<>();
    @Replicate(FieldReplicateType.SERVER_TO_CLIENT)
    public List<WheelDefinition> wheels = new ArrayList<>();
}
