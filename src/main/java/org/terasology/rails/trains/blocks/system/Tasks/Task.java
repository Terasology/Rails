/*
 * Copyright 2014 MovingBlocks
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
package org.terasology.rails.trains.blocks.system.Tasks;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.rails.trains.blocks.system.Misc.Orientation;

import javax.vecmath.Vector3f;

/**
 * Created by adeon on 08.09.14.
 */
public interface Task {
    public boolean run(EntityRef selectedTrack, Vector3f position, Orientation orientation, boolean reverse);
}
