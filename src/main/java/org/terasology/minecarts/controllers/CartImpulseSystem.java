/*
 * Copyright 2017 MovingBlocks
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

import org.terasology.engine.Time;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.EventPriority;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.characters.CharacterComponent;
import org.terasology.logic.characters.CharacterImpulseEvent;
import org.terasology.logic.characters.CharacterMovementComponent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.geom.Vector3f;
import org.terasology.minecarts.Constants;
import org.terasology.minecarts.Util;
import org.terasology.minecarts.components.CollisionFilterComponent;
import org.terasology.minecarts.components.RailVehicleComponent;
import org.terasology.physics.components.RigidBodyComponent;
import org.terasology.physics.events.CollideEvent;
import org.terasology.registry.In;
import org.terasology.segmentedpaths.components.SegmentEntityComponent;
import org.terasology.segmentedpaths.controllers.SegmentSystem;

/**
 * Created by michaelpollind on 7/15/17.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class CartImpulseSystem extends BaseComponentSystem implements UpdateSubscriberSystem {

    @In
    Time time;
    @In
    SegmentSystem segmentSystem;

    public static void AddCollisionFilter(EntityRef cart, EntityRef child) {
        CollisionFilterComponent collisionFilterComponent = cart.getComponent(CollisionFilterComponent.class);
        if (collisionFilterComponent == null)
            collisionFilterComponent = new CollisionFilterComponent();
        collisionFilterComponent.filter.add(child);
        cart.addOrSaveComponent(collisionFilterComponent);
    }


    public static void RemoveCollisionFilter(EntityRef cart, EntityRef child) {
        CollisionFilterComponent collisionFilterComponent = cart.getComponent(CollisionFilterComponent.class);
        if (collisionFilterComponent == null)
            collisionFilterComponent = new CollisionFilterComponent();
        collisionFilterComponent.filter.remove(child);
        cart.addOrSaveComponent(collisionFilterComponent);
    }


    @ReceiveEvent(components = {RailVehicleComponent.class, SegmentEntityComponent.class, LocationComponent.class, RigidBodyComponent.class}, priority = EventPriority.PRIORITY_HIGH)
    public void onBump(CollideEvent event, EntityRef entity) {
        CollisionFilterComponent collisionFilterComponent = entity.getComponent(CollisionFilterComponent.class);
        if (collisionFilterComponent != null && collisionFilterComponent.filter.contains(event.getOtherEntity()))
            return;

        if (event.getOtherEntity().hasComponent(CharacterComponent.class)) {
            handleCharacterCollision(event, entity);
        } else if (event.getOtherEntity().hasComponent(RailVehicleComponent.class) && event.getOtherEntity().hasComponent(SegmentEntityComponent.class)) {
            this.handleCartCollision(event, entity);
        }
    }


    private void handleCharacterCollision(CollideEvent event, EntityRef entity) {

        RailVehicleComponent v1 = entity.getComponent(RailVehicleComponent.class);

        LocationComponent v1l = entity.getComponent(LocationComponent.class);
        LocationComponent v2l = event.getOtherEntity().getComponent(LocationComponent.class);

        RigidBodyComponent r1 = entity.getComponent(RigidBodyComponent.class);
        CharacterMovementComponent r2 = event.getOtherEntity().getComponent(CharacterMovementComponent.class);

        float jv = ((event.getNormal().x * v1.velocity.x) + (event.getNormal().y * v1.velocity.y) + (event.getNormal().z * v1.velocity.z)) -
                ((event.getNormal().x * r2.getVelocity().x) + (event.getNormal().y * r2.getVelocity().y) + (event.getNormal().z * r2.getVelocity().z));


        float effectiveMass = (1.0f / r1.mass) + (1.0f / Constants.PLAYER_MASS);

        Vector3f df = new Vector3f(v2l.getWorldPosition()).sub(v1l.getWorldPosition()).normalize();

        float b = -df.dot(event.getNormal()) * (Constants.BAUMGARTE_COFF / time.getGameDelta()) * event.getPenetration();

        float lambda = -(jv + b) / effectiveMass;

        if (lambda < 0)
            return;

        Vector3f r1v = new Vector3f(event.getNormal().x / r1.mass, event.getNormal().y / r1.mass, event.getNormal().z / r1.mass).mul(lambda);
        Vector3f r2v = new Vector3f(event.getNormal().x / Constants.PLAYER_MASS, event.getNormal().y / Constants.PLAYER_MASS, event.getNormal().z / Constants.PLAYER_MASS).mul(lambda).invert();

        v1.velocity.add(r1v);
        event.getOtherEntity().send(new CharacterImpulseEvent(r2v));

        entity.saveComponent(v1);
    }


    private void handleCartCollision(CollideEvent event, EntityRef entity) {
        RailVehicleComponent v1 = entity.getComponent(RailVehicleComponent.class);
        RailVehicleComponent v2 = event.getOtherEntity().getComponent(RailVehicleComponent.class);

        RigidBodyComponent r1 = entity.getComponent(RigidBodyComponent.class);
        RigidBodyComponent r2 = event.getOtherEntity().getComponent(RigidBodyComponent.class);

        LocationComponent v1l = entity.getComponent(LocationComponent.class);
        LocationComponent v2l = event.getOtherEntity().getComponent(LocationComponent.class);


        Vector3f df = new Vector3f(v2l.getWorldPosition()).sub(v1l.getWorldPosition()).add(new Vector3f(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)).normalize();

        //calculate the half normal vector
        Vector3f halfNormal = new Vector3f(df);

        float jv = ((halfNormal.x * v1.velocity.x) + (halfNormal.y * v1.velocity.y) + (halfNormal.z * v1.velocity.z)) -
                ((halfNormal.x * v2.velocity.x) + (halfNormal.y * v2.velocity.y) + (halfNormal.z * v2.velocity.z));

        float b = -df.dot(halfNormal) * (Constants.BAUMGARTE_COFF / time.getGameDelta()) * event.getPenetration();

        float effectiveMass = (1.0f / r1.mass) + (1.0f / r2.mass);
        float lambda = -(jv + b) / effectiveMass;
        if (lambda > 0)
            return;
        Vector3f r1v = new Vector3f(halfNormal.x / r1.mass, halfNormal.y / r1.mass, halfNormal.z / r1.mass).mul(lambda);
        Vector3f r2v = new Vector3f(halfNormal.x / r2.mass, halfNormal.y / r2.mass, halfNormal.z / r2.mass).mul(lambda).invert();

        Util.bound(r1v);
        Util.bound(r2v);

        v1.velocity.add(r1v);
        v2.velocity.add(r2v);


        entity.saveComponent(v1);
        event.getOtherEntity().saveComponent(v2);
    }

    @Override
    public void update(float delta) {

    }
}
