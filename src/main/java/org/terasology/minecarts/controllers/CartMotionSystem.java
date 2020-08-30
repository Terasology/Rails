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

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.Time;
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
import org.terasology.math.JomlUtil;
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
import org.terasology.segmentedpaths.SegmentMeta;
import org.terasology.segmentedpaths.components.PathDescriptorComponent;
import org.terasology.segmentedpaths.components.PathFollowerComponent;
import org.terasology.segmentedpaths.controllers.PathFollowerSystem;
import org.terasology.segmentedpaths.controllers.SegmentCacheSystem;
import org.terasology.segmentedpaths.controllers.SegmentSystem;
import org.terasology.segmentedpaths.segments.Segment;
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
    PathFollowerSystem pathFollowerSystem;
    @In
    SegmentSystem segmentSystem;
    @In
    SegmentCacheSystem segmentCacheSystem;

    private RailBlockSegmentMapper segmentMapping;

    @Override
    public void initialise() {
        segmentMapping = new RailBlockSegmentMapper(blockEntityRegistry, pathFollowerSystem, segmentSystem, segmentCacheSystem);
    }


    @Override
    public void update(float delta) {
        for (EntityRef railVehicle : entityManager.getEntitiesWith(RailVehicleComponent.class, RigidBodyComponent.class)) {
            updateCart(railVehicle, delta);
        }
    }

    public Vector3f updateHeading(EntityRef railVehcile, Vector3f oldHeading) {
        PathFollowerComponent pathFollowerComponent = railVehcile.getComponent(PathFollowerComponent.class);
        if (pathFollowerComponent != null) {
            return Util.project(pathFollowerComponent.heading, oldHeading, new Vector3f()).normalize();
        }
        return null;
    }

    private void updateCart(EntityRef railVehicle, float delta) {

        LocationComponent location = railVehicle.getComponent(LocationComponent.class);
        RigidBodyComponent rigidBodyComponent = railVehicle.getComponent(RigidBodyComponent.class);
        RailVehicleComponent railVehicleComponent = railVehicle.getComponent(RailVehicleComponent.class);
        PathFollowerComponent segmentVehicleComponent = railVehicle.getComponent(PathFollowerComponent.class);

        if (segmentVehicleComponent == null) {
            //checks to see if the cart hits a rail segment
            HitResult hit = physics.rayTrace(JomlUtil.from(location.getWorldPosition()), new Vector3f(0, -1, 0), 1.2f, StandardCollisionGroup.DEFAULT, StandardCollisionGroup.WORLD);
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

                float segmentPosition = segment.nearestSegmentPosition(JomlUtil.from(location.getWorldPosition()), position, rotation);


                segmentVehicleComponent.segmentMeta = new SegmentMeta(segmentPosition, ref, prefab);
                railVehicle.addComponent(segmentVehicleComponent);
                segmentVehicleComponent.heading = pathFollowerSystem.vehicleTangent(railVehicle);
                rigidBodyComponent.collidesWith.remove(StandardCollisionGroup.WORLD);
                railVehicleComponent.velocity = Util.project(segmentVehicleComponent.heading, rigidBodyComponent.velocity, new Vector3f());

                railVehicle.addOrSaveComponent(segmentVehicleComponent);
            }
        } else {
            if (railVehicleComponent.velocity.length() > Constants.VELOCITY_CAP) {
                railVehicleComponent.velocity.normalize().mul(Constants.VELOCITY_CAP);
            }

            if (pathFollowerSystem.isVehicleValid(railVehicle)) {
                Vector3f position = pathFollowerSystem.vehiclePoint(railVehicle);
                MeshComponent mesh = railVehicle.getComponent(MeshComponent.class);
                position.y = mesh.mesh.getAABB().getMax().y / 2.0f + position.y + .01f;

                Vector3f normal = pathFollowerSystem.vehicleNormal(railVehicle);
                Vector3f tangent = pathFollowerSystem.vehicleTangent(railVehicle);

                Vector3f gravity = new Vector3f(0, -1, 0).mul(Constants.GRAVITY).mul(delta);

                railVehicleComponent.velocity.add(Util.project(tangent, gravity, new Vector3f()));

                //apply some friction based off the gravity vector projected on the normal multiplied against a friction coff
                RailComponent rail = segmentVehicleComponent.segmentMeta.association.getComponent(RailComponent.class);
                Vector3f friction = Util.project(normal, gravity, new Vector3f()).mul(-1).mul(rail.frictionCoefficient);

                float mag = railVehicleComponent.velocity.length() - friction.length();
                //make sure the magnitude is not less then zero when the friction value is subtracted off of the velocity
                if (mag < 0) {
                    mag = 0;
                }

                //apply the new velocity to the rail component
                railVehicleComponent.velocity = Util.project(segmentVehicleComponent.heading, railVehicleComponent.velocity, new Vector3f()).normalize().mul(mag);

                //make sure the value is not nan or infinite
                //occurs when the cart hits a perpendicular segment.
                //currently, the vehicle and its axle are the same components
                Util.bound(railVehicleComponent.velocity);
                if (pathFollowerSystem.move(railVehicle, Math.signum(segmentVehicleComponent.heading.dot(railVehicleComponent.velocity)) * mag * delta, segmentMapping)) {

                    //calculate the cart rotation
                    Vector3f horzY = new Vector3f(segmentVehicleComponent.heading);
                    horzY.y = 0;
                    horzY.normalize();

                    Quaternionf horizontalRotation = new Quaternionf().rotateTo(new Vector3f(0, 0, 1), horzY);
                    Quaternionf verticalRotation = new Quaternionf().rotateTo(horzY, segmentVehicleComponent.heading);

                    verticalRotation.mul(horizontalRotation);
                    location.setLocalRotation(JomlUtil.from(verticalRotation));
                    location.setWorldPosition(JomlUtil.from(position));
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

        vehicle.saveComponent(railVehicleComponent);
        vehicle.saveComponent(rigidBodyComponent);
    }
}
