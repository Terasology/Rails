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
package org.terasology.rails.carts.components;

import com.bulletphysics.collision.dispatch.PairCachingGhostObject;
import com.google.common.collect.Lists;
import org.terasology.rails.carts.componentsystem.controllers.velocityDescriptors.BoatVelocityDescriptor;
import org.terasology.rails.carts.componentsystem.controllers.velocityDescriptors.MinecartVelocityDescriptor;
import org.terasology.rails.carts.componentsystem.controllers.velocityDescriptors.VelocityDescriptor;
import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.physics.CollisionGroup;
import org.terasology.physics.StandardCollisionGroup;
import org.terasology.world.block.Block;

import javax.vecmath.Vector3f;
import java.util.List;

/**
 * @author Pencilcheck <pennsu@gmail.com>
 */
public final class DynamicBlockComponent implements Component {

    public enum DynamicType {
        Minecart,
        Boat
    }

    public CollisionGroup collisionGroup = StandardCollisionGroup.WORLD;
    public List<CollisionGroup> collidesWith = Lists.<CollisionGroup>newArrayList(StandardCollisionGroup.WORLD, StandardCollisionGroup.SENSOR);

    public DynamicType dynamicType = DynamicType.Minecart;

    public boolean isCreated          = false;
    private EntityRef hasStandingPlayer = EntityRef.NULL;

    public transient PairCachingGhostObject collider;

    public List<String> pathBlocks = Lists.newArrayList();
    public Vector3f direction = new Vector3f();

    public EntityRef vehicleFront = null;
    public EntityRef vehicleBack = null;
    public EnvironmentInfo environmentInfo = new EnvironmentInfo();

    public DynamicType getDynamicType() {
        return dynamicType;
    }


    private float pitch = 0f;
    private float yaw = 0f;

    private VelocityDescriptor velocityDescriptor = null;

    public void setPitch( float pitch ){
        this.pitch = pitch;
    }

    public float getPitch( ){
        return pitch;
    }

    public void setYaw( float yaw ){
        this.yaw = yaw;
    }

    public float getYaw( ){
        return yaw;
    }

    public EntityRef getStandingPlayer(){
        return hasStandingPlayer;
    }

    public void setStandingPlayer( EntityRef hasStandingPlayer ){
        this.hasStandingPlayer =    hasStandingPlayer;
    }

    public VelocityDescriptor getVelocityDescriptor(){
        return velocityDescriptor;
    }

    public void setDynamicType(DynamicType new_type) {
        dynamicType = new_type;

        switch( dynamicType ){
            case Minecart:
                velocityDescriptor = new MinecartVelocityDescriptor();
            break;

            case Boat:
                velocityDescriptor = new BoatVelocityDescriptor();
                break;
        }
    }

    public void setDirection( Vector3f direction ){
        this.direction = direction;
    }

    public Vector3f getDirection(){
        return direction;
    }

    public class EnvironmentInfo{
        public Block prevBlock    = null;
        public Block currentBlock = null;
        public Block nextBlock    = null;

        public Vector3f prevBlockPos    = null;
        public Vector3f currentBlockPos = null;
        public Vector3f nextBlockPos    = null;

        public Vector3f distanceMoved = new Vector3f();
        public Vector3f pathDirection    = new Vector3f(1f,1f,1f);
        public int  lastCornerSign    = 1;
        public int  currentCornerSign = 1;

        public boolean possibleEdge     = false;
        public boolean onThePath = false;
    }

}
