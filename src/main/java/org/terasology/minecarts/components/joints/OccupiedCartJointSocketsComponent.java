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

import java.util.EnumSet;
import java.util.Set;

public class OccupiedCartJointSocketsComponent implements Component {
    public Set<CartJointSocketLocation> occupiedSocketLocations;

    public OccupiedCartJointSocketsComponent() {
        occupiedSocketLocations = EnumSet.noneOf(CartJointSocketLocation.class);
    }

    public boolean areAllSocketsOccupied() {
        return !occupiedSocketLocations.containsAll(EnumSet.allOf(CartJointSocketLocation.class));
    }

    public EnumSet<CartJointSocketLocation> getUnoccupiedSocketLocations() {
        return EnumSet.complementOf(getOccupiedSocketLocations());
    }

    public EnumSet<CartJointSocketLocation> getOccupiedSocketLocations() {
        if (occupiedSocketLocations.isEmpty()) {
            return EnumSet.noneOf(CartJointSocketLocation.class);
        }

        return EnumSet.copyOf(occupiedSocketLocations);
    }

    public void occupySocket(CartJointSocketLocation socketLocation) {
        occupiedSocketLocations.add(socketLocation);
    }

    public void unoccupySocket(CartJointSocketLocation socketLocation) {
        occupiedSocketLocations.remove(socketLocation);
    }
}
