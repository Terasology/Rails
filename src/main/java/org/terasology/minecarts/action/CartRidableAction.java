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
package org.terasology.minecarts.action;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.characters.CharacterComponent;
import org.terasology.logic.characters.CharacterMovementComponent;
import org.terasology.logic.characters.MovementMode;
import org.terasology.logic.characters.events.CollisionEvent;
import org.terasology.logic.characters.events.SetMovementModeEvent;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.health.BeforeDestroyEvent;
import org.terasology.logic.location.Location;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.geom.Quat4f;
import org.terasology.math.geom.Vector3f;
import org.terasology.minecarts.components.CollisionFilterComponent;
import org.terasology.minecarts.controllers.CartImpulseSystem;
import org.terasology.physics.CollisionGroup;
import org.terasology.physics.StandardCollisionGroup;
import org.terasology.physics.components.RigidBodyComponent;
import org.terasology.minecarts.components.CartRidableComponent;
import org.terasology.minecarts.components.RailVehicleComponent;
import org.terasology.physics.events.CollideEvent;

/**
 * Created by michaelpollind on 3/31/17.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class CartRidableAction extends BaseComponentSystem {

    @ReceiveEvent(components = {CartRidableComponent.class, LocationComponent.class})
    public void onUseFunctional(ActivateEvent event, EntityRef entity) {
        CartRidableComponent cartRidableComponent = entity.getComponent(CartRidableComponent.class);
        RigidBodyComponent railVehicleRigidBody = entity.getComponent(RigidBodyComponent.class);

        if (cartRidableComponent.rider == null) {
            event.getInstigator().send(new SetMovementModeEvent(MovementMode.NONE));
            cartRidableComponent.rider = event.getInstigator();
            Location.attachChild(entity, cartRidableComponent.rider, new Vector3f(0, 1.5f, 0), new Quat4f());
            CartImpulseSystem.AddCollisionFilter(entity, cartRidableComponent.rider);
            railVehicleRigidBody.collidesWith.remove(StandardCollisionGroup.CHARACTER);
            railVehicleRigidBody.collidesWith.remove(StandardCollisionGroup.DEFAULT);
        } else {
            event.getInstigator().send(new SetMovementModeEvent(MovementMode.WALKING));
            Location.removeChild(entity, cartRidableComponent.rider);
            CartImpulseSystem.RemoveCollisionFilter(entity, cartRidableComponent.rider);
            cartRidableComponent.rider = null;
            railVehicleRigidBody.collidesWith.add(StandardCollisionGroup.CHARACTER);
            railVehicleRigidBody.collidesWith.add(StandardCollisionGroup.DEFAULT);
        }
        entity.saveComponent(cartRidableComponent);
        entity.saveComponent(railVehicleRigidBody);
    }


    @ReceiveEvent(components = {CartRidableComponent.class, LocationComponent.class})
    public void onCartDestroyed(BeforeDestroyEvent event, EntityRef entity) {
        CartRidableComponent cartRidableComponent = entity.getComponent(CartRidableComponent.class);
        RigidBodyComponent railVehicleRigidBody = entity.getComponent(RigidBodyComponent.class);

        if (cartRidableComponent.rider != null) {
            event.getInstigator().send(new SetMovementModeEvent(MovementMode.WALKING));
            Location.removeChild(entity, cartRidableComponent.rider);
            CartImpulseSystem.RemoveCollisionFilter(entity, cartRidableComponent.rider);
            cartRidableComponent.rider = null;
            railVehicleRigidBody.collidesWith.add(StandardCollisionGroup.CHARACTER);
            railVehicleRigidBody.collidesWith.add(StandardCollisionGroup.DEFAULT);
        }
        entity.saveComponent(cartRidableComponent);
        entity.saveComponent(railVehicleRigidBody);
    }

}
