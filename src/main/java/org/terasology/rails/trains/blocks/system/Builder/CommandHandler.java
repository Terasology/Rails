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
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.asset.Assets;
import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.health.DoDamageEvent;
import org.terasology.logic.health.EngineDamageTypes;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.AABB;
import org.terasology.math.TeraMath;
import org.terasology.physics.CollisionGroup;
import org.terasology.physics.Physics;
import org.terasology.physics.StandardCollisionGroup;
import org.terasology.physics.components.RigidBodyComponent;
import org.terasology.protobuf.EntityData;
import org.terasology.rails.trains.blocks.components.TrainRailComponent;
import org.terasology.rails.trains.blocks.system.Misc.Orientation;
import org.terasology.rails.trains.blocks.system.RailsSystem;
import org.terasology.rails.trains.blocks.system.Railway;
import org.terasology.registry.CoreRegistry;
import org.terasology.registry.In;
import org.terasology.rendering.logic.MeshComponent;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by adeon on 09.09.14.
 */
public class CommandHandler {
    private EntityManager entityManager;
    private BlockEntityRegistry blockEntityRegistry;
    private Physics physics;
    private final Logger logger = LoggerFactory.getLogger(CommandHandler.class);
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
        this.blockEntityRegistry = CoreRegistry.get(BlockEntityRegistry.class);
    }

    public TaskResult run(List<Command> commands, EntityRef selectedTrack, boolean preview) {
        EntityRef track = null;
        for( Command command : commands ) {
            if (command.build) {
                selectedTrack = buildTrack(selectedTrack, command, preview);
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

    private EntityRef buildTrack(EntityRef selectedTrack, Command command, boolean preview) {

        Orientation newOrientation = null;
        Orientation fixOrientation = null;
        Vector3f newPosition;
        Vector3f prevPosition = command.checkedPosition;
        boolean newTrack = false;
        boolean findedNearestRail = false;
        float startYaw = 0;
        float startPitch = 0;


        if (selectedTrack.equals(EntityRef.NULL)) {
            AABB aabb = AABB.createCenterExtent(prevPosition,
                    new Vector3f(1.5f, 0.5f, 1.5f)
            );

            List<EntityRef> aroundList = physics.scanArea(aabb, StandardCollisionGroup.WORLD, StandardCollisionGroup.KINEMATIC);
            if (!aroundList.isEmpty()) {
                for (EntityRef checkEntity : aroundList) {
                    if (checkEntity.hasComponent(TrainRailComponent.class)) {
                        TrainRailComponent trainRailComponent = checkEntity.getComponent(TrainRailComponent.class);
                        if (Math.abs(trainRailComponent.yaw - command.orientation.yaw)<=7.5 &&
                            Math.abs(trainRailComponent.pitch - command.orientation.pitch)<=7.5 &&
                            trainRailComponent.linkedTracks.size()<2
                           ) {
                            selectedTrack = checkEntity;
                            findedNearestRail = true;
                            break;
                        }
                    }
                }
            }
        }

        if (!selectedTrack.equals(EntityRef.NULL)) {
            TrainRailComponent trainRailComponent = selectedTrack.getComponent(TrainRailComponent.class);
            if (trainRailComponent.chunkKey.equals(command.chunkKey)){
                prevPosition = trainRailComponent.endPosition;
                startYaw = trainRailComponent.yaw;
                startPitch = trainRailComponent.pitch;
            }else{
                if (findedNearestRail) {
                    float firstSide, secondSide;
                    Vector3f diff = new Vector3f(prevPosition);
                    diff.sub(trainRailComponent.startPosition);
                    firstSide = diff.lengthSquared();
                    diff.set(prevPosition);
                    diff.sub(trainRailComponent.endPosition);
                    secondSide = diff.lengthSquared();

                    if (firstSide > secondSide) {
                        prevPosition = trainRailComponent.endPosition;
                    }else{
                        prevPosition = trainRailComponent.startPosition;
                    }
                } else {
                    EntityRef linkedTrack =  trainRailComponent.linkedTracks.get(0);
                    LocationComponent locationComponent = linkedTrack.getComponent(LocationComponent.class);
                    prevPosition = locationComponent.getWorldPosition();

                    float firstSide, secondSide;
                    Vector3f diff = new Vector3f(prevPosition);
                    diff.sub(trainRailComponent.startPosition);
                    firstSide = diff.lengthSquared();
                    diff.set(prevPosition);
                    diff.sub(trainRailComponent.endPosition);
                    secondSide = diff.lengthSquared();

                    if (firstSide < secondSide) {
                        prevPosition = trainRailComponent.endPosition;
                    }else{
                        prevPosition = trainRailComponent.startPosition;
                    }
                }
                newTrack = true;
            }
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

        newPosition = new Vector3f(
                prevPosition.x + (float)(Math.sin(TeraMath.DEG_TO_RAD * newOrientation.yaw) * (float) Math.cos(TeraMath.DEG_TO_RAD * newOrientation.pitch) * RailsSystem.TRACK_LENGTH / 2f),
                prevPosition.y + (float)(Math.sin(TeraMath.DEG_TO_RAD * newOrientation.pitch) * RailsSystem.TRACK_LENGTH / 2),
                prevPosition.z + (float)(Math.cos(TeraMath.DEG_TO_RAD * newOrientation.yaw) * (float)Math.cos(TeraMath.DEG_TO_RAD * newOrientation.pitch) * RailsSystem.TRACK_LENGTH / 2f)
        );

        EntityRef track = createEntityInTheWorld(prefab, command, selectedTrack, newPosition, newOrientation, fixOrientation, preview);

        return track;
    }

    private boolean removeChunk(EntityRef selectedTrack) {
        //tracks.remove();
        return true;
    }

    private EntityRef createEntityInTheWorld(String prefab, Command command, EntityRef prevTrack,  Vector3f position, Orientation newOrientation, Orientation fixOrientation, boolean preview) {
        Quat4f yawPitch = new Quat4f(0, 0, 0, 1);
        QuaternionUtil.setEuler(yawPitch, TeraMath.DEG_TO_RAD * (newOrientation.yaw + fixOrientation.yaw), TeraMath.DEG_TO_RAD * (newOrientation.roll + fixOrientation.roll), TeraMath.DEG_TO_RAD * (newOrientation.pitch + fixOrientation.pitch));
        EntityRef railBlock = entityManager.create(prefab, position);

        AABB aabb = AABB.createCenterExtent(position,
                new Vector3f(1.2f, 0.5f, 1.2f)
        );
        List<EntityRef> aroundList = physics.scanArea(aabb, StandardCollisionGroup.DEFAULT, StandardCollisionGroup.WORLD, StandardCollisionGroup.KINEMATIC);
        if (!aroundList.isEmpty()) {
            if (preview) {
                MeshComponent mesh = railBlock.getComponent(MeshComponent.class);
                RigidBodyComponent rigidBodyComponent = railBlock.getComponent(RigidBodyComponent.class);
                rigidBodyComponent.collisionGroup = StandardCollisionGroup.STATIC;
                rigidBodyComponent.collidesWith = Lists.<CollisionGroup>newArrayList();
                mesh.material = Assets.getMaterial("rails:minecart-unjoin");
                railBlock.saveComponent(mesh);
                railBlock.saveComponent(rigidBodyComponent);
            } else {
                for (EntityRef checkEntity : aroundList) {
                    if (checkEntity.hasComponent(TrainRailComponent.class)) {
                        if (!checkEntity.equals(prevTrack)) {
                                Railway.getInstance().removeChunk(command.chunkKey);
                                railBlock.destroy();
                                return EntityRef.NULL;
                        }
                    }
                }
            }
        } else {
            if (preview) {
                MeshComponent mesh = railBlock.getComponent(MeshComponent.class);
                RigidBodyComponent rigidBodyComponent = railBlock.getComponent(RigidBodyComponent.class);
                rigidBodyComponent.collisionGroup = StandardCollisionGroup.STATIC;
                rigidBodyComponent.collidesWith = Lists.<CollisionGroup>newArrayList();
                mesh.material = Assets.getMaterial("rails:minecart-join");
                railBlock.saveComponent(mesh);
                railBlock.saveComponent(rigidBodyComponent);
            }
        }

        if (!preview) {
            for(int y = 0; y<5; y++) {
                for(int z = -2; z<3; z++) {
                    for(int x = -2; x<3; x++) {
                        EntityRef blockEntity = blockEntityRegistry.getBlockEntityAt(new Vector3f(position.x + x, position.y + y, position.z + z));
                        if (blockEntity!=null) {
                            blockEntity.send(new DoDamageEvent(1000, EngineDamageTypes.EXPLOSIVE.get()));
                        }
                    }
                }
            }
        }

        LocationComponent locationComponent = railBlock.getComponent(LocationComponent.class);
        locationComponent.setWorldRotation(yawPitch);

        TrainRailComponent trainRailComponent = railBlock.getComponent(TrainRailComponent.class);
        trainRailComponent.pitch = newOrientation.pitch;
        trainRailComponent.yaw = newOrientation.yaw;
        trainRailComponent.roll = newOrientation.roll;
        trainRailComponent.type = command.type;
        trainRailComponent.startPosition = calculateStartPosition(newOrientation, position);
        trainRailComponent.endPosition = calculateEndPosition(newOrientation, position);
        trainRailComponent.chunkKey = command.chunkKey;

        Railway.getInstance().getChunk(command.chunkKey).add(railBlock);

        if (!prevTrack.equals(EntityRef.NULL)&&!preview) {
            trainRailComponent.linkedTracks.add(prevTrack);
            TrainRailComponent prevTrainRailComponent = prevTrack.getComponent(TrainRailComponent.class);
            prevTrainRailComponent.linkedTracks.add(railBlock);
            prevTrack.saveComponent(prevTrainRailComponent);
        }

        railBlock.saveComponent(locationComponent);
        railBlock.saveComponent(trainRailComponent);
        return railBlock;
    }

    private Vector3f calculateStartPosition(Orientation orientation, Vector3f position) {
        return  new Vector3f(
                position.x - (float)(Math.sin(TeraMath.DEG_TO_RAD * orientation.yaw) * Math.cos(TeraMath.DEG_TO_RAD * orientation.pitch) * RailsSystem.TRACK_LENGTH / 2),
                position.y - (float)(Math.sin(TeraMath.DEG_TO_RAD * orientation.pitch ) * RailsSystem.TRACK_LENGTH / 2),
                position.z - (float)(Math.cos(TeraMath.DEG_TO_RAD * orientation.yaw ) * Math.cos(TeraMath.DEG_TO_RAD * orientation.pitch) * RailsSystem.TRACK_LENGTH / 2)
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
