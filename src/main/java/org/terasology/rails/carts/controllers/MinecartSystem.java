/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.rails.carts.controllers;

import com.bulletphysics.linearmath.QuaternionUtil;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.OnChangedComponent;
import org.terasology.entitySystem.event.EventPriority;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.*;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.location.LocationComponent;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.math.Side;
import org.terasology.math.TeraMath;
import org.terasology.math.Vector3i;
import org.terasology.physics.HitResult;
import org.terasology.physics.Physics;
import org.terasology.physics.StandardCollisionGroup;
import org.terasology.physics.components.RigidBodyComponent;
import org.terasology.physics.events.ChangeVelocityEvent;
import org.terasology.rails.blocks.ConnectsToRailsComponent;
import org.terasology.rails.carts.components.MinecartComponent;
import org.terasology.rails.carts.utils.MinecartHelper;
import org.terasology.registry.In;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.util.Map;

@RegisterSystem(RegisterMode.AUTHORITY)
public class MinecartSystem extends BaseComponentSystem implements UpdateSubscriberSystem {
    @In
    private EntityManager entityManager;
    @In
    private WorldProvider worldProvider;
    @In
    private Physics physics;
    @In
    private LocalPlayer localPlayer;
    @In
    private InventoryManager inventoryManager;

    private MoveDescriptor moveDescriptor;
    private Map<EntityRef, MotionState> moveStates = Maps.newHashMap();
    private final Logger logger = LoggerFactory.getLogger(MinecartSystem.class);
    private static final Vector3f FREE_MOTION   = new Vector3f(1f, 1f, 1f);
    private static final Vector3f LOCKED_MOTION = new Vector3f(0f, 0f, 0f);
    private static final Vector3f UNDER_MINECART_DIRECTION = new Vector3f(0f, -1f, 0f);

    @Override
    public void initialise() {
        moveDescriptor = new MoveDescriptor();
    }

    @Override
    public void update(float delta) {
        for (EntityRef minecart : entityManager.getEntitiesWith(MinecartComponent.class)) {

            MinecartComponent minecartComponent = minecart.getComponent(MinecartComponent.class);

            if (minecartComponent.isCreated) {
                moveMinecart(minecart);
            }
        }
    }

    private MotionState getCurrentState(EntityRef minecart) {
        MotionState motionState;
        if (!moveStates.containsKey(minecart)) {
            motionState = new MotionState();
            motionState.minecartComponent = minecart.getComponent(MinecartComponent.class);
            moveStates.put(minecart, motionState);
        } else {
            motionState = moveStates.get(minecart);
        }
        return motionState;
    }

