/*
 * Copyright 2015 MovingBlocks
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
package org.terasology.rails.minecarts.components;


import com.google.common.collect.Lists;
import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.math.geom.Vector3f;
import org.terasology.network.Replicate;

import java.util.List;

public class RailVehicleComponent implements Component {
    @Replicate
    public boolean isCreated;

    @Replicate
    public Types type = Types.minecart;

    @Replicate
    public List<EntityRef> vehicles = Lists.newArrayList();

    @Replicate
    public EntityRef pipe = null;

    public enum Types { locomotive, minecart };

    @Replicate
    public EntityRef parentNode;

    @Replicate
    public EntityRef childNode;

    @Replicate
    public EntityRef locomotiveRef;

    @Replicate
    public EntityRef characterInsideCart;

    @Replicate
    public float pitch;

    @Replicate
    public float prevYaw;

    @Replicate
    public float yaw;

    @Replicate
    public float drive;

    @Replicate
    public float changeDriveByStep = 2f;

    @Replicate
    public float maxDrive = 10f;

    @Replicate
    public Vector3f pathDirection;

    @Replicate
    public Vector3f direction;

    @Replicate
    public float needRevertVelocity;
}
