// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.minecarts.components;

import org.terasology.reflection.MappedContainer;

@MappedContainer
public class WheelDefinition {
    public String prefab;
    public float offset;
    public float voffset;

    public WheelDefinition copy() {
        WheelDefinition newWd = new WheelDefinition();
        newWd.prefab = this.prefab;
        newWd.offset = this.offset;
        newWd.voffset = this.voffset;
        return newWd;
    }
}
