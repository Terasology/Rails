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
import org.terasology.protobuf.EntityData;
import org.terasology.rails.minecarts.blocks.RailBlockTrackSegment;

import java.util.List;

public class RailVehicleComponent implements Component {
    @Replicate
    public Types type = Types.minecart;

    @Replicate
    public List<EntityRef> vehicles = Lists.newArrayList();

    public EntityRef pipe = null;
    public enum Types { locomotive, minecart };

    @Replicate(initialOnly = true)
    public  Vector3f velocity = new Vector3f();

    public EntityRef currentSegment;
    public RailBlockTrackSegment trackSegment;

    @Replicate(initialOnly = true)
    public  float t = 0;

    public EntityRef characterInsideCart;

    public  boolean isCreated;


}
