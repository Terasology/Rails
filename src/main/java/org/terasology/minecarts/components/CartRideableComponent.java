// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.minecarts.components;

import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.network.FieldReplicateType;
import org.terasology.network.Replicate;

import java.util.List;

/**
 * Created by michaelpollind on 3/31/17.
 */
public class CartRideableComponent implements Component {
    @Replicate(FieldReplicateType.SERVER_TO_CLIENT)
    public EntityRef rider;

}
