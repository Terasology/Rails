/*
 * Copyright 2014 MovingBlocks
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
package org.terasology.rails.carts.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.BeforeDeactivateComponent;
import org.terasology.entitySystem.event.EventPriority;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.input.binds.movement.ForwardsMovementAxis;
import org.terasology.input.binds.movement.VerticalMovementAxis;
import org.terasology.logic.characters.CharacterComponent;
import org.terasology.logic.characters.MovementMode;
import org.terasology.logic.characters.events.SetMovementModeEvent;
import org.terasology.logic.inventory.action.GiveItemAction;
import org.terasology.logic.location.Location;
import org.terasology.network.ClientComponent;
import org.terasology.physics.StandardCollisionGroup;
import org.terasology.physics.components.RigidBodyComponent;
import org.terasology.physics.events.ChangeVelocityEvent;
import org.terasology.physics.events.CollideEvent;
import org.terasology.physics.events.ForceEvent;
import org.terasology.physics.events.ImpulseEvent;
import org.terasology.rails.blocks.ConnectsToRailsComponent;
import org.terasology.rails.carts.utils.MinecartHelper;
import org.terasology.registry.In;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.inventory.InventoryComponent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.logic.players.event.OnPlayerSpawnedEvent;
import org.terasology.math.Vector3i;
import org.terasology.rails.carts.components.MinecartComponent;
import org.terasology.rails.carts.controllers.MinecartFactory;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.items.BlockItemFactory;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

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
    private LocalPlayer localPlayer;

    private MinecartFactory minecartFactory;
    private final Logger logger = LoggerFactory.getLogger(MinecartAction.class);

    @Override
    public void initialise() {
        minecartFactory = new MinecartFactory();
        minecartFactory.setEntityManager(entityManager);
    }

    @ReceiveEvent(components = {MinecartComponent.class, LocationComponent.class}, priority = EventPriority.PRIORITY_HIGH)
    public void onBump(CollideEvent event, EntityRef entity) {
        MinecartComponent minecart = entity.getComponent(MinecartComponent.class);
        LocationComponent minecartLocation = entity.getComponent(LocationComponent.class);
        if (minecart == null || (minecart.characterInsideCart != null && minecart.characterInsideCart.equals(event.getOtherEntity()))) {
            return;
        }
        EntityRef other = event.getOtherEntity();
        LocationComponent location = other.getComponent(LocationComponent.class);
        Vector3f bumpForce = new Vector3f(minecartLocation.getWorldPosition());
        bumpForce.sub(location.getWorldPosition());
        bumpForce.normalize();
        float bumpScale = 80f;
        minecart.direction.set(minecart.pathDirection);
        MinecartHelper.setVectorToDirection(bumpForce, minecart.pathDirection);
        MinecartHelper.setVectorToDirection(minecart.direction, bumpForce);

        if (other.hasComponent(CharacterComponent.class)) {
            bumpForce.scale(5f);
            entity.send(new ImpulseEvent(bumpForce));
        } else {
            bumpForce.scale(bumpScale);
            entity.send(new ForceEvent(bumpForce));
        }

        logger.info("Sended force: " + bumpForce + " minecart direction: " + minecart.direction + " minecart psth direction: " + minecart.pathDirection);
        entity.saveComponent(minecart);
    }


    @ReceiveEvent
    public void onPlayerSpawn(OnPlayerSpawnedEvent event, EntityRef player, InventoryComponent inventory) {
        BlockItemFactory blockFactory = new BlockItemFactory(entityManager);
        inventoryManager.giveItem(player,player,entityManager.create("rails:minecart"));
        inventoryManager.giveItem(player,player,blockFactory.newInstance(blockManager.getBlockFamily("rails:Rails"), 99));
    }

    @ReceiveEvent(components = {MinecartComponent.class, LocationComponent.class}, priority = EventPriority.PRIORITY_HIGH)
    public void onDestroyMinecart(BeforeDeactivateComponent event, EntityRef entity) {
        logger.info("Destroy minecart");
        MinecartComponent minecart = entity.getComponent(MinecartComponent.class);

        if (minecart.characterInsideCart != null) {
            Location.removeChild(entity, minecart.characterInsideCart);
            minecart.characterInsideCart.send(new SetMovementModeEvent(MovementMode.WALKING));
            minecart.characterInsideCart = null;
        }

        for (EntityRef vehicle : minecart.vehicles) {
            if (vehicle != null && !vehicle.equals(EntityRef.NULL)) {
                vehicle.destroy();
            }
        }

        entity.saveComponent(minecart);
    }

    @ReceiveEvent(components = {MinecartComponent.class, ItemComponent.class})
    public void onPlaceFunctional(ActivateEvent event, EntityRef item) {
        MinecartComponent functionalItem = item.getComponent(MinecartComponent.class);

        if (functionalItem.isCreated) {
            return;
        }

        EntityRef targetEntity = event.getTarget();
        BlockComponent blockComponent = targetEntity.getComponent(BlockComponent.class);
        ConnectsToRailsComponent connectsToRailsComponent = targetEntity.getComponent(ConnectsToRailsComponent.class);

        if (blockComponent == null || connectsToRailsComponent == null || !connectsToRailsComponent.type.equals(ConnectsToRailsComponent.RAILS.PLANE)) {
            return;
        }

        Vector3i placementPos = new Vector3i(event.getTarget().getComponent(BlockComponent.class).getPosition());
        placementPos.y += 0.2f;

        logger.info("Created minecart at {}", placementPos);

        EntityRef entity = minecartFactory.create(placementPos.toVector3f(), functionalItem.type);
        event.consume();
    }

    @ReceiveEvent(components = {MinecartComponent.class, LocationComponent.class})
    public void onUseFunctional(ActivateEvent event, EntityRef minecartEntity) {
        MinecartComponent minecartComponent = minecartEntity.getComponent(MinecartComponent.class);
        RigidBodyComponent minecartRigidBody = minecartEntity.getComponent(RigidBodyComponent.class);
        if (minecartComponent.isCreated) {
            if (minecartComponent.characterInsideCart == null && (minecartComponent.pathDirection.x == 0 || minecartComponent.pathDirection.z == 0)) {
                event.getInstigator().send(new SetMovementModeEvent(MovementMode.NONE));
                minecartComponent.characterInsideCart = event.getInstigator();
                Location.attachChild(minecartEntity, minecartComponent.characterInsideCart, new Vector3f(0,1.5f,0), new Quat4f());
                minecartRigidBody.collidesWith.remove(StandardCollisionGroup.CHARACTER);
                minecartRigidBody.collidesWith.remove(StandardCollisionGroup.DEFAULT);
                minecartComponent.drive = 5;
            } else {
                event.getInstigator().send(new SetMovementModeEvent(MovementMode.WALKING));
                Location.removeChild(minecartEntity, minecartComponent.characterInsideCart);
                minecartComponent.characterInsideCart = null;
                minecartRigidBody.collidesWith.add(StandardCollisionGroup.CHARACTER);
                minecartRigidBody.collidesWith.add(StandardCollisionGroup.DEFAULT);
                minecartComponent.drive = 0;
            }
            minecartEntity.saveComponent(minecartComponent);
            minecartEntity.saveComponent(minecartRigidBody);
        }
    }
}
