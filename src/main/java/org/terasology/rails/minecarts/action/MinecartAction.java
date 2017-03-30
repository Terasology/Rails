/*
 * Copyright 2015 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.rails.minecarts.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.characters.MovementMode;
import org.terasology.logic.characters.events.SetMovementModeEvent;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.inventory.InventoryComponent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.logic.location.Location;
import org.terasology.logic.location.LocationComponent;
import org.terasology.logic.players.event.OnPlayerSpawnedEvent;
import org.terasology.math.geom.Quat4f;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector3i;
import org.terasology.physics.Physics;
import org.terasology.physics.StandardCollisionGroup;
import org.terasology.physics.components.RigidBodyComponent;
import org.terasology.rails.minecarts.blocks.RailComponent;
import org.terasology.rails.minecarts.components.RailVehicleComponent;
import org.terasology.rails.minecarts.components.WrenchComponent;
import org.terasology.rails.minecarts.controllers.MinecartFactory;
import org.terasology.registry.In;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.items.BlockItemFactory;

@RegisterSystem(RegisterMode.AUTHORITY)
public class MinecartAction extends BaseComponentSystem {
    @In
    private EntityManager entityManager;
    @In
    private WorldProvider worldProvider;
    @In
    private InventoryManager inventoryManager;
    @In
    private BlockManager blockManager;
    @In
    private Physics physics;

    private MinecartFactory railVehicleFactory;
    private final Logger logger = LoggerFactory.getLogger(MinecartAction.class);

    @Override
    public void initialise() {
        railVehicleFactory = new MinecartFactory();
        railVehicleFactory.setEntityManager(entityManager);
    }


    @ReceiveEvent
    public void onPlayerSpawn(OnPlayerSpawnedEvent event, EntityRef player, InventoryComponent inventory) {
        BlockItemFactory blockFactory = new BlockItemFactory(entityManager);
        inventoryManager.giveItem(player, player, entityManager.create("rails:minecart"));
        inventoryManager.giveItem(player, player, entityManager.create("rails:loco"));
        inventoryManager.giveItem(player, player, entityManager.create("rails:wrench"));
        inventoryManager.giveItem(player, player, blockFactory.newInstance(blockManager.getBlockFamily("rails:Rails"), 99));
    }

    @ReceiveEvent(components = {RailVehicleComponent.class, ItemComponent.class})
    public void onPlaceFunctional(ActivateEvent event, EntityRef item) {

        if (item.hasComponent(WrenchComponent.class)) {
            return;
        }

        RailVehicleComponent functionalItem = item.getComponent(RailVehicleComponent.class);

        EntityRef targetEntity = event.getTarget();
        BlockComponent blockComponent = targetEntity.getComponent(BlockComponent.class);
        if (!targetEntity.hasComponent(RailComponent.class))
            return;

        if (blockComponent == null) {
            return;
        }

        Vector3i placementPos = new Vector3i(event.getTarget().getComponent(BlockComponent.class).getPosition());
        placementPos.y += 0.2f;

        logger.info("Created vehicle at {}", placementPos);

        EntityRef entity = railVehicleFactory.create(placementPos.toVector3f(), functionalItem.type);
        event.consume();
    }

    @ReceiveEvent(components = {RailVehicleComponent.class, LocationComponent.class})
    public void onUseFunctional(ActivateEvent event, EntityRef railVehicleEntity) {
        RailVehicleComponent railVehicleComponent = railVehicleEntity.getComponent(RailVehicleComponent.class);
        RigidBodyComponent railVehicleRigidBody = railVehicleEntity.getComponent(RigidBodyComponent.class);
        if (railVehicleComponent.type.equals(RailVehicleComponent.Types.minecart)) {
            if (railVehicleComponent.isCreated) {
                if (railVehicleComponent.characterInsideCart == null) {
                    event.getInstigator().send(new SetMovementModeEvent(MovementMode.NONE));
                    railVehicleComponent.characterInsideCart = event.getInstigator();
                    Location.attachChild(railVehicleEntity, railVehicleComponent.characterInsideCart, new Vector3f(0, 1.5f, 0), new Quat4f());
                    railVehicleRigidBody.collidesWith.remove(StandardCollisionGroup.CHARACTER);
                    railVehicleRigidBody.collidesWith.remove(StandardCollisionGroup.DEFAULT);
                } else {
                    event.getInstigator().send(new SetMovementModeEvent(MovementMode.WALKING));
                    Location.removeChild(railVehicleEntity, railVehicleComponent.characterInsideCart);
                    railVehicleComponent.characterInsideCart = null;
                    railVehicleRigidBody.collidesWith.add(StandardCollisionGroup.CHARACTER);
                    railVehicleRigidBody.collidesWith.add(StandardCollisionGroup.DEFAULT);
                }
                railVehicleEntity.saveComponent(railVehicleComponent);
                railVehicleEntity.saveComponent(railVehicleRigidBody);
            }
        } else {
            if (railVehicleComponent.isCreated) {
                /*if (railVehicleComponent.drive > 0) {
                    railVehicleComponent.drive = 0;
                } else {
                    railVehicleComponent.drive = 5;
                }*/
                railVehicleEntity.saveComponent(railVehicleComponent);
            }

        }
    }

}
