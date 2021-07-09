// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.minecarts.components;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.network.FieldReplicateType;
import org.terasology.engine.network.Replicate;
import org.terasology.gestalt.entitysystem.component.Component;

/**
 * Created by michaelpollind on 3/31/17.
 */
public class CartRideableComponent implements Component<CartRideableComponent> {
    @Replicate(FieldReplicateType.SERVER_TO_CLIENT)
    public EntityRef rider;

    @Override
    public void copy(CartRideableComponent other) {
        this.rider = other.rider;
    }
}
