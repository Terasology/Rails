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
package org.terasology.rails.carts.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.systems.ComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.Vector3i;
import org.terasology.physics.CollisionGroup;
import org.terasology.physics.HitResult;
import org.terasology.physics.Physics;
import org.terasology.physics.StandardCollisionGroup;
import org.terasology.physics.bullet.BulletPhysics;
import org.terasology.physics.components.RigidBodyComponent;
import org.terasology.physics.events.ForceEvent;
import org.terasology.rails.blocks.ConnectsToRailsComponent;
import org.terasology.rails.carts.components.MinecartComponent;
import org.terasology.registry.CoreRegistry;
import org.terasology.registry.In;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;

import javax.vecmath.Vector3f;

@RegisterSystem(RegisterMode.AUTHORITY)
public class MinecartSystem implements ComponentSystem, UpdateSubscriberSystem{
    @In
    private EntityManager entityManager;

    @In
    private WorldProvider worldProvider;

    @In
    private Physics physics;

    private static final Logger logger = LoggerFactory.getLogger(MinecartSystem.class);

    @Override
    public void initialise() {
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

    @Override
    public void update(float delta) {
        for( EntityRef minecart  : entityManager.getEntitiesWith(MinecartComponent.class) ){

            MinecartComponent minecartComponent = minecart.getComponent(MinecartComponent.class);

           if ( minecartComponent.isCreated ){
               LocationComponent location = minecart.getComponent(LocationComponent.class);
               HitResult hit = physics.rayTrace(location.getWorldPosition(), new Vector3f(0, -1, 0), 6, StandardCollisionGroup.DEFAULT, StandardCollisionGroup.WORLD);

               Vector3i blockPosition = hit.getBlockPosition();
               Block currentBlock = null;

               if ( blockPosition != null ){
//                   Vector3f pos = location.getWorldPosition();
//                   pos.y -= 0.6;
                   currentBlock = worldProvider.getBlock( blockPosition );
                   EntityRef blockEntity = currentBlock.getEntity();

                   if( blockEntity!=null && blockEntity.hasComponent(ConnectsToRailsComponent.class) ){
                       ConnectsToRailsComponent c = blockEntity.getComponent(ConnectsToRailsComponent.class);
                       logger.info(c.type + "");
                   }else{
                       logger.info(currentBlock.toString());
                   }
               }else{
                   logger.info("Air!!!");
               }

            /*    RigidBodyComponent rigidBodyComponent = minecart.getComponent(RigidBodyComponent.class);
                if (rigidBodyComponent.velocity.length() < 5){
                    minecart.send( new ForceEvent(new Vector3f(30f,0,0))  );
                }*/
            }

        }
    }
}
