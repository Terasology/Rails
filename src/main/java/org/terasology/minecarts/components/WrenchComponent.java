// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.minecarts.components;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.gestalt.entitysystem.component.Component;

public class WrenchComponent implements Component<WrenchComponent> {
    public EntityRef lastSelectedCart;

    @Override
    public void copyFrom(WrenchComponent other) {
        this.lastSelectedCart = other.lastSelectedCart;
    }
}
