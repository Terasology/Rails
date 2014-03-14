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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.OnChangedComponent;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.*;
import org.terasology.logic.location.LocationComponent;
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

@RegisterSystem(RegisterMode.AUTHORITY)
public class MinecartSystem extends BaseComponentSystem implements UpdateSubscriberSystem {
    @In
    private EntityManager entityManager;

    @In
    private WorldProvider worldProvider;

    @In
    private Physics physics;

    private MoveDescriptor moveDescriptor;

    private final Logger logger = LoggerFactory.getLogger(MinecartSystem.class);

    @Override
    public void initialise() {
        moveDescriptor = new MoveDescriptor();
    }

    @Override
    public void shutdown() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void update(float delta) {
        for (EntityRef minecart : entityManager.getEntitiesWith(MinecartComponent.class)) {

            MinecartComponent minecartComponent = minecart.getComponent(MinecartComponent.class);

            if (minecartComponent.isCreated) {
                LocationComponent location = minecart.getComponent(LocationComponent.class);
                Vector3f minecartWorldPosition = location.getWorldPosition();
                HitResult hit = physics.rayTrace(minecartWorldPosition, new Vector3f(0, -1, 0), 6, StandardCollisionGroup.DEFAULT, StandardCollisionGroup.WORLD);
                RigidBodyComponent rb = minecart.getComponent(RigidBodyComponent.class);
                Vector3i blockPosition = hit.getBlockPosition();
                float angularFactor = 0;
                Block currentBlock = null;

                if (blockPosition != null) {
                    currentBlock = worldProvider.getBlock(blockPosition);
                    EntityRef blockEntity = currentBlock.getEntity();

                    if (blockPosition.toVector3f().equals(minecartComponent.currentBlockPosition)) {
                        if (minecartComponent.currentPositionStatus.equals(MinecartComponent.PositionStatus.ON_THE_PATH)) {
                            minecartComponent.positionCorrected = false;
                            minecart.saveComponent(minecartComponent);
                        }
                        float drive = minecartComponent.drive.lengthSquared()/100;
                        float speed = rb.velocity.lengthSquared();
                        ConnectsToRailsComponent railsComponent = blockEntity.getComponent(ConnectsToRailsComponent.class);
                          if (speed/drive < 60 || ConnectsToRailsComponent.RAILS.valueOf(railsComponent.type).equals(ConnectsToRailsComponent.RAILS.CURVE)) {
                            Vector3f velocity = new Vector3f(rb.velocity);
                            moveDescriptor.calculateDirection(
                                    velocity,
                                    ConnectsToRailsComponent.RAILS.valueOf(railsComponent.type),
                                    currentBlock.getDirection(),
                                    minecartComponent
                            );
                            minecart.send(new ChangeVelocityEvent(velocity));
                        }

                        continue;
                    }

                    if (blockEntity != null && blockEntity.hasComponent(ConnectsToRailsComponent.class)) {
                        Vector3f velocity = new Vector3f(rb.velocity);
                        ConnectsToRailsComponent railsComponent = blockEntity.getComponent(ConnectsToRailsComponent.class);
                        minecartComponent.currentPositionStatus = MinecartComponent.PositionStatus.ON_THE_PATH;
                        minecartComponent.prevBlockPosition = minecartComponent.currentBlockPosition;
                        minecartComponent.currentBlockPosition = blockPosition.toVector3f();
                        moveDescriptor.calculateDirection(
                                velocity,
                                ConnectsToRailsComponent.RAILS.valueOf(railsComponent.type),
                                currentBlock.getDirection(),
                                minecartComponent
                        );

                        minecart.send(new ChangeVelocityEvent(velocity));
                        minecartComponent.positionCorrected = false;
                        angularFactor = 0f;
                    } else {
                        minecartComponent.pathDirection.set(1f, 1f, 1f);
                        minecartComponent.currentPositionStatus = MinecartComponent.PositionStatus.ON_THE_GROUND;
                        angularFactor = 1f;
                        minecart.saveComponent(rb);
                    }
                } else {
                    minecartComponent.pathDirection.set(1f, 1f, 1f);
                    minecartComponent.currentPositionStatus = MinecartComponent.PositionStatus.ON_THE_AIR;
                    angularFactor = 1f;
                    minecart.saveComponent(rb);

                }
                minecartWorldPosition.x *= minecartComponent.pathDirection.x;
                minecartWorldPosition.z *= minecartComponent.pathDirection.z;

                if (rb.angularFactor != angularFactor) {
                    rb.angularFactor = angularFactor;
                    minecart.saveComponent(rb);
                }

                minecart.saveComponent(minecartComponent);
            }
        }
    }

