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
import org.terasology.math.AABB;
import org.terasology.math.TeraMath;
import org.terasology.physics.Physics;
import org.terasology.physics.StandardCollisionGroup;
import org.terasology.protobuf.EntityData;
import org.terasology.rails.trains.blocks.components.TrainRailComponent;
import org.terasology.rails.trains.blocks.system.Misc.Orientation;
import org.terasology.rails.trains.blocks.system.RailsSystem;
import org.terasology.rails.trains.blocks.system.Railway;
import org.terasology.registry.CoreRegistry;
import org.terasology.rendering.logic.MeshComponent;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by adeon on 09.09.14.
 */
public class CommandHandler {
    private EntityManager entityManager;
    private final Logger logger = LoggerFactory.getLogger(CommandHandler.class);
    private Physics physics;
    private static CommandHandler instance = null;

    public static CommandHandler getInstance() {
        if (instance == null) {
            instance = new CommandHandler();
        }

        return instance;
    }

    private CommandHandler() {
        this.entityManager = CoreRegistry.get(EntityManager.class);
        this.physics = CoreRegistry.get(Physics.class);
    }

    public TaskResult run(List<Command> commands, EntityRef selectedTrack, boolean ghost) {
        EntityRef track = null;
        for( Command command : commands ) {
            if (command.build) {
                selectedTrack = buildTrack(selectedTrack, command, ghost);
                if (selectedTrack.equals(EntityRef.NULL)) {
                    return new TaskResult(track, false);
                }
                track = selectedTrack;
            } else {
                boolean removeResult = removeChunk(selectedTrack);
                if (!removeResult) {
                    return new TaskResult(null, false);
                }
            }
        }
        return new TaskResult(track, true);
    }

    private EntityRef buildTrack(EntityRef selectedTrack, Command command, boolean ghost) {

        Orientation newOrientation = null;
        Orientation fixOrientation = null;
        Vector3f newPosition;
        Vector3f prevPosition = command.checkedPosition;
        boolean newTrack = false;
        float startYaw = 0;
        float startPitch = 0;
        int revC = 1;


        if (selectedTrack.equals(EntityRef.NULL)) {
            AABB aabb = AABB.createCenterExtent(prevPosition,
                    new Vector3f(1.5f, 0.5f, 1.5f)
            );

            List<EntityRef> aroundList = physics.scanArea(aabb, StandardCollisionGroup.WORLD, StandardCollisionGroup.KINEMATIC);
            if (!aroundList.isEmpty()) {
                for (EntityRef checkEntity : aroundList) {
                    if (checkEntity.hasComponent(TrainRailComponent.class)) {
                        LocationComponent location = checkEntity.getComponent(LocationComponent.class);
                        TrainRailComponent trainRailComponent = checkEntity.getComponent(TrainRailComponent.class);

                        if (Math.abs(trainRailComponent.yaw - command.orientation.yaw)<=7.5 &&
                                Math.abs(trainRailComponent.pitch - command.orientation.pitch)<=7.5
                                ) {
                            selectedTrack = checkEntity;
                            break;
                        }
                    }
                }
            }
        }

        if (!selectedTrack.equals(EntityRef.NULL)) {
            TrainRailComponent trainRailComponent = selectedTrack.getComponent(TrainRailComponent.class);
            startYaw = trainRailComponent.yaw;
            startPitch = trainRailComponent.pitch;
        } else {
            newTrack = true;
        }

        String prefab = "rails:railBlock";

        switch(command.type) {
            case STRAIGHT:
                newOrientation = new Orientation(startYaw, startPitch, 0);

                if (newTrack) {
                    newOrientation.add(command.orientation);
                }

                if (startPitch != 0) {
                    fixOrientation = new Orientation(270f, 0, 0);
                } else {
                    fixOrientation = new Orientation(90f, 0, 0);
                }
                logger.info("Try to add straight. Pitch is " + startPitch);
                break;
            case UP:
                float pitch = startPitch + RailsSystem.STANDARD_PITCH_ANGLE_CHANGE;

                if (pitch > RailsSystem.STANDARD_ANGLE_CHANGE) {
                    newOrientation = new Orientation(startYaw, RailsSystem.STANDARD_ANGLE_CHANGE, 0);
                } else {
                    newOrientation = new Orientation(startYaw, startPitch + RailsSystem.STANDARD_PITCH_ANGLE_CHANGE, 0);
                }

                fixOrientation = new Orientation(270f, 0, 0);
                prefab = "rails:railBlock-up";
                break;
            case DOWN:
                newOrientation = new Orientation(startYaw, startPitch - RailsSystem.STANDARD_PITCH_ANGLE_CHANGE, 0);
                fixOrientation = new Orientation(270f, 0, 0);
                prefab = "rails:railBlock-down";
                break;
            case LEFT:
                newOrientation = new Orientation(startYaw + RailsSystem.STANDARD_ANGLE_CHANGE, startPitch, 0);
                fixOrientation = new Orientation(90f, 0, 0);
                prefab = "rails:railBlock-left";
                break;
            case RIGHT:
                newOrientation = new Orientation(startYaw - RailsSystem.STANDARD_ANGLE_CHANGE, startPitch, 0);
                logger.info("left -- " + newOrientation.yaw);
                fixOrientation = new Orientation(90f, 0, 0);
                prefab = "rails:railBlock-right";
                break;
            case CUSTOM:
                newOrientation = new Orientation(command.orientation.yaw, command.orientation.pitch, command.orientation.roll);
                fixOrientation = new Orientation(90f, 0, 0);
                break;
        }

        logger.info(prefab);

        newPosition = new Vector3f(
                prevPosition.x + revC * (float)(Math.sin(TeraMath.DEG_TO_RAD * newOrientation.yaw) * (float) Math.cos(TeraMath.DEG_TO_RAD * newOrientation.pitch) * RailsSystem.TRACK_LENGTH / 2f),
                prevPosition.y + revC * (float)(Math.sin(TeraMath.DEG_TO_RAD * newOrientation.pitch) * RailsSystem.TRACK_LENGTH / 2),
                prevPosition.z + revC * (float)(Math.cos(TeraMath.DEG_TO_RAD * newOrientation.yaw) * (float)Math.cos(TeraMath.DEG_TO_RAD * newOrientation.pitch) * RailsSystem.TRACK_LENGTH / 2f)
        );

        EntityRef track = createEntityInTheWorld(prefab, command.type, command.chunkKey, selectedTrack, newPosition, newOrientation, fixOrientation, ghost);

        return track;
    }

