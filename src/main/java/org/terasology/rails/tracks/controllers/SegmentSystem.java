/*
 * Copyright 2017 MovingBlocks
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
package org.terasology.rails.tracks.controllers;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.Rotation;
import org.terasology.math.geom.Quat4f;
import org.terasology.math.geom.Vector3f;
import org.terasology.rails.minecarts.blocks.PathFamily;
import org.terasology.rails.minecarts.components.SegmentVehicleComponent;
import org.terasology.rails.tracks.Segment;
import org.terasology.registry.In;
import org.terasology.registry.Share;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.family.BlockFamily;

/**
 * Created by michaelpollind on 4/1/17.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
@Share(value = SegmentSystem.class)
public class SegmentSystem extends BaseComponentSystem {

    public static final float MATCH_EPSILON = .09f;

    public enum JointMatch {
        Start_End,
        Start_Start,
        End_End,
        End_Start,
        None
    }

    @In
    SegmentCacheSystem segmentCacheSystem;

    public static class SegmentPair {
        public float t;
        public Segment segment;
        public EntityRef association;

        public SegmentPair(float t, Segment segment, EntityRef association) {
            this.t = t;
            this.segment = segment;
            this.association = association;
        }

        public SegmentPair(Segment segment, EntityRef association) {
            this.segment = segment;
            this.association = association;
        }
    }

    public float findDeltaT(EntityRef vehicleEntity, Vector3f vector) {
        SegmentVehicleComponent segmentVehicleComponent = vehicleEntity.getComponent(SegmentVehicleComponent.class);
        return segmentVehicleComponent.heading.dot(vector);
    }

    private float calculateDeltaT(EntityRef vehicleEntity, float deltaT, boolean updateHeading) {
        SegmentVehicleComponent segmentVehicleComponent = vehicleEntity.getComponent(SegmentVehicleComponent.class);
        Vector3f tangent = vehicleTangent(vehicleEntity);
        if (tangent.dot(segmentVehicleComponent.heading) < 0) {
            deltaT *= -1;
            tangent.invert();
        }
        if (updateHeading)
            segmentVehicleComponent.heading = tangent;
        return deltaT;
    }

    public Vector3f vehicleTangent(EntityRef vehicleEntity) {
        SegmentVehicleComponent vehicle = vehicleEntity.getComponent(SegmentVehicleComponent.class);
        Segment vehicleSegment = segmentCacheSystem.getSegment(vehicle.descriptor);
        int index = vehicleSegment.index(vehicle.t);
        Quat4f rotation = this.segmentRotation(vehicle.segmentEntity);
        return vehicleSegment.tangent(index, vehicleSegment.t(index, vehicle.t), rotation);
    }

    public Vector3f vehiclePoint(EntityRef vehicleEntity) {
        SegmentVehicleComponent vehicle = vehicleEntity.getComponent(SegmentVehicleComponent.class);
        Segment vehicleSegment = segmentCacheSystem.getSegment(vehicle.descriptor);
        int index = vehicleSegment.index(vehicle.t);
        Quat4f rotation = this.segmentRotation(vehicle.segmentEntity);
        Vector3f position = this.segmentPosition(vehicle.segmentEntity);
        return vehicleSegment.point(index, vehicleSegment.t(index, vehicle.t), position, rotation);
    }

    public Vector3f vehicleNormal(EntityRef vehicleEntity) {
        SegmentVehicleComponent vehicle = vehicleEntity.getComponent(SegmentVehicleComponent.class);
        Segment vehicleSegment = segmentCacheSystem.getSegment(vehicle.descriptor);
        int index = vehicleSegment.index(vehicle.t);
        Quat4f rotation = this.segmentRotation(vehicle.segmentEntity);
        Vector3f position = this.segmentPosition(vehicle.segmentEntity);
        return vehicleSegment.normal(index, vehicleSegment.t(index, vehicle.t), rotation);
    }

    public boolean move(EntityRef vehicleEntity, float tDelta, SegmentMapping mapping) {
        if (tDelta == 0)
            return true;
        float deltaT = calculateDeltaT(vehicleEntity, tDelta, true);
        SegmentVehicleComponent vehicle = vehicleEntity.getComponent(SegmentVehicleComponent.class);
        Segment current = segmentCacheSystem.getSegment(vehicle.descriptor);

        Vector3f p1 = this.segmentPosition(vehicle.segmentEntity);
        Quat4f q1 = this.segmentRotation(vehicle.segmentEntity);

        float t = vehicle.t + deltaT;
        if (t < 0) {
            float result = -t;
            SegmentMapping.SegmentPair segmentPair = mapping.nextSegment(vehicle, SegmentMapping.SegmentEnd.S1);
            if (segmentPair == null)
                return false;

            Segment nextSegment = segmentCacheSystem.getSegment(segmentPair.prefab);

            Vector3f p2 = this.segmentPosition(segmentPair.entity);
            Quat4f q2 = this.segmentRotation(segmentPair.entity);

            JointMatch match = segmentMatch(current, p1, q1, nextSegment, p2, q2);
            if (match == JointMatch.Start_End)
                vehicle.t = nextSegment.maxDistance() - result;
            else if (match == JointMatch.Start_Start)
                vehicle.t = result;
            else
                return false;


            vehicle.descriptor = segmentPair.prefab;
            vehicle.segmentEntity = segmentPair.entity;
        } else if (t > current.maxDistance()) {

            float result = t - current.maxDistance();
            SegmentMapping.SegmentPair segmentPair = mapping.nextSegment(vehicle, SegmentMapping.SegmentEnd.S2);
            if (segmentPair == null)
                return false;
            Segment nextSegment = segmentCacheSystem.getSegment(segmentPair.prefab);

            Vector3f p2 = this.segmentPosition(segmentPair.entity);
            Quat4f q2 = this.segmentRotation(segmentPair.entity);

            JointMatch match = segmentMatch(current, p1, q1, nextSegment, p2, q2);
            if (match == JointMatch.End_Start)
                vehicle.t = result;
            else if (match == JointMatch.End_End)
                vehicle.t = nextSegment.maxDistance() - result;
            else
                return false;

            vehicle.segmentEntity = segmentPair.entity;
            vehicle.descriptor = segmentPair.prefab;
        } else {
            vehicle.t = t;
        }

        vehicleEntity.saveComponent(vehicle);
        return true;
    }

    public JointMatch segmentMatch(Segment current, Vector3f p1, Quat4f r1, Segment next, Vector3f p2, Quat4f r2) {
        Vector3f s1 = current.point(0, 0, p1, r1);
        Vector3f e1 = current.point(current.maxIndex(), 1, p1, r1);

        Vector3f s2 = next.point(0, 0, p2, r2);
        Vector3f e2 = next.point(next.maxIndex(), 1, p2, r2);

        if (s1.distance(s2) < MATCH_EPSILON)
            return JointMatch.Start_Start;
        if (s1.distance(e2) < MATCH_EPSILON)
            return JointMatch.Start_End;
        if (e1.distance(s2) < MATCH_EPSILON)
            return JointMatch.End_Start;
        if (e1.distance(e2) < MATCH_EPSILON)
            return JointMatch.End_End;
        return JointMatch.None;
    }

    public Vector3f segmentPosition(EntityRef entity) {
        if (entity.hasComponent(BlockComponent.class)) {
            BlockComponent blockComponent = entity.getComponent(BlockComponent.class);
            return blockComponent.getPosition().toVector3f();
        }
        if (entity.hasComponent(LocationComponent.class)) {
            return entity.getComponent(LocationComponent.class).getWorldPosition();
        }
        return Vector3f.zero();
    }

    public Quat4f segmentRotation(EntityRef entity) {
        if (entity.hasComponent(BlockComponent.class)) {
            BlockComponent blockComponent = entity.getComponent(BlockComponent.class);
            BlockFamily blockFamily = blockComponent.getBlock().getBlockFamily();
            if (blockFamily instanceof PathFamily) {
                Rotation rotation = ((PathFamily) blockFamily).getRotationFor(blockComponent.getBlock().getURI());
                return rotation.getQuat4f();
            }
        }
        if (entity.hasComponent(LocationComponent.class)) {
            return entity.getComponent(LocationComponent.class).getWorldRotation();
        }
        return new Quat4f(Quat4f.IDENTITY);
    }


}
