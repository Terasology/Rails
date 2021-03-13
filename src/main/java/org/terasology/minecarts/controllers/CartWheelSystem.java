// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.minecarts.controllers;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.entity.lifecycleEvents.OnAddedComponent;
import org.terasology.engine.entitySystem.event.ReceiveEvent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.engine.logic.location.Location;
import org.terasology.engine.logic.location.LocationComponent;
import org.terasology.engine.registry.In;
import org.terasology.engine.rendering.logic.MeshComponent;
import org.terasology.engine.world.BlockEntityRegistry;
import org.terasology.minecarts.Util;
import org.terasology.minecarts.blocks.RailBlockSegmentMapper;
import org.terasology.minecarts.components.CartWheelComponent;
import org.terasology.minecarts.components.RailVehicleComponent;
import org.terasology.minecarts.components.WheelDefinition;
import org.terasology.segmentedpaths.components.PathFollowerComponent;
import org.terasology.segmentedpaths.controllers.PathFollowerSystem;
import org.terasology.segmentedpaths.controllers.SegmentCacheSystem;
import org.terasology.segmentedpaths.controllers.SegmentSystem;

@RegisterSystem(RegisterMode.CLIENT)
public class CartWheelSystem  extends BaseComponentSystem implements UpdateSubscriberSystem {
    private static final Logger logger = LoggerFactory.getLogger(CartWheelSystem.class);

    @In
    protected EntityManager entityManager;
    @In
    protected PathFollowerSystem pathFollowerSystem;
    @In
    protected SegmentSystem segmentSystem;
    @In
    protected SegmentCacheSystem segmentCacheSystem;
    @In
    protected BlockEntityRegistry blockEntityRegistry;

    private RailBlockSegmentMapper segmentMapping;

    @Override
    public void initialise() {
        segmentMapping = new RailBlockSegmentMapper(blockEntityRegistry, pathFollowerSystem, segmentSystem,
                segmentCacheSystem);
    }

    @ReceiveEvent
    public void activeWheelComponent(OnAddedComponent event, EntityRef entity, CartWheelComponent cartWheelComponent, RailVehicleComponent railVehicleComponent) {
        logger.info("wheels refreshing {}", entity);
        for (EntityRef ref : cartWheelComponent.targets) {
            ref.destroy();
        }
        cartWheelComponent.targets.clear();
        for (WheelDefinition definition : cartWheelComponent.wheels) {
            EntityRef wheel = entityManager.create(definition.prefab, new Vector3f());
            Location.attachChild(entity, wheel);
            cartWheelComponent.targets.add(wheel);
        }
        entity.saveComponent(cartWheelComponent);

    }

    @Override
    public void update(float delta) {
        for (EntityRef railVehicle : entityManager.getEntitiesWith(RailVehicleComponent.class,
                CartWheelComponent.class)) {
            PathFollowerComponent segmentVehicleComponent = railVehicle.getComponent(PathFollowerComponent.class);
            CartWheelComponent wheelComponent = railVehicle.getComponent(CartWheelComponent.class);
            MeshComponent meshComponent = railVehicle.getComponent(MeshComponent.class);
            LocationComponent locationComponent = railVehicle.getComponent(LocationComponent.class);

            if (wheelComponent.wheels.size() != wheelComponent.targets.size() || segmentVehicleComponent == null || meshComponent == null) {
                continue;
            }
            for (int index = 0; index < wheelComponent.targets.size(); index++) {
                EntityRef wheel = wheelComponent.targets.get(index);
                if (!wheel.exists()) {
                    break;
                }
                LocationComponent wheelLocation = wheel.getComponent(LocationComponent.class);
                WheelDefinition wheelDefinition = wheelComponent.wheels.get(index);

                wheelLocation.setLocalPosition(0, wheelDefinition.voffset, wheelDefinition.offset);
                Vector3f tangent = pathFollowerSystem.vehicleTangent(railVehicle, wheelDefinition.offset, segmentMapping);
                if (tangent == null) {
                    wheelLocation.setLocalRotation(new Quaternionf());
                } else {
                    float sign = Math.signum(locationComponent.getWorldDirection(new Vector3f()).dot(tangent));
                    wheelLocation.setWorldRotation(Util.rotation(tangent.mul(sign)));
                }

                wheel.saveComponent(wheelLocation);

            }
        }
    }
}
