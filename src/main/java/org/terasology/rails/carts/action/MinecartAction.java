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
import org.terasology.logic.characters.events.AttackRequest;
import org.terasology.logic.characters.events.SetMovementModeEvent;
import org.terasology.logic.health.DoDamageEvent;
import org.terasology.logic.inventory.InventoryUtils;
import org.terasology.logic.inventory.action.GiveItemAction;
import org.terasology.logic.location.Location;
import org.terasology.logic.players.event.SelectedItemChangedEvent;
import org.terasology.math.Side;
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
import org.terasology.rails.carts.components.LocomotiveComponent;
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
        MinecartComponent minecartComponent = entity.getComponent(MinecartComponent.class);

        switch(minecartComponent.type) {
            case locomotive:
                LocomotiveComponent locomotiveComponent = entity.getComponent(LocomotiveComponent.class);

                for(EntityRef minecart: locomotiveComponent.childs) {
                    MinecartComponent childMinecartComponent = minecart.getComponent(MinecartComponent.class);
                    childMinecartComponent.locomotiveRef = null;
                    childMinecartComponent.parentNode = null;
                    childMinecartComponent.childNode = null;
                    minecart.saveComponent(childMinecartComponent);
                }

                break;
            case minecart:

                if (minecartComponent.childNode != null) {
                    MinecartComponent childMinecartComponent = minecartComponent.childNode.getComponent(MinecartComponent.class);
                    childMinecartComponent.parentNode = minecartComponent.parentNode;
                    minecartComponent.childNode.saveComponent(childMinecartComponent);
                }

                break;
        }

        if (minecartComponent.characterInsideCart != null) {
            Location.removeChild(entity, minecartComponent.characterInsideCart);
            minecartComponent.characterInsideCart.send(new SetMovementModeEvent(MovementMode.WALKING));
            minecartComponent.characterInsideCart = null;
        }

        for (EntityRef vehicle : minecartComponent.vehicles) {
            if (vehicle != null && !vehicle.equals(EntityRef.NULL)) {
                vehicle.destroy();
            }
        }

        entity.saveComponent(minecartComponent);
    }

    @ReceiveEvent(components = {MinecartComponent.class, ItemComponent.class})
    public void onPlaceFunctional(ActivateEvent event, EntityRef item) {

        if (item.hasComponent(WrenchComponent.class)) {
            return;
        }

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

    @ReceiveEvent(components = {WrenchComponent.class, ItemComponent.class})
    public void joinMinecart(ActivateEvent event, EntityRef item) {
        EntityRef targetEntity = event.getTarget();
        if (targetEntity.hasComponent(MinecartComponent.class)) {
            MinecartComponent minecartComponent = targetEntity.getComponent(MinecartComponent.class);
            if (minecartComponent.type.equals(MinecartComponent.Types.minecart)) {
                LocationComponent location = targetEntity.getComponent(LocationComponent.class);
                if (minecartComponent.parentNode != null) {
                    if (minecartComponent.childNode == null) {
                        LocomotiveComponent loco = minecartComponent.locomotiveRef.getComponent(LocomotiveComponent.class);
                        loco.childs.remove(targetEntity);
                        minecartComponent.locomotiveRef.saveComponent(loco);
                        minecartComponent.parentNode = null;
                        minecartComponent.locomotiveRef = null;
                    }
                }else {
                    EntityRef parent = checkMineCartJoin(minecartComponent, location.getWorldPosition());
                    if (parent != null) {
                        MinecartComponent parentMinecartComponent = parent.getComponent(MinecartComponent.class);
                        if (parentMinecartComponent.locomotiveRef != null || parentMinecartComponent.type.equals(MinecartComponent.Types.locomotive)) {
                            minecartComponent.parentNode = parent;
                            minecartComponent.locomotiveRef = parentMinecartComponent.type.equals(MinecartComponent.Types.locomotive)?parent:parentMinecartComponent.locomotiveRef;
                            LocomotiveComponent loco = minecartComponent.locomotiveRef.getComponent(LocomotiveComponent.class);
                            loco.childs.add(targetEntity);
                            minecartComponent.locomotiveRef.saveComponent(loco);
                            parentMinecartComponent.childNode = targetEntity;
                            parent.saveComponent(parentMinecartComponent);
                        }
                    }
                }
                targetEntity.saveComponent(minecartComponent);
            }
        }
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
                setSelectMaterial(oldTarget, "rails:minecart");
            }
        }

        if (newTarget.hasComponent(MinecartComponent.class)) {
            MinecartComponent minecartComponent = newTarget.getComponent(MinecartComponent.class);
            if (minecartComponent.type.equals(MinecartComponent.Types.minecart)) {
                LocationComponent location = newTarget.getComponent(LocationComponent.class);
                if (minecartComponent.parentNode != null) {
                    setSelectMaterial(newTarget, "rails:minecart-unjoin");
                }else if (checkMineCartJoin(minecartComponent, location.getWorldPosition()) != null) {
                    setSelectMaterial(newTarget, "rails:minecart-join");
                }
            }
        }
    }

    private void setSelectMaterial(EntityRef minecart, String urlMaterial) {
        MinecartComponent minecartComponent = minecart.getComponent(MinecartComponent.class);
        MeshComponent mesh = minecart.getComponent(MeshComponent.class);
        mesh.material = Assets.getMaterial(urlMaterial);
        for (EntityRef vehicle : minecartComponent.vehicles) {
            MeshComponent meshVehicle = vehicle.getComponent(MeshComponent.class);
            meshVehicle.material = Assets.getMaterial(urlMaterial);
            vehicle.saveComponent(meshVehicle);
        }
        minecart.saveComponent(mesh);
    }

    private EntityRef checkMineCartJoin(MinecartComponent minecartComponent, Vector3f position) {
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
                LocationComponent lc = entity.getComponent(LocationComponent.class);
                if (mn.type.equals(MinecartComponent.Types.locomotive)) {
                    if (MinecartHelper.getSideOfLocomotive(position, lc.getWorldPosition(), mn.yaw).equals(Side.BACK)) {
                        return entity;
                    } else {
                        return null;
                    }
                } else {
                    if (mn.parentNode != null) {
                        return entity;
                    }
                }
            }
        }


        return null;
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
                    minecartComponent.drive = 5;
                }
                minecartEntity.saveComponent(minecartComponent);
            }
        }
    }

    private void onBumpLocomotive(EntityRef locomotive, EntityRef entity) {
        if (entity.hasComponent(CharacterComponent.class)) {
            LocationComponent locPos = locomotive.getComponent(LocationComponent.class);
            LocationComponent playerPos = entity.getComponent(LocationComponent.class);
            MinecartComponent mn = locomotive.getComponent(MinecartComponent.class);
            if (MinecartHelper.getSideOfLocomotive(playerPos.getWorldPosition(), locPos.getWorldPosition(), mn.yaw).equals(Side.BACK)) {
                Vector3f bumpForce = new Vector3f(locPos.getWorldPosition());
                bumpForce.sub(playerPos.getWorldPosition());
                bumpForce.normalize();
                bumpForce.x *= mn.pathDirection.x;
                bumpForce.y *= mn.pathDirection.y;
                bumpForce.z *= mn.pathDirection.z;
                bumpForce.scale(5f);
                locomotive.send(new ImpulseEvent(bumpForce));
            }
        }
    }

    private void onBumpMinecart(EntityRef minecart, EntityRef entity) {
        LocationComponent minecartLocation = minecart.getComponent(LocationComponent.class);
        LocationComponent location = entity.getComponent(LocationComponent.class);
        MinecartComponent minecartComponent = minecart.getComponent(MinecartComponent.class);

        if ( minecartComponent.parentNode != null && minecartComponent.parentNode.equals(entity) ) {
            return;
        }

        if (entity.hasComponent(CharacterComponent.class)) {
            Vector3f bumpForce = new Vector3f(minecartLocation.getWorldPosition());
            bumpForce.sub(location.getWorldPosition());
            bumpForce.normalize();
            bumpForce.x *= minecartComponent.pathDirection.x;
            bumpForce.y *= minecartComponent.pathDirection.y;
            bumpForce.z *= minecartComponent.pathDirection.z;
            bumpForce.scale(5f);
            minecart.send(new ImpulseEvent(bumpForce));
        } else if (entity.hasComponent(MinecartComponent.class)) {
            RigidBodyComponent rb = entity.getComponent(RigidBodyComponent.class);
            rb.velocity.y = 0;
            if (rb.velocity.length() <= 0.2f) {
                return;
            }
            Vector3f diffPosition = new Vector3f(minecartLocation.getWorldPosition());
            MinecartComponent otherMinecartComponent = entity.getComponent(MinecartComponent.class);
            diffPosition.sub(location.getWorldPosition());
            Vector3f forceDirection = new Vector3f(Math.signum(diffPosition.x), Math.signum(diffPosition.y), Math.signum(diffPosition.z));
            float forceScale = 5f;
            minecartComponent.direction.set(minecartComponent.pathDirection);
            MinecartHelper.setVectorToDirection(forceDirection, minecartComponent.pathDirection);
            MinecartHelper.setVectorToDirection(minecartComponent.direction, forceDirection);
            forceDirection.y = 1f;
            forceDirection.scale(forceScale);
            minecart.send(new ImpulseEvent(forceDirection));

            if (otherMinecartComponent.drive > 0) {
                otherMinecartComponent.needRevertVelocity = 3;
                entity.saveComponent(otherMinecartComponent);
            }
        }
        minecart.saveComponent(minecartComponent);
    }
}
