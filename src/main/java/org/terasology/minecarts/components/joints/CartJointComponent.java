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
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.geom.Vector3f;
import org.terasology.minecarts.Constants;
import org.terasology.minecarts.components.RailVehicleComponent;
import org.terasology.physics.components.RigidBodyComponent;
import org.terasology.segmentedpaths.components.SegmentEntityComponent;

public class CartJointComponent implements Component {
    public CartJointSocket frontJointSocket;
    public CartJointSocket rearJointSocket;

    private static void applySocketImpulse(CartJointSocket socket, float delta) {
        CartJointSocket otherSocket = socket.getConnectingSocket();

        if (socket.hasImpulseBeenApplied || otherSocket.hasImpulseBeenApplied) {
            return;
        }

        EntityRef vehicle = otherSocket.connectingVehicle;
        EntityRef otherVehicle = socket.connectingVehicle;

        LocationComponent location = vehicle.getComponent(LocationComponent.class);
        LocationComponent otherLocation = otherVehicle.getComponent(LocationComponent.class);

        RailVehicleComponent railVehicle = vehicle.getComponent(RailVehicleComponent.class);
        RailVehicleComponent otherRailVehicle = otherVehicle.getComponent(RailVehicleComponent.class);

        SegmentEntityComponent segmentVehicle = vehicle.getComponent(SegmentEntityComponent.class);
        SegmentEntityComponent otherSegmentVehicle = otherVehicle.getComponent(SegmentEntityComponent.class);

        RigidBodyComponent rigidBody = vehicle.getComponent(RigidBodyComponent.class);
        RigidBodyComponent otherRigidBody = otherVehicle.getComponent(RigidBodyComponent.class);

        Vector3f normal = otherLocation.getWorldPosition().sub(location.getWorldPosition());
        float distance = normal.length();

        float relVelAlongNormal = otherRailVehicle.velocity.dot(otherSegmentVehicle.heading) - railVehicle.velocity.dot(segmentVehicle.heading);
        float inverseMassSum = 1 / rigidBody.mass + 1 / otherRigidBody.mass;
        float bias = (Constants.BAUMGARTE_COFF / delta) * (distance - Constants.JOINT_DISTANCE);
        float j = -(relVelAlongNormal + bias) / inverseMassSum;

        railVehicle.velocity.sub(new Vector3f(segmentVehicle.heading).mul(j / rigidBody.mass));
        otherRailVehicle.velocity.add(new Vector3f(otherSegmentVehicle.heading).mul(j / otherRigidBody.mass));

        vehicle.saveComponent(railVehicle);
        otherVehicle.saveComponent(otherRailVehicle);

        socket.hasImpulseBeenApplied = true;
        otherSocket.hasImpulseBeenApplied = true;
    }

    public CartJointSocket getJointSocketAt(CartJointSocketLocation socketLocation) {
        CartJointSocket socket = frontJointSocket;

        switch (socketLocation) {
            case REAR:
                socket = rearJointSocket;
        }

        return socket;
    }

    public void setJointSocketAt(CartJointSocket socket, CartJointSocketLocation socketLocation) {
        switch (socketLocation) {
            case FRONT:
                frontJointSocket = socket;
                break;
            case REAR:
                rearJointSocket = socket;
                break;
        }
    }

    public void applyImpulse(float delta) {
        if (frontJointSocket != null) {
            applySocketImpulse(frontJointSocket, delta);
        }
        if (rearJointSocket != null) {
            applySocketImpulse(rearJointSocket, delta);
        }
    }
}
