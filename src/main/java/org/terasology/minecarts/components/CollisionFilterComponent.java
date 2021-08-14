// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.minecarts.components;

import com.google.common.collect.Lists;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.network.FieldReplicateType;
import org.terasology.engine.network.Replicate;
import org.terasology.gestalt.entitysystem.component.Component;

import java.util.List;

/**
 * Created by michaelpollind on 7/17/17.
 */
public class CollisionFilterComponent implements Component<CollisionFilterComponent> {
    @Replicate(FieldReplicateType.SERVER_TO_CLIENT)
    public List<EntityRef> filter = Lists.newArrayList();

    @Override
    public void copyFrom(CollisionFilterComponent other) {
        this.filter = Lists.newArrayList(other.filter);
    }
}
