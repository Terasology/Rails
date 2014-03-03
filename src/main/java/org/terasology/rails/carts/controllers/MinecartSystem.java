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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.systems.ComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.location.LocationComponent;
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

import javax.vecmath.Vector3f;

@RegisterSystem(RegisterMode.AUTHORITY)
public class MinecartSystem implements ComponentSystem, UpdateSubscriberSystem {
    @In
    private EntityManager entityManager;

    @In
    private WorldProvider worldProvider;

    @In
    private Physics physics;

    private final Logger logger = LoggerFactory.getLogger(MinecartSystem.class);

    @Override
    public void initialise() {
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

                Vector3i blockPosition = hit.getBlockPosition();

                Block currentBlock = null;

                if (blockPosition != null) {

                    if (blockPosition.toVector3f().equals(minecartComponent.moveDescriptor.getCurrentPosition())) {
                        if (minecartComponent.moveDescriptor.getCurrentPositionStatus().equals(MoveDescriptor.POSITION_STATUS.ON_THE_PATH)) {
                            correctPosition(location, minecartComponent.moveDescriptor.getPathDirection(), blockPosition.toVector3f() );
                            minecart.saveComponent(location);
                        }
                        continue;
                    }

                    currentBlock = worldProvider.getBlock(blockPosition);
                    EntityRef blockEntity = currentBlock.getEntity();

                    if (blockEntity != null && blockEntity.hasComponent(ConnectsToRailsComponent.class)) {
                        ConnectsToRailsComponent railsComponent = blockEntity.getComponent(ConnectsToRailsComponent.class);
                        minecartComponent.moveDescriptor.setCurrentPositionStatus(MoveDescriptor.POSITION_STATUS.ON_THE_PATH);
                        minecartComponent.moveDescriptor.setCurrentBlockOfPathType(ConnectsToRailsComponent.RAILS.valueOf(railsComponent.type));
                        minecartComponent.moveDescriptor.setCurrentBlockOfPathSide(currentBlock.getDirection());
                        minecartComponent.moveDescriptor.calculateDirection(blockPosition.toVector3f());
                        if (ConnectsToRailsComponent.RAILS.valueOf(railsComponent.type) != ConnectsToRailsComponent.RAILS.CURVE) {
                            correctPosition(location, minecartComponent.moveDescriptor.getPathDirection(), blockPosition.toVector3f() );
                            minecart.saveComponent(location);
                        } else {
                            RigidBodyComponent rb = minecart.getComponent(RigidBodyComponent.class);
                            Vector3f velocity = new Vector3f(rb.velocity);
                            minecartComponent.moveDescriptor.correctVelocity(velocity);
                            minecart.send(new ChangeVelocityEvent(velocity));
                            logger.info("new velocity " + velocity);
                        }
                    } else {
                      minecartComponent.moveDescriptor.setPathDirection(new Vector3f(1f, 1f, 1f));
                      minecartComponent.moveDescriptor.setCurrentBlockOfPathType(null);
                      minecartComponent.moveDescriptor.setCurrentPositionStatus(MoveDescriptor.POSITION_STATUS.ON_THE_GROUND);
                    }
                } else {
                    minecartComponent.moveDescriptor.setPathDirection(new Vector3f(1f, 1f, 1f));
                    minecartComponent.moveDescriptor.setCurrentBlockOfPathType(null);
                    minecartComponent.moveDescriptor.setCurrentPositionStatus(MoveDescriptor.POSITION_STATUS.ON_THE_AIR);
                }

                minecart.saveComponent(minecartComponent);
            }

        }
    }

    private void correctPosition(LocationComponent location, Vector3f pathDirection, Vector3f blockPosition) {
        if (pathDirection.z != 0) {
            Vector3f worldPosition = location.getWorldPosition();
            worldPosition.x = blockPosition.x;
            location.setWorldPosition(worldPosition);
        } else {
            Vector3f worldPosition = location.getWorldPosition();
            worldPosition.z = blockPosition.z;
            location.setWorldPosition(worldPosition);
        }
    }
}
