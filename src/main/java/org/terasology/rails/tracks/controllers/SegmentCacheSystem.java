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
package org.terasology.rails.tracks.controllers;

import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.rails.tracks.Segment;
import org.terasology.rails.tracks.components.PathComponent;
import org.terasology.registry.Share;

import java.util.HashMap;

/**
 * Created by michaelpollind on 4/3/17.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
@Share(value = SegmentCacheSystem.class)
public class SegmentCacheSystem extends BaseComponentSystem {
    private HashMap<String, Segment> segments = new HashMap<>();

    public Segment getSegment(Prefab prefab) {

        Segment segment = segments.get(prefab.getName());
        if (segment != null)
            return segment;

        PathComponent pathComponent = prefab.getComponent(PathComponent.class);
        if (pathComponent == null)
            return null;

        PathComponent.CubicBezier[] c = new PathComponent.CubicBezier[pathComponent.path.size()];
        pathComponent.path.toArray(c);
        segment = new Segment(c, pathComponent.startingBinormal);
        segments.put(prefab.getName(), segment);

        return segment;
    }

}
