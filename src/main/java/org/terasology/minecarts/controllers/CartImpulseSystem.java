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
import org.terasology.logic.characters.CharacterComponent;
import org.terasology.logic.characters.CharacterImpulseEvent;
import org.terasology.logic.characters.CharacterMovementComponent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.JomlUtil;
import org.terasology.math.geom.Vector3f;
import org.terasology.minecarts.Constants;
import org.terasology.minecarts.Util;
import org.terasology.minecarts.components.CartJointComponent;
import org.terasology.minecarts.components.CollisionFilterComponent;
import org.terasology.minecarts.components.RailVehicleComponent;
import org.terasology.physics.components.RigidBodyComponent;
import org.terasology.physics.events.CollideEvent;
import org.terasology.registry.In;
import org.terasology.segmentedpaths.components.PathFollowerComponent;
import org.terasology.segmentedpaths.controllers.PathFollowerSystem;

@RegisterSystem(RegisterMode.AUTHORITY)
public class CartImpulseSystem extends BaseComponentSystem  {

    @In
    Time time;
    @In
    PathFollowerSystem segmentSystem;


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


    @ReceiveEvent(components = {RailVehicleComponent.class, PathFollowerComponent.class, LocationComponent.class, RigidBodyComponent.class}, priority = EventPriority.PRIORITY_HIGH)
    public void onBump(CollideEvent event, EntityRef entity) {
        CollisionFilterComponent collisionFilterComponent = entity.getComponent(CollisionFilterComponent.class);
        if (collisionFilterComponent != null && collisionFilterComponent.filter.contains(event.getOtherEntity()))
            return;

        if (event.getOtherEntity().hasComponent(CharacterComponent.class)) {
            handleCharacterCollision(event, entity);
        } else if (event.getOtherEntity().hasComponent(RailVehicleComponent.class) && event.getOtherEntity().hasComponent(PathFollowerComponent.class)) {
            if (areJoinedTogether(entity, event.getOtherEntity())) {
                return;
            }

            this.handleCartCollision(event, entity);
        }
    }

    private boolean areJoinedTogether(EntityRef entity, EntityRef otherEntity) {
        if (!entity.hasComponent(CartJointComponent.class) || !otherEntity.hasComponent(CartJointComponent.class)) {
            return false;
        }

        CartJointComponent joint = entity.getComponent(CartJointComponent.class);
        if (joint.findJoint(otherEntity) != null) {
            return true;
        }
        return false;
    }


    private void handleCharacterCollision(CollideEvent event, EntityRef entity) {

        RailVehicleComponent v1 = entity.getComponent(RailVehicleComponent.class);

        LocationComponent v1l = entity.getComponent(LocationComponent.class);
        LocationComponent v2l = event.getOtherEntity().getComponent(LocationComponent.class);

        RigidBodyComponent r1 = entity.getComponent(RigidBodyComponent.class);
        CharacterMovementComponent r2 = event.getOtherEntity().getComponent(CharacterMovementComponent.class);

        float jv =event.getNormal().dot(v1.velocity) - event.getNormal().dot(JomlUtil.from(r2.getVelocity()));
        float effectiveMass = (1.0f / r1.mass) + (1.0f / Constants.PLAYER_MASS);

        Vector3f df = new Vector3f(v2l.getWorldPosition()).sub(v1l.getWorldPosition()).normalize();

        float b = -df.dot(JomlUtil.from(event.getNormal())) * (Constants.BAUMGARTE_COFF / time.getGameDelta()) * event.getPenetration();

        float lambda = -(jv + b) / effectiveMass;

        if (lambda < 0)
            return;

        Vector3f r1v = new Vector3f(event.getNormal().x / r1.mass, event.getNormal().y / r1.mass, event.getNormal().z / r1.mass).mul(lambda);
        Vector3f r2v = new Vector3f(event.getNormal().x / Constants.PLAYER_MASS, event.getNormal().y / Constants.PLAYER_MASS, event.getNormal().z / Constants.PLAYER_MASS).mul(lambda).invert();

        v1.velocity.add(JomlUtil.from(r1v));
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


        Vector3f df = new Vector3f(v2l.getWorldPosition()).sub(v1l.getWorldPosition()).add(new Vector3f(Float.MIN_VALUE,Float.MIN_VALUE,Float.MIN_VALUE)).normalize();

        //calculate the half normal vector
        Vector3f normal = new Vector3f(df);

        float jv = normal.dot(JomlUtil.from(v1.velocity)) - normal.dot (JomlUtil.from(v2.velocity));
        float b = -df.dot(normal) * (Constants.BAUMGARTE_COFF / time.getGameDelta()) * event.getPenetration();

        float effectiveMass = (1.0f / r1.mass) + (1.0f / r2.mass);
        float lambda = -(jv + b) / effectiveMass;
        if (lambda > 0)
            return;
        Vector3f r1v = new Vector3f(normal.x / r1.mass, normal.y / r1.mass, normal.z / r1.mass).mul(lambda);
        Vector3f r2v = new Vector3f(normal.x / r2.mass, normal.y / r2.mass, normal.z / r2.mass).mul(lambda).invert();

        Util.bound(r1v);
        Util.bound(r2v);

        v1.velocity.add(JomlUtil.from(r1v));
        v2.velocity.add(JomlUtil.from(r2v));


        entity.saveComponent(v1);
        event.getOtherEntity().saveComponent(v2);
    }

}
