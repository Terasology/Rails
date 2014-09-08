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
package org.terasology.rails.trains.blocks.system;

import org.terasology.math.TeraMath;
import org.terasology.rails.trains.blocks.system.Misc.Orientation;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

/**
 * Created by adeon on 08.09.14.
 */
public class Track {
    public static enum TrackType {STRAIGHT, UP, DOWN, LEFT, RIGHT, CUSTOM};
    private TrackType type;
    private Vector3f position;
    private Vector3f startPosition;
    private Vector3f endPosition;
    private Vector3f blockPosition;
    private Orientation orientation;
    private Track prevTrack;
    private Track nextTrack;

    public Track(TrackType type, Vector3f position, Orientation orientation) {
        this.type = type;
        this.position = new Vector3f(position);
        this.orientation = orientation;
        calculatePositions();
    }

    public Vector3f getStartPosition() {
        return startPosition;
    }

    public Vector3f getEndPosition() {
        return endPosition;
    }

    private void calculatePositions() {
        startPosition =  new Vector3f(
                (float)(Math.cos(TeraMath.DEG_TO_RAD * orientation.yaw ) * Math.cos(TeraMath.DEG_TO_RAD * orientation.pitch) * Config.TRACK_LENGTH / 2),
                (float)(Math.sin(TeraMath.DEG_TO_RAD * orientation.yaw) * Math.cos(TeraMath.DEG_TO_RAD * orientation.pitch) * Config.TRACK_LENGTH / 2),
                (float)(Math.sin(TeraMath.DEG_TO_RAD * orientation.pitch ) * Config.TRACK_LENGTH / 2)
        );

        endPosition = new Vector3f(
                position.x + (float)(Math.cos(TeraMath.DEG_TO_RAD * orientation.yaw ) * Math.cos(TeraMath.DEG_TO_RAD * orientation.pitch) * Config.TRACK_LENGTH / 2),
                position.y + (float)(Math.sin(TeraMath.DEG_TO_RAD * orientation.yaw) * Math.cos(TeraMath.DEG_TO_RAD * orientation.pitch) * Config.TRACK_LENGTH / 2),
                position.z + (float)(Math.sin(TeraMath.DEG_TO_RAD * orientation.pitch ) * Config.TRACK_LENGTH / 2)
        );
    }

    public Vector3f getPosition() {
        return position;
    }

    public TrackType getType() {
        return type;
    }

    public float getYaw() {
        return  orientation.yaw;
    }

    public float getPitch() {
        return  orientation.yaw;
    }

    public boolean equals(Object obj) {
        try {
            Track track = (Track) obj;
            return position == track.getPosition() && type == track.getType() && orientation.yaw == track.getYaw() && orientation.pitch == track.getPitch();
        } catch (Exception exception) {
            return false;
        }
    }

}
