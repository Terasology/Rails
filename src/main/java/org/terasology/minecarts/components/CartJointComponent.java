// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.minecarts.components;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.gestalt.entitysystem.component.Component;
import org.terasology.reflection.MappedContainer;

public class CartJointComponent implements Component<CartJointComponent> {
    public CartJointSocket front;
    public CartJointSocket back;

    @Override
    public void copyFrom(CartJointComponent other) {
        this.front = other.front.copy();
        this.back = other.back.copy();
    }

    @MappedContainer
    public static class CartJointSocket {
        public EntityRef entity;
        public boolean isOwning = false;
        public float range = .5f;

        CartJointSocket copy() {
            CartJointSocket newSocket = new CartJointSocket();
            newSocket.entity = this.entity;
            newSocket.isOwning = this.isOwning;
            newSocket.range = this.range;
            return newSocket;
        }
    }

    public CartJointSocket findJoint(EntityRef ref) {
        if (front != null && front.entity == ref) {
            return front;
        }
        if (back != null && back.entity == ref) {
            return back;
        }
        return null;
    }
}
