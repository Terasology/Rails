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
package org.terasology.minecarts.components.joints;

import org.terasology.entitySystem.Component;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.geom.Vector3f;
import org.terasology.minecarts.Util;
import org.terasology.minecarts.components.RailVehicleComponent;
import org.terasology.physics.components.RigidBodyComponent;

public class CartJointComponent implements Component {
    // TODO: Make configurable?
    public static final float JOINT_DISTANCE = 0.5f;

    public CartJointSocket jointEndA;
    public CartJointSocket jointEndB;

    public CartJointComponent() {}

    public CartJointComponent(CartJointSocket jointEndA, CartJointSocket jointEndB) {
        this.jointEndA = jointEndA;
        this.jointEndB = jointEndB;
    }

    public void applyImpulse() {
        // Relative velocity along the line joining the two sockets should be zero
        RailVehicleComponent railVehicleA = jointEndA.vehicle.getComponent(RailVehicleComponent.class);
        RailVehicleComponent railVehicleB = jointEndB.vehicle.getComponent(RailVehicleComponent.class);

        RigidBodyComponent rigidBodyA = jointEndA.vehicle.getComponent(RigidBodyComponent.class);
        RigidBodyComponent rigidBodyB = jointEndB.vehicle.getComponent(RigidBodyComponent.class);
        
        LocationComponent locationA = jointEndA.vehicle.getComponent(LocationComponent.class);
        LocationComponent locationB = jointEndB.vehicle.getComponent(LocationComponent.class);

        Vector3f worldSocketPointA = Util.localToWorldPosition(jointEndA.localSocketPoint, locationA);
        Vector3f worldSocketPointB = Util.localToWorldPosition(jointEndB.localSocketPoint, locationB);

        Vector3f normal = worldSocketPointB.sub(worldSocketPointA);
        float distance = normal.length();

        normal.div(distance);

        float relVelAlongNormal = railVehicleB.velocity.dot(normal) - railVehicleA.velocity.dot(normal);
        float inverseMassSum = 1 / rigidBodyA.mass + 1 / rigidBodyB.mass;
        float j = -relVelAlongNormal / inverseMassSum;

        Vector3f impulse = new Vector3f(normal).mul(j);

        railVehicleA.velocity.sub(new Vector3f(impulse).div(rigidBodyA.mass));
        railVehicleB.velocity.add(new Vector3f(impulse).div(rigidBodyB.mass));

        // Positional correction
        Vector3f posCorrection = normal.mul(-(distance - JOINT_DISTANCE) / inverseMassSum);
        Vector3f posA = locationA.getWorldPosition();
        Vector3f posB = locationB.getWorldPosition();

        locationA.setWorldPosition(posA.sub(new Vector3f(posCorrection).div(rigidBodyA.mass)));
        locationB.setWorldPosition(posB.add(new Vector3f(posCorrection).div(rigidBodyB.mass)));

        jointEndA.vehicle.saveComponent(railVehicleA);
        jointEndB.vehicle.saveComponent(railVehicleB);
    }
}
