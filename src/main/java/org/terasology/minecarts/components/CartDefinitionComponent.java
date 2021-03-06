// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.minecarts.components;

import org.terasology.engine.entitySystem.Component;
import org.terasology.engine.network.FieldReplicateType;
import org.terasology.engine.network.Replicate;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by michaelpollind on 3/31/17.
 */
public class CartDefinitionComponent implements Component {
    @Replicate(FieldReplicateType.SERVER_TO_CLIENT)
    public String prefab;
}
