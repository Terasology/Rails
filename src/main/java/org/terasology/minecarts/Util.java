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
package org.terasology.minecarts;

import org.terasology.logic.location.LocationComponent;
import org.terasology.math.geom.Vector3f;

public class Util {
    public static void bound(Vector3f v) {
        if (Float.isNaN(v.x) || Float.isInfinite(v.x))
            v.x = 0.0f;
        if (Float.isNaN(v.y) || Float.isInfinite(v.y))
            v.y = 0.0f;
        if (Float.isNaN(v.z) || Float.isInfinite(v.z))
            v.z = 0.0f;
    }

    public static Vector3f localToWorldPosition(Vector3f localPosition, LocationComponent locationComponent) {
        Vector3f worldPosition = new Vector3f(localPosition);

        LocationComponent parentLocation = locationComponent;
        while (parentLocation != null) {
            worldPosition.scale(parentLocation.getLocalScale());
            parentLocation.getLocalRotation().rotate(worldPosition, worldPosition);
            worldPosition.add(parentLocation.getLocalPosition());
            parentLocation = parentLocation.getParent().getComponent(LocationComponent.class);
        }

        return worldPosition;
    }
}
