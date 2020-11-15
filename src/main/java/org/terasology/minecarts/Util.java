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

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.JomlUtil;

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
            worldPosition.mul(parentLocation.getLocalScale());
            parentLocation.getLocalRotation().rotate(JomlUtil.from(worldPosition), JomlUtil.from(worldPosition));
            Vector3f parentLocalPosition = new Vector3f(JomlUtil.from(parentLocation.getLocalPosition()));
            worldPosition.add(parentLocalPosition);
            parentLocation = parentLocation.getParent().getComponent(LocationComponent.class);
        }

        return worldPosition;
    }
  
    public static Vector3f project(Vector3fc u, Vector3fc v, Vector3f dest) {
        return dest.set(v).mul(u.dot(v) / v.lengthSquared());
    }
}
