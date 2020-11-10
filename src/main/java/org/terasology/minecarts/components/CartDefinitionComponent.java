// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.minecarts.components;

import org.terasology.entitySystem.Component;
import org.terasology.network.FieldReplicateType;
import org.terasology.network.Replicate;

/**
 * Created by michaelpollind on 3/31/17.
 */
public class CartDefinitionComponent implements Component {
    @Replicate(FieldReplicateType.SERVER_TO_CLIENT)
    public String prefab;
}
