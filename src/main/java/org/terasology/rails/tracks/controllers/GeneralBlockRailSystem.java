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
import org.terasology.entitySystem.entity.lifecycleEvents.OnActivatedComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnAddedComponent;
import org.terasology.entitySystem.event.EventPriority;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.math.Rotation;
import org.terasology.math.Side;
import org.terasology.math.geom.Quat4f;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector3i;
import org.terasology.rails.minecarts.blocks.PathFamily;
import org.terasology.rails.minecarts.blocks.RailsUpdateFamily;
import org.terasology.rails.tracks.Segment;
import org.terasology.rails.tracks.components.BlockMappingComponent;
import org.terasology.rails.tracks.components.GeneralBlockRailComponent;
import org.terasology.rails.tracks.components.PathDescriptorComponent;
import org.terasology.registry.In;
import org.terasology.registry.Share;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.family.BlockFamily;

/**
 * Created by michaelpollind on 4/4/17.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class GeneralBlockRailSystem extends BaseComponentSystem {
    /*@In
    SegmentSystem segmentSystem;

    @In
    SegmentCacheSystem segmentCacheSystem;

    @In
    private BlockEntityRegistry blockEntityRegistry;

    @In
    WorldProvider worldProvider;

    //@ReceiveEvent(components = {GeneralBlockRailComponent.class,BlockComponent.class})
    public void onGeneralRailBlockComponent(OnAddedComponent event, EntityRef entity)
    {
        if(!entity.hasComponent(GeneralBlockRailComponent.class))
            return;
        entity.removeComponent(GeneralBlockRailComponent.class);

        PathDescriptorComponent pathDescriptorComponent =  entity.getComponent(PathDescriptorComponent.class);
        BlockComponent blockComponent =  entity.getComponent(BlockComponent.class);

        Vector3f p1 = segmentSystem.segmentPosition(entity);
        Quat4f q1 = segmentSystem.segmentRotation(entity);

        BlockFamily blockFamily = blockComponent.getBlock().getBlockFamily();
        if(blockFamily instanceof PathFamily)
        {
            for(PathDescriptorComponent.Descriptor d : pathDescriptorComponent.descriptors)
            {
                BlockMappingComponent blockMapping =  d.reference.getComponent(BlockMappingComponent.class);
                Rotation rotation = ((PathFamily) blockFamily).getRotationFor(blockComponent.getBlock().getURI());

                Vector3i s1 = new Vector3i(blockComponent.getPosition()).add(rotation.rotate(blockMapping.s1).getVector3i());
                EntityRef e1 = blockEntityRegistry.getBlockEntityAt(s1);
                if(isValidEntiy(e1)) {
                    blockJoin(e1);
                }

                Vector3i s2 = new Vector3i(blockComponent.getPosition()).add(rotation.rotate(blockMapping.s2).getVector3i());
                EntityRef e2 = blockEntityRegistry.getBlockEntityAt(s2);
                if(isValidEntiy(e2)) {
                    blockJoin(e2);
                }
            }
            if(isValidEntiy(entity))
             blockJoin(entity);
        }

    }

    private boolean isValidEntiy(EntityRef e)
    {
        if(e == null)
            return false;
        BlockComponent component = e.getComponent(BlockComponent.class);
        if(component  == null)
            return false;
        return component.getBlock().getBlockFamily() instanceof PathFamily;
    }

    private  void  blockJoin(EntityRef entity)
    {
        BlockComponent blockComponent =  entity.getComponent(BlockComponent.class);
        PathDescriptorComponent pathDescriptorComponent =  entity.getComponent(PathDescriptorComponent.class);

        Vector3f p1 = segmentSystem.segmentPosition(entity);
        Quat4f r1 = segmentSystem.segmentRotation(entity);

        BlockFamily blockFamily = blockComponent.getBlock().getBlockFamily();
        if(blockFamily instanceof PathFamily)
        {
            for(PathDescriptorComponent.Descriptor d : pathDescriptorComponent.descriptors)
            {
                BlockMappingComponent blockMappingComponent =  d.reference.getComponent(BlockMappingComponent.class);
                Rotation rotation = ((PathFamily) blockFamily).getRotationFor(blockComponent.getBlock().getURI());

                Vector3i s1 = new Vector3i(blockComponent.getPosition()).add(rotation.rotate(blockMappingComponent.s1).getVector3i());
                EntityRef e1 = blockEntityRegistry.getBlockEntityAt(s1);
                Quat4f q2 = segmentSystem.segmentRotation(e1);
                if(isValidEntiy(e1)) {
                    segmentSystem.createdEndpointJoint(d, p1, r1, e1, s1.toVector3f(), q2);
                }

                Vector3i s2 = new Vector3i(blockComponent.getPosition()).add(rotation.rotate(blockMappingComponent.s2).getVector3i());
                EntityRef e2 = blockEntityRegistry.getBlockEntityAt(s2);
                Quat4f q3 = segmentSystem.segmentRotation(e1);
                if(isValidEntiy(e2))
                    segmentSystem.createdEndpointJoint(d,p1,r1,e2,s2.toVector3f(),q3);

            }
        }
        entity.saveComponent(pathDescriptorComponent);
    }*/

}
