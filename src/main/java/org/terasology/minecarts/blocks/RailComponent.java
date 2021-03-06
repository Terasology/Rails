// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.minecarts.blocks;

import org.terasology.engine.entitySystem.Component;
import org.terasology.engine.world.block.ForceBlockActive;

@ForceBlockActive
public class RailComponent implements Component {
    public float frictionCoefficient = 0.01f;
}
