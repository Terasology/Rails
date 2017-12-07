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
import org.terasology.reflection.MappedContainer;

@MappedContainer
public class CartJointSocket {
    public EntityRef connectingVehicle;
    public CartJointSocketLocation connectingSocketLocation;

    public boolean hasImpulseBeenApplied;

    public CartJointSocket getConnectingSocket() {
        return connectingVehicle.getComponent(CartJointComponent.class).getJointSocketAt(connectingSocketLocation);
    }

    public static CartJointSocket connectToVehicle(EntityRef vehicle,
                                                   CartJointSocketLocation connectingSocketLocation) {
        CartJointSocket jointSocket = new CartJointSocket();

        jointSocket.connectingSocketLocation = connectingSocketLocation;
        jointSocket.connectingVehicle = vehicle;

        return jointSocket;
    }
}
