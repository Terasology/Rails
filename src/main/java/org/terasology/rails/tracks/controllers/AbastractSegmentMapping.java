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
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.math.Rotation;
import org.terasology.math.Side;
import org.terasology.math.geom.Quat4f;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector3i;
import org.terasology.rails.minecarts.blocks.PathFamily;
import org.terasology.rails.minecarts.components.SegmentVehicleComponent;
import org.terasology.rails.tracks.Segment;
import org.terasology.rails.tracks.components.BlockMappingComponent;
import org.terasology.rails.tracks.components.PathDescriptorComponent;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.family.BlockFamily;

/**
 * Created by michaelpollind on 4/5/17.
 */
public class AbastractSegmentMapping implements SegmentMapping {
    private SegmentSystem segmentSystem;
    private SegmentCacheSystem segmentCacheSystem;
    private BlockEntityRegistry blockEntityRegistry;

    public AbastractSegmentMapping(BlockEntityRegistry blockEntityRegistry, SegmentSystem segmentSystem, SegmentCacheSystem segmentCacheSystem) {
        this.blockEntityRegistry = blockEntityRegistry;
        this.segmentSystem = segmentSystem;
        this.segmentCacheSystem = segmentCacheSystem;
    }


    @Override
    public SegmentPair nextSegment(SegmentVehicleComponent vehicle, SegmentEnd ends) {
        BlockComponent blockComponent = vehicle.segmentEntity.getComponent(BlockComponent.class);
        if(vehicle.segmentEntity.hasComponent(BlockComponent.class)) {
            BlockFamily blockFamily = blockComponent.getBlock().getBlockFamily();

            Vector3f v1 = segmentSystem.segmentPosition(vehicle.segmentEntity);
            Quat4f q1 = segmentSystem.segmentRotation(vehicle.segmentEntity);

            Segment currentSegment = segmentCacheSystem.getSegment(vehicle.descriptor);


            BlockMappingComponent blockMappingComponent = vehicle.descriptor.getComponent(BlockMappingComponent.class);
            if (blockFamily instanceof PathFamily) {

                Rotation rotation = ((PathFamily) blockFamily).getRotationFor(blockComponent.getBlock().getURI());
                switch (ends) {
                    case S1: {
                        Vector3i segment = findOffset(blockComponent.getPosition(), blockMappingComponent.s1, blockMappingComponent.s2, rotation);//rotation.rotate(blockMappingComponent.s1).getVector3i());
                        EntityRef blockEntity = blockEntityRegistry.getBlockEntityAt(segment);
                        PathDescriptorComponent pathDescriptor = blockEntity.getComponent(PathDescriptorComponent.class);
                        if (pathDescriptor == null)
                            return null;

                        Vector3f v2 = segmentSystem.segmentPosition(blockEntity);
                        Quat4f q2 = segmentSystem.segmentRotation(blockEntity);

                        for (Prefab d : pathDescriptor.descriptors) {

                            Segment nextSegment = segmentCacheSystem.getSegment(d);
                            if (segmentSystem.segmentMatch(currentSegment, v1, q1, nextSegment, v2, q2) != SegmentSystem.JointMatch.None) {
                                return new SegmentPair(d, blockEntity);
                            }
                        }
                    }
                    break;
                    case S2: {
                        Vector3i segment = findOffset(blockComponent.getPosition(), blockMappingComponent.s2, blockMappingComponent.s1, rotation);//rotation.rotate(blockMappingComponent.s2).getVector3i());
                        EntityRef blockEntity = blockEntityRegistry.getBlockEntityAt(segment);
                        PathDescriptorComponent pathDescriptor = blockEntity.getComponent(PathDescriptorComponent.class);
                        if (pathDescriptor == null)
                            return null;

                        Vector3f v2 = segmentSystem.segmentPosition(blockEntity);
                        Quat4f q2 = segmentSystem.segmentRotation(blockEntity);

                        for (Prefab d : pathDescriptor.descriptors) {

                            Segment nextSegment = segmentCacheSystem.getSegment(d);
                            if (segmentSystem.segmentMatch(currentSegment, v1, q1, nextSegment, v2, q2) != SegmentSystem.JointMatch.None) {
                                return new SegmentPair(d, blockEntity);
                            }
                        }
                    }
                    break;
                }
            }
        }

        return null;
    }

    private Vector3i findOffset(Vector3i loc, Side main, Side influence, Rotation r) {
        if (main == Side.TOP)
            return new Vector3i(loc).add(r.rotate(main).getVector3i()).add(new Vector3i(r.rotate(influence).getVector3i()).invert());

        Vector3i current = new Vector3i(loc).add(r.rotate(main).getVector3i());
        EntityRef entity = blockEntityRegistry.getBlockEntityAt(current);
        BlockComponent blockComponent = entity.getComponent(BlockComponent.class);
        if (!(blockComponent.getBlock().getBlockFamily() instanceof PathFamily))
            current.add(Vector3i.down());
        return current;
    }
}
