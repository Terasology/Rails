// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.minecarts.components;

import org.terasology.engine.entitySystem.Component;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.network.FieldReplicateType;
import org.terasology.engine.network.Replicate;

/**
 * The presence of this component marks the entity as currently riding a cart.
 */
public class RidingCartComponent implements Component {
    @Replicate(FieldReplicateType.SERVER_TO_CLIENT)
    public EntityRef cart;

    //This is required to make reflection stuff work, but shouldn't be used otherwise.
    private RidingCartComponent() {
    }

    public RidingCartComponent(EntityRef cart) {
        this.cart = cart;
    }
}
