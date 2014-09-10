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

import com.bulletphysics.linearmath.QuaternionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.TeraMath;
import org.terasology.rails.trains.blocks.system.Config;
import org.terasology.rails.trains.blocks.system.Misc.Orientation;
import org.terasology.rails.trains.blocks.system.Tasks.Task;
import org.terasology.rails.trains.blocks.system.Track;
import org.terasology.registry.In;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.util.List;

/**
 * Created by adeon on 09.09.14.
 */
public class CommandHandler {
    private EntityManager entityManager;
    private final Logger logger = LoggerFactory.getLogger(CommandHandler.class);

    public CommandHandler(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public TaskResult run(List<Command> commands, List<Track> tracks, List<Integer> chunks) {
        Track track = null;
        for( Command command : commands ) {
            if (command.build) {
                Track tTrack = buildTrack(tracks, command.type, command.checkedPosition, command.orientation);
                if (tTrack == null) {
                    return new TaskResult(track, false);
                }
                track = tTrack;
            } else {
                boolean removeResult = removeTrack(tracks, chunks);
                if (!removeResult) {
                    return new TaskResult(null, false);
                }
            }
        }
        return new TaskResult(track, true);
    }

    private Track buildTrack(List<Track> tracks, Track.TrackType type, Vector3f checkedPosition, Orientation orientation) {

        Orientation newOrientation = null;
        Vector3f newPosition;
        Vector3f prevPosition = checkedPosition;
        float startYaw = 0;
        float startPitch = 0;

        if (!tracks.isEmpty()) {
            Track lastTrack;
            lastTrack = tracks.get(tracks.size() - 1);
            startYaw = lastTrack.getYaw();
            startPitch = lastTrack.getPitch();
            prevPosition = lastTrack.getEndPosition();
        }


        switch(type) {
            case STRAIGHT:
                newOrientation = new Orientation(startYaw, startPitch, 0);
                break;
            case UP:
                newOrientation = new Orientation(startYaw, startPitch + Config.STANDARD_ANGLE_CHANGE, 0);
                break;
            case DOWN:
                newOrientation = new Orientation(startYaw, startPitch - Config.STANDARD_ANGLE_CHANGE, 0);
                break;
            case LEFT:
                newOrientation = new Orientation(startYaw + Config.STANDARD_ANGLE_CHANGE, startPitch, 0);
                break;
            case RIGHT:
                newOrientation = new Orientation(startYaw - Config.STANDARD_ANGLE_CHANGE, startPitch, 0);
                break;
            case CUSTOM:
                newOrientation = new Orientation(orientation.yaw, orientation.pitch, orientation.roll);
                break;
        }

        newPosition = new Vector3f(
                prevPosition.x + (float)(Math.sin(TeraMath.DEG_TO_RAD * newOrientation.yaw) * (float) Math.cos(TeraMath.DEG_TO_RAD * newOrientation.pitch) * Config.TRACK_LENGTH / 2),
                prevPosition.y + (float)(Math.sin(TeraMath.DEG_TO_RAD * newOrientation.pitch) * Config.TRACK_LENGTH / 2),
                prevPosition.z + (float)(Math.cos(TeraMath.DEG_TO_RAD * newOrientation.yaw) * (float)Math.cos(TeraMath.DEG_TO_RAD * newOrientation.pitch) * Config.TRACK_LENGTH / 2)
        );

        Track newTrack = new Track(type, newPosition, newOrientation);

        tracks.add(newTrack);

        addTrackToWorld(newTrack);

        return newTrack;
    }

    private boolean removeTrack(List<Track> tracks, List<Integer> chunks) {
        tracks.remove(tracks.size() - 1);
        int countTracks = ( (Integer) chunks.get( chunks.size() -1 ) ).intValue();
        if (countTracks == 0) {
            chunks.remove(chunks.size() - 1);
        } else {
            chunks.set(chunks.size() - 1, countTracks - 1 );
        }

        return true;
    }

    private void addTrackToWorld(Track track) {
        Quat4f yawPitch = new Quat4f(0, 0, 0, 1);
        QuaternionUtil.setEuler(yawPitch, TeraMath.DEG_TO_RAD * (track.getYaw() + 90), 0, 0);
        EntityRef railBlock = entityManager.create("rails:railBlock-left", track.getPosition());
        LocationComponent locationComponent = railBlock.getComponent(LocationComponent.class);
        locationComponent.setWorldRotation(yawPitch);
        railBlock.saveComponent(locationComponent);
    }
}
