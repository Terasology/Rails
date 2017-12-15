/*
 * Copyright 2017 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.minecarts.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.behavior.tree.Interpreter;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.geom.Vector3f;
import org.terasology.minecarts.Constants;
import org.terasology.minecarts.Util;
import org.terasology.minecarts.components.CartJointComponent;
import org.terasology.minecarts.components.RailVehicleComponent;
import org.terasology.physics.components.RigidBodyComponent;
import org.terasology.registry.In;
import org.terasology.registry.Share;
import org.terasology.segmentedpaths.components.SegmentEntityComponent;

@RegisterSystem(RegisterMode.AUTHORITY)
@Share(CartJointSystem.class)
public class CartJointSystem extends BaseComponentSystem implements  UpdateSubscriberSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(CartJointSystem.class);

    @In
    EntityManager entityManager;

    public boolean joinVehicles(EntityRef entity1, EntityRef entity2) {
        if (entity1.equals(entity2) && !entity1.exists() && !entity2.exists())
            return false;
        CartJointComponent cartJointComponent1 = entity1.getComponent(CartJointComponent.class);
        CartJointComponent cartJointComponent2 = entity2.getComponent(CartJointComponent.class);
        if (cartJointComponent1 == null || cartJointComponent2 == null)
            return false;


        boolean isJoined = false;

        if (cartJointComponent1.back != null && cartJointComponent2.back != null)
            isJoined = tryJoin(Vector3f.south(), entity1, cartJointComponent1.back, Vector3f.south(), entity2, cartJointComponent2.back);
        if (!isJoined && cartJointComponent1.back != null && cartJointComponent2.front != null)
            isJoined = tryJoin(Vector3f.south(), entity1, cartJointComponent1.back, Vector3f.north(), entity2, cartJointComponent2.front);
        if (!isJoined && cartJointComponent1.front != null && cartJointComponent2.back != null)
            isJoined = tryJoin(Vector3f.north(), entity1, cartJointComponent1.front, Vector3f.south(), entity2, cartJointComponent2.back);
        if (!isJoined && cartJointComponent1.front != null && cartJointComponent2.front != null)
            isJoined = tryJoin(Vector3f.north(), entity1, cartJointComponent1.front, Vector3f.north(), entity2, cartJointComponent2.front);

        if (isJoined) {
            entity1.saveComponent(cartJointComponent1);
            entity2.saveComponent(cartJointComponent2);
        }

        return isJoined;
    }

    private boolean tryJoin(Vector3f d1, EntityRef e1, CartJointComponent.CartJointSocket j1, Vector3f d2, EntityRef e2, CartJointComponent.CartJointSocket j2) {
        LocationComponent l1 = e1.getComponent(LocationComponent.class);
        LocationComponent l2 = e2.getComponent(LocationComponent.class);

        Vector3f cart1Direction = l1.getWorldRotation().rotate(d1);
        Vector3f cart2Direction = l2.getWorldRotation().rotate(d2);
        if (cart1Direction.dot(cart2Direction) < 0) {
            Vector3f cart1Offset = new Vector3f(l1.getWorldPosition()).add(l1.getWorldRotation().rotate(j1.offset));
            Vector3f cart2Offset = new Vector3f(l2.getWorldPosition()).add(l2.getWorldRotation().rotate(j2.offset));
            if (cart1Offset.distanceSquared(cart2Offset) < Constants.CART_JOINT_DISTANCE * Constants.CART_JOINT_DISTANCE) {
                j1.entity = e2;
                j1.isOwning = true;
                j2.entity = e1;
                j2.isOwning = false;
                return true;
            }
        }
        return false;
    }

    @Override
    public void update(float delta) {
        for (EntityRef railVehicle : entityManager.getEntitiesWith(RailVehicleComponent.class, RigidBodyComponent.class, CartJointComponent.class)) {
            CartJointComponent cartJointComponent = railVehicle.getComponent(CartJointComponent.class);
            if (cartJointComponent.front != null && cartJointComponent.front.isOwning) {
                CartJointComponent frontCartJoint = cartJointComponent.front.entity.getComponent(CartJointComponent.class);
                if (frontCartJoint != null) {
                    applyImpulseOnSocket(delta, cartJointComponent.front, frontCartJoint.findJoint(railVehicle));
                }
            }
            if (cartJointComponent.back != null && cartJointComponent.back.isOwning) {
                CartJointComponent backCartJoin = cartJointComponent.back.entity.getComponent(CartJointComponent.class);
                if (backCartJoin != null) {
                    applyImpulseOnSocket(delta, cartJointComponent.back, backCartJoin.findJoint(railVehicle));
                }
            }

        }
    }

    private void clearJoinSocket(CartJointComponent.CartJointSocket jointSocket) {
        jointSocket.entity = null;
        jointSocket.isOwning = false;
    }

    private void applyImpulseOnSocket(float delta, CartJointComponent.CartJointSocket j1, CartJointComponent.CartJointSocket j2) {


        LocationComponent location = j2.entity.getComponent(LocationComponent.class);
        LocationComponent otherLocation = j1.entity.getComponent(LocationComponent.class);

        RailVehicleComponent railVehicle = j2.entity.getComponent(RailVehicleComponent.class);
        RailVehicleComponent otherRailVehicle = j1.entity.getComponent(RailVehicleComponent.class);

        SegmentEntityComponent segmentVehicle = j2.entity.getComponent(SegmentEntityComponent.class);
        SegmentEntityComponent otherSegmentVehicle = j1.entity.getComponent(SegmentEntityComponent.class);
        if (segmentVehicle == null || otherSegmentVehicle == null) {
            clearJoinSocket(j1);
            clearJoinSocket(j2);
            return;
        }


        RigidBodyComponent rigidBody = j2.entity.getComponent(RigidBodyComponent.class);
        RigidBodyComponent otherRigidBody = j1.entity.getComponent(RigidBodyComponent.class);


        Vector3f cart1Offset = new Vector3f(location.getWorldPosition()).add(location.getWorldRotation().rotate(j1.offset));
        Vector3f cart2Offset = new Vector3f(otherLocation.getWorldPosition()).add(otherLocation.getWorldRotation().rotate(j2.offset));

        Vector3f normal = cart2Offset.sub(cart1Offset);
        float distance = normal.length();
        if (distance > Constants.CART_JOINT_BREAK_DISTANCE) {
            clearJoinSocket(j1);
            clearJoinSocket(j2);
            return;
        }

        Vector3f projectedNormal =  segmentVehicle.heading.project(normal).normalize();
        Vector3f otherProjectedNormal = otherSegmentVehicle.heading.project(normal).normalize();

        float relVelAlongNormal = otherRailVehicle.velocity.dot(otherProjectedNormal) - railVehicle.velocity.dot(projectedNormal);
        float inverseMassSum = 1 / rigidBody.mass + 1 / otherRigidBody.mass;
        float bias = (Constants.BAUMGARTE_COFF / delta) * distance;
        float j = -(relVelAlongNormal + bias) / inverseMassSum;


        railVehicle.velocity.sub(new Vector3f(projectedNormal).mul(j / rigidBody.mass));
        otherRailVehicle.velocity.add(new Vector3f(otherProjectedNormal).mul(j / otherRigidBody.mass));

        Util.bound(railVehicle.velocity);
        Util.bound(otherRailVehicle.velocity);
        j2.entity.saveComponent(railVehicle);
        j1.entity.saveComponent(otherRailVehicle);


    }
}