    private void moveMinecart(EntityRef minecart) {
        LocationComponent location = minecart.getComponent(LocationComponent.class);
        RigidBodyComponent rigidBody = minecart.getComponent(RigidBodyComponent.class);
        MinecartComponent minecartComponent = minecart.getComponent(MinecartComponent.class);

        Vector3f position = location.getWorldPosition();
        Vector3f velocity = new Vector3f(rigidBody.velocity);
        int slopeFactor = 0;
        MotionState motionState = getCurrentState(minecart);
        motionState.angularFactor.set(rigidBody.angularFactor);

        if (!minecartComponent.pathDirection.equals(FREE_MOTION) || !minecartComponent.pathDirection.equals(LOCKED_MOTION)) {
            Vector3f direction = new Vector3f(minecartComponent.pathDirection);
            MinecartHelper.setVectorToDirection(direction, velocity.x, 0, velocity.z);
            BlockInfo blockPossibleSlope = getBlockInDirection(position, direction, 1.2f);
            if (blockPossibleSlope.isRails() && blockPossibleSlope.isSlope()) {
                slopeFactor = 1;
                motionState.nextBlockIsSlope = true;
            }
        }

        BlockInfo currentBlock = getBlockInDirection(position, UNDER_MINECART_DIRECTION, 1.3f);
        if (!currentBlock.isEmptyBlock()) {
            if (slopeFactor == 0 && currentBlock.isRails() && currentBlock.isSlope()) {
                Vector3f distance = new Vector3f(currentBlock.getBlockPosition().x, currentBlock.getBlockPosition().y, currentBlock.getBlockPosition().z);
                distance.sub(motionState.prevBlockPosition);
                if (distance.y < 0) {
                    slopeFactor = -1;
                } else {
                    slopeFactor = 1;
                    motionState.nextBlockIsSlope = false;
                }
            }

            if (currentBlock.isRails()) {
                if (currentBlock.isSameBlock(motionState.currentBlockPosition) && !lowSpeed(minecartComponent.drive, velocity) && slopeFactor == 0) {
                    motionState.setCurrentState(FREE_MOTION, FREE_MOTION, currentBlock.getBlockPosition(), MotionState.PositionStatus.ON_THE_PATH);
                    return;
                }
                motionState.yawSign = 0;
                motionState.setCurrentBlockPosition(currentBlock.getBlockPosition().toVector3f());
                moveDescriptor.calculateDirection(
                        velocity,
                        currentBlock,
                        minecartComponent,
                        motionState,
                        position,
                        slopeFactor
                );
                motionState.setCurrentState(minecartComponent.pathDirection, LOCKED_MOTION, currentBlock.getBlockPosition(), MotionState.PositionStatus.ON_THE_PATH);

                if (motionState.prevBlockPosition.length() > 0) {
                    Vector3i prevBlockPostion = new Vector3i(motionState.prevBlockPosition);

                    if (velocity.y > 0 && slopeFactor < 1) {
                        Block prevblock = worldProvider.getBlock(prevBlockPostion);
                        EntityRef prevBlockEntity = prevblock.getEntity();
                        ConnectsToRailsComponent prevBlockRailsComponent = prevBlockEntity.getComponent(ConnectsToRailsComponent.class);

                        if (prevBlockRailsComponent != null && prevBlockRailsComponent.type == ConnectsToRailsComponent.RAILS.SLOPE) {
                            velocity.y *= -1;
                        }
                    }
                }
                minecart.send(new ChangeVelocityEvent(velocity));
            } else {
                motionState.setCurrentState(FREE_MOTION, FREE_MOTION, currentBlock.getBlockPosition(), MotionState.PositionStatus.ON_THE_GROUND);
            }
        } else {
            motionState.setCurrentState(FREE_MOTION, FREE_MOTION, currentBlock.getBlockPosition(), MotionState.PositionStatus.ON_THE_AIR);
        }

        setAngularAndLinearFactors(minecart, rigidBody, minecartComponent.pathDirection, motionState.angularFactor);
        minecart.saveComponent(minecartComponent);
    }

    @ReceiveEvent(components = {MinecartComponent.class, LocationComponent.class}, priority = EventPriority.PRIORITY_LOW)
    public void correctPositionAndRotation(OnChangedComponent event, EntityRef entity) {
        MinecartComponent minecartComponent = entity.getComponent(MinecartComponent.class);
        LocationComponent location = entity.getComponent(LocationComponent.class);
        MotionState motionState = getCurrentState(entity);
        Vector3f position = location.getWorldPosition();
        RigidBodyComponent rb = entity.getComponent(RigidBodyComponent.class);

        if (motionState == null || position.equals(motionState.prevPosition)) {
            return;
        }

        if (minecartComponent.isCreated && motionState.currentPositionStatus == MotionState.PositionStatus.ON_THE_PATH) {
            Block currentBlock = worldProvider.getBlock(motionState.currentBlockPosition);
            EntityRef blockEntity = currentBlock.getEntity();
            ConnectsToRailsComponent railsComponent = blockEntity.getComponent(ConnectsToRailsComponent.class);
            BlockInfo blockInfo = new BlockInfo(currentBlock, new Vector3i(motionState.currentBlockPosition), blockEntity, railsComponent);

            if (blockInfo.isIntersection()) {
                blockInfo.getBlock().setDirection(minecartComponent.pathDirection.x != 0 ? Side.LEFT : Side.FRONT);
            }

            position = setPositionOnTheRail(minecartComponent.pathDirection, motionState.currentBlockPosition, position);

            Vector3f distance = new Vector3f(position);
            distance.sub(motionState.prevPosition);
            Quat4f yawPitch = new Quat4f(0, 0, 0, 1);
            moveDescriptor.setYawOnPath(minecartComponent, motionState, blockInfo, distance);
            moveDescriptor.setPitchOnPath(minecartComponent, position, motionState, blockInfo);

            QuaternionUtil.setEuler(yawPitch, TeraMath.DEG_TO_RAD * minecartComponent.yaw, TeraMath.DEG_TO_RAD * minecartComponent.pitch, 0);

            motionState.prevPosition.set(position);

            location.setWorldPosition(position);
            location.setWorldRotation(yawPitch);

            entity.saveComponent(minecartComponent);
            entity.saveComponent(location);
        }
        rotateVehicles(rb.velocity, minecartComponent);

    }

