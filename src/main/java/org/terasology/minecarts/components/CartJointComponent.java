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
package org.terasology.minecarts.components;

import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.math.geom.Vector3f;
import org.terasology.reflection.MappedContainer;

public class CartJointComponent implements Component {
    public CartJointSocket front;
    public CartJointSocket back;

    @MappedContainer
    public static class CartJointSocket {
        public EntityRef entity;
        public boolean isOwning = false;
        public float range = .5f;
    }

    public CartJointSocket findJoint(EntityRef ref) {
        if(front != null && front.entity == ref)
            return front;
        if(back != null && back.entity == ref)
            return back;
        return null;
    }
}
