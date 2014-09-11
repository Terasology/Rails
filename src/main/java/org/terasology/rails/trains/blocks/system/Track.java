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

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.TeraMath;
import org.terasology.rails.trains.blocks.components.TrainRailComponent;
import org.terasology.rails.trains.blocks.system.Misc.Orientation;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

/**
 * Created by adeon on 08.09.14.
 */
public class Track {
    private TrainRailComponent.TrackType type;
    private Vector3f position;
    private Vector3f startPosition;
    private Vector3f endPosition;
    private Orientation orientation;
    private EntityRef entity;
    private EntityRef prevTrack;
    private EntityRef nextTrack;

    public Track(EntityRef entity) {
        this(entity, false);
    }

    public Track(EntityRef entity, boolean calculateStartAndEndPositions) {
        TrainRailComponent trainRailComponent = entity.getComponent(TrainRailComponent.class);
        LocationComponent locationComponent = entity.getComponent(LocationComponent.class);
        this.type = trainRailComponent.type;
        this.position = locationComponent.getWorldPosition();
        this.orientation = new Orientation(trainRailComponent.yaw, trainRailComponent.pitch, trainRailComponent.roll);
        this.entity = entity;
        this.prevTrack = trainRailComponent.prevTrack;
        this.nextTrack = trainRailComponent.nextTrack;

        if (calculateStartAndEndPositions) {
            calculatePositions();
            trainRailComponent.startPosition = this.startPosition;
            trainRailComponent.endPosition = this.endPosition;
        } else {
            this.startPosition = trainRailComponent.startPosition;
            this.endPosition = trainRailComponent.endPosition;
        }

        trainRailComponent.track = this;
        entity.saveComponent(trainRailComponent);
    }

    public Vector3f getStartPosition() {
        return startPosition;
    }

    public Vector3f getEndPosition() {
        return endPosition;
    }

    private void calculatePositions() {
        startPosition =  new Vector3f(
                (float)(Math.sin(TeraMath.DEG_TO_RAD * orientation.yaw) * Math.cos(TeraMath.DEG_TO_RAD * orientation.pitch) * Config.TRACK_LENGTH / 2),
                (float)(Math.sin(TeraMath.DEG_TO_RAD * orientation.pitch ) * Config.TRACK_LENGTH / 2),
                (float)(Math.cos(TeraMath.DEG_TO_RAD * orientation.yaw ) * Math.cos(TeraMath.DEG_TO_RAD * orientation.pitch) * Config.TRACK_LENGTH / 2)
        );

        endPosition = new Vector3f(
                position.x + (float)(Math.sin(TeraMath.DEG_TO_RAD * orientation.yaw) * Math.cos(TeraMath.DEG_TO_RAD * orientation.pitch) * Config.TRACK_LENGTH / 2),
                position.y + (float)(Math.sin(TeraMath.DEG_TO_RAD * orientation.pitch ) * Config.TRACK_LENGTH / 2),
                position.z + (float)(Math.cos(TeraMath.DEG_TO_RAD * orientation.yaw ) * Math.cos(TeraMath.DEG_TO_RAD * orientation.pitch) * Config.TRACK_LENGTH / 2)
        );
    }

    public void setEntity(EntityRef entity) {
        this.entity = entity;
    }

    public EntityRef getEntity() {
        return entity;
    }

    public void setPrevTrack(EntityRef track) {
        this.prevTrack = track;
    }

    public EntityRef getPrevTrack() {
        return prevTrack;
    }

    public void setNextTrack(EntityRef track) {
        this.nextTrack = track;
    }

    public EntityRef getNextTrack() {
        return  nextTrack;
    }

    public Vector3f getPosition() {
        return position;
    }

    public TrainRailComponent.TrackType getType() {
        return type;
    }

    public float getYaw() {
        return  orientation.yaw;
    }

    public float getPitch() {
        return  orientation.pitch;
    }

    public float getRoll() {
        return  orientation.roll;
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
