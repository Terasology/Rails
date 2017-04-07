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
package org.terasology.rails.minecarts.controllers;

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
import org.terasology.physics.HitResult;
import org.terasology.physics.Physics;
import org.terasology.physics.StandardCollisionGroup;
import org.terasology.physics.components.RigidBodyComponent;
import org.terasology.physics.events.CollideEvent;
import org.terasology.rails.minecarts.blocks.RailComponent;
import org.terasology.rails.minecarts.components.RailVehicleComponent;
import org.terasology.rails.minecarts.components.SegmentVehicleComponent;
import org.terasology.rails.tracks.Segment;
import org.terasology.rails.tracks.components.PathDescriptorComponent;
import org.terasology.rails.tracks.controllers.AbastractSegmentMapping;
import org.terasology.rails.tracks.controllers.SegmentCacheSystem;
import org.terasology.rails.tracks.controllers.SegmentSystem;
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
    public static final float GRAVITY = 9.8f;
    public static final float FRICTION_COFF = .1f;
    public static final float BAUMGARTE_COFF = .1f;
    public static final float VELOCITY_CAP = 15f;
    @In
    private Time time;
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
    @In
    private BlockEntityRegistry blockEntityRegistry;
    @In
    SegmentSystem segmentSystem;
    @In
    SegmentCacheSystem segmentCacheSystem;

    private AbastractSegmentMapping segmentMapping;

    @Override
    public void initialise() {
        segmentMapping = new AbastractSegmentMapping(blockEntityRegistry, segmentSystem, segmentCacheSystem);
    }


    @Override
    public void update(float delta) {
        for (EntityRef railVehicle : entityManager.getEntitiesWith(RailVehicleComponent.class, RigidBodyComponent.class))
            updateCart(railVehicle, delta);

    }

    private void updateCart(EntityRef railVehicle, float delta) {

        LocationComponent location = railVehicle.getComponent(LocationComponent.class);
        RigidBodyComponent rigidBodyComponent = railVehicle.getComponent(RigidBodyComponent.class);
        RailVehicleComponent railVehicleComponent = railVehicle.getComponent(RailVehicleComponent.class);
        SegmentVehicleComponent segmentVehicleComponent = railVehicle.getComponent(SegmentVehicleComponent.class);

        if (segmentVehicleComponent == null) {
            HitResult hit = physics.rayTrace(location.getWorldPosition(), Vector3f.down(), 1.2f, StandardCollisionGroup.DEFAULT, StandardCollisionGroup.WORLD);
            if (hit == null || hit.getBlockPosition() == null)
                return;

            EntityRef ref = blockEntityRegistry.getBlockEntityAt(hit.getBlockPosition());

            if (ref.hasComponent(RailComponent.class)) {
                segmentVehicleComponent = new SegmentVehicleComponent();
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

                railVehicle.addOrSaveComponent(segmentVehicleComponent);

            }
        } else {
            if (railVehicleComponent.velocity.length() > VELOCITY_CAP)
                railVehicleComponent.velocity.normalize().mul(VELOCITY_CAP);

            Vector3f position = segmentSystem.vehiclePoint(railVehicle);
            if (position == null)
                return;

            MeshComponent mesh = railVehicle.getComponent(MeshComponent.class);
            position.y = mesh.mesh.getAABB().getMax().y / 2.0f + position.y + .01f;

            Vector3f normal = segmentSystem.vehicleNormal(railVehicle);
            Vector3f tangent = segmentSystem.vehicleTangent(railVehicle);

            Vector3f gravity = Vector3f.down().mul(GRAVITY).mul(delta);
            railVehicleComponent.velocity.add(project(gravity, tangent));

            Vector3f friction = project(gravity, normal).invert().mul(FRICTION_COFF);

            float mag = railVehicleComponent.velocity.length() - friction.length();
            if (mag < 0)
                mag = railVehicleComponent.velocity.length();

            railVehicleComponent.velocity = project(railVehicleComponent.velocity, segmentVehicleComponent.heading).normalize().mul(mag);

            bound(railVehicleComponent.velocity);
            if (segmentSystem.move(railVehicle, Math.signum(segmentVehicleComponent.heading.dot(railVehicleComponent.velocity)) * mag * delta, segmentMapping)) {

                Quat4f horizontalRotation = Quat4f.shortestArcQuat(Vector3f.north(), new Vector3f(segmentVehicleComponent.heading).setY(0).normalize());
                Quat4f verticalRotation = Quat4f.shortestArcQuat(new Vector3f(segmentVehicleComponent.heading).setY(0).normalize(), new Vector3f(segmentVehicleComponent.heading));
                verticalRotation.mul(horizontalRotation);
                location.setLocalRotation(verticalRotation);
                location.setWorldPosition(position);

                rigidBodyComponent.kinematic = true;
            } else {
                rigidBodyComponent.kinematic = false;
                railVehicle.removeComponent(SegmentVehicleComponent.class);
                rigidBodyComponent.velocity = new Vector3f(railVehicleComponent.velocity);
                rigidBodyComponent.collidesWith.add(StandardCollisionGroup.WORLD);
            }

            railVehicle.saveComponent(railVehicleComponent);
            railVehicle.saveComponent(rigidBodyComponent);
            railVehicle.saveComponent(location);
        }
    }


    @ReceiveEvent(components = {RailVehicleComponent.class, SegmentVehicleComponent.class, LocationComponent.class, RigidBodyComponent.class}, priority = EventPriority.PRIORITY_HIGH)
    public void onBump(CollideEvent event, EntityRef entity) {
        RailVehicleComponent v1 = entity.getComponent(RailVehicleComponent.class);
        RigidBodyComponent r1 = entity.getComponent(RigidBodyComponent.class);

        if (event.getOtherEntity().hasComponent(CharacterComponent.class)) {
            LocationComponent playerLocation = event.getOtherEntity().getComponent(LocationComponent.class);
            LocationComponent cartLocation = entity.getComponent(LocationComponent.class);

            Vector3f bumpForce = new Vector3f(cartLocation.getWorldPosition());
            bumpForce.sub(playerLocation.getWorldPosition());
            bumpForce.normalize();
            bumpForce.scale(15f);

            Vector3f tangent = segmentSystem.vehicleTangent(entity);// v1.trackSegment.getTangent(v1.t,v1.trackSegment.getRotation().getQuat4f(),v1.currentSegment);
            bumpForce = project(bumpForce, tangent);
            v1.velocity.add(bumpForce.div(r1.mass));
            entity.saveComponent(v1);

        } else if (event.getOtherEntity().hasComponent(RailVehicleComponent.class)) {
            if (!event.getOtherEntity().hasComponent(SegmentVehicleComponent.class))
                return;


            RailVehicleComponent v2 = event.getOtherEntity().getComponent(RailVehicleComponent.class);
            RigidBodyComponent r2 = event.getOtherEntity().getComponent(RigidBodyComponent.class);

            Vector3f v1n = segmentSystem.vehicleTangent(entity);
            Vector3f v2n = segmentSystem.vehicleTangent(event.getOtherEntity());

            LocationComponent v1l = entity.getComponent(LocationComponent.class);
            LocationComponent v2l = event.getOtherEntity().getComponent(LocationComponent.class);

            Vector3f halfNormal = new Vector3f(v1n);
            if (v1n.dot(v2n) < 0)
                halfNormal.invert();
            halfNormal.add(v2n).normalize();

            if (!(new Vector3f(v1l.getWorldPosition()).sub(v2l.getWorldPosition()).normalize().dot(halfNormal) < 0))
                halfNormal.invert();

            float jv = ((halfNormal.x * v1.velocity.x) + (halfNormal.y * v1.velocity.y) + (halfNormal.z * v1.velocity.z)) -
                    ((halfNormal.x * v2.velocity.x) + (halfNormal.y * v2.velocity.y) + (halfNormal.z * v2.velocity.z));

            Vector3f df = new Vector3f(v2l.getWorldPosition()).sub(v1l.getWorldPosition()).normalize();
            float b = -df.dot(halfNormal) * (BAUMGARTE_COFF / time.getGameDelta()) * event.getPenetration();

            if (jv <= 0)
                return;

            float effectiveMass = (1.0f / r1.mass) + (1.0f / r2.mass);

            float lambda = -(jv + b) / effectiveMass;

            Vector3f r1v = new Vector3f(halfNormal.x / r1.mass, halfNormal.y / r1.mass, halfNormal.z / r1.mass).mul(lambda);
            Vector3f r2v = new Vector3f(halfNormal.x / r2.mass, halfNormal.y / r2.mass, halfNormal.z / r2.mass).mul(lambda).invert();

            v1.velocity.add(r1v);
            v2.velocity.add(r2v);


            entity.saveComponent(v1);
            event.getOtherEntity().saveComponent(v2);
        }
    }

    private void bound(Vector3f v) {
        if (Float.isNaN(v.x) || Float.isInfinite(v.x))
            v.x = 0.0f;
        if (Float.isNaN(v.y) || Float.isInfinite(v.y))
            v.y = 0.0f;
        if (Float.isNaN(v.z) || Float.isInfinite(v.z))
            v.z = 0.0f;
    }

    public final Vector3f project(Vector3f u, Vector3f v) {
        if (v.lengthSquared() == 0)
            return Vector3f.zero();
        return new Vector3f(v).mul(new Vector3f(u).dot(v) / (v.lengthSquared()));
    }


    private void findTrackToAttachTo(EntityRef ref) {

    }

}