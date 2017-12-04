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

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.math.AABB;
import org.terasology.math.geom.Vector3f;
import org.terasology.physics.Physics;
import org.terasology.physics.StandardCollisionGroup;

import java.util.List;

public class Util {
    public static void bound(Vector3f v) {
        if (Float.isNaN(v.x) || Float.isInfinite(v.x))
            v.x = 0.0f;
        if (Float.isNaN(v.y) || Float.isInfinite(v.y))
            v.y = 0.0f;
        if (Float.isNaN(v.z) || Float.isInfinite(v.z))
            v.z = 0.0f;
    }

    public static List<EntityRef> getEntitiesNearPosition(Physics physics,
                                                          Vector3f position, float maxSearchDistance) {
        return physics.scanArea(
                AABB.createCenterExtent(position,
                        Vector3f.one().mul(maxSearchDistance)),
                StandardCollisionGroup.DEFAULT);
    }
}
