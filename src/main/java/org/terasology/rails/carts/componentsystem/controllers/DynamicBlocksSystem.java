/*
 * Copyright 2012 Benjamin Glatzel <benjamin.glatzel@me.com>
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
package org.terasology.rails.carts.componentsystem.controllers;

import com.bulletphysics.collision.dispatch.*;
import com.bulletphysics.collision.shapes.ConvexShape;
import com.bulletphysics.linearmath.Transform;
import org.terasology.rails.carts.components.DynamicBlockComponent;
import org.terasology.rails.carts.componentsystem.entityfactory.DynamicFactory;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.BeforeDeactivateComponent;
import org.terasology.entitySystem.event.EventPriority;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.RenderSystem;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.health.DoDamageEvent;
import org.terasology.logic.health.OnDamagedEvent;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.math.*;
import org.terasology.physics.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.location.LocationComponent;
import org.terasology.physics.bullet.BulletPhysics;
import org.terasology.physics.events.CollideEvent;
import org.terasology.physics.shapes.BoxShapeComponent;
import org.terasology.rendering.logic.MeshComponent;
import org.terasology.rendering.world.WorldRenderer;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.entitySystem.systems.In;
import org.terasology.world.block.BlockComponent;
import org.terasology.entitySystem.entity.EntityManager;

import javax.vecmath.*;

/**
 * @author Pencilcheck <pennsu@gmail.com>
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public final class DynamicBlocksSystem implements UpdateSubscriberSystem, RenderSystem {
    @In
    private LocalPlayer localPlayer;

    @In
    private EntityManager entityManager;

    @In
    private WorldProvider worldProvider;

    @In
    private WorldRenderer worldRenderer;

    @In
    private BulletPhysics physics;

    DynamicFactory dynamicFactory;
    DynamicMover   dynamicMover;
    RailsDirectionFinder railsDirectionFinder;

    private static final Logger logger = LoggerFactory.getLogger(DynamicBlocksSystem.class);

    @Override
    public void initialise() {
        dynamicFactory = new DynamicFactory();
        dynamicFactory.setEntityManager(entityManager);
        dynamicMover = new DynamicMover();
        railsDirectionFinder = new RailsDirectionFinder();
    }

    @Override
    public void shutdown() {
    }

    @ReceiveEvent(components = {DynamicBlockComponent.class, ItemComponent.class}, priority = EventPriority.PRIORITY_HIGH)
    public void onPlaceFunctional(ActivateEvent event, EntityRef item) {

        for (EntityRef entity : entityManager.getEntitiesWith(DynamicBlockComponent.class, LocationComponent.class)) {
            DynamicBlockComponent dynamicBlockComp = entity.getComponent(DynamicBlockComponent.class);
            if ( !dynamicBlockComp.isCreated ){
                continue;
            }

            entity.send(new DoDamageEvent(100));
        }

        DynamicBlockComponent functionalItem = item.getComponent(DynamicBlockComponent.class);

        Side surfaceDir = Side.inDirection(event.getHitNormal());

        // TODO: Check whether it is possible to place it (e.g. boat cannot be placed on land)

        EntityRef targetEntity = event.getTarget();

        BlockComponent blockComponent = targetEntity.getComponent(BlockComponent.class);

        if ( blockComponent == null ){
            return;
        }

        Vector3i placementPos = new Vector3i(event.getTarget().getComponent(BlockComponent.class).getPosition());

        if (!worldProvider.getBlock(placementPos).isPenetrable()){
            placementPos.add(surfaceDir.getVector3i());
        }

        placementPos.y += item.getComponent(MeshComponent.class).mesh.getAABB().maxY();

        for (EntityRef entityNew : entityManager.getEntitiesWith(DynamicBlockComponent.class, LocationComponent.class)) {
            DynamicBlockComponent dynamicBlockComp = entityNew.getComponent(DynamicBlockComponent.class);

            if ( dynamicBlockComp.isCreated ){
                if (dynamicBlockComp.collider != null) {
                    //physics.removeCollider(dynamicBlockComp.collider);
                    //physics.
                }
                entityNew.destroy();
            }
        }

        EntityRef entity = dynamicFactory.generateDynamicBlock(placementPos.toVector3f(), functionalItem.getDynamicType());
    }

    @ReceiveEvent(components = {DynamicBlockComponent.class, LocationComponent.class})
    public void onDestroy(final BeforeDeactivateComponent event, final EntityRef entity) {
        DynamicBlockComponent comp = entity.getComponent(DynamicBlockComponent.class);
        if (comp.collider != null) {
            //physics.removeCollider(comp.collider);
        }
        if ( comp.vehicleBack != null ){
            comp.vehicleBack.destroy();
        }

        if ( comp.vehicleFront != null ){
            comp.vehicleFront.destroy();
        }
    }

    @ReceiveEvent(components = {DynamicBlockComponent.class})
    public void onDamage(OnDamagedEvent event, EntityRef entity) {
        if ( entity.hasComponent( DynamicBlockComponent.class ) ){
            DynamicBlockComponent comp = entity.getComponent(DynamicBlockComponent.class);
            if (comp.collider != null) {
               // physics.removeCollider(comp.collider);
            }

            if ( comp.vehicleBack != null ){
                comp.vehicleBack.destroy();
            }

            if ( comp.vehicleFront != null ){
                comp.vehicleFront.destroy();
            }

            entity.destroy();
        }
    }

    @ReceiveEvent(components = DynamicBlockComponent.class)
    public void onBump(CollideEvent event, EntityRef entity) {

        EntityRef dynamicEntity = null;
        EntityRef otherEntity   = null;

        if (entity.hasComponent( DynamicBlockComponent.class )){
            dynamicEntity = entity;
            otherEntity   = event.getOtherEntity();
        }else{
            dynamicEntity = event.getOtherEntity();
            otherEntity   = entity;
        }

        dynamicEntity.getComponent(DynamicBlockComponent.class).getVelocityDescriptor().onBump(dynamicEntity, otherEntity);
    }

    public void update(float delta) {
        for (EntityRef entity : entityManager.getEntitiesWith(DynamicBlockComponent.class, LocationComponent.class)) {


            LocationComponent location = entity.getComponent(LocationComponent.class);
            BoxShapeComponent boxshape = entity.getComponent(BoxShapeComponent.class);
            Vector3f worldPos = location.getWorldPosition();

            DynamicBlockComponent dynamicBlockComp = entity.getComponent(DynamicBlockComponent.class);

            /*if (dynamicBlockComp.collider == null) {
                BoxShape box = new BoxShape(new Vector3f(
                        (boxshape.extents.x/2 * location.getWorldScale()),    // 10
                        ((boxshape.extents.y/2) * location.getWorldScale()),
                        (boxshape.extents.z/2 * location.getWorldScale())          //10
                )
                );
               // SphereShape box = new SphereShape( 0.5f * location.getWorldScale() );

                //box.setMargin(0.1f);
                dynamicBlockComp.collider = createCollider(worldPos, box, Lists.<CollisionGroup>newArrayList(dynamicBlockComp.collisionGroup), dynamicBlockComp.collidesWith, CollisionFlags.KINEMATIC_OBJECT);
                dynamicBlockComp.collider.setUserPointer(entity);
                continue;
            }                                                    */

            if ( !dynamicBlockComp.isCreated ){
                continue;
            }

            if (!localPlayer.isValid())
                return;

            Vector3f oldPosition = worldPos;

            DynamicMover.MoveResult moveResult =  dynamicMover.updatePosition(delta, location, dynamicBlockComp);

            Block block = null;
            Vector3i blockPosition = null;
            dynamicBlockComp.environmentInfo.onThePath = false;

            if ( moveResult.hitBottom ){
                HitResult hit = physics.rayTrace(location.getWorldPosition(), new Vector3f(0, -1, 0), 2*boxshape.extents.y);

                blockPosition = hit.getBlockPosition();


                if ( blockPosition != null ){
                  block = worldProvider.getBlock( blockPosition );
                  if ( railsDirectionFinder.isOnPath(dynamicBlockComp, block) ){
                      dynamicBlockComp.environmentInfo.onThePath = true;
                  }
                }
            }

            railsDirectionFinder.findDirection( dynamicBlockComp, block, blockPosition.toVector3f() );

            if ( !block.getAdjacentBlockType().equals( BlockAdjacentType.CORNER ) ){
                if ( dynamicBlockComp.environmentInfo.pathDirection.z != 0 ){
                    Vector3f worldPosition = location.getWorldPosition();
                    worldPosition.x = blockPosition.x;
                    location.setWorldPosition( worldPosition );
                    dynamicBlockComp.collider.setWorldTransform(new Transform(new Matrix4f(new Quat4f(0, 0, 0, 1), worldPosition, 1.0f)));
                }else{
                    Vector3f worldPosition = location.getWorldPosition();
                    worldPosition.z = blockPosition.z;
                    location.setWorldPosition( worldPosition );
                    dynamicBlockComp.collider.setWorldTransform(new Transform(new Matrix4f(new Quat4f(0, 0, 0, 1), worldPosition, 1.0f)));
                }
            }


            if ( dynamicBlockComp.vehicleFront != null && dynamicBlockComp.vehicleBack != null ){
             //   EntityRef vehicleFront = dynamicBlockComp.vehicleFront;
             //   EntityRef vehicleBack  = dynamicBlockComp.vehicleBack;
             //   LocationComponent locationVehicleFront = vehicleFront.getComponent(LocationComponent.class);
               // LocationComponent locationVehicleBack  = vehicleBack.getComponent(LocationComponent.class);
               // locationVehicleFront.
                //locationVehicleBack.getLocalPosition().z += 0.3;
             //   locationVehicleFront.setWorldPosition(new Vector3f(location.getWorldPosition().x, location.getWorldPosition().y, location.getWorldPosition().z));
                //locationVehicleBack.setWorldPosition(new Vector3f(location.getWorldPosition().x, location.getWorldPosition().y, location.getWorldPosition().z));
             //   locationVehicleFront.setLocalRotation(new Quat4f(location.getLocalRotation()));
            //    locationVehicleBack.setLocalRotation(new Quat4f(location.getLocalRotation()));

              //  vehicleFront.saveComponent(locationVehicleFront);
              //  vehicleBack.saveComponent(locationVehicleBack);
            }

            if (standingOn(entity)) {
                // update player position
                Vector3f newPosition = location.getWorldPosition();
                newPosition.sub(oldPosition);
                LocationComponent playerLocation = localPlayer.getEntity().getComponent(LocationComponent.class);
                Vector3f playerPosition = playerLocation.getWorldPosition();
                playerPosition.add(newPosition);
                playerLocation.setWorldPosition(playerPosition);
                localPlayer.getEntity().saveComponent(playerLocation);
            }
       //     dynamicBlockComp.getVelocityDescriptor().setDrive(new Vector3f(0,0,-0.005f));
            entity.saveComponent(dynamicBlockComp);
            entity.saveComponent(location);
        }
    }

    private boolean standingOn(EntityRef entity) {
        BoxShapeComponent boxshape = localPlayer.getCharacterEntity().getComponent(BoxShapeComponent.class);
        HitResult hit = physics.rayTrace(localPlayer.getPosition(), new Vector3f(0, -1, 0), boxshape.extents.y);
        return hit.isHit() && hit.getEntity() == entity;
    }

    @Override
    public void renderOpaque() {
      Vector3f cameraPosition = worldRenderer.getActiveCamera().getPosition();

        /* for (EntityRef entity : entityManager.iteratorEntities(DynamicBlockComponent.class, LocationComponent.class)) {
DynamicBlockComponent dynamicBlockComp = entity.getComponent(DynamicBlockComponent.class);
LocationComponent location = entity.getComponent(LocationComponent.class);

Vector3f renderTarget =  dynamicBlockComp.getVelocityDescriptor().getOldBlockPos(); //location.getWorldPosition();


if ( renderTarget!= null ){
glPushMatrix();
glTranslated(renderTarget.x - cameraPosition.x, renderTarget.y - cameraPosition.y, renderTarget.z - cameraPosition.z);
glBegin(GL_LINES);
glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
glVertex3f(0, 0, 0);
glVertex3f(0, 20f, 0);
glEnd();

glPopMatrix();
}
  Vector3f renderTarget2 = location.getWorldPosition();

glPushMatrix();
glTranslated(renderTarget2.x - cameraPosition.x, renderTarget2.y - cameraPosition.y, renderTarget2.z - cameraPosition.z);
glBegin(GL_LINES);
glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
glVertex3f(0, 0, 0);
glVertex3f(0, 20f, 0);
glEnd();

glPopMatrix();

}               */


    }

    @Override
    public void renderAlphaBlend() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void renderOverlay() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void renderFirstPerson() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void renderShadows() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    private PairCachingGhostObject createCollider(Vector3f pos, ConvexShape shape, short groups, short filters, int collisionFlags) {
        Transform startTransform = new Transform(new Matrix4f(new Quat4f(0, 0, 0, 1), pos, 1.0f));
        PairCachingGhostObject result = new PairCachingGhostObject();
        result.setWorldTransform(startTransform);
        result.setCollisionShape(shape);
        result.setCollisionFlags(collisionFlags);
        discreteDynamicsWorld.addCollisionObject(result, groups, filters);
        return result;
    }
}
