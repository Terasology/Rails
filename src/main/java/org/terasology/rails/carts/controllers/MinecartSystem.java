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

import com.bulletphysics.linearmath.IntUtil;
import com.bulletphysics.linearmath.QuaternionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.OnChangedComponent;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.*;
import org.terasology.logic.location.LocationComponent;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.math.AABB;
import org.terasology.math.Side;
import org.terasology.math.TeraMath;
import org.terasology.math.Vector3i;
import org.terasology.physics.HitResult;
import org.terasology.physics.Physics;
import org.terasology.physics.StandardCollisionGroup;
import org.terasology.physics.components.RigidBodyComponent;
import org.terasology.physics.events.ChangeVelocityEvent;
import org.terasology.physics.events.ForceEvent;
import org.terasology.rails.blocks.ConnectsToRailsComponent;
import org.terasology.rails.carts.components.MinecartComponent;
import org.terasology.registry.In;
import org.terasology.rendering.logic.MeshComponent;
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

    @In
    private LocalPlayer localPlayer;

    private MoveDescriptor moveDescriptor;

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
                LocationComponent location = minecart.getComponent(LocationComponent.class);
                MeshComponent mesh     = minecart.getComponent(MeshComponent.class);
                RigidBodyComponent rb = minecart.getComponent(RigidBodyComponent.class);

                Vector3f minecartWorldPosition = location.getWorldPosition();
                Vector3f velocity = new Vector3f(rb.velocity);

                float rayLength = 2.3f;

                if (minecartComponent.currentBlockPosition!=null && minecartComponent.prevBlockPosition!=null) {
                    AABB meshAABB = mesh.mesh.getAABB();
                    rayLength = meshAABB.maxY() - meshAABB.minY() / 1.5f;
                    Vector3f meshExtents = meshAABB.getExtents();
                    meshExtents.y = 0;
                    if (meshExtents.x > meshExtents.z) {
                        meshExtents.z = 0;
                        meshExtents.x /= Math.signum(velocity.x)*2.8;
                    } else {
                        meshExtents.x = 0;
                        meshExtents.z /= Math.signum(velocity.z)*2.8;
                    }
                    minecartWorldPosition.add(meshExtents);
                }

                Vector3i blockPosition = new Vector3i(minecartWorldPosition);
                logger.info("blockPosition: " + blockPosition);
                logger.info("minecartWorldPosition: " + minecartWorldPosition);

                //HitResult hit = physics.rayTrace(minecartWorldPosition, new Vector3f(0, -1f, 0), rayLength, StandardCollisionGroup.DEFAULT, StandardCollisionGroup.WORLD);

                // = minecartWorldPosition;
                //IntUtil.floorToInt(rayFromTrans.origin.x + 0.5f);
                Block currentBlock = null;
                Vector3f angularFactor = new Vector3f(rb.angularFactor);
                if (blockPosition != null) {
                    currentBlock = worldProvider.getBlock(blockPosition);
                    EntityRef blockEntity = currentBlock.getEntity();

                    if (blockPosition.toVector3f().equals(minecartComponent.currentBlockPosition)) {
                        if (minecartComponent.currentPositionStatus.equals(MinecartComponent.PositionStatus.ON_THE_PATH)) {
                            minecartComponent.positionCorrected = false;
                            minecart.saveComponent(minecartComponent);
                        }
                        float drive = minecartComponent.drive.lengthSquared() / 100;
                        float speed = rb.velocity.lengthSquared();
                        ConnectsToRailsComponent railsComponent = blockEntity.getComponent(ConnectsToRailsComponent.class);
                        if (speed / drive < 60 || isCorner(railsComponent.type)) {
                            logger.info("-----------------------------------------");
                            logger.info("prevBlockPosition: " + minecartComponent.prevBlockPosition);
                            logger.info("currentBlockPosition: " + minecartComponent.currentBlockPosition);
                            logger.info("-----------------------------------------");
                            moveDescriptor.calculateDirection(
                                    velocity,
                                    railsComponent.type,
                                    currentBlock.getDirection(),
                                    minecartComponent
                            );
                            minecart.send(new ChangeVelocityEvent(velocity));
                        }

                        continue;
                    }

                    if (blockEntity != null && blockEntity.hasComponent(ConnectsToRailsComponent.class)) {
                        ConnectsToRailsComponent railsComponent = blockEntity.getComponent(ConnectsToRailsComponent.class);
                        logger.info("type: " + railsComponent.type);
                        logger.info("prevBlockPosition: " + minecartComponent.prevBlockPosition);
                        logger.info("currentBlockPosition: " + minecartComponent.currentBlockPosition);
                        minecartComponent.currentPositionStatus = MinecartComponent.PositionStatus.ON_THE_PATH;
                        minecartComponent.prevBlockPosition = minecartComponent.currentBlockPosition;
                        minecartComponent.currentBlockPosition = blockPosition.toVector3f();
                        moveDescriptor.calculateDirection(
                                velocity,
                                railsComponent.type,
                                currentBlock.getDirection(),
                                minecartComponent
                        );

                        minecart.send(new ChangeVelocityEvent(velocity));
                        minecartComponent.positionCorrected = false;
                        angularFactor.set(0f, 0f, 0f);
                    } else {
                        minecartComponent.currentBlockPosition = null;
                        minecartComponent.prevBlockPosition = null;
                        minecartComponent.pathDirection.set(1f, 1f, 1f);
                        angularFactor.set(minecartComponent.pathDirection);
                        minecartComponent.currentPositionStatus = MinecartComponent.PositionStatus.ON_THE_GROUND;
                        minecart.saveComponent(rb);
                    }
                } else {
                    minecartComponent.currentBlockPosition = null;
                    minecartComponent.prevBlockPosition = null;
                    minecartComponent.pathDirection.set(1f, 1f, 1f);
                    angularFactor.set(minecartComponent.pathDirection);
                    minecartComponent.currentPositionStatus = MinecartComponent.PositionStatus.ON_THE_AIR;
                    minecart.saveComponent(rb);

                }

                if (!rb.angularFactor.equals(angularFactor) || !rb.linearFactor.equals(minecartComponent.pathDirection)) {
                    rb.angularFactor.set(angularFactor);
                    rb.linearFactor.set(minecartComponent.pathDirection);
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

            if (pastLength >= 0.707f) {
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
            movePosition(minecartComponent, minecartWorldPosition, offsetCornerPoint);

            movedDistance = new Vector3f(minecartWorldPosition);
            movedDistance.sub(minecartComponent.prevPosition);

            Quat4f yawPitch = new Quat4f(0, 0, 0, 1);
            Side side = null;
            if (railsComponent.type == ConnectsToRailsComponent.RAILS.INTERSECTION) {
                side = moveDescriptor.correctSide(railsComponent.type, (minecartComponent.pathDirection.x != 0 ? Side.LEFT : Side.FRONT));
            } else {
                side = moveDescriptor.correctSide(railsComponent.type, currentBlock.getDirection());
            }

            moveDescriptor.getYawOnPath(minecartComponent, side, movedDistance);
            moveDescriptor.getPitchOnPath(minecartComponent, railsComponent.type);
            if (movedDistance.y > 0.0001f) {
                ///minecartComponent.pitch = (float)Math.atan2(movedDistance.y, movedDistance.x != 0?movedDistance.x:movedDistance.z);
                /*if (railsComponent.type == ConnectsToRailsComponent.RAILS.SLOPE) {
                    logger.info("movedDistance length: " + movedDistance.length());
                }            */

            }
            QuaternionUtil.setEuler(yawPitch, TeraMath.DEG_TO_RAD * minecartComponent.yaw, 0, minecartComponent.pitch);

            location.setWorldRotation(yawPitch);
            location.setWorldPosition(minecartWorldPosition);

            minecartComponent.prevPosition.set(minecartWorldPosition);
            entity.saveComponent(minecartComponent);
            entity.saveComponent(location);
        }
    }

    private boolean isCorner(ConnectsToRailsComponent.RAILS type) {
        return type == ConnectsToRailsComponent.RAILS.CURVE ||
                type == ConnectsToRailsComponent.RAILS.TEE ||
                type == ConnectsToRailsComponent.RAILS.TEE_INVERSED;
    }
}
