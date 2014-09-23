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
package org.terasology.rails.trains.blocks.system.Builder;

import org.terasology.rails.trains.blocks.components.TrainRailComponent;
import org.terasology.rails.trains.blocks.system.Misc.Orientation;

import javax.vecmath.Vector3f;

/**
 * Created by adeon on 09.09.14.
 */
public class Command {
    public boolean build;
    public boolean newTrack;
    public boolean reverse;
    public TrainRailComponent.TrackType type;
    public Orientation orientation;
    public Vector3f checkedPosition;
    public String chunkKey;

    public Command(boolean build, TrainRailComponent.TrackType type, Vector3f checkedPosition, Orientation orientation, String chunkKey, boolean newTrack, boolean reverse) {
        this.build = build;
        this.type  = type;
        this.orientation = orientation;
        this.checkedPosition = checkedPosition;
        this.newTrack = newTrack;
        this.reverse = reverse;
        this.chunkKey = chunkKey;
    }

    public Command(boolean build, TrainRailComponent.TrackType type, Vector3f checkedPosition, Orientation orientation, String chunkKey) {
        this(build, type, checkedPosition, orientation, chunkKey, false, false);
    }

    public Command(boolean build, TrainRailComponent.TrackType type) {
        this.build = build;
        this.type  = type;
    }
}
