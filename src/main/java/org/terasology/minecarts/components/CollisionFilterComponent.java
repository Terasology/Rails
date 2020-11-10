// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.minecarts.components;

import com.google.common.collect.Lists;
import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.network.FieldReplicateType;
import org.terasology.network.Replicate;

import java.util.List;

/**
 * Created by michaelpollind on 7/17/17.
 */
public class CollisionFilterComponent implements Component {
    @Replicate(FieldReplicateType.SERVER_TO_CLIENT)
    public List<EntityRef> filter = Lists.newArrayList();
}
