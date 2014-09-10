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
import org.terasology.rails.trains.blocks.system.Tasks.Task;
import org.terasology.rails.trains.blocks.system.Track;

import javax.vecmath.Vector3f;

/**
 * Created by adeon on 09.09.14.
 */
public class TaskHandler {
    private float lastTrackPitch = 0;
    private Railway railway;
    private CommandHandler commandHandler;

    public TaskHandler(Railway railway, EntityManager entityManager) {
        this.railway = railway;
        this.commandHandler = new CommandHandler(entityManager);
    }


    public boolean start(Task task, Vector3f position, Orientation orientation) {
        Track track = null;
        return runTask(task, track, position, orientation);
    }

    private boolean runTask(Task task, Track track, Vector3f position, Orientation orientation) {
        if (railway.getTracks().size() > 0) {
            lastTrackPitch = railway.getTracks().get(railway.getTracks().size()-1).getPitch();
        }

        return task.run(commandHandler, railway.getTracks(), railway.getChunks(), position, orientation);
    }
}
