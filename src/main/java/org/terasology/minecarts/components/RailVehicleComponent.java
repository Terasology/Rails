// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.minecarts.components;

import org.joml.Vector3f;
import org.terasology.engine.network.Replicate;
import org.terasology.engine.network.ServerEvent;
import org.terasology.gestalt.entitysystem.component.Component;

@ServerEvent(lagCompensate = true)
public class RailVehicleComponent implements Component<RailVehicleComponent> {
    @Replicate
    public Vector3f velocity = new Vector3f();
    @Replicate
    public float backAxisOffset = 0.0f;
    @Replicate
    public float frontAxisOffset = 0.0f;
    @Replicate
    public float lastDetach = 0;

    @Override
    public void copyFrom(RailVehicleComponent other) {
        this.velocity = new Vector3f(other.velocity);
        this.backAxisOffset = other.backAxisOffset;
        this.frontAxisOffset = other.frontAxisOffset;
        this.lastDetach = other.lastDetach;
    }
}
