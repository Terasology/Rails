// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.minecarts.components;

import org.joml.Vector3f;
import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.network.FieldReplicateType;
import org.terasology.network.Replicate;
import org.terasology.network.ServerEvent;

import java.util.ArrayList;
import java.util.List;

@ServerEvent(lagCompensate = true)
public class RailVehicleComponent implements Component {
    @Replicate
    public Vector3f velocity = new Vector3f();
    @Replicate
    public float backAxisOffset = 0.0f;
    @Replicate
    public float frontAxisOffset = 0.0f;
    @Replicate
    public float lastDetach = 0;
}
