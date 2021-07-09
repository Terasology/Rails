// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.minecarts.blocks;

import org.terasology.engine.world.block.ForceBlockActive;
import org.terasology.gestalt.entitysystem.component.Component;

@ForceBlockActive
public class RailComponent implements Component<RailComponent> {
    public float frictionCoefficient = 0.01f;

    @Override
    public void copy(RailComponent other) {
        this.frictionCoefficient = other.frictionCoefficient;
    }
}
