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

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.math.AABB;
import org.terasology.math.geom.Vector3f;
import org.terasology.reflection.MappedContainer;
import org.terasology.rendering.logic.MeshComponent;

@MappedContainer
public class CartJointSocket {

    public CartJointSocketLocation socketLocation;
    public Vector3f localSocketPoint;
    public EntityRef vehicle;

    public static CartJointSocket createForVehicleAtLocation(EntityRef vehicle,
                                                             CartJointSocketLocation socketLocation) {
        CartJointSocket jointSocket = new CartJointSocket();

        AABB aabb = vehicle.getComponent(MeshComponent.class).mesh.getAABB();

        // Get farthest localSocketPoint along socket location direction on AABB
        // TODO: Replace with something better?
        jointSocket.localSocketPoint = aabb.centerPointForNormal(socketLocation.getDirection());

        jointSocket.socketLocation = socketLocation;
        jointSocket.vehicle = vehicle;

        OccupiedCartJointSocketsComponent occupiedCartJointSocketsComponent =
                vehicle.getComponent(OccupiedCartJointSocketsComponent.class);

        occupiedCartJointSocketsComponent.occupySocket(socketLocation);

        return jointSocket;
    }
}
