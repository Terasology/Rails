// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.minecarts.controllers;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.OnActivatedComponent;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.location.Location;
import org.terasology.logic.location.LocationComponent;
import org.terasology.minecarts.components.CartWheelComponent;
import org.terasology.minecarts.components.RailVehicleComponent;
import org.terasology.minecarts.components.WheelDefinition;
import org.terasology.registry.In;
import org.terasology.registry.Share;
import org.terasology.rendering.logic.MeshComponent;
import org.terasology.segmentedpaths.components.PathFollowerComponent;
import org.terasology.segmentedpaths.controllers.PathFollowerSystem;

@RegisterSystem(RegisterMode.AUTHORITY)
@Share(CartWheelSystem.class)
public class CartWheelSystem  extends BaseComponentSystem implements UpdateSubscriberSystem {
    @In
    protected EntityManager entityManager;
    @In
    PathFollowerSystem pathFollowerSystem;


    @ReceiveEvent(components = {CartWheelComponent.class, RailVehicleComponent.class})
    public void activeWheelComponent(OnActivatedComponent event, EntityRef entity) {
        rebuild(entity);
    }

    private void rebuild(EntityRef cartEntity) {
        CartWheelComponent component = cartEntity.getComponent(CartWheelComponent.class);
        if (component != null) {
            for (EntityRef ref : component.targets) {
                ref.destroy();
            }
            component.targets.clear();
            for (WheelDefinition definition : component.wheels) {
                EntityRef wheel = entityManager.create(definition.prefab, new Vector3f());
                Location.attachChild(cartEntity, wheel);
                component.targets.add(wheel);
            }
            cartEntity.saveComponent(component);
        }
    }

    @Override
    public void update(float delta) {
        for (EntityRef railVehicle : entityManager.getEntitiesWith(RailVehicleComponent.class,
                CartWheelComponent.class)) {
            PathFollowerComponent segmentVehicleComponent = railVehicle.getComponent(PathFollowerComponent.class);
            CartWheelComponent wheelComponent = railVehicle.getComponent(CartWheelComponent.class);
            if (wheelComponent.wheels.size() != wheelComponent.targets.size() || segmentVehicleComponent == null) {
                continue;
            }
            for (int index = 0; index < wheelComponent.targets.size(); index++) {
                EntityRef wheel = wheelComponent.targets.get(index);
                LocationComponent wheelLocation = wheel.getComponent(LocationComponent.class);
                WheelDefinition wheelDefinition = wheelComponent.wheels.get(index);

                MeshComponent mesh = railVehicle.getComponent(MeshComponent.class);

                wheelLocation.setLocalPosition(0,mesh.mesh.getAABB().maxY() / 2.0f, wheelDefinition.offset);

                Vector3f tangent = pathFollowerSystem.vehicleTangent(railVehicle, wheelDefinition.offset, null);

                Vector3f horzY = new Vector3f(tangent);
                horzY.y = 0;
                horzY.normalize();

                Quaternionf horizontalRotation = new Quaternionf().rotateTo(new Vector3f(0, 0, 1), horzY);
                Quaternionf verticalRotation = new Quaternionf().rotateTo(horzY, segmentVehicleComponent.heading);

                verticalRotation.mul(horizontalRotation);
                wheelLocation.setLocalRotation(verticalRotation);

                wheel.saveComponent(wheelComponent);

            }
        }
    }
}
