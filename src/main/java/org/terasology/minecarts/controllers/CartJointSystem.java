// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.minecarts.controllers;

import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.JomlUtil;
import org.terasology.minecarts.Constants;
import org.terasology.minecarts.Util;
import org.terasology.minecarts.components.CartJointComponent;
import org.terasology.minecarts.components.RailVehicleComponent;
import org.terasology.physics.components.RigidBodyComponent;
import org.terasology.registry.In;
import org.terasology.registry.Share;
import org.terasology.segmentedpaths.components.PathFollowerComponent;

@RegisterSystem(RegisterMode.AUTHORITY)
@Share(CartJointSystem.class)
public class CartJointSystem extends BaseComponentSystem implements  UpdateSubscriberSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(CartJointSystem.class);

    @In
    EntityManager entityManager;

    public boolean joinVehicles(EntityRef entity1, EntityRef entity2) {
        if (entity1.equals(entity2) && !entity1.exists() && !entity2.exists()) {
            return false;
        }
        CartJointComponent cartJointComponent1 = entity1.getComponent(CartJointComponent.class);
        CartJointComponent cartJointComponent2 = entity2.getComponent(CartJointComponent.class);
        if (cartJointComponent1 == null || cartJointComponent2 == null) {
            return false;
        }


        boolean isJoined = false;

        if (cartJointComponent1.back != null && cartJointComponent2.back != null) {
            isJoined = tryJoin(new Vector3f(0, 0, -1), entity1, cartJointComponent1.back, new Vector3f(0, 0, -1), entity2, cartJointComponent2.back);
        }
        if (!isJoined && cartJointComponent1.back != null && cartJointComponent2.front != null) {
            isJoined = tryJoin(new Vector3f(0, 0, -1), entity1, cartJointComponent1.back, new Vector3f(0, 0, 1), entity2, cartJointComponent2.front);
        }
        if (!isJoined && cartJointComponent1.front != null && cartJointComponent2.back != null) {
            isJoined = tryJoin(new Vector3f(0, 0, 1), entity1, cartJointComponent1.front, new Vector3f(0, 0, -1), entity2, cartJointComponent2.back);
        }
        if (!isJoined && cartJointComponent1.front != null && cartJointComponent2.front != null) {
            isJoined = tryJoin(new Vector3f(0, 0, 1), entity1, cartJointComponent1.front, new Vector3f(0, 0, 1), entity2, cartJointComponent2.front);
        }

        if (isJoined) {
            LOGGER.info("Joint created between: " + entity1 + " and " + entity2);
            entity1.saveComponent(cartJointComponent1);
            entity2.saveComponent(cartJointComponent2);
        }

        return isJoined;
    }

    private boolean tryJoin(Vector3f d1, EntityRef e1, CartJointComponent.CartJointSocket j1, Vector3f d2, EntityRef e2, CartJointComponent.CartJointSocket j2) {
        LocationComponent l1 = e1.getComponent(LocationComponent.class);
        LocationComponent l2 = e2.getComponent(LocationComponent.class);

        Vector3f cart1Direction = JomlUtil.from(l1.getWorldRotation()).transform(new Vector3f(d1));
        Vector3f cart2Direction = JomlUtil.from(l2.getWorldRotation()).transform(new Vector3f(d2));
        if (cart1Direction.dot(cart2Direction) < 0) {
            if (l1.getWorldPosition().distanceSquared(l2.getWorldPosition()) < (j1.range + j2.range) * (j1.range + j2.range)) {
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
        if (j1.entity == null || j2.entity == null) {
            return;
        }

        LocationComponent location = j2.entity.getComponent(LocationComponent.class);
        LocationComponent otherLocation = j1.entity.getComponent(LocationComponent.class);

        RailVehicleComponent railVehicle = j2.entity.getComponent(RailVehicleComponent.class);
        RailVehicleComponent otherRailVehicle = j1.entity.getComponent(RailVehicleComponent.class);

        PathFollowerComponent segmentVehicle = j2.entity.getComponent(PathFollowerComponent.class);
        PathFollowerComponent otherSegmentVehicle = j1.entity.getComponent(PathFollowerComponent.class);
        if (segmentVehicle == null || otherSegmentVehicle == null) {
            LOGGER.info("Joint broken between: " + j1.entity + " and " + j2.entity);
            clearJoinSocket(j1);
            clearJoinSocket(j2);
            return;
        }


        RigidBodyComponent rigidBody = j2.entity.getComponent(RigidBodyComponent.class);
        RigidBodyComponent otherRigidBody = j1.entity.getComponent(RigidBodyComponent.class);

        Vector3f normal = new Vector3f(JomlUtil.from(location.getWorldPosition())).sub(JomlUtil.from(otherLocation.getWorldPosition()));
        float distance = normal.length();
        if (distance > Constants.CART_JOINT_BREAK_DISTANCE) {
            clearJoinSocket(j1);
            clearJoinSocket(j2);
            LOGGER.info("Joint broken between: " + j1.entity + " and " + j2.entity);
            return;
        }

        Vector3f projectedNormal = Util.project(segmentVehicle.heading, normal, new Vector3f()).normalize();//segmentVehicle.heading.project(normal).normalize();
        Vector3f otherProjectedNormal = Util.project(otherSegmentVehicle.heading, normal, new Vector3f()).normalize();

        float relVelAlongNormal = otherRailVehicle.velocity.dot(otherProjectedNormal) - railVehicle.velocity.dot(projectedNormal);
        float inverseMassSum = 1 / rigidBody.mass + 1 / otherRigidBody.mass;
        float bias = (Constants.BAUMGARTE_COFF / delta) * ((j1.range + j2.range) - distance);
        float j = -(relVelAlongNormal + bias) / inverseMassSum;


        railVehicle.velocity.sub(projectedNormal.mul(j / rigidBody.mass));
        otherRailVehicle.velocity.add(otherProjectedNormal.mul(j / otherRigidBody.mass));

        if (!railVehicle.velocity.isFinite()) {
            railVehicle.velocity.set(0);
        }

        if (!otherRailVehicle.velocity.isFinite()) {
            otherRailVehicle.velocity.set(0);
        }

        j2.entity.saveComponent(railVehicle);
        j1.entity.saveComponent(otherRailVehicle);
    }
}
