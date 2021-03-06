// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.minecarts.blocks;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.prefab.Prefab;
import org.terasology.engine.math.Rotation;
import org.terasology.engine.math.Side;
import org.terasology.engine.world.BlockEntityRegistry;
import org.terasology.engine.world.block.BlockComponent;
import org.terasology.engine.world.block.family.BlockFamily;
import org.terasology.segmentedpaths.SegmentMeta;
import org.terasology.segmentedpaths.blocks.PathFamily;
import org.terasology.segmentedpaths.components.BlockMappingComponent;
import org.terasology.segmentedpaths.components.PathDescriptorComponent;
import org.terasology.segmentedpaths.controllers.PathFollowerSystem;
import org.terasology.segmentedpaths.controllers.SegmentCacheSystem;
import org.terasology.segmentedpaths.controllers.SegmentMapping;
import org.terasology.segmentedpaths.controllers.SegmentSystem;
import org.terasology.segmentedpaths.segments.Segment;

/**
 * Created by michaelpollind on 4/9/17.
 */
public class RailBlockSegmentMapper implements SegmentMapping {

    private final PathFollowerSystem pathFollowerSystem;
    private final SegmentSystem segmentSystem;
    private final SegmentCacheSystem segmentCacheSystem;
    private final BlockEntityRegistry blockEntityRegistry;

    public RailBlockSegmentMapper(BlockEntityRegistry blockEntityRegistry, PathFollowerSystem pathFollowerSystem,
                                  SegmentSystem segmentSystem, SegmentCacheSystem segmentCacheSystem) {
        this.blockEntityRegistry = blockEntityRegistry;
        this.pathFollowerSystem = pathFollowerSystem;
        this.segmentCacheSystem = segmentCacheSystem;
        this.segmentSystem = segmentSystem;
    }


    @Override
    public MappingResult nextSegment(SegmentMeta meta, SegmentEnd ends) {
        if (meta.association.hasComponent(BlockComponent.class)) {
            BlockComponent blockComponent = meta.association.getComponent(BlockComponent.class);
            BlockFamily blockFamily = blockComponent.getBlock().getBlockFamily();

            Vector3f v1 = segmentSystem.segmentPosition(meta.association);
            Quaternionf q1 = segmentSystem.segmentRotation(meta.association);

            Segment currentSegment = segmentCacheSystem.getSegment(meta.prefab);


            BlockMappingComponent blockMappingComponent = meta.prefab.getComponent(BlockMappingComponent.class);
            if (blockFamily instanceof PathFamily) {

                Rotation rotation = ((PathFamily) blockFamily).getRotationFor(blockComponent.getBlock().getURI());
                switch (ends) {
                    case START: {
                        Vector3i segment = findOffset(blockComponent.getPosition(),
                                blockMappingComponent.s1, blockMappingComponent.s2, rotation);//rotation.rotate
                        // (blockMappingComponent.s1).getVector3i());
                        EntityRef blockEntity = blockEntityRegistry.getBlockEntityAt(segment);
                        PathDescriptorComponent pathDescriptor =
                                blockEntity.getComponent(PathDescriptorComponent.class);
                        if (pathDescriptor == null) {
                            return null;
                        }

                        Vector3f v2 = segmentSystem.segmentPosition(blockEntity);
                        Quaternionf q2 = segmentSystem.segmentRotation(blockEntity);

                        for (Prefab d : pathDescriptor.descriptors) {

                            Segment nextSegment = segmentCacheSystem.getSegment(d);
                            if (segmentSystem.segmentMatch(currentSegment, v1, q1, nextSegment, v2, q2) != SegmentSystem.JointMatch.None) {
                                return new MappingResult(d, blockEntity);
                            }
                        }
                    }
                    break;
                    case END: {
                        Vector3i segment = findOffset(blockComponent.getPosition(),
                                blockMappingComponent.s2, blockMappingComponent.s1, rotation);//rotation.rotate
                        // (blockMappingComponent.s2).getVector3i());
                        EntityRef blockEntity = blockEntityRegistry.getBlockEntityAt(segment);
                        PathDescriptorComponent pathDescriptor =
                                blockEntity.getComponent(PathDescriptorComponent.class);
                        if (pathDescriptor == null) {
                            return null;
                        }

                        Vector3f v2 = segmentSystem.segmentPosition(blockEntity);
                        Quaternionf q2 = segmentSystem.segmentRotation(blockEntity);

                        for (Prefab d : pathDescriptor.descriptors) {

                            Segment nextSegment = segmentCacheSystem.getSegment(d);
                            if (segmentSystem.segmentMatch(currentSegment, v1, q1, nextSegment, v2, q2) != SegmentSystem.JointMatch.None) {
                                return new MappingResult(d, blockEntity);
                            }
                        }
                    }
                    break;
                }
            }
        }

        return null;
    }

    private Vector3i findOffset(Vector3ic loc, Side main, Side influence, Rotation r) {
        if (main == Side.TOP) {
            return new Vector3i(loc).add(r.rotate(main).direction()).add(new Vector3i(r.rotate(influence).direction()).mul(-1));
        }

        Vector3i current = new Vector3i(loc).add(r.rotate(main).direction());
        EntityRef entity = blockEntityRegistry.getBlockEntityAt(current);
        BlockComponent blockComponent = entity.getComponent(BlockComponent.class);
        if (!(blockComponent.getBlock().getBlockFamily() instanceof PathFamily)) {
            current.add(new Vector3i(0, -1, 0));
        }
        return current;
    }
}