    private void movePosition(MinecartComponent minecartComponent, Vector3f minecartPosition, Vector3f offsetCornerPoint) {
        if (minecartComponent.pathDirection.x == 0 || minecartComponent.pathDirection.z == 0) {
            if (minecartComponent.pathDirection.z != 0) {
                minecartPosition.x = minecartComponent.currentBlockPosition.x;
            } else {
                minecartPosition.z = minecartComponent.currentBlockPosition.z;
            }
        } else {
            Vector3f revertDirection = new Vector3f(minecartComponent.pathDirection);
            revertDirection.negate();
            Vector3f startPosition = getStartCornerPosition(minecartComponent.currentBlockPosition, offsetCornerPoint, revertDirection);

            Vector3f lastMinecartDirection = new Vector3f(minecartPosition);
            lastMinecartDirection.sub(startPosition);

            revertDirection.negate();

            Vector3f newPos = new Vector3f(startPosition);
            float pastLength = lastMinecartDirection.lengthSquared();

            if ( pastLength >= 0.707f ) {
                pastLength = 0.6f;
            }
            revertDirection.scale(pastLength);
            newPos.add(revertDirection);
            minecartPosition.x = newPos.x;
            minecartPosition.z = newPos.z;
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

    @ReceiveEvent(components = {MinecartComponent.class, LocationComponent.class})
    public void correctPositionAndRotation(OnChangedComponent event, EntityRef entity) {
        MinecartComponent minecartComponent = entity.getComponent(MinecartComponent.class);
        LocationComponent location = entity.getComponent(LocationComponent.class);

        if (minecartComponent.positionCorrected) {
            return;
        }

        if (minecartComponent.isCreated && minecartComponent.currentPositionStatus == MinecartComponent.PositionStatus.ON_THE_PATH) {
            Vector3f minecartWorldPosition = location.getWorldPosition();
            Block currentBlock = worldProvider.getBlock(minecartComponent.currentBlockPosition);
            Vector3f offsetCornerPoint = moveDescriptor.getRotationOffsetPoint(currentBlock.getDirection());
            EntityRef blockEntity = currentBlock.getEntity();
            Vector3f movedDistance = null;
            ConnectsToRailsComponent railsComponent = blockEntity.getComponent(ConnectsToRailsComponent.class);

            minecartComponent.positionCorrected = true;

            if (false && minecartComponent.drive.lengthSquared() == 0) {
                logger.info("minecartComponent.currentBlockPosition: " + minecartComponent.currentBlockPosition);
                logger.info("railsComponent.type: " + ConnectsToRailsComponent.RAILS.valueOf(railsComponent.type));
                logger.info("offsetCornerPoint: " + offsetCornerPoint);
                logger.info("direction: " + minecartComponent.pathDirection);
                logger.info("From: " + minecartWorldPosition);
            }
            movePosition(minecartComponent, minecartWorldPosition, offsetCornerPoint);

            if (minecartComponent.pathDirection.x != 0 && minecartComponent.pathDirection.z != 0) {
                movedDistance = new Vector3f(minecartWorldPosition);
                movedDistance.sub(minecartComponent.prevPosition);
            }

            Quat4f yawPitch = new Quat4f(0, 0, 0, 1);
            Side side = moveDescriptor.correctSide(ConnectsToRailsComponent.RAILS.valueOf(railsComponent.type), currentBlock.getDirection());
            moveDescriptor.getYawOnPath(minecartComponent, side, movedDistance);
            moveDescriptor.getPitchOnPath(minecartComponent, ConnectsToRailsComponent.RAILS.valueOf(railsComponent.type));
            QuaternionUtil.setEuler(yawPitch, TeraMath.DEG_TO_RAD * minecartComponent.yaw, TeraMath.DEG_TO_RAD * minecartComponent.pitch, 0);

            location.setWorldRotation(yawPitch);
            location.setWorldPosition(minecartWorldPosition);
            /*if (minecartComponent.characterInsideCart != null) {
                minecartComponent.characterInsideCart.send(new UpdateCameraPositionEvent());
            }                            */
            if (false && minecartComponent.drive.lengthSquared() == 0) {
                logger.info("To: " + minecartWorldPosition);
            }
            minecartComponent.prevPosition.set(minecartWorldPosition);
            entity.saveComponent(minecartComponent);
            entity.saveComponent(location);
        }
    }
}