    private boolean lowSpeed(Vector3f drive, Vector3f velocity) {
        float driveSpeed = drive.lengthSquared() / 100;
        float velocitySpeed = velocity.lengthSquared();
        return (velocitySpeed / driveSpeed) < 60;
    }

    private BlockInfo getBlockInDirection(Vector3f from, Vector3f to, float length) {
        HitResult hit = physics.rayTrace(from, to, length, StandardCollisionGroup.DEFAULT, StandardCollisionGroup.WORLD);
        Vector3i blockPosition = hit.getBlockPosition();
        Block block = null;
        EntityRef blockEntity = null;
        ConnectsToRailsComponent railsComponent = null;

        if (blockPosition != null) {
            block = worldProvider.getBlock(blockPosition);
            blockEntity = block.getEntity();
            railsComponent = blockEntity.getComponent(ConnectsToRailsComponent.class);
        }

        return new BlockInfo(block, blockPosition, blockEntity, railsComponent);
    }

    private void setAngularAndLinearFactors (EntityRef entity, RigidBodyComponent rigidBodyComponent, Vector3f linearFactor, Vector3f angularFactor) {
        boolean needSave = false;
        if (!linearFactor.equals(rigidBodyComponent.linearFactor)) {
            rigidBodyComponent.linearFactor.set(linearFactor);
            needSave = true;
        }
        if (!angularFactor.equals(rigidBodyComponent.angularFactor)) {
            rigidBodyComponent.angularFactor.set(angularFactor);
            needSave = true;
        }

        if (needSave) {
            entity.saveComponent(rigidBodyComponent);
        }
    }

    private void rotateVehicles(Vector3f velocity, MinecartComponent minecartComponent) {
        velocity.y = 0;
        if (velocity.length() == 0) {
            return;
        }
        for (EntityRef vehicle : minecartComponent.vehicles) {
            LocationComponent locationComponent = vehicle.getComponent(LocationComponent.class);
            if (locationComponent == null) {
                continue;
            }
            Quat4f rotate = new Quat4f(0, 0, 0, 1);
            float angleSign = velocity.x >= 0 && velocity.z >= 0 ? 1 : -1;
            float angle = angleSign*(velocity.length()/MinecartHelper.TWO_PI) + QuaternionUtil.getAngle(locationComponent.getLocalRotation());
            if (angle > MinecartHelper.TWO_PI) {
                angle = 0;
            } else if ( angle < 0 ) {
                angle = MinecartHelper.TWO_PI;
            }

            QuaternionUtil.setRotation(rotate, new Vector3f(1, 0, 0), angle);
            locationComponent.setLocalRotation(rotate);
            vehicle.saveComponent(locationComponent);
        }
    }

    private Vector3f setPositionOnTheRail(Vector3f direction, Vector3f blockPositon, Vector3f position) {
        Vector3f fixedPosition = new Vector3f(position);
        if (direction.x == 0 || direction.z == 0) {
            if (direction.z != 0) {
                fixedPosition.x = blockPositon.x;
            } else {
                fixedPosition.z = blockPositon.z;
            }
        }
        return fixedPosition;
    }
}
