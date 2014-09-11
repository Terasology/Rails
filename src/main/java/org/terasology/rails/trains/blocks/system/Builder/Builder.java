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

import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.rails.trains.blocks.system.Misc.Orientation;
import org.terasology.rails.trains.blocks.system.Railway;
import org.terasology.rails.trains.blocks.system.Tasks.Standart.*;
import org.terasology.rails.trains.blocks.system.Track;

import javax.vecmath.Vector3f;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by adeon on 09.09.14.
 */
public class Builder {
    private TaskHandler taskHandler;
    private Railway railway;

    public Builder(EntityManager entityManager) {
        railway = new Railway();
        taskHandler = new TaskHandler(railway, entityManager);
    }

    public boolean buildStraight(Vector3f checkedPosition, Track selectedTrack, Orientation orientation) {
        return taskHandler.start(new BuildStraightTask(),selectedTrack, checkedPosition, orientation);
    }

    public boolean buildLeft(Vector3f checkedPosition, Track selectedTrack, Orientation orientation) {
        return taskHandler.start(new BuildLeftTask(),selectedTrack, checkedPosition, orientation);
    }

    public boolean buildRight(Vector3f checkedPosition, Track selectedTrack, Orientation orientation) {
        return taskHandler.start(new BuildRightTask(),selectedTrack, checkedPosition, orientation);
    }

    public boolean buildUp(Vector3f checkedPosition, Track selectedTrack, Orientation orientation) {
        return taskHandler.start(new BuildUpTask(),selectedTrack, checkedPosition, orientation);
    }

    public boolean buildDown(Vector3f checkedPosition, Track selectedTrack, Orientation orientation) {
        return taskHandler.start(new BuildDownTask(),selectedTrack, checkedPosition, orientation);
    }

    public Map<EntityRef, Track> getTracks() {
        return railway.getTracks();
    }
}
