// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.minecarts.components;

import org.terasology.engine.entitySystem.Component;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.reflection.MappedContainer;

public class CartJointComponent implements Component {
    public CartJointSocket front;
    public CartJointSocket back;

    public CartJointSocket findJoint(EntityRef ref) {
        if (front != null && front.entity == ref)
            return front;
        if (back != null && back.entity == ref)
            return back;
        return null;
    }

    @MappedContainer
    public static class CartJointSocket {
        public EntityRef entity;
        public boolean isOwning = false;
        public float range = .5f;
    }
}
