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
package org.terasology.rails.minecarts.blocks;

import gnu.trove.map.TByteObjectMap;
import gnu.trove.map.hash.TByteObjectHashMap;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.math.Rotation;
import org.terasology.math.Side;
import org.terasology.math.geom.Vector3f;
import org.terasology.rails.minecarts.components.PathDescriptorComponent;
import org.terasology.rails.tracks.CubicBezier;
import org.terasology.registry.In;
import org.terasology.registry.Share;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.BlockUri;
import org.terasology.world.block.family.BlockFamily;

/**
 * Created by michaelpollind on 8/17/16.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
@Share(value = RailBlockTrackSegmentSystem.class)
public class RailBlockTrackSegmentSystem extends BaseComponentSystem {

    @In
    private  WorldProvider worldProvider;

    @In
    private  BlockEntityRegistry blockEntityRegistry;

    @In
    private BlockManager blockManager;

    private TByteObjectMap<RailBlockTrackSegment[]> trackSegment = new TByteObjectHashMap<>();


    public  RailBlockTrackSegment getSegment(BlockUri uri,Side direction )
    {
        byte connection = getConnections(uri);
        Block block = blockManager.getBlock(uri);
        BlockFamily family =  block.getBlockFamily();

        if(family instanceof  RailsUpdatesFamily)
        {
            if(!trackSegment.containsKey(connection))
            {
                RailsUpdatesFamily railFamily =  ((RailsUpdatesFamily)family);

                PathDescriptorComponent pathDescriptor = block.getEntity().getComponent(PathDescriptorComponent.class);
                RailBlockTrackSegment[] segments = new RailBlockTrackSegment[pathDescriptor.descriptors.size()];
                for(int x = 0;x < pathDescriptor.descriptors.size(); x++)
                {
                   PathDescriptorComponent.Descriptor descriptor =  pathDescriptor.descriptors.get(x);

                    CubicBezier[] curves = new CubicBezier[descriptor.path.size()];
                    pathDescriptor.descriptors.get(x).path.toArray(curves);

                    Vector3f startingBinormal = new Vector3f(pathDescriptor.descriptors.get(x).startingBinormal);
                    Rotation blockRotation = railFamily.getRotationFor(uri);
                    if(blockRotation == null)
                        blockRotation = Rotation.none();


                    segments[x] = new RailBlockTrackSegment(curves,blockEntityRegistry,descriptor,blockRotation,worldProvider,this,startingBinormal);
                }

                trackSegment.put(connection,segments);
            }


            RailBlockTrackSegment[] segments = trackSegment.get(connection);
            if(direction == null)
                return  trackSegment.get(connection)[0];
            for(int x = 0; x < segments.length; x++)
            {

                boolean hasMatching = false;
                if(direction.reverse() == segments[x].getStart())
                    return segments[x];
                if(direction.reverse() == segments[x].getEnd())
                    return segments[x];

            }
            return  trackSegment.get(connection)[0];
        }

        return  null;

    }

    private Byte getConnections(BlockUri blockUri) {
        return Byte.parseByte(blockUri.getIdentifier().toString());
    }

}
