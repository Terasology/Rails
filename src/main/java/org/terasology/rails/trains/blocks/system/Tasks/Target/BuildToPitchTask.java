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
package org.terasology.rails.trains.blocks.system.Tasks.Target;

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
 * Created by adeon on 09.09.14.
 */
public class BuildToPitchTask implements Task {
    @Override
    public boolean run(CommandHandler commandHandler, List<Track> tracks, Track selectedTrack, List<Integer> chunks, Vector3f position, Orientation orientation) {

        boolean hasTracks = tracks.size() > 0;
        boolean up = false;

        if (hasTracks) {
            Track tTrack = tracks.get(tracks.size() - 1);

            if (tTrack.getPitch() > orientation.pitch) {
                if (tTrack.getPitch() - orientation.pitch < 180) {
                    up = false;
                } else {
                    up = true;
                }
            } else {
                if (orientation.pitch - tTrack.getPitch() < 180) {
                    up = true;
                } else {
                    up = false;
                }
            }

        } else {
            return false;
        }

        if (up) {
            TaskResult result = tryTrackType(commandHandler, tracks, selectedTrack, chunks, position, orientation, TrainRailComponent.TrackType.UP);
        } else {
            TaskResult result = tryTrackType(commandHandler, tracks, selectedTrack, chunks, position, orientation, TrainRailComponent.TrackType.DOWN);
        }

        return true;
    }

    private TaskResult tryTrackType(CommandHandler commandHandler, List<Track> tracks, Track selectedTrack, List<Integer> chunks, Vector3f position, Orientation orientation, TrainRailComponent.TrackType type) {
        ArrayList<Command> commands = new ArrayList<>();
        TaskResult taskResult = null;
        Track lastTrack = tracks.get(tracks.size()-1);
        commands.add(new Command(true, type, position, orientation));

        while( lastTrack.getPitch()!= orientation.pitch) {
            taskResult = commandHandler.run(commands, tracks, chunks, selectedTrack);
            lastTrack = taskResult.track;
            if (!taskResult.success) {
                break;
            }
        }

        return taskResult;
    }
}
