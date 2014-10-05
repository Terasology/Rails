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
import org.terasology.rails.trains.blocks.system.Tasks.Standart.*;
import javax.vecmath.Vector3f;

/**
 * Created by adeon on 09.09.14.
 */
public class Builder {
    private TaskHandler taskHandler;

    public Builder(EntityManager entityManager) {
        taskHandler = new TaskHandler();
    }

    public boolean buildStraight(Vector3f checkedPosition, EntityRef selectedTrack, Orientation orientation, boolean preview) {
        return taskHandler.start(new BuildStraightTask(), selectedTrack, checkedPosition, orientation, preview);
    }

    public boolean buildLeft(Vector3f checkedPosition, EntityRef selectedTrack, Orientation orientation, boolean preview) {
        return taskHandler.start(new BuildLeftTask(),selectedTrack, checkedPosition, orientation, preview);
    }

    public boolean buildRight(Vector3f checkedPosition, EntityRef selectedTrack, Orientation orientation, boolean preview) {
        return taskHandler.start(new BuildRightTask(),selectedTrack, checkedPosition, orientation, preview);
    }

    public boolean buildUp(Vector3f checkedPosition, EntityRef selectedTrack, Orientation orientation, boolean preview) {
        return taskHandler.start(new BuildUpTask(),selectedTrack, checkedPosition, orientation, preview);
    }

    public boolean buildDown(Vector3f checkedPosition, EntityRef selectedTrack, Orientation orientation, boolean preview) {
        return taskHandler.start(new BuildDownTask(),selectedTrack, checkedPosition, orientation, preview);
    }
}
