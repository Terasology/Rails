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

import org.terasology.logic.location.LocationComponent;
import org.terasology.math.geom.Vector3f;

import java.util.Arrays;
import java.util.Comparator;

public enum CartJointSocketLocation {
    FRONT(Vector3f.north()),
    REAR(Vector3f.south());

    private Vector3f direction;

    CartJointSocketLocation(Vector3f direction) {
        this.direction = direction;
    }

    /**
     * Returns the {@link CartJointSocketLocation} for an entity that is most oriented towards another entity.
     *
     * @param entityLocation      The {@link LocationComponent} for the entity.
     * @param otherEntityLocation The {@link LocationComponent} for the other entity.
     * @return The {@link CartJointSocketLocation} most oriented towards the other entity.
     */
    public static CartJointSocketLocation getSocketLocationTowards(LocationComponent entityLocation,
                                                                   LocationComponent otherEntityLocation) {
        Vector3f relativePosition = otherEntityLocation.getWorldPosition().sub(entityLocation.getWorldPosition());

        return Arrays.stream(values())
                .max(Comparator.comparing(socketLocation -> {
                    Vector3f socketWorldDirection = entityLocation.getWorldRotation().rotate(socketLocation.direction);
                    return socketWorldDirection.dot(relativePosition);
                }))
                // We should never not have a value in the Optional returned by max
                .orElse(CartJointSocketLocation.FRONT);
    }
}
