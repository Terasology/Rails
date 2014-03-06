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
import org.terasology.entitySystem.systems.ComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.TeraMath;
import org.terasology.math.Vector3i;
import org.terasology.physics.HitResult;
import org.terasology.physics.Physics;
import org.terasology.physics.StandardCollisionGroup;
import org.terasology.physics.components.RigidBodyComponent;
import org.terasology.physics.engine.RigidBody;
import org.terasology.physics.events.ChangeVelocityEvent;
import org.terasology.rails.blocks.ConnectsToRailsComponent;
import org.terasology.rails.carts.components.MinecartComponent;
import org.terasology.registry.In;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

@RegisterSystem(RegisterMode.AUTHORITY)
public class MinecartSystem implements ComponentSystem, UpdateSubscriberSystem {
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
    public void preBegin() {

    }

    @Override
    public void postBegin() {

    }

    @Override
    public void preSave() {

    }

    @Override
    public void postSave() {

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

                HitResult hit = physics.rayTrace(location.getWorldPosition(), new Vector3f(0, -1, 0), 6, StandardCollisionGroup.DEFAULT, StandardCollisionGroup.WORLD);
                RigidBodyComponent rb = minecart.getComponent(RigidBodyComponent.class);
                Vector3i blockPosition = hit.getBlockPosition();

                Block currentBlock = null;

                if (blockPosition != null) {
                    currentBlock = worldProvider.getBlock(blockPosition);
                    EntityRef blockEntity = currentBlock.getEntity();

                    if (blockPosition.toVector3f().equals(minecartComponent.prevBlockPosition)) {
                        if (minecartComponent.currentPositionStatus.equals(MinecartComponent.PositionStatus.ON_THE_PATH)) {
                            ConnectsToRailsComponent railsComponent = blockEntity.getComponent(ConnectsToRailsComponent.class);
                            if (ConnectsToRailsComponent.RAILS.valueOf(railsComponent.type) != ConnectsToRailsComponent.RAILS.CURVE) {
                                location.setWorldPosition(correctPosition(location.getWorldPosition(), minecartComponent.pathDirection, blockPosition.toVector3f()));
                              /*  Quat4f yawPitch = new Quat4f(0, 0, 0, 1);
                                float yaw   = minecartComponent.moveDescriptor.getYaw();
                                float pitch = minecartComponent.moveDescriptor.getPitch();
                                QuaternionUtil.setEuler(yawPitch, TeraMath.DEG_TO_RAD * yaw, TeraMath.DEG_TO_RAD * pitch, 0);
                                location.setWorldRotation(yawPitch);*/
                                minecart.saveComponent(location);
                            }
                            //Vector3f velocity = new Vector3f(rb.velocity);
                           // minecartComponent.moveDescriptor.correctVelocity(velocity);
                           // minecart.send(new ChangeVelocityEvent(velocity));
                        }
                        continue;
                    }

                    if (blockEntity != null && blockEntity.hasComponent(ConnectsToRailsComponent.class)) {
                        Vector3f velocity = new Vector3f(rb.velocity);
                        Vector3f direction = location.getWorldPosition();
                        direction.sub(minecartComponent.prevPosition);
                        ConnectsToRailsComponent railsComponent = blockEntity.getComponent(ConnectsToRailsComponent.class);
                        minecartComponent.currentPositionStatus = MinecartComponent.PositionStatus.ON_THE_PATH;
                        moveDescriptor.calculateDirection(
                                blockPosition.toVector3f(),
                                velocity,
                                direction,
                                ConnectsToRailsComponent.RAILS.valueOf(railsComponent.type),
                                currentBlock.getDirection(),
                                minecartComponent
                        );
                        rb.angularFactor = 0f;
                        if (ConnectsToRailsComponent.RAILS.valueOf(railsComponent.type) != ConnectsToRailsComponent.RAILS.CURVE) {
                            location.setWorldPosition(correctPosition(location.getWorldPosition(), minecartComponent.pathDirection, blockPosition.toVector3f()));
                        }
                        minecart.send(new ChangeVelocityEvent(velocity));
                        /*Quat4f yawPitch = new Quat4f(0, 0, 0, 1);
                        float yaw   = minecartComponent.moveDescriptor.getYaw();
                        float pitch = minecartComponent.moveDescriptor.getPitch();
                        QuaternionUtil.setEuler(yawPitch, TeraMath.DEG_TO_RAD * yaw, TeraMath.DEG_TO_RAD * pitch, 0);
                        location.setWorldRotation(yawPitch);*/
                        minecart.saveComponent(location);
                        minecart.saveComponent(rb);
                    } else {
                        minecartComponent.pathDirection.set(1f, 1f, 1f);
                        minecartComponent.currentPositionStatus = MinecartComponent.PositionStatus.ON_THE_GROUND;
                        rb.angularFactor = 1f;
                        minecart.saveComponent(rb);
                    }
                } else {
                    minecartComponent.pathDirection.set(1f, 1f, 1f);
                    minecartComponent.currentPositionStatus = MinecartComponent.PositionStatus.ON_THE_AIR;
                    rb.angularFactor = 1f;
                    minecart.saveComponent(rb);

                }
                minecartComponent.prevPosition.set(location.getWorldPosition());
                minecart.saveComponent(minecartComponent);
            }

        }
    }

    private Vector3f correctPosition(Vector3f minecartPosition, Vector3f pathDirection, Vector3f blockPosition) {
        if (pathDirection.z != 0) {
            minecartPosition.x = blockPosition.x;
        } else {
            minecartPosition.z = blockPosition.z;
        }
        return minecartPosition;
    }
}
