// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.minecarts.components;

import org.joml.Vector3f;
import org.terasology.entitySystem.Component;
import org.terasology.network.FieldReplicateType;
import org.terasology.network.Replicate;
import org.terasology.network.ServerEvent;

@ServerEvent(lagCompensate = true)
public class RailVehicleComponent implements Component {
    @Replicate(FieldReplicateType.SERVER_TO_CLIENT)
    public Vector3f velocity = new Vector3f();

    @Replicate(FieldReplicateType.SERVER_TO_CLIENT)
    public float lastDetach = 0;
}
