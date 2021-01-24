// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.minecarts.components;

import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityRef;

public class WrenchComponent implements Component {
    public EntityRef lastSelectedCart;
}
