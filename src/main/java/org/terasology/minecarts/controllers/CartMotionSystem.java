/*
 * Copyright 2016 MovingBlocks
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
package org.terasology.minecarts.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.Time;
import org.terasology.entitySystem.entity.EntityBuilder;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.location.LocationComponent;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.math.geom.Quat4f;
import org.terasology.math.geom.Vector3f;
import org.terasology.minecarts.Constants;
import org.terasology.minecarts.Util;
import org.terasology.minecarts.blocks.RailBlockSegmentMapper;
import org.terasology.minecarts.blocks.RailComponent;
import org.terasology.minecarts.components.RailVehicleComponent;
import org.terasology.physics.HitResult;
import org.terasology.physics.Physics;
import org.terasology.physics.StandardCollisionGroup;
import org.terasology.physics.components.RigidBodyComponent;
import org.terasology.registry.In;
import org.terasology.rendering.logic.MeshComponent;
import org.terasology.segmentedpaths.Segment;
import org.terasology.segmentedpaths.components.PathDescriptorComponent;
import org.terasology.segmentedpaths.components.PathFollowerComponent;
import org.terasology.segmentedpaths.controllers.SegmentCacheSystem;
import org.terasology.segmentedpaths.controllers.SegmentSystem;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;

/**
 * Created by michaelpollind on 8/16/16.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class CartMotionSystem extends BaseComponentSystem implements UpdateSubscriberSystem {

    private static final Logger logger = LoggerFactory.getLogger(CartMotionSystem.class);

    @In
    Time time;
    @In
    EntityManager entityManager;
    @In
    WorldProvider worldProvider;
    @In
    Physics physics;
    @In
    LocalPlayer localPlayer;
    @In
    InventoryManager inventoryManager;
    @In
    BlockEntityRegistry blockEntityRegistry;
    @In
    SegmentSystem segmentSystem;
    @In
    SegmentCacheSystem segmentCacheSystem;

    private RailBlockSegmentMapper segmentMapping;

    @Override
    public void initialise() {
        segmentMapping = new RailBlockSegmentMapper(blockEntityRegistry, segmentSystem, segmentCacheSystem);
    }


    @Override
    public void update(float delta) {
        for (EntityRef railVehicle : entityManager.getEntitiesWith(RailVehicleComponent.class, RigidBodyComponent.class)) {
            updateCart(railVehicle, delta);
        }
    }

    public Vector3f updateHeading(EntityRef railVehicle, Vector3f oldHeading) {
        PathFollowerComponent pathFollowerComponent = railVehicle.getComponent(PathFollowerComponent.class);
        if (pathFollowerComponent != null) {
            return pathFollowerComponent.heading.project(oldHeading).normalize();
        }
        return null;
    }

    private void updateCart(EntityRef railVehicle, float delta) {

        LocationComponent location = railVehicle.getComponent(LocationComponent.class);
        RigidBodyComponent rigidBodyComponent = railVehicle.getComponent(RigidBodyComponent.class);
        RailVehicleComponent railVehicleComponent = railVehicle.getComponent(RailVehicleComponent.class);

        if (railVehicleComponent.rearAxle == null) {
            railVehicleComponent.rearAxle = entityManager.create();
        }

        if (railVehicleComponent.frontAxle == null) {
            railVehicleComponent.frontAxle = entityManager.create();
        }

        PathFollowerComponent rearPathFollowerComponent = railVehicleComponent.rearAxle.getComponent(PathFollowerComponent.class);
        PathFollowerComponent frontPathFollowerComponent = railVehicleComponent.frontAxle.getComponent(PathFollowerComponent.class);

        if (rearPathFollowerComponent == null || frontPathFollowerComponent == null) {
            // Check to see if the cart hits a rail segment
            HitResult hit = physics.rayTrace(location.getWorldPosition(), Vector3f.down(), 1.2f, StandardCollisionGroup.DEFAULT, StandardCollisionGroup.WORLD);
            if (hit == null || hit.getBlockPosition() == null) {
                return;
            }

            EntityRef ref = blockEntityRegistry.getBlockEntityAt(hit.getBlockPosition());

            // Attach cart to rail segment
            if (ref.hasComponent(RailComponent.class)) {
                rearPathFollowerComponent = new PathFollowerComponent();
                rearPathFollowerComponent.segmentEntity = ref;
                frontPathFollowerComponent = new PathFollowerComponent();
                frontPathFollowerComponent.segmentEntity = ref;

                Prefab prefab = ref.getComponent(PathDescriptorComponent.class).descriptors.get(0);
                Segment segment = segmentCacheSystem.getSegment(prefab);

                Vector3f position = segmentSystem.segmentPosition(ref);
                Quat4f rotation = segmentSystem.segmentRotation(ref);
                float nearestT = segment.nearestSegmentPosition(location.getWorldPosition(), position, rotation);
                rearPathFollowerComponent.segmentPosition = nearestT - railVehicleComponent.axleSpacing / 2;
                rearPathFollowerComponent.descriptor = prefab;
                railVehicleComponent.rearAxle.addComponent(rearPathFollowerComponent);
                rearPathFollowerComponent.heading = segmentSystem.vehicleTangent(railVehicleComponent.rearAxle);
                frontPathFollowerComponent.segmentPosition = nearestT + railVehicleComponent.axleSpacing / 2;
                frontPathFollowerComponent.descriptor = prefab;
                railVehicleComponent.frontAxle.addComponent(frontPathFollowerComponent);
                frontPathFollowerComponent.heading = segmentSystem.vehicleTangent(railVehicleComponent.frontAxle);
                rigidBodyComponent.collidesWith.remove(StandardCollisionGroup.WORLD);

                railVehicleComponent.velocity = rearPathFollowerComponent.heading.project(rigidBodyComponent.velocity);

                railVehicleComponent.rearAxle.addOrSaveComponent(rearPathFollowerComponent);
                railVehicleComponent.frontAxle.addOrSaveComponent(frontPathFollowerComponent);
            }
        } else {
            if (railVehicleComponent.velocity.length() > Constants.VELOCITY_CAP) {
                railVehicleComponent.velocity.normalize().mul(Constants.VELOCITY_CAP);
            }

            if (segmentSystem.isVehicleValid(railVehicleComponent.rearAxle)
                    && segmentSystem.isVehicleValid(railVehicleComponent.frontAxle)) {
                Vector3f rearAxlePosition = segmentSystem.vehiclePoint(railVehicleComponent.rearAxle, 0f);
                Vector3f frontAxlePosition = segmentSystem.vehiclePoint(railVehicleComponent.frontAxle, 0f);
                MeshComponent mesh = railVehicle.getComponent(MeshComponent.class);
                rearAxlePosition.y += mesh.mesh.getAABB().getMax().y / 2.0f + .01f;
                frontAxlePosition.y += mesh.mesh.getAABB().getMax().y / 2.0f + .01f;

                Vector3f normal = segmentSystem.vehicleNormal(railVehicleComponent.rearAxle);
                Vector3f tangent = segmentSystem.vehicleTangent(railVehicleComponent.rearAxle);

                Vector3f gravity = Vector3f.down().mul(Constants.GRAVITY).mul(delta);
                railVehicleComponent.velocity.add(tangent.project(gravity));

                // Apply friction based off the gravity vector projected on the normal multiplied against a friction coeff.
                Vector3f friction = normal.project(gravity).invert().mul(Constants.FRICTION_COFF);

                float mag = railVehicleComponent.velocity.length() - friction.length();
                // Ensure magnitude is not less than zero when the friction value is subtracted off of the velocity
                if (mag < 0) {
                    mag = railVehicleComponent.velocity.length();
                }

                //apply the new velocity to the rail component
                railVehicleComponent.velocity = rearPathFollowerComponent.heading.project(railVehicleComponent.velocity).normalize().mul(mag);

                //make sure the value is not nan or infinite
                //occurs when the cart hits a perpendicular segment.
                Util.bound(railVehicleComponent.velocity);
                if (segmentSystem.move(railVehicle, railVehicleComponent.rearAxle, Math.signum(rearPathFollowerComponent.heading.dot(railVehicleComponent.velocity)) * mag * delta, segmentMapping)
                        && segmentSystem.move(railVehicle, railVehicleComponent.frontAxle, Math.signum(frontPathFollowerComponent.heading.dot(railVehicleComponent.velocity)) * mag * delta, segmentMapping)) {

                    //calculate the cart rotation
                    Quat4f horizontalRotation = Quat4f.shortestArcQuat(Vector3f.north(), new Vector3f(frontAxlePosition).sub(rearAxlePosition).setY(0).normalize());
                    Quat4f verticalRotation = Quat4f.shortestArcQuat(new Vector3f(rearPathFollowerComponent.heading).setY(0).normalize(), new Vector3f(rearPathFollowerComponent.heading));
                    verticalRotation.mul(horizontalRotation);
                    location.setLocalRotation(verticalRotation);
                    location.setWorldPosition((rearAxlePosition.add(frontAxlePosition)).div(2));
                    rigidBodyComponent.kinematic = true;
                } else {
                    detachFromRail(railVehicle);
                }
            } else {
                detachFromRail(railVehicle);
            }

            railVehicle.saveComponent(location);
            railVehicle.saveComponent(railVehicleComponent);
            railVehicle.saveComponent(rigidBodyComponent);

        }
    }

    private void detachFromRail(EntityRef vehicle) {
        RigidBodyComponent rigidBodyComponent = vehicle.getComponent(RigidBodyComponent.class);
        RailVehicleComponent railVehicleComponent = vehicle.getComponent(RailVehicleComponent.class);

        //change the rigidbody into a non kinematic body and remove the PathFollowerComponent
        rigidBodyComponent.kinematic = false;
        railVehicleComponent.rearAxle.removeComponent(PathFollowerComponent.class);
        railVehicleComponent.frontAxle.removeComponent(PathFollowerComponent.class);
        rigidBodyComponent.velocity = new Vector3f(railVehicleComponent.velocity);
        rigidBodyComponent.collidesWith.add(StandardCollisionGroup.WORLD);

        vehicle.saveComponent(railVehicleComponent);
        vehicle.saveComponent(rigidBodyComponent);
    }


}