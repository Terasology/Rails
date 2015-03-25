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
package org.terasology.rails.carts.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.asset.Assets;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.EventPriority;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.input.cameraTarget.CameraTargetChangedEvent;
import org.terasology.logic.characters.CharacterComponent;
import org.terasology.logic.characters.MovementMode;
import org.terasology.logic.characters.events.SetMovementModeEvent;
import org.terasology.logic.health.DestroyEvent;
import org.terasology.logic.inventory.InventoryUtils;
import org.terasology.logic.location.Location;
import org.terasology.math.Side;
import org.terasology.math.geom.Quat4f;
import org.terasology.math.geom.Vector3f;
import org.terasology.physics.HitResult;
import org.terasology.physics.Physics;
import org.terasology.physics.StandardCollisionGroup;
import org.terasology.physics.components.RigidBodyComponent;
import org.terasology.physics.events.CollideEvent;
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
import org.terasology.logic.players.event.OnPlayerSpawnedEvent;
import org.terasology.math.geom.Vector3i;
import org.terasology.rails.carts.components.RailVehicleComponent;
import org.terasology.rails.carts.controllers.MinecartFactory;
import org.terasology.rendering.logic.MeshComponent;
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

    @ReceiveEvent(components = {RailVehicleComponent.class, LocationComponent.class}, priority = EventPriority.PRIORITY_HIGH)
    public void onBump(CollideEvent event, EntityRef entity) {
        RailVehicleComponent railVehicle = entity.getComponent(RailVehicleComponent.class);
        if (railVehicle == null || !railVehicle.isCreated) {
            return;
        }

        if (railVehicle.characterInsideCart != null && railVehicle.characterInsideCart.equals(event.getOtherEntity())) {
            return;
        }

        switch (railVehicle.type) {
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
        inventoryManager.giveItem(player, player, entityManager.create("rails:minecart"));
        inventoryManager.giveItem(player, player, entityManager.create("rails:loco"));
        inventoryManager.giveItem(player, player, entityManager.create("rails:wrench"));
        inventoryManager.giveItem(player, player, blockFactory.newInstance(blockManager.getBlockFamily("rails:Rails"), 99));
    }

    @ReceiveEvent(components = {RailVehicleComponent.class, LocationComponent.class}, priority = EventPriority.PRIORITY_HIGH)
    public void onDestroyRailVehicle(DestroyEvent event, EntityRef entity) {
        logger.info("Destroy vehicle");
        RailVehicleComponent railVehicle = entity.getComponent(RailVehicleComponent.class);

        switch(railVehicle.type) {
            case locomotive:
                LocomotiveComponent locomotiveComponent = entity.getComponent(LocomotiveComponent.class);

                for (EntityRef minecart : locomotiveComponent.childs) {
                    RailVehicleComponent childRailVehicleComponent = minecart.getComponent(RailVehicleComponent.class);
                    childRailVehicleComponent.locomotiveRef = null;
                    childRailVehicleComponent.parentNode = null;
                    childRailVehicleComponent.childNode = null;
                    minecart.saveComponent(childRailVehicleComponent);
                }

                break;
            case minecart:

                if (railVehicle.childNode != null) {
                    RailVehicleComponent childRailVehicleComponent = railVehicle.childNode.getComponent(RailVehicleComponent.class);
                    childRailVehicleComponent.parentNode = railVehicle.parentNode;
                    railVehicle.childNode.saveComponent(childRailVehicleComponent);
                }

                if (railVehicle.locomotiveRef != null) {
                    LocomotiveComponent locComponent = railVehicle.locomotiveRef.getComponent(LocomotiveComponent.class);
                    locComponent.childs.remove(entity);
                    railVehicle.locomotiveRef.saveComponent(locComponent);
                }

                break;
        }

        if (railVehicle.characterInsideCart != null) {
            Location.removeChild(entity, railVehicle.characterInsideCart);
            railVehicle.characterInsideCart.send(new SetMovementModeEvent(MovementMode.WALKING));
            railVehicle.characterInsideCart = null;
        }

        for (EntityRef vehicle : railVehicle.vehicles) {
            if (vehicle != null && !vehicle.equals(EntityRef.NULL)) {
                vehicle.destroy();
            }
        }

        entity.saveComponent(railVehicle);
    }

    @ReceiveEvent(components = {RailVehicleComponent.class, ItemComponent.class})
    public void onPlaceFunctional(ActivateEvent event, EntityRef item) {

        if (item.hasComponent(WrenchComponent.class)) {
            return;
        }

        RailVehicleComponent functionalItem = item.getComponent(RailVehicleComponent.class);

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

        logger.info("Created vehicle at {}", placementPos);

        EntityRef entity = railVehicleFactory.create(placementPos.toVector3f(), functionalItem.type);
        event.consume();
    }

    @ReceiveEvent(components = {WrenchComponent.class, ItemComponent.class})
    public void joinMinecart(ActivateEvent event, EntityRef item) {
        EntityRef targetEntity = event.getTarget();
        if (targetEntity.hasComponent(RailVehicleComponent.class)) {
            RailVehicleComponent railVehicleComponent = targetEntity.getComponent(RailVehicleComponent.class);
            if (railVehicleComponent.type.equals(RailVehicleComponent.Types.minecart)) {
                LocationComponent location = targetEntity.getComponent(LocationComponent.class);
                if (railVehicleComponent.parentNode != null) {
                    if (railVehicleComponent.childNode == null) {
                        LocomotiveComponent loco = railVehicleComponent.locomotiveRef.getComponent(LocomotiveComponent.class);
                        loco.childs.remove(targetEntity);
                        railVehicleComponent.locomotiveRef.saveComponent(loco);
                        railVehicleComponent.parentNode = null;
                        railVehicleComponent.locomotiveRef = null;
                    }
                } else {
                    EntityRef parent = checkMineCartJoin(railVehicleComponent, location.getWorldPosition());
                    if (parent != null) {
                        RailVehicleComponent parentRailVehicleComponent = parent.getComponent(RailVehicleComponent.class);
                        if (parentRailVehicleComponent.locomotiveRef != null || parentRailVehicleComponent.type.equals(RailVehicleComponent.Types.locomotive)) {
                            railVehicleComponent.parentNode = parent;
                            railVehicleComponent.locomotiveRef = parentRailVehicleComponent.type.equals(RailVehicleComponent.Types.locomotive)
                                    ? parent : parentRailVehicleComponent.locomotiveRef;
                            LocomotiveComponent loco = railVehicleComponent.locomotiveRef.getComponent(LocomotiveComponent.class);
                            if (loco == null) {
                                logger.info("Ahtung!!!");
                                return;
                            }
                            loco.childs.add(targetEntity);
                            railVehicleComponent.locomotiveRef.saveComponent(loco);
                            parentRailVehicleComponent.childNode = targetEntity;
                            parent.saveComponent(parentRailVehicleComponent);
                        }
                    }
                }
                targetEntity.saveComponent(railVehicleComponent);
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

        if (oldTarget.hasComponent(RailVehicleComponent.class)) {
            RailVehicleComponent railVehicleComponent = oldTarget.getComponent(RailVehicleComponent.class);
            if (railVehicleComponent.type.equals(RailVehicleComponent.Types.minecart)) {
                setSelectMaterial(oldTarget, "rails:minecart");
            }
        }

        if (newTarget.hasComponent(RailVehicleComponent.class)) {
            RailVehicleComponent railVehicleComponent = newTarget.getComponent(RailVehicleComponent.class);
            if (railVehicleComponent.type.equals(RailVehicleComponent.Types.minecart)) {
                LocationComponent location = newTarget.getComponent(LocationComponent.class);
                if (railVehicleComponent.parentNode != null) {
                    setSelectMaterial(newTarget, "rails:minecart-unjoin");
                } else if (checkMineCartJoin(railVehicleComponent, location.getWorldPosition()) != null) {
                    setSelectMaterial(newTarget, "rails:minecart-join");
                }
            }
        }
    }

    private void setSelectMaterial(EntityRef railVehicle, String urlMaterial) {
        RailVehicleComponent railVehicleComponent = railVehicle.getComponent(RailVehicleComponent.class);
        MeshComponent mesh = railVehicle.getComponent(MeshComponent.class);
        mesh.material = Assets.getMaterial(urlMaterial);
        for (EntityRef vehicle : railVehicleComponent.vehicles) {
            MeshComponent meshVehicle = vehicle.getComponent(MeshComponent.class);
            meshVehicle.material = Assets.getMaterial(urlMaterial);
            vehicle.saveComponent(meshVehicle);
        }
        railVehicle.saveComponent(mesh);
    }

    private EntityRef checkMineCartJoin(RailVehicleComponent railVehicleComponent, Vector3f position) {
        Vector3f pathDirection = new Vector3f(railVehicleComponent.pathDirection);
        pathDirection.y = 0;

        Vector3f pathDirectionNegate = new Vector3f(pathDirection);

        pathDirectionNegate.negate();
        Vector3f[] directions = {pathDirection, pathDirectionNegate};

        for (Vector3f dir : directions) {
            HitResult hit = physics.rayTrace(position, dir, 2.5f, StandardCollisionGroup.DEFAULT, StandardCollisionGroup.WORLD);
            EntityRef entity = hit.getEntity();

            if (entity.hasComponent(RailVehicleComponent.class)) {
                RailVehicleComponent mn = entity.getComponent(RailVehicleComponent.class);
                LocationComponent lc = entity.getComponent(LocationComponent.class);
                if (mn.type.equals(RailVehicleComponent.Types.locomotive)) {
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

    @ReceiveEvent(components = {RailVehicleComponent.class, LocationComponent.class})
    public void onUseFunctional(ActivateEvent event, EntityRef railVehicleEntity) {
        RailVehicleComponent railVehicleComponent = railVehicleEntity.getComponent(RailVehicleComponent.class);
        RigidBodyComponent railVehicleRigidBody = railVehicleEntity.getComponent(RigidBodyComponent.class);
        if (railVehicleComponent.type.equals(RailVehicleComponent.Types.minecart)) {
            if (railVehicleComponent.isCreated) {
                if (railVehicleComponent.characterInsideCart == null && (railVehicleComponent.pathDirection.x == 0 || railVehicleComponent.pathDirection.z == 0)) {
                   event.getInstigator().send(new SetMovementModeEvent(MovementMode.NONE));
                    railVehicleComponent.characterInsideCart = event.getInstigator();
                    Location.attachChild(railVehicleEntity, railVehicleComponent.characterInsideCart, new Vector3f(0, 1.5f, 0), new Quat4f());
                    railVehicleRigidBody.collidesWith.remove(StandardCollisionGroup.CHARACTER);
                    railVehicleRigidBody.collidesWith.remove(StandardCollisionGroup.DEFAULT);
                    railVehicleComponent.drive = 0;
                } else {
                    event.getInstigator().send(new SetMovementModeEvent(MovementMode.WALKING));
                    Location.removeChild(railVehicleEntity, railVehicleComponent.characterInsideCart);
                    railVehicleComponent.characterInsideCart = null;
                    railVehicleRigidBody.collidesWith.add(StandardCollisionGroup.CHARACTER);
                    railVehicleRigidBody.collidesWith.add(StandardCollisionGroup.DEFAULT);
                    railVehicleComponent.drive = 0;
                }
                railVehicleEntity.saveComponent(railVehicleComponent);
                railVehicleEntity.saveComponent(railVehicleRigidBody);
            }
        } else {
            if (railVehicleComponent.isCreated) {
                if (railVehicleComponent.drive > 0) {
                    railVehicleComponent.drive = 0;
                } else {
                    railVehicleComponent.drive = 5;
                }
                railVehicleEntity.saveComponent(railVehicleComponent);
            }
        }
    }

    private void onBumpLocomotive(EntityRef locomotive, EntityRef entity) {
        if (entity.hasComponent(CharacterComponent.class)) {
            LocationComponent locPos = locomotive.getComponent(LocationComponent.class);
            LocationComponent playerPos = entity.getComponent(LocationComponent.class);
            RailVehicleComponent mn = locomotive.getComponent(RailVehicleComponent.class);
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

    private void onBumpMinecart(EntityRef railVehicle, EntityRef entity) {
        LocationComponent railVehicleLocation = railVehicle.getComponent(LocationComponent.class);
        LocationComponent location = entity.getComponent(LocationComponent.class);
        RailVehicleComponent railVehicleComponent = railVehicle.getComponent(RailVehicleComponent.class);

        if (railVehicleComponent.parentNode != null) {
            return;
        }

        if (entity.hasComponent(CharacterComponent.class)) {
            Vector3f bumpForce = new Vector3f(railVehicleLocation.getWorldPosition());
            bumpForce.sub(location.getWorldPosition());
            bumpForce.normalize();
            bumpForce.x *= railVehicleComponent.pathDirection.x;
            bumpForce.y *= railVehicleComponent.pathDirection.y;
            bumpForce.z *= railVehicleComponent.pathDirection.z;
            bumpForce.scale(5f);
            railVehicleComponent.direction.set(railVehicleComponent.pathDirection);
            MinecartHelper.setVectorToDirection(bumpForce, railVehicleComponent.pathDirection);
            MinecartHelper.setVectorToDirection(railVehicleComponent.direction, bumpForce);
            railVehicle.send(new ImpulseEvent(bumpForce));
        } else if (entity.hasComponent(RailVehicleComponent.class)) {
            RigidBodyComponent rb = entity.getComponent(RigidBodyComponent.class);
            rb.velocity.y = 0;
            if (rb.velocity.length() <= 0.2f) {
                return;
            }
            Vector3f diffPosition = new Vector3f(railVehicleLocation.getWorldPosition());
            RailVehicleComponent otherRailVehicleComponent = entity.getComponent(RailVehicleComponent.class);
            diffPosition.sub(location.getWorldPosition());
            Vector3f forceDirection = new Vector3f(Math.signum(diffPosition.x), Math.signum(diffPosition.y), Math.signum(diffPosition.z));
            float forceScale = 5f;
            railVehicleComponent.direction.set(railVehicleComponent.pathDirection);
            MinecartHelper.setVectorToDirection(forceDirection, railVehicleComponent.pathDirection);
            MinecartHelper.setVectorToDirection(railVehicleComponent.direction, forceDirection);
            forceDirection.y = 1f;
            forceDirection.scale(forceScale);
            railVehicle.send(new ImpulseEvent(forceDirection));

            if (otherRailVehicleComponent.drive > 0) {
                otherRailVehicleComponent.needRevertVelocity = 3;
                entity.saveComponent(otherRailVehicleComponent);
            }
        }
        railVehicle.saveComponent(railVehicleComponent);
    }
}
