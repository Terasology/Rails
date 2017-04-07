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
package org.terasology.rails.minecarts.action;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.characters.MovementMode;
import org.terasology.logic.characters.events.SetMovementModeEvent;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.health.BeforeDestroyEvent;
import org.terasology.logic.health.DestroyEvent;
import org.terasology.logic.location.Location;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.geom.Quat4f;
import org.terasology.math.geom.Vector3f;
import org.terasology.physics.StandardCollisionGroup;
import org.terasology.physics.components.RigidBodyComponent;
import org.terasology.rails.minecarts.components.CartRidableComponent;
import org.terasology.rails.minecarts.components.RailVehicleComponent;

/**
 * Created by michaelpollind on 3/31/17.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class CartRidableAction extends BaseComponentSystem {

    @ReceiveEvent(components = {RailVehicleComponent.class,CartRidableComponent.class, LocationComponent.class})
    public void onUseFunctional(ActivateEvent event, EntityRef railVehicleEntity) {
        RailVehicleComponent railVehicleComponent = railVehicleEntity.getComponent(RailVehicleComponent.class);
        RigidBodyComponent railVehicleRigidBody = railVehicleEntity.getComponent(RigidBodyComponent.class);
        CartRidableComponent cartRidableComponent = railVehicleEntity.getComponent(CartRidableComponent.class);

        if (cartRidableComponent.characterInsideCart == null) {
            event.getInstigator().send(new SetMovementModeEvent(MovementMode.NONE));
            cartRidableComponent.characterInsideCart = event.getInstigator();
            Location.attachChild(railVehicleEntity, cartRidableComponent.characterInsideCart, new Vector3f(0, 1.5f, 0), new Quat4f());
            railVehicleRigidBody.collidesWith.remove(StandardCollisionGroup.CHARACTER);
            railVehicleRigidBody.collidesWith.remove(StandardCollisionGroup.DEFAULT);
        } else {
            event.getInstigator().send(new SetMovementModeEvent(MovementMode.WALKING));
            Location.removeChild(railVehicleEntity, cartRidableComponent.characterInsideCart);
            cartRidableComponent.characterInsideCart = null;
            railVehicleRigidBody.collidesWith.add(StandardCollisionGroup.CHARACTER);
            railVehicleRigidBody.collidesWith.add(StandardCollisionGroup.DEFAULT);
        }
        railVehicleEntity.saveComponent(cartRidableComponent);
        railVehicleEntity.saveComponent(railVehicleComponent);
        railVehicleEntity.saveComponent(railVehicleRigidBody);
    }

    @ReceiveEvent(components = {RailVehicleComponent.class,CartRidableComponent.class, LocationComponent.class})
    public void onCartDestroyed(BeforeDestroyEvent event, EntityRef railVehicleEntity)
    {
        RailVehicleComponent railVehicleComponent = railVehicleEntity.getComponent(RailVehicleComponent.class);
        RigidBodyComponent railVehicleRigidBody = railVehicleEntity.getComponent(RigidBodyComponent.class);
        CartRidableComponent cartRidableComponent = railVehicleEntity.getComponent(CartRidableComponent.class);
        if (cartRidableComponent.characterInsideCart != null) {
            event.getInstigator().send(new SetMovementModeEvent(MovementMode.WALKING));
            Location.removeChild(railVehicleEntity, cartRidableComponent.characterInsideCart);
            cartRidableComponent.characterInsideCart = null;
            railVehicleRigidBody.collidesWith.add(StandardCollisionGroup.CHARACTER);
            railVehicleRigidBody.collidesWith.add(StandardCollisionGroup.DEFAULT);
        }
        railVehicleEntity.saveComponent(cartRidableComponent);
        railVehicleEntity.saveComponent(railVehicleComponent);
        railVehicleEntity.saveComponent(railVehicleRigidBody);
    }

}