    private boolean removeChunk(EntityRef selectedTrack) {
        //tracks.remove();
        return true;
    }

    private EntityRef createEntityInTheWorld(String prefab, TrainRailComponent.TrackType type, String chunkKey, EntityRef prevTrack,  Vector3f position, Orientation newOrientation, Orientation fixOrientation, boolean ghost) {
        Quat4f yawPitch = new Quat4f(0, 0, 0, 1);
        QuaternionUtil.setEuler(yawPitch, TeraMath.DEG_TO_RAD * (newOrientation.yaw + fixOrientation.yaw), TeraMath.DEG_TO_RAD * (newOrientation.roll + fixOrientation.roll), TeraMath.DEG_TO_RAD * (newOrientation.pitch + fixOrientation.pitch));
        EntityRef railBlock = entityManager.create(prefab, position);

        AABB aabb = AABB.createCenterExtent(position,
                new Vector3f(1.2f, 0.5f, 1.2f)
        );
        List<EntityRef> aroundList = physics.scanArea(aabb, StandardCollisionGroup.WORLD, StandardCollisionGroup.KINEMATIC);
        if (!aroundList.isEmpty()) {
            for (EntityRef checkEntity : aroundList) {
                if (checkEntity.hasComponent(TrainRailComponent.class)) {
                    if (!checkEntity.equals(prevTrack)) {
                        Railway.getInstance().removeChunk(chunkKey);
                        railBlock.destroy();
                        return EntityRef.NULL;
                    }
                }
            }
        } else {
            if (ghost) {

            }
        }

        LocationComponent locationComponent = railBlock.getComponent(LocationComponent.class);
        locationComponent.setWorldRotation(yawPitch);

        TrainRailComponent trainRailComponent = railBlock.getComponent(TrainRailComponent.class);
        trainRailComponent.pitch = newOrientation.pitch;
        trainRailComponent.yaw = newOrientation.yaw;
        trainRailComponent.roll = newOrientation.roll;
        trainRailComponent.type = type;
        trainRailComponent.startPosition = calculateStartPosition(newOrientation);
        trainRailComponent.endPosition = calculateEndPosition(newOrientation, position);
        trainRailComponent.chunkKey = chunkKey;
        Railway.getInstance().getChunk(chunkKey).add(railBlock);

        if (!prevTrack.equals(EntityRef.NULL)) {
            trainRailComponent.prevTrack = prevTrack;
            TrainRailComponent prevTrainRailComponent = prevTrack.getComponent(TrainRailComponent.class);
            prevTrainRailComponent.nextTrack = railBlock;
            prevTrack.saveComponent(prevTrainRailComponent);
        }

        railBlock.saveComponent(locationComponent);
        railBlock.saveComponent(trainRailComponent);
        return railBlock;
    }

    private Vector3f calculateStartPosition(Orientation orientation) {
        return  new Vector3f(
                (float)(Math.sin(TeraMath.DEG_TO_RAD * orientation.yaw) * Math.cos(TeraMath.DEG_TO_RAD * orientation.pitch) * RailsSystem.TRACK_LENGTH / 2),
                (float)(Math.sin(TeraMath.DEG_TO_RAD * orientation.pitch ) * RailsSystem.TRACK_LENGTH / 2),
                (float)(Math.cos(TeraMath.DEG_TO_RAD * orientation.yaw ) * Math.cos(TeraMath.DEG_TO_RAD * orientation.pitch) * RailsSystem.TRACK_LENGTH / 2)
        );

    }

    private Vector3f calculateEndPosition(Orientation orientation, Vector3f position) {
        return new Vector3f(
                position.x + (float)(Math.sin(TeraMath.DEG_TO_RAD * orientation.yaw) * Math.cos(TeraMath.DEG_TO_RAD * orientation.pitch) * RailsSystem.TRACK_LENGTH / 2),
                position.y + (float)(Math.sin(TeraMath.DEG_TO_RAD * orientation.pitch ) * RailsSystem.TRACK_LENGTH / 2),
                position.z + (float)(Math.cos(TeraMath.DEG_TO_RAD * orientation.yaw ) * Math.cos(TeraMath.DEG_TO_RAD * orientation.pitch) * RailsSystem.TRACK_LENGTH / 2)
        );
    }
}
