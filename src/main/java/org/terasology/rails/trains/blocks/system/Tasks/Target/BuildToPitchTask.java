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

import org.terasology.rails.trains.blocks.system.Misc.Orientation;
import org.terasology.rails.trains.blocks.system.Tasks.Task;
import org.terasology.rails.trains.blocks.system.Track;

import javax.vecmath.Vector3f;
import java.util.List;

/**
 * Created by adeon on 09.09.14.
 */
public class BuildToPitchTask implements Task {
    @Override
    public boolean run(List<Track> tracks, List<Integer> chunks, Vector3f position, Orientation orientation) {

        boolean hasTracks = tracks.size() > 0;
        boolean up = false;

        if (hasTracks) {
            Track lastTrack = tracks.get(tracks.size() - 1);

            if (lastTrack.getPitch() > orientation.pitch) {
                if (lastTrack.getPitch() - orientation.pitch < 180) {
                    up = false;
                } else {
                    up = true;
                }
            } else {
                if (orientation.pitch - lastTrack.getPitch() < 180) {
                    up = true;
                } else {
                    up = false;
                }
            }

        } else {
            up = false;
        }

        if (up) {

        } else {

        }

        return true;
    }

    private boolean tryTrackType(List<Track> tracks, List<Integer> chunks, Track.TrackType type) {

        return true;
    }
}
