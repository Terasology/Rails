// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.minecarts.controllers;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.core.Time;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.prefab.Prefab;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.engine.logic.inventory.InventoryManager;
import org.terasology.engine.logic.location.LocationComponent;
import org.terasology.engine.logic.players.LocalPlayer;
import org.terasology.engine.physics.HitResult;
import org.terasology.engine.physics.Physics;
import org.terasology.engine.physics.StandardCollisionGroup;
import org.terasology.engine.physics.components.RigidBodyComponent;
import org.terasology.engine.registry.In;
import org.terasology.engine.world.BlockEntityRegistry;
import org.terasology.engine.world.WorldProvider;
import org.terasology.minecarts.Constants;
import org.terasology.minecarts.Util;
import org.terasology.minecarts.blocks.RailBlockSegmentMapper;
import org.terasology.minecarts.blocks.RailComponent;
import org.terasology.minecarts.components.RailVehicleComponent;
import org.terasology.segmentedpaths.SegmentMeta;
import org.terasology.segmentedpaths.components.PathDescriptorComponent;
import org.terasology.segmentedpaths.components.PathFollowerComponent;
import org.terasology.segmentedpaths.controllers.PathFollowerSystem;
import org.terasology.segmentedpaths.controllers.SegmentCacheSystem;
import org.terasology.segmentedpaths.controllers.SegmentSystem;
import org.terasology.segmentedpaths.segments.Segment;

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
    Physics physics;
    @In
    BlockEntityRegistry blockEntityRegistry;
    @In
    PathFollowerSystem pathFollowerSystem;
    @In
    SegmentSystem segmentSystem;
    @In
    SegmentCacheSystem segmentCacheSystem;

    private RailBlockSegmentMapper segmentMapping;

    @Override
    public void initialise() {
        segmentMapping = new RailBlockSegmentMapper(blockEntityRegistry, pathFollowerSystem, segmentSystem,
                segmentCacheSystem);
    }


    @Override
    public void update(float delta) {
        for (EntityRef railVehicle : entityManager.getEntitiesWith(RailVehicleComponent.class,
                RigidBodyComponent.class)) {
            updateCart(railVehicle, delta);
        }
    }

    private void updateCart(EntityRef railVehicle, float delta) {

        LocationComponent location = railVehicle.getComponent(LocationComponent.class);
        RigidBodyComponent rigidBodyComponent = railVehicle.getComponent(RigidBodyComponent.class);
        RailVehicleComponent railVehicleComponent = railVehicle.getComponent(RailVehicleComponent.class);
        PathFollowerComponent segmentVehicleComponent = railVehicle.getComponent(PathFollowerComponent.class);

        if (segmentVehicleComponent == null) {
            if (railVehicleComponent.lastDetach + 1.5f > time.getGameTime()) {
                return;
            }

            //checks to see if the cart hits a rail segment
            HitResult hit = physics.rayTrace(location.getWorldPosition(new Vector3f()), new Vector3f(0, -1, 0), 1.2f,
                    StandardCollisionGroup.DEFAULT, StandardCollisionGroup.WORLD);
            if (hit == null || hit.getBlockPosition() == null) {
                return;
            }

            EntityRef ref = blockEntityRegistry.getBlockEntityAt(hit.getBlockPosition());

            //attach cart to rail segment
            if (ref.hasComponent(RailComponent.class)) {
                segmentVehicleComponent = new PathFollowerComponent();

                Prefab prefab = ref.getComponent(PathDescriptorComponent.class).descriptors.get(0);
                Segment segment = segmentCacheSystem.getSegment(prefab);

                Vector3f position = segmentSystem.segmentPosition(ref);
                Quaternionf rotation = segmentSystem.segmentRotation(ref);

                float segmentPosition = segment.nearestSegmentPosition(location.getWorldPosition(new Vector3f()),
                        position, rotation);


                segmentVehicleComponent.segmentMeta = new SegmentMeta(segmentPosition, ref, prefab);
                railVehicle.addComponent(segmentVehicleComponent);
                segmentVehicleComponent.heading = pathFollowerSystem.vehicleTangent(railVehicle);
                rigidBodyComponent.collidesWith.remove(StandardCollisionGroup.WORLD);
                railVehicleComponent.velocity = Util.project(segmentVehicleComponent.heading,
                        rigidBodyComponent.velocity, new Vector3f());
                if (!railVehicleComponent.velocity.isFinite()) {
                    railVehicleComponent.velocity.set(.001f);
                }
                railVehicle.addOrSaveComponent(segmentVehicleComponent);
            }
        } else {
            if (railVehicleComponent.velocity.length() > Constants.VELOCITY_CAP) {
                railVehicleComponent.velocity.normalize().mul(Constants.VELOCITY_CAP);
            }

            if (pathFollowerSystem.isVehicleValid(railVehicle)) {
                Vector3f position = pathFollowerSystem.vehiclePoint(railVehicle);
                position.y = position.y + .01f;

                Vector3f normal = pathFollowerSystem.vehicleNormal(railVehicle);
                Vector3f tangent = pathFollowerSystem.vehicleTangent(railVehicle);
                Vector3f gravity = new Vector3f(0, -1, 0).mul(Constants.GRAVITY).mul(delta);

                railVehicleComponent.velocity.add(Util.project(gravity, tangent, new Vector3f()));

                //apply some friction based off the gravity vector projected on the normal multiplied against a
                // friction coff
                RailComponent rail = segmentVehicleComponent.segmentMeta.association.getComponent(RailComponent.class);
                Vector3f friction = Util.project(normal, gravity, new Vector3f()).mul(-1).mul(rail.frictionCoefficient);

                float mag = railVehicleComponent.velocity.length() - friction.length();
                //make sure the magnitude is not less then zero when the friction value is subtracted off of the
                // velocity
                if (mag < 0) {
                    mag = 0;
                }

                //apply the new velocity to the rail component
                railVehicleComponent.velocity = Util.project(railVehicleComponent.velocity,
                        segmentVehicleComponent.heading, new Vector3f()).normalize().mul(mag);

                //make sure the value is not nan or infinite
                //occurs when the cart hits a perpendicular segment.
                //currently, the vehicle and its axle are the same components
                if (!railVehicleComponent.velocity.isFinite()) {
                    railVehicleComponent.velocity.set(0);
                }
                if (pathFollowerSystem.move(railVehicle,
                        Math.signum(segmentVehicleComponent.heading.dot(railVehicleComponent.velocity)) * mag * delta
                        , segmentMapping)) {

                    if (railVehicleComponent.backAxisOffset == 0.0f && railVehicleComponent.frontAxisOffset == 0.0f) {
                        location.setWorldRotation(Util.rotation(segmentVehicleComponent.heading));
                        location.setWorldPosition(position);
                    } else {
                        Vector3f frontAxisPosition = pathFollowerSystem.vehiclePoint(railVehicle,
                                railVehicleComponent.frontAxisOffset, segmentMapping);
                        Vector3f backAxisPosition = pathFollowerSystem.vehiclePoint(railVehicle,
                                railVehicleComponent.backAxisOffset, segmentMapping);
                        if (frontAxisPosition == null || backAxisPosition == null) {
                            location.setWorldRotation(Util.rotation(segmentVehicleComponent.heading));
                            location.setWorldPosition(position);
                        } else {
                            Vector3f dir = frontAxisPosition.sub(backAxisPosition, new Vector3f()).normalize();
                            location.setWorldPosition(backAxisPosition.add(new Vector3f(dir).mul(-railVehicleComponent.backAxisOffset)));
                            location.setWorldRotation(Util.rotation(dir.normalize()));
                        }
                    }

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
        vehicle.removeComponent(PathFollowerComponent.class);
        rigidBodyComponent.velocity = new Vector3f(railVehicleComponent.velocity);
        rigidBodyComponent.collidesWith.add(StandardCollisionGroup.WORLD);

        railVehicleComponent.lastDetach = time.getGameTime();

        vehicle.saveComponent(railVehicleComponent);
        vehicle.saveComponent(rigidBodyComponent);
    }
}
