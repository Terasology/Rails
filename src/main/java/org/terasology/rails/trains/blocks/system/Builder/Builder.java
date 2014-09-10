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
import org.terasology.rails.trains.blocks.system.Misc.Orientation;
import org.terasology.rails.trains.blocks.system.Railway;
import org.terasology.rails.trains.blocks.system.Tasks.Standart.*;

import javax.vecmath.Vector3f;

/**
 * Created by adeon on 09.09.14.
 */
public class Builder {
    private TaskHandler taskHandler;
    private Railway railway;
    private CommandHandler entityManager;

    public Builder(EntityManager entityManager) {
        railway = new Railway();
        taskHandler = new TaskHandler(railway, entityManager);
    }

    public boolean buildStraight(Vector3f checkedPosition) {
        Orientation orientation = new Orientation(0,0,0);
        return taskHandler.start(new BuildStraightTask(), checkedPosition, orientation);
    }

    public boolean buildLeft(Vector3f checkedPosition) {
        Orientation orientation = new Orientation(0,0,0);
        return taskHandler.start(new BuildLeftTask(), checkedPosition, orientation);
    }

    public boolean buildRight(Vector3f checkedPosition) {
        Orientation orientation = new Orientation(0,0,0);
        return taskHandler.start(new BuildRightTask(), checkedPosition, orientation);
    }

    public boolean buildUp(Vector3f checkedPosition) {
        Orientation orientation = new Orientation(0,0,0);
        return taskHandler.start(new BuildUpTask(), checkedPosition, orientation);
    }

    public boolean buildDown(Vector3f checkedPosition) {
        Orientation orientation = new Orientation(0,0,0);
        return taskHandler.start(new BuildDownTask(), checkedPosition, orientation);
    }
}
