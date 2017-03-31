/*
 * Copyright 2016 MovingBlocks
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
package org.terasology.rails.minecarts.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.Time;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.EventPriority;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.characters.CharacterComponent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.location.LocationComponent;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.math.geom.Quat4f;
import org.terasology.math.geom.Vector3f;
import org.terasology.physics.HitResult;
import org.terasology.physics.Physics;
import org.terasology.physics.StandardCollisionGroup;
import org.terasology.physics.components.RigidBodyComponent;
import org.terasology.physics.events.ChangeVelocityEvent;
import org.terasology.physics.events.CollideEvent;
import org.terasology.physics.shapes.HullShapeComponent;
import org.terasology.rails.minecarts.blocks.RailBlockTrackSegment;
import org.terasology.rails.minecarts.blocks.RailBlockTrackSegmentSystem;
import org.terasology.rails.minecarts.blocks.RailComponent;
import org.terasology.rails.minecarts.components.RailVehicleComponent;
import org.terasology.rails.tracks.TrackSegment;
import org.terasology.registry.In;
import org.terasology.rendering.logic.MeshComponent;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;

/**
 * Created by michaelpollind on 8/16/16.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class CartMotionSystem extends BaseComponentSystem implements UpdateSubscriberSystem {

    private static final Logger logger = LoggerFactory.getLogger(CartMotionSystem.class);
    public  static  final  float GRAVITY = 4.9f;
    public  static  final  float FRICTION_COFF = .1f;
    public  static  final  float BAUMGARTE_COFF = .2f;
    public  static  final  float SLOP_COFF = .01f;
    @In
    private Time time;
    @In
    private EntityManager entityManager;
    @In
    private WorldProvider worldProvider;
    @In
    private Physics physics;
    @In
    private LocalPlayer localPlayer;
    @In
    private InventoryManager inventoryManager;
    @In
    private BlockEntityRegistry blockEntityRegistry;
    @In
    private RailBlockTrackSegmentSystem railBlockTrackSegment;

    @Override
    public void update(float delta) {
        for (EntityRef railVehicle : entityManager.getEntitiesWith(RailVehicleComponent.class,RigidBodyComponent.class))
            updateCart(railVehicle, delta);

    }

    private void updateCart(EntityRef railVehicle, float delta)
    {

        LocationComponent location = railVehicle.getComponent(LocationComponent.class);
        RigidBodyComponent rigidBodyComponent = railVehicle.getComponent(RigidBodyComponent.class);
        RailVehicleComponent railVehicleComponent = railVehicle.getComponent(RailVehicleComponent.class);
        if(!railVehicleComponent.isCreated)
            return;

        if(railVehicleComponent.currentSegment == null)
        {
            HitResult hit =  physics.rayTrace(location.getWorldPosition(), Vector3f.down(), 1.2f, StandardCollisionGroup.DEFAULT, StandardCollisionGroup.WORLD);
            if(hit == null || hit.getBlockPosition() == null)
                return;

            EntityRef ref= blockEntityRegistry.getBlockEntityAt(hit.getBlockPosition());
            Block block = worldProvider.getBlock(hit.getBlockPosition());

            if(ref.hasComponent(RailComponent.class))
            {
                railVehicleComponent.currentSegment = ref;

                RailBlockTrackSegment segment = railBlockTrackSegment.getSegment(block.getURI(),null);
                railVehicleComponent.t = segment.getNearestT(hit.getHitPoint(),hit.getBlockPosition().toVector3f(),segment.getRotation().getQuat4f());
                rigidBodyComponent.collidesWith.remove(StandardCollisionGroup.WORLD);
                railVehicle.saveComponent(railVehicleComponent);
                railVehicleComponent.velocity = Vector3f.zero();
            }

        }


        repositionAxis(railVehicle, delta);


    }

    public  void repositionAxis(EntityRef railVehicle,float delta)
    {
        LocationComponent location = railVehicle.getComponent(LocationComponent.class);
        RailVehicleComponent railVehicleComponent = railVehicle.getComponent(RailVehicleComponent.class);
        RigidBodyComponent rigidBodyComponent = railVehicle.getComponent(RigidBodyComponent.class);

        if(railVehicleComponent.currentSegment != null)
        {

            BlockComponent blockComponent = railVehicleComponent.currentSegment.getComponent(BlockComponent.class);

            if(blockComponent == null)
            {
                railVehicleComponent.currentSegment = null;
                railVehicle.saveComponent(railVehicleComponent);
                return;
            }

            if(railVehicleComponent.trackSegment == null)
            {
                railVehicleComponent.trackSegment = railBlockTrackSegment.getSegment(blockComponent.getBlock().getURI(),null);
            }

            RailBlockTrackSegment segment =railVehicleComponent.trackSegment;//getRailBlockTrackSegment(railVehicleComponent.previousSegment,railVehicleComponent.currentSegment);


            Vector3f position = segment.getPoint(railVehicleComponent.t,blockComponent.getPosition().toVector3f(),segment.getRotation().getQuat4f(),railVehicleComponent.currentSegment);
            if(position == null)
                return;

            MeshComponent mesh = railVehicle.getComponent(MeshComponent.class);
            position.y = mesh.mesh.getAABB().getMax().y/2.0f + position.y + .01f;

            Vector3f normal =  segment.getNormal(railVehicleComponent.t,segment.getRotation().getQuat4f(),railVehicleComponent.currentSegment);
            Vector3f tangent = segment.getTangent(railVehicleComponent.t,segment.getRotation().getQuat4f(),railVehicleComponent.currentSegment);

            TrackSegment.TrackSegmentPair proceedingPair;


            Vector3f gravity = Vector3f.down().mul(9.8f).mul(delta);
            railVehicleComponent.velocity.add(project(gravity,tangent));

            Vector3f friction = project(gravity,normal).invert().mul(FRICTION_COFF);

            float mag = railVehicleComponent.velocity.length() - friction.length() ;
            if(mag < 0)
                mag = railVehicleComponent.velocity.length();

            if(railVehicleComponent.velocity.length() != 0)
                railVehicleComponent.velocity = project(railVehicleComponent.velocity,tangent).normalize().mul(mag);
            else
                railVehicleComponent.velocity = Vector3f.zero();

            bound(railVehicleComponent.velocity);


            if( tangent.dot(railVehicleComponent.velocity) > 0) {
                proceedingPair = segment.getTrackSegment(railVehicleComponent.t + railVehicleComponent.velocity.length() * delta, railVehicleComponent.currentSegment);
            }
            else {
                proceedingPair = segment.getTrackSegment(railVehicleComponent.t - railVehicleComponent.velocity.length() * delta, railVehicleComponent.currentSegment);
            }
            if(proceedingPair == null)
                return;

            Quat4f horizontalRotation =Quat4f.shortestArcQuat(Vector3f.north(),new Vector3f(tangent).setY(0).normalize());
            Quat4f verticalRotation = Quat4f.shortestArcQuat(new Vector3f(tangent).setY(0).normalize(),new Vector3f(tangent));
            verticalRotation.mul(horizontalRotation);


            location.setLocalRotation(verticalRotation);
            location.setWorldPosition(position);


            rigidBodyComponent.kinematic = true;
            railVehicleComponent.t = proceedingPair.t;
            railVehicleComponent.currentSegment = proceedingPair.association;
            railVehicleComponent.trackSegment = (RailBlockTrackSegment) proceedingPair.segment;

            railVehicle.saveComponent(railVehicleComponent);
            railVehicle.saveComponent(rigidBodyComponent);
            railVehicle.saveComponent(location);

            railVehicle.send(new ChangeVelocityEvent(railVehicleComponent.velocity,Vector3f.zero()));

        }
    }

    @ReceiveEvent(components = {RailVehicleComponent.class, LocationComponent.class,RigidBodyComponent.class}, priority = EventPriority.PRIORITY_HIGH)
    public void onBump(CollideEvent event, EntityRef entity) {
        RailVehicleComponent v1 = entity.getComponent(RailVehicleComponent.class);
        RigidBodyComponent r1 = entity.getComponent(RigidBodyComponent.class);

        if(event.getOtherEntity().hasComponent(CharacterComponent.class))
        {
            LocationComponent playerLocation = event.getOtherEntity().getComponent(LocationComponent.class);
            LocationComponent cartLocation = entity.getComponent(LocationComponent.class);

            Vector3f bumpForce = new Vector3f(cartLocation.getWorldPosition());
            bumpForce.sub(playerLocation.getWorldPosition());
            bumpForce.normalize();
            bumpForce.scale(5f);

            Vector3f tangent = v1.trackSegment.getTangent(v1.t,v1.trackSegment.getRotation().getQuat4f(),v1.currentSegment);
            bumpForce = project(bumpForce,tangent);
            v1.velocity.add(bumpForce.div(r1.mass));
            entity.saveComponent(v1);

        }
        else if(event.getOtherEntity().hasComponent(RailVehicleComponent.class))
        {
            RailVehicleComponent v2 = event.getOtherEntity().getComponent(RailVehicleComponent.class);
            RigidBodyComponent r2 = event.getOtherEntity().getComponent(RigidBodyComponent.class);

            Vector3f v1n = v1.trackSegment.getTangent(v1.t,v1.trackSegment.getRotation().getQuat4f(),v1.currentSegment);
            Vector3f v2n = v2.trackSegment.getTangent(v2.t,v2.trackSegment.getRotation().getQuat4f(),v2.currentSegment);



            Vector3f halfNormal = new Vector3f(v1n);
            if(v1n.dot(v2n) < 0)
                halfNormal.invert();
            halfNormal.add(v2n).normalize();

            Vector3f normal = event.getNormal();

            float jv = ((normal.x * v1.velocity.x) + (normal.y * v1.velocity.y) +(normal.z * v1.velocity.z))-
                    ((normal.x * v2.velocity.x) + (normal.y * v2.velocity.y) + (normal.z * v2.velocity.z));


            float b =  - Math.abs(BAUMGARTE_COFF/time.getGameDelta()* Math.max(Math.abs(event.getPenetration())-SLOP_COFF,0));

            //if(event.getNormal().dot(halfNormal) < 0)
             //   b*= -1.0f;

            if(jv +b  <= 0)
                return;

            float effectiveMass = 1.0f / r1.mass + 1.0f / r2.mass;

            float lambda = -(jv+b)/ effectiveMass;

            Vector3f r1v = new Vector3f(normal.x / r1.mass, normal.y / r1.mass, normal.z / r1.mass).mul( lambda);
            Vector3f r2v = new Vector3f(normal.x / r2.mass, normal.y / r2.mass, normal.z / r2.mass).mul(lambda).invert();

            v1.velocity.add(r1v);
            v2.velocity.add(r2v);


            entity.saveComponent(v1);
            event.getOtherEntity().saveComponent(v2);
        }
    }

    private void bound(Vector3f v)
    {

        if(Float.isNaN(v.x) || Float.isInfinite(v.x))
            v.x = 0.0f;
        if(Float.isNaN(v.y) || Float.isInfinite(v.y))
            v.y = 0.0f;
        if(Float.isNaN(v.z) || Float.isInfinite(v.z))
            v.z = 0.0f;
    }

    public final Vector3f project(Vector3f u, Vector3f v)
    {
        if(v.lengthSquared() == 0.0f)
            return Vector3f.zero();
        return new Vector3f(v).mul(new Vector3f(u).dot(v)/ (v.lengthSquared()));
    }


    private  void  findTrackToAttachTo(EntityRef ref)
    {

    }

}
