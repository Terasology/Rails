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
package org.terasology.rails.trains.blocks.components;

import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityRef;

import javax.vecmath.Vector3f;

/**
 * Created by adeon on 22.09.14.
 */
public class TrainComponent implements Component {
    public float maxSpeed = 30f;
    public EntityRef currentRailBlock = EntityRef.NULL;
    public Vector3f targetBlock = new Vector3f();
    public Vector3f velocity = new Vector3f();

}
