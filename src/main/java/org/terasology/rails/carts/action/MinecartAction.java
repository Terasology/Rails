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
import org.terasology.asset.Assets;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.BeforeDeactivateComponent;
import org.terasology.entitySystem.event.EventPriority;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.input.binds.movement.ForwardsMovementAxis;
import org.terasology.input.binds.movement.VerticalMovementAxis;
import org.terasology.input.cameraTarget.CameraTargetChangedEvent;
import org.terasology.logic.characters.CharacterComponent;
import org.terasology.logic.characters.MovementMode;
import org.terasology.logic.characters.events.SetMovementModeEvent;
import org.terasology.logic.inventory.InventoryUtils;
import org.terasology.logic.inventory.action.GiveItemAction;
import org.terasology.logic.location.Location;
import org.terasology.network.ClientComponent;
import org.terasology.physics.HitResult;
import org.terasology.physics.Physics;
import org.terasology.physics.StandardCollisionGroup;
import org.terasology.physics.components.RigidBodyComponent;
import org.terasology.physics.engine.RigidBody;
import org.terasology.physics.events.ChangeVelocityEvent;
import org.terasology.physics.events.CollideEvent;
import org.terasology.physics.events.ForceEvent;
import org.terasology.physics.events.ImpulseEvent;
import org.terasology.rails.blocks.ConnectsToRailsComponent;
import org.terasology.rails.carts.components.WrenchComponent;
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
import org.terasology.rendering.logic.MeshComponent;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.items.BlockItemFactory;
import org.terasology.world.selection.BlockSelectionComponent;
import org.terasology.world.selection.event.SetBlockSelectionEndingPointEvent;

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
    @In
    private Physics physics;

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
        if (minecart == null || !minecart.isCreated) {
            return;
        }

        if (minecart == null || (minecart.characterInsideCart != null && minecart.characterInsideCart.equals(event.getOtherEntity()))) {
            return;
        }

        switch (minecart.type) {
            case minecart:
                onBumpMinecart(entity, event.getOtherEntity());
                break;
            case locomotive:
                onBumpLocomotive(entity, event.getOtherEntity());
                break;
        }
    }

    @ReceiveEvent
    public void onPlayerSpawn(OnPlayerSpawnedEvent event, EntityRef player, InventoryComponent inventory) {
        BlockItemFactory blockFactory = new BlockItemFactory(entityManager);
        inventoryManager.giveItem(player,player,entityManager.create("rails:minecart"));
        inventoryManager.giveItem(player,player,entityManager.create("rails:loco"));
        inventoryManager.giveItem(player,player,entityManager.create("rails:wrench"));
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

    @ReceiveEvent(components = {LocationComponent.class}, priority = EventPriority.PRIORITY_HIGH)
    public void onCamTargetChanged(CameraTargetChangedEvent event, EntityRef entity) {
        EntityRef oldTarget = event.getOldTarget();
        EntityRef newTarget = event.getNewTarget();
        CharacterComponent characterComponent = entity.getComponent(CharacterComponent.class);
        EntityRef heldItem = InventoryUtils.getItemAt(entity, characterComponent.selectedItem);

        if (!heldItem.hasComponent(WrenchComponent.class)) {
            return;
        }

        if (oldTarget.hasComponent(MinecartComponent.class)) {
            MinecartComponent minecartComponent = oldTarget.getComponent(MinecartComponent.class);
            if (minecartComponent.type.equals(MinecartComponent.Types.minecart)) {
                MeshComponent mesh = oldTarget.getComponent(MeshComponent.class);
                mesh.material = Assets.getMaterial("rails:minecart");
                for (EntityRef vehicle : minecartComponent.vehicles) {
                    MeshComponent meshVehicle = vehicle.getComponent(MeshComponent.class);
                    meshVehicle.material = Assets.getMaterial("rails:minecart");
                    vehicle.saveComponent(meshVehicle);
                }
                oldTarget.saveComponent(mesh);
            }
        }

        if (newTarget.hasComponent(MinecartComponent.class)) {
            MinecartComponent minecartComponent = newTarget.getComponent(MinecartComponent.class);
            if (minecartComponent.type.equals(MinecartComponent.Types.minecart)) {
                MeshComponent mesh = newTarget.getComponent(MeshComponent.class);
                LocationComponent location = newTarget.getComponent(LocationComponent.class);
                if (checkMineCartJoin(minecartComponent, location.getWorldPosition())) {
                    mesh.material = Assets.getMaterial("rails:minecart-join");
                    for (EntityRef vehicle : minecartComponent.vehicles) {
                        MeshComponent meshVehicle = vehicle.getComponent(MeshComponent.class);
                        meshVehicle.material = Assets.getMaterial("rails:minecart-join");
                        vehicle.saveComponent(meshVehicle);
                    }
                    newTarget.saveComponent(mesh);
                }
            }
        }
    }

    private boolean checkMineCartJoin(MinecartComponent minecartComponent, Vector3f position) {
        Vector3f pathDirection = new Vector3f(minecartComponent.pathDirection);
        pathDirection.y = 0;

        Vector3f pathDirectionNegate = new Vector3f(pathDirection);
        pathDirectionNegate.negate(pathDirection);
        Vector3f[] directions = {pathDirection, pathDirectionNegate};

        for (Vector3f dir : directions) {
            HitResult hit = physics.rayTrace(position, dir, 2.5f, StandardCollisionGroup.DEFAULT, StandardCollisionGroup.WORLD);
            EntityRef entity = hit.getEntity();

            if (entity.hasComponent(MinecartComponent.class)) {
                MinecartComponent mn = entity.getComponent(MinecartComponent.class);
                if (mn.type.equals(MinecartComponent.Types.locomotive)) {
                    return true;
                } else {
                    return true;
                }
            }
        }


        return false;
    }

    @ReceiveEvent(components = {MinecartComponent.class, LocationComponent.class})
    public void onUseFunctional(ActivateEvent event, EntityRef minecartEntity) {
        MinecartComponent minecartComponent = minecartEntity.getComponent(MinecartComponent.class);
        RigidBodyComponent minecartRigidBody = minecartEntity.getComponent(RigidBodyComponent.class);
        if (minecartComponent.type.equals(MinecartComponent.Types.minecart)) {
            if (minecartComponent.isCreated) {
                if (minecartComponent.characterInsideCart == null && (minecartComponent.pathDirection.x == 0 || minecartComponent.pathDirection.z == 0)) {
                   event.getInstigator().send(new SetMovementModeEvent(MovementMode.NONE));
                    minecartComponent.characterInsideCart = event.getInstigator();
                    Location.attachChild(minecartEntity, minecartComponent.characterInsideCart, new Vector3f(0,1.5f,0), new Quat4f());
                    minecartRigidBody.collidesWith.remove(StandardCollisionGroup.CHARACTER);
                    minecartRigidBody.collidesWith.remove(StandardCollisionGroup.DEFAULT);
                    minecartComponent.drive = 0;
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
        } else {
            if (minecartComponent.isCreated) {
                if (minecartComponent.drive > 0) {
                    minecartComponent.drive = 0;
                } else {
                    minecartComponent.drive = 2;
                }
                minecartEntity.saveComponent(minecartComponent);
            }
        }
    }

    private void onBumpLocomotive(EntityRef locomotive, EntityRef enity) {

    }

    private void onBumpMinecart(EntityRef minecart, EntityRef entity) {
        LocationComponent minecartLocation = minecart.getComponent(LocationComponent.class);
        LocationComponent location = entity.getComponent(LocationComponent.class);
        MinecartComponent minecartComponent = minecart.getComponent(MinecartComponent.class);
        MinecartComponent entityMinecartComponent = entity.getComponent(MinecartComponent.class);
        Vector3f diffPosition = new Vector3f(minecartLocation.getWorldPosition());
        diffPosition.sub(location.getWorldPosition());
        Vector3f forceDirection = new Vector3f(Math.signum(diffPosition.x), Math.signum(diffPosition.y), Math.signum(diffPosition.z));

        if (entity.hasComponent(CharacterComponent.class)) {
            forceDirection.scale(5f);
            minecart.send(new ImpulseEvent(forceDirection));
        } else if (entity.hasComponent(MinecartComponent.class)) {
            float forceScale = 10f;
            RigidBodyComponent rb = entity.getComponent(RigidBodyComponent.class);
            minecartComponent.direction.set(minecartComponent.pathDirection);
            MinecartHelper.setVectorToDirection(forceDirection, minecartComponent.pathDirection);
            MinecartHelper.setVectorToDirection(minecartComponent.direction, forceDirection);
            forceDirection.scale(forceScale);
            rb.velocity.scale(0.1f);
            forceDirection.y = 1f;
            minecart.send(new ImpulseEvent(forceDirection));
            entity.send(new ChangeVelocityEvent(rb.velocity));
        }
        minecart.saveComponent(minecartComponent);
    }
}
