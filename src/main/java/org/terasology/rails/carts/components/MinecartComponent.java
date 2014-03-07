/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.rails.carts.components;


import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.network.Replicate;
import org.terasology.rails.carts.controllers.MoveDescriptor;

import javax.vecmath.Vector3f;
import java.util.ArrayList;
import java.util.List;

public class MinecartComponent implements Component {
    @Replicate
    public boolean isCreated;

    @Replicate
    public static Types type = Types.minecart;

    @Replicate
    public List<EntityRef> vehicles = new ArrayList<EntityRef>();

    @Replicate
    public float angleSign;

    public enum Types { locomotive, minecart };

    @Replicate
    public float pitch;

    @Replicate
    public float yaw;

    @Replicate
    public Vector3f drive;

    @Replicate
    public Vector3f pathDirection;

    @Replicate
    public Vector3f prevPosition;

    @Replicate
    public Vector3f prevBlockPosition;

    @Replicate
    public PositionStatus currentPositionStatus = PositionStatus.ON_THE_AIR;

    public static enum PositionStatus {ON_THE_AIR, ON_THE_GROUND, ON_THE_PATH, ON_THE_LIQUID};
}
