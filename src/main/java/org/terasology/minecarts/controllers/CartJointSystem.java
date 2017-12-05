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
import org.terasology.logic.location.LocationComponent;
import org.terasology.minecarts.Constants;
import org.terasology.minecarts.components.RailVehicleComponent;
import org.terasology.minecarts.components.joints.CartJointComponent;
import org.terasology.minecarts.components.joints.CartJointSocket;
import org.terasology.minecarts.components.joints.CartJointSocketLocation;
import org.terasology.minecarts.components.joints.OccupiedCartJointSocketsComponent;
import org.terasology.registry.In;

@RegisterSystem(RegisterMode.AUTHORITY)
public class CartJointSystem extends BaseComponentSystem implements UpdateSubscriberSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(CartJointSystem.class);

    @In
    EntityManager entityManager;

    @Override
    public void update(float delta) {

    }

    private boolean hasDesiredJointSocketUnoccupied(EntityRef vehicle,
                                                    CartJointSocketLocation desiredSocketLocation) {
        if (!vehicle.hasComponent(OccupiedCartJointSocketsComponent.class)) {
            vehicle.addComponent(new OccupiedCartJointSocketsComponent());
        }

        return vehicle
                .getComponent(OccupiedCartJointSocketsComponent.class)
                .getUnoccupiedSocketLocations()
                .contains(desiredSocketLocation);
    }

    private void createJointEntityFor(EntityRef vehicleA, CartJointSocketLocation socketLocationA, EntityRef vehicleB, CartJointSocketLocation socketLocationB) {
        CartJointSocket socketA = CartJointSocket.createForVehicleAtLocation(vehicleA, socketLocationA);
        CartJointSocket socketB = CartJointSocket.createForVehicleAtLocation(vehicleB, socketLocationB);

        entityManager.create(new CartJointComponent(socketA, socketB));
    }

    public boolean attachVehicles(EntityRef vehicleA, EntityRef vehicleB) {
        LocationComponent vehicleLocationA = vehicleA.getComponent(LocationComponent.class);
        LocationComponent vehicleLocationB = vehicleB.getComponent(LocationComponent.class);

        CartJointSocketLocation socketLocationA = CartJointSocketLocation.getSocketLocationTowards(
                vehicleLocationA,
                vehicleLocationB
        );

        CartJointSocketLocation socketLocationB = CartJointSocketLocation.getSocketLocationTowards(
                vehicleLocationB,
                vehicleLocationA
        );


        if (!hasDesiredJointSocketUnoccupied(vehicleA, socketLocationA) ||
                !hasDesiredJointSocketUnoccupied(vehicleB, socketLocationB)) {
            return false;
        }

        createJointEntityFor(vehicleA, socketLocationA, vehicleB, socketLocationB);
        return true;
    }

    public boolean attachVehicleToNearbyVehicle(EntityRef vehicle) {
        EntityRef nearbyVehicle = findNearbyJoinableVehicle(vehicle);

        if (nearbyVehicle.equals(EntityRef.NULL)) {
            return false;
        }

        return attachVehicles(vehicle, nearbyVehicle);
    }

    /**
     * Gets the nearest rail vehicle that can be connected to this one, if any.
     *
     * @param vehicle
     * @return The {@link EntityRef} of the nearest rail vehicle
     */
    private EntityRef findNearbyJoinableVehicle(EntityRef vehicle) {
        LocationComponent locationComponent = vehicle.getComponent(LocationComponent.class);

        // TODO: Find better way, possibly querying the physics engine
        EntityRef closestVehicle = EntityRef.NULL;
        float minSqrDistance = Float.POSITIVE_INFINITY;

        for (EntityRef otherVehicle : entityManager.getEntitiesWith(RailVehicleComponent.class)) {
            if (vehicle.equals(otherVehicle)) {
                continue;
            }

            LocationComponent otherLocationComponent = otherVehicle.getComponent(LocationComponent.class);

            // If we cannot join this vehicle because of unavailable sockets, skip to the next one
            CartJointSocketLocation socketLocation = CartJointSocketLocation.getSocketLocationTowards(
                    locationComponent,
                    otherLocationComponent
            );

            CartJointSocketLocation otherSocketLocation = CartJointSocketLocation.getSocketLocationTowards(
                    otherLocationComponent,
                    locationComponent
            );


            if (!hasDesiredJointSocketUnoccupied(vehicle, socketLocation) ||
                    !hasDesiredJointSocketUnoccupied(otherVehicle, otherSocketLocation)) {
               continue;
            }

            float sqrDistance = otherLocationComponent.getWorldPosition()
                    .distanceSquared(locationComponent.getWorldPosition());

            if (sqrDistance < Constants.MAX_VEHICLE_JOIN_DISTANCE * Constants.MAX_VEHICLE_JOIN_DISTANCE
                    && sqrDistance < minSqrDistance) {
                minSqrDistance = sqrDistance;
                closestVehicle = otherVehicle;
            }
        }

        return closestVehicle;
    }
}
