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
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.EventPriority;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.characters.CharacterComponent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.location.LocationComponent;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.math.geom.Quat4f;
import org.terasology.math.geom.Vector3f;
import org.terasology.minecarts.Constants;
import org.terasology.minecarts.blocks.RailBlockSegmentMapper;
import org.terasology.physics.HitResult;
import org.terasology.physics.Physics;
import org.terasology.physics.StandardCollisionGroup;
import org.terasology.physics.components.RigidBodyComponent;
import org.terasology.physics.events.CollideEvent;
import org.terasology.minecarts.blocks.RailComponent;
import org.terasology.minecarts.components.RailVehicleComponent;
import org.terasology.segmentedpaths.Segment;
import org.terasology.segmentedpaths.components.PathDescriptorComponent;
import org.terasology.segmentedpaths.components.SegmentEntityComponent;
import org.terasology.segmentedpaths.controllers.SegmentCacheSystem;
import org.terasology.segmentedpaths.controllers.SegmentSystem;
import org.terasology.registry.In;
import org.terasology.rendering.logic.MeshComponent;
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

    private void updateCart(EntityRef railVehicle, float delta) {

        LocationComponent location = railVehicle.getComponent(LocationComponent.class);
        RigidBodyComponent rigidBodyComponent = railVehicle.getComponent(RigidBodyComponent.class);
        RailVehicleComponent railVehicleComponent = railVehicle.getComponent(RailVehicleComponent.class);
        SegmentEntityComponent segmentVehicleComponent = railVehicle.getComponent(SegmentEntityComponent.class);

        if (segmentVehicleComponent == null) {
            HitResult hit = physics.rayTrace(location.getWorldPosition(), Vector3f.down(), 1.2f, StandardCollisionGroup.DEFAULT, StandardCollisionGroup.WORLD);
            if (hit == null || hit.getBlockPosition() == null)
                return;

            EntityRef ref = blockEntityRegistry.getBlockEntityAt(hit.getBlockPosition());

            if (ref.hasComponent(RailComponent.class)) {
                segmentVehicleComponent = new SegmentEntityComponent();
                segmentVehicleComponent.segmentEntity = ref;

                Prefab prefab = ref.getComponent(PathDescriptorComponent.class).descriptors.get(0);
                Segment segment = segmentCacheSystem.getSegment(prefab);

                Vector3f position = segmentSystem.segmentPosition(ref);
                Quat4f rotation = segmentSystem.segmentRotation(ref);
                segmentVehicleComponent.t = segment.nearestT(location.getWorldPosition(), position, rotation);
                segmentVehicleComponent.descriptor = prefab;
                railVehicle.addComponent(segmentVehicleComponent);
                segmentVehicleComponent.heading = segmentSystem.vehicleTangent(railVehicle);
                rigidBodyComponent.collidesWith.remove(StandardCollisionGroup.WORLD);

                railVehicleComponent.velocity = segmentVehicleComponent.heading.project(rigidBodyComponent.velocity);

                railVehicle.addOrSaveComponent(segmentVehicleComponent);

            }
        } else {
            if (railVehicleComponent.velocity.length() > Constants.VELOCITY_CAP)
                railVehicleComponent.velocity.normalize().mul(Constants.VELOCITY_CAP);

            if (segmentSystem.isvehicleValid(railVehicle)) {

                Vector3f position = segmentSystem.vehiclePoint(railVehicle);
                MeshComponent mesh = railVehicle.getComponent(MeshComponent.class);
                position.y = mesh.mesh.getAABB().getMax().y / 2.0f + position.y + .01f;

                Vector3f normal = segmentSystem.vehicleNormal(railVehicle);
                Vector3f tangent = segmentSystem.vehicleTangent(railVehicle);

                Vector3f gravity = Vector3f.down().mul(Constants.GRAVITY).mul(delta);
                railVehicleComponent.velocity.add(tangent.project(gravity));


                Vector3f friction = normal.project(gravity).invert().mul(Constants.FRICTION_COFF);

                float mag = railVehicleComponent.velocity.length() - friction.length();
                if (mag < 0)
                    mag = railVehicleComponent.velocity.length();

                railVehicleComponent.velocity = segmentVehicleComponent.heading.project(railVehicleComponent.velocity).normalize().mul(mag);

                bound(railVehicleComponent.velocity);
                if (segmentSystem.move(railVehicle, Math.signum(segmentVehicleComponent.heading.dot(railVehicleComponent.velocity)) * mag * delta, segmentMapping)) {

                    Quat4f horizontalRotation = Quat4f.shortestArcQuat(Vector3f.north(), new Vector3f(segmentVehicleComponent.heading).setY(0).normalize());
                    Quat4f verticalRotation = Quat4f.shortestArcQuat(new Vector3f(segmentVehicleComponent.heading).setY(0).normalize(), new Vector3f(segmentVehicleComponent.heading));
                    verticalRotation.mul(horizontalRotation);
                    location.setLocalRotation(verticalRotation);
                    location.setWorldPosition(position);
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

        rigidBodyComponent.kinematic = false;
        vehicle.removeComponent(SegmentEntityComponent.class);
        rigidBodyComponent.velocity = new Vector3f(railVehicleComponent.velocity);
        rigidBodyComponent.collidesWith.add(StandardCollisionGroup.WORLD);

        vehicle.saveComponent(railVehicleComponent);
        vehicle.saveComponent(rigidBodyComponent);
    }

    private void bound(Vector3f v) {
        if (Float.isNaN(v.x) || Float.isInfinite(v.x))
            v.x = 0.0f;
        if (Float.isNaN(v.y) || Float.isInfinite(v.y))
            v.y = 0.0f;
        if (Float.isNaN(v.z) || Float.isInfinite(v.z))
            v.z = 0.0f;
    }

}