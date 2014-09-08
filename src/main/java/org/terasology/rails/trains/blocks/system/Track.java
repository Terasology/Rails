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

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

/**
 * Created by adeon on 08.09.14.
 */
public class Track {
    public static enum TrackType {STRIGHT, UP, DOWN, LEFT, RIGHT, CUSTOM};
    private TrackType type;
    private Vector3f position;
    private float yaw;
    private float pitch;

    public Track(TrackType type, Vector3f position, float yaw, float pitch) {
        this.type = type;
        this.position = new Vector3f(position);
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Vector3f getStartPosition() {
        return new Vector3f(
                (float)(Math.cos(TeraMath.DEG_TO_RAD * yaw ) * Math.cos(TeraMath.DEG_TO_RAD * pitch) * Config.TRACK_LENGTH / 2),
                (float)(Math.sin(TeraMath.DEG_TO_RAD * yaw) * Math.cos(TeraMath.DEG_TO_RAD * pitch) * Config.TRACK_LENGTH / 2),
                (float)(Math.sin(TeraMath.DEG_TO_RAD * pitch ) * Config.TRACK_LENGTH / 2)
        );
    }

    public Vector3f getEndPosition() {
        return new Vector3f(
                position.x + (float)(Math.cos(TeraMath.DEG_TO_RAD * yaw ) * Math.cos(TeraMath.DEG_TO_RAD * pitch) * Config.TRACK_LENGTH / 2),
                position.y + (float)(Math.sin(TeraMath.DEG_TO_RAD * yaw) * Math.cos(TeraMath.DEG_TO_RAD * pitch) * Config.TRACK_LENGTH / 2),
                position.z + (float)(Math.sin(TeraMath.DEG_TO_RAD * pitch ) * Config.TRACK_LENGTH / 2)
        );
    }

    public Vector3f getPosition() {
        return position;
    }

    public TrackType getType() {
        return type;
    }

    public float getYaw() {
        return  yaw;
    }

    public float getPitch() {
        return  yaw;
    }

    public boolean equals(Object obj) {
        try {
            Track track = (Track) obj;
            return position == track.getPosition() && type == track.getType() && yaw == track.getYaw() && pitch == track.getPitch();
        } catch (Exception exception) {
            return false;
        }
    }

}
