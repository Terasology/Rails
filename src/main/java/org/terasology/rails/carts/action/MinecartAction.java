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
import org.terasology.entitySystem.systems.ComponentSystem;
import org.terasology.logic.characters.CharacterComponent;
import org.terasology.logic.inventory.action.GiveItemAction;
import org.terasology.logic.location.Location;
import org.terasology.physics.components.RigidBodyComponent;
import org.terasology.physics.events.ChangeVelocityEvent;
import org.terasology.physics.events.CollideEvent;
import org.terasology.physics.events.ImpulseEvent;
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
import org.terasology.math.Side;
import org.terasology.math.Vector3i;
import org.terasology.rails.carts.components.MinecartComponent;
import org.terasology.rails.carts.controllers.MinecartFactory;
import org.terasology.rendering.logic.MeshComponent;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.items.BlockItemFactory;

import javax.vecmath.Vector3f;

@RegisterSystem(RegisterMode.AUTHORITY)
public class MinecartAction implements ComponentSystem {
    @In
    private EntityManager entityManager;
    @In
    private WorldProvider worldProvider;
    @In
    private InventoryManager inventoryManager;

    private MinecartFactory minecartFactory;

    @In
    private BlockManager blockManager;

    @In
    private LocalPlayer localPlayer;

    private final Logger logger = LoggerFactory.getLogger(MinecartAction.class);

    @Override
    public void initialise() {
        minecartFactory = new MinecartFactory();
        minecartFactory.setEntityManager(entityManager);
    }

    @Override
    public void preBegin() {

    }

    @Override
    public void postBegin() {

    }

    @Override
    public void preSave() {

    }

    @Override
    public void postSave() {

    }

    @Override
    public void shutdown() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @ReceiveEvent(components = {MinecartComponent.class, LocationComponent.class}, priority = EventPriority.PRIORITY_HIGH)
    public void onBump(CollideEvent event, EntityRef entity) {
       // logger.info("Bump event");

        MinecartComponent minecart = entity.getComponent(MinecartComponent.class);
        LocationComponent minecartLocation = entity.getComponent(LocationComponent.class);
        EntityRef other = event.getOtherEntity();

        if (other.hasComponent(CharacterComponent.class)) {
            LocationComponent location = other.getComponent(LocationComponent.class);
            Vector3f bumpForce = new Vector3f(minecartLocation.getWorldPosition());
            bumpForce.sub(location.getWorldPosition());
            bumpForce.normalize();
            bumpForce.scale(5f);
            bumpForce.x *= minecart.moveDescriptor.getPathDirection().x;
            bumpForce.y *= minecart.moveDescriptor.getPathDirection().y;
            bumpForce.z *= minecart.moveDescriptor.getPathDirection().z;
            entity.send(new ImpulseEvent(bumpForce));
           // logger.info("Send bump force: " + bumpForce);
        } else {
            RigidBodyComponent rb = entity.getComponent(RigidBodyComponent.class);
            Vector3f velocity = new Vector3f(rb.velocity);

            if ( velocity.x > 1 || velocity.z > 1) {
                velocity.x *= minecart.moveDescriptor.getPathDirection().x;
                velocity.y *= minecart.moveDescriptor.getPathDirection().y;
                velocity.z *= minecart.moveDescriptor.getPathDirection().z;
                entity.send(new ChangeVelocityEvent(velocity));
              //  logger.info("Send change velocity: " + velocity);
            }
        }
    }


    @ReceiveEvent
    public void onPlayerSpawn(OnPlayerSpawnedEvent event, EntityRef player, InventoryComponent inventory) {
        BlockItemFactory blockFactory = new BlockItemFactory(entityManager);
        GiveItemAction action = new GiveItemAction(localPlayer.getCharacterEntity(), entityManager.create("rails:minecart"));
        player.send(new GiveItemAction(EntityRef.NULL, blockFactory.newInstance(blockManager.getBlockFamily("rails:Rails"), 99)));
        player.send(new GiveItemAction(EntityRef.NULL, blockFactory.newInstance(blockManager.getBlockFamily("rails:RailsSlope"), 99)));
        localPlayer.getCharacterEntity().send(action);
    }

    @ReceiveEvent(components = {MinecartComponent.class, LocationComponent.class}, priority = EventPriority.PRIORITY_HIGH)
    public void onDestroyMinecart(BeforeDeactivateComponent event, EntityRef entity) {
        logger.info("Destroy minecart");
        MinecartComponent minecart = entity.getComponent(MinecartComponent.class);
        minecart.moveDescriptor = null;
        for (EntityRef vehicle : minecart.vehicles) {
            Location.removeChild(entity, vehicle);
            if (vehicle != null && !vehicle.equals(EntityRef.NULL)) {
                vehicle.destroy();
            }
        }

        entity.saveComponent(minecart);
    }

    @ReceiveEvent(components = {MinecartComponent.class, ItemComponent.class})
    public void onPlaceFunctional(ActivateEvent event, EntityRef item) {
        logger.info("onPlaceFunctional");

        MinecartComponent functionalItem = item.getComponent(MinecartComponent.class);

        Side surfaceDir = Side.inDirection(event.getHitNormal());

        EntityRef targetEntity = event.getTarget();

        BlockComponent blockComponent = targetEntity.getComponent(BlockComponent.class);

        if (blockComponent == null) {
            return;
        }

        Vector3i placementPos = new Vector3i(event.getTarget().getComponent(BlockComponent.class).getPosition());

        if (!worldProvider.getBlock(placementPos).isPenetrable()) {
            placementPos.add(surfaceDir.getVector3i());
        }

        placementPos.y += item.getComponent(MeshComponent.class).mesh.getAABB().maxY();

        logger.info("Created minecart at {}", placementPos);

        EntityRef entity = minecartFactory.create(placementPos.toVector3f(), functionalItem.type);
    }

    @ReceiveEvent(components = {MinecartComponent.class, LocationComponent.class})
    public void onUseFunctional(ActivateEvent event, EntityRef minecartEntity) {
        logger.info("onUseFunctional");
        MinecartComponent minecartComponent = minecartEntity.getComponent(MinecartComponent.class);

        if(minecartComponent.isCreated) {
            minecartComponent.moveDescriptor.setDrive(new Vector3f(10f,0,10f));
            minecartEntity.saveComponent(minecartComponent);
        }
    }

}
