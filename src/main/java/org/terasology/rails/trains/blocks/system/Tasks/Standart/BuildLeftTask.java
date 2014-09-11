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
package org.terasology.rails.trains.blocks.system.Tasks.Standart;

import org.terasology.rails.trains.blocks.components.TrainRailComponent;
import org.terasology.rails.trains.blocks.system.Builder.Command;
import org.terasology.rails.trains.blocks.system.Builder.CommandHandler;
import org.terasology.rails.trains.blocks.system.Builder.TaskResult;
import org.terasology.rails.trains.blocks.system.Misc.Orientation;
import org.terasology.rails.trains.blocks.system.Tasks.Task;
import org.terasology.rails.trains.blocks.system.Track;

import javax.vecmath.Vector3f;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by adeon on 10.09.14.
 */
public class BuildLeftTask implements Task {
    @Override
    public boolean run(CommandHandler commandHandler, List<Track> tracks, List<Integer> chunks, Vector3f position, Orientation orientation, boolean newTrack) {

        ArrayList<Command> commands = new ArrayList<>();
        commands.add(new Command(true, TrainRailComponent.TrackType.LEFT, position, new Orientation(0,0,0)));
        commands.add(new Command(true, TrainRailComponent.TrackType.LEFT, position, new Orientation(0,0,0)));
        commands.add(new Command(true, TrainRailComponent.TrackType.LEFT, position, new Orientation(0,0,0)));

        TaskResult taskResult = commandHandler.run(commands, tracks, chunks);
        return taskResult.success;
    }
}

