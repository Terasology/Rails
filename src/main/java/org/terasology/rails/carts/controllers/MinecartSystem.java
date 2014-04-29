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
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.*;
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

    private MoveDescriptor moveDescriptor;
    private Map<EntityRef, MotionState> moveStates = Maps.newHashMap();
    private final Logger logger = LoggerFactory.getLogger(MinecartSystem.class);

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


    private Vector3f getStartCornerPosition(Vector3f blockPosition, Vector3f offsetPosition, Vector3f direction) {
        Vector3f startPosition = new Vector3f(blockPosition);
        if (Math.signum(offsetPosition.x) == Math.signum(direction.x)) {
            startPosition.x += offsetPosition.x;
        } else {
            startPosition.z += offsetPosition.z;
        }
        return startPosition;
    }

    private MotionState getCurrentState(EntityRef minecart) {
        MotionState motionState;
        if (!moveStates.containsKey(minecart)) {
            motionState = new MotionState();
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
        Vector3f angularFactor = new Vector3f(rigidBody.angularFactor);

        int slopeFactor = 0;
        HitResult hit = null;
        Block currentBlock = null;
        MotionState motionState = getCurrentState(minecart);
        Vector3i blockPosition = null;
        EntityRef blockEntity = null;

        if (minecartComponent.pathDirection != null) {

            Vector3f direction = new Vector3f(minecartComponent.pathDirection);

            if (minecartComponent.pathDirection.x == 0 || minecartComponent.pathDirection.z == 0) {
                direction.x *= Math.signum(velocity.x);
                direction.z *= Math.signum(velocity.z);
            }

            direction.y = 0f;
            hit = physics.rayTrace(position, direction, 1.2f, StandardCollisionGroup.DEFAULT, StandardCollisionGroup.WORLD);

            blockPosition = hit.getBlockPosition();
            if (blockPosition != null) {
                Block possibleSlopeBlock = worldProvider.getBlock(blockPosition);
                EntityRef possibleSlopeBlockEntity = possibleSlopeBlock.getEntity();
                ConnectsToRailsComponent railsComponent = possibleSlopeBlockEntity.getComponent(ConnectsToRailsComponent.class);
                if (railsComponent != null && railsComponent.type == ConnectsToRailsComponent.RAILS.SLOPE) {
                    slopeFactor = 1;
                    motionState.nextBlockIsSlope = true;
                }
            }
        }

        hit = physics.rayTrace(position, new Vector3f(0, -1f, 0), 1.3f, StandardCollisionGroup.DEFAULT, StandardCollisionGroup.WORLD);
        blockPosition = hit.getBlockPosition();

        if (blockPosition != null) {
            currentBlock = worldProvider.getBlock(blockPosition);
            blockEntity = currentBlock.getEntity();
            ConnectsToRailsComponent railsComponent = blockEntity.getComponent(ConnectsToRailsComponent.class);
            if (railsComponent != null && railsComponent.type.equals(ConnectsToRailsComponent.RAILS.SLOPE)) {
                Vector3f distance = new Vector3f(blockPosition.x, blockPosition.y, blockPosition.z);
                distance.sub(motionState.prevBlockPosition);
                if (distance.y < 0) {
                    slopeFactor = -1;
                } else {
                    slopeFactor = 1;
                    motionState.nextBlockIsSlope = false;
                }
            }
            if (isSameBlock(blockPosition, motionState.currentBlockPosition)) {
                if (motionState.currentPositionStatus.equals(MotionState.PositionStatus.ON_THE_PATH)) {
                    motionState.positionCorrected = false;
                    minecart.saveComponent(minecartComponent);

                    float drive = minecartComponent.drive.lengthSquared() / 100;
                    float speed = rigidBody.velocity.lengthSquared();

                    if (speed / drive < 60 || moveDescriptor.isCorner(railsComponent.type) || slopeFactor != 0) {
                        moveDescriptor.calculateDirection(
                                velocity,
                                railsComponent.type,
                                currentBlock.getDirection(),
                                minecartComponent,
                                motionState,
                                slopeFactor
                        );

                        Vector3i prevBlockPosition = new Vector3i(motionState.prevBlockPosition);

                        if (velocity.y > 0 && slopeFactor == 0) {
                            Block prevblock = worldProvider.getBlock(prevBlockPosition);
                            EntityRef prevBlockEntity = prevblock.getEntity();
                            ConnectsToRailsComponent prevBlockRailsComponent = prevBlockEntity.getComponent(ConnectsToRailsComponent.class);

                            if (prevBlockRailsComponent != null && prevBlockRailsComponent.type == ConnectsToRailsComponent.RAILS.SLOPE) {
                                velocity.y *= -1;
                            }
                        }
                        minecart.send(new ChangeVelocityEvent(velocity));
                    }
                }

                return;
            }

            motionState.prevBlockPosition = motionState.currentBlockPosition;
            motionState.currentBlockPosition = blockPosition.toVector3f();
            if (blockEntity != null && blockEntity.hasComponent(ConnectsToRailsComponent.class)) {
                motionState.currentPositionStatus = MotionState.PositionStatus.ON_THE_PATH;

                moveDescriptor.calculateDirection(
                        velocity,
                        railsComponent.type,
                        currentBlock.getDirection(),
                        minecartComponent,
                        motionState,
                        slopeFactor
                );

                if (moveDescriptor.isCorner(railsComponent.type)) {
                    logger.info("side:" + currentBlock.getDirection());
                }

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
                motionState.positionCorrected = false;
                angularFactor.set(0f, 0f, 0f);
            } else {
                minecartComponent.pathDirection.set(1f, 1f, 1f);
                angularFactor.set(minecartComponent.pathDirection);
                motionState.currentPositionStatus = MotionState.PositionStatus.ON_THE_GROUND;
            }
        } else {
            minecartComponent.pathDirection.set(1f, 1f, 1f);
            angularFactor.set(minecartComponent.pathDirection);
            motionState.currentPositionStatus = MotionState.PositionStatus.ON_THE_AIR;
        }

        if (!rigidBody.angularFactor.equals(angularFactor) || !rigidBody.linearFactor.equals(minecartComponent.pathDirection)) {
            rigidBody.angularFactor.set(angularFactor);
            rigidBody.linearFactor.set(minecartComponent.pathDirection);
            rigidBody.linearFactor.absolute();
            minecart.saveComponent(rigidBody);
        }

        minecart.saveComponent(minecartComponent);
    }

    private boolean isSameBlock(Vector3i block, Vector3f anotherBlock) {
        if (block == null || anotherBlock == null) {
            return false;
        }
        return block.x == anotherBlock.x && block.y == anotherBlock.y && block.z == anotherBlock.z;
    }

    @ReceiveEvent(components = {MinecartComponent.class, LocationComponent.class})
    public void correctPositionAndRotation(OnChangedComponent event, EntityRef entity) {
        MinecartComponent minecartComponent = entity.getComponent(MinecartComponent.class);
        LocationComponent location = entity.getComponent(LocationComponent.class);
        MotionState motionState = getCurrentState(entity);
        if (motionState == null || motionState.positionCorrected) {
            return;
        }


        if (minecartComponent.isCreated && motionState.currentPositionStatus == MotionState.PositionStatus.ON_THE_PATH) {
            Block currentBlock = worldProvider.getBlock(motionState.currentBlockPosition);
            EntityRef blockEntity = currentBlock.getEntity();
            ConnectsToRailsComponent railsComponent = blockEntity.getComponent(ConnectsToRailsComponent.class);
            Vector3f position = location.getWorldPosition();
            Side side;
            if (railsComponent.type == ConnectsToRailsComponent.RAILS.INTERSECTION) {
                side = moveDescriptor.correctSide(railsComponent.type, (minecartComponent.pathDirection.x != 0 ? Side.LEFT : Side.FRONT));
            } else {
                side = moveDescriptor.correctSide(railsComponent.type, currentBlock.getDirection());
            }

            Vector3f offsetCornerPoint = moveDescriptor.getRotationOffsetPoint(side);


            motionState.positionCorrected = true;
            position = setPositionOnTheRail(minecartComponent, motionState, position, offsetCornerPoint);

            Vector3f distance = new Vector3f(position);
            distance.sub(motionState.prevPosition);

            Quat4f yawPitch = new Quat4f(0, 0, 0, 1);

            moveDescriptor.getYawOnPath(minecartComponent, side, motionState, distance);
            moveDescriptor.getPitchOnPath(minecartComponent, position, motionState, side, railsComponent.type);

            QuaternionUtil.setEuler(yawPitch, TeraMath.DEG_TO_RAD * minecartComponent.yaw, 0, TeraMath.DEG_TO_RAD * minecartComponent.pitch);

            motionState.prevPosition.set(position);

            location.setWorldPosition(position);
            location.setWorldRotation(yawPitch);

            entity.saveComponent(minecartComponent);
            entity.saveComponent(location);
        }
        RigidBodyComponent rb = entity.getComponent(RigidBodyComponent.class);
        rotateVehicles(rb.velocity, minecartComponent);

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
            float angle = angleSign*velocity.length() + QuaternionUtil.getAngle(locationComponent.getLocalRotation());
            if (angle >= TeraMath.PI * 2 || angle < 0) {
                angle = 0;
            }
            //logger.info("angle " + angle);
            //logger.info("distanceMoved.length() " + distanceMoved.length());
            QuaternionUtil.setRotation(rotate, new Vector3f(1, 0, 0), angle);
            locationComponent.setLocalRotation(rotate);
            vehicle.saveComponent(locationComponent);
        }
    }

    private Vector3f setPositionOnTheRail(MinecartComponent minecartComponent, MotionState motionState, Vector3f position, Vector3f offsetCornerPoint) {
        Vector3f fixedPosition = new Vector3f(position);
        if (minecartComponent.pathDirection.x == 0 || minecartComponent.pathDirection.z == 0) {
            if (minecartComponent.pathDirection.z != 0) {
                fixedPosition.x = motionState.currentBlockPosition.x;
            } else {
                fixedPosition.z = motionState.currentBlockPosition.z;
            }
        } else {
            Vector3f revertDirection = new Vector3f(minecartComponent.pathDirection);
            revertDirection.negate();
            Vector3f startPosition = getStartCornerPosition(motionState.currentBlockPosition, offsetCornerPoint, revertDirection);

            Vector3f lastMinecartDirection = new Vector3f(position);
            lastMinecartDirection.sub(startPosition);

            revertDirection.negate();

            Vector3f newPos = new Vector3f(startPosition);
            float pastLength = lastMinecartDirection.lengthSquared();

            if (pastLength >= 0.707f) {
                pastLength = 0.6f;
            }
            revertDirection.scale(pastLength);
            newPos.add(revertDirection);
            fixedPosition.x = newPos.x;
            fixedPosition.z = newPos.z;
        }

        return fixedPosition;
    }
}
