// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.minecarts.action;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.ReceiveEvent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.characters.MovementMode;
import org.terasology.engine.logic.characters.events.SetMovementModeEvent;
import org.terasology.engine.logic.common.ActivateEvent;
import org.terasology.engine.logic.destruction.BeforeDestroyEvent;
import org.terasology.engine.logic.location.Location;
import org.terasology.engine.logic.location.LocationComponent;
import org.terasology.engine.physics.StandardCollisionGroup;
import org.terasology.engine.physics.components.RigidBodyComponent;
import org.terasology.math.geom.Quat4f;
import org.terasology.math.geom.Vector3f;
import org.terasology.minecarts.components.CartRidableComponent;
import org.terasology.minecarts.components.RidingCartComponent;
import org.terasology.minecarts.controllers.CartImpulseSystem;

/**
 * Created by michaelpollind on 3/31/17.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class CartRidableAction extends BaseComponentSystem {

    @ReceiveEvent(components = {CartRidableComponent.class, LocationComponent.class})
    public void onUseFunctional(ActivateEvent event, EntityRef entity) {
        CartRidableComponent cartRidableComponent = entity.getComponent(CartRidableComponent.class);

        if (cartRidableComponent.rider == null) {
            mount(entity, event.getInstigator());
        } else {
            dismount(entity, event.getInstigator());
        }
    }


    @ReceiveEvent(components = {CartRidableComponent.class, LocationComponent.class})
    public void onCartDestroyed(BeforeDestroyEvent event, EntityRef entity) {
        CartRidableComponent cartRidableComponent = entity.getComponent(CartRidableComponent.class);

        dismount(entity, cartRidableComponent.rider);
    }

    @ReceiveEvent(components = {RidingCartComponent.class})
    public void onRiderDestroyed(BeforeDestroyEvent event, EntityRef rider, RidingCartComponent ridingComponent) {
        dismount(ridingComponent.cart, rider);
    }

    private void mount(EntityRef cart, EntityRef rider) {
        CartRidableComponent cartRidableComponent = cart.getComponent(CartRidableComponent.class);
        RigidBodyComponent railVehicleRigidBody = cart.getComponent(RigidBodyComponent.class);

        if (rider.getComponent(RidingCartComponent.class) != null) {
            return;
        }
        if (cartRidableComponent.rider != null) {
            return;
        }
        rider.send(new SetMovementModeEvent(MovementMode.NONE));
        cartRidableComponent.rider = rider;
        Location.attachChild(cart, rider, new Vector3f(0, 1.5f, 0), new Quat4f());
        CartImpulseSystem.AddCollisionFilter(cart, rider);
        railVehicleRigidBody.collidesWith.remove(StandardCollisionGroup.CHARACTER);
        railVehicleRigidBody.collidesWith.remove(StandardCollisionGroup.DEFAULT);
        rider.addComponent(new RidingCartComponent(cart));

        cart.saveComponent(cartRidableComponent);
        cart.saveComponent(railVehicleRigidBody);
    }

    private void dismount(EntityRef cart, EntityRef rider) {
        CartRidableComponent cartRidableComponent = cart.getComponent(CartRidableComponent.class);
        if (rider == null || !rider.equals(cartRidableComponent.rider)) {
            return;
        }
        rider.removeComponent(RidingCartComponent.class);
        rider.send(new SetMovementModeEvent(MovementMode.WALKING));
        Location.removeChild(cart, rider);

        RigidBodyComponent railVehicleRigidBody = cart.getComponent(RigidBodyComponent.class);
        CartImpulseSystem.RemoveCollisionFilter(cart, rider);
        cartRidableComponent.rider = null;
        railVehicleRigidBody.collidesWith.add(StandardCollisionGroup.CHARACTER);
        railVehicleRigidBody.collidesWith.add(StandardCollisionGroup.DEFAULT);

        cart.saveComponent(cartRidableComponent);
        cart.saveComponent(railVehicleRigidBody);
    }
}
