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

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.math.Rotation;
import org.terasology.math.Side;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector3i;
import org.terasology.rails.minecarts.components.PathDescriptorComponent;
import org.terasology.rails.tracks.CubicBezier;
import org.terasology.rails.tracks.TrackSegment;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.BlockComponent;

/**
 * Created by michaelpollind on 8/16/16.
 */
public class RailBlockTrackSegment extends TrackSegment {

    private  Rotation rotation;
    private  Side start;
    private  Side end;
    private  WorldProvider worldProvider;
    private  RailBlockTrackSegmentSystem railBlockTrackSegmentSystem;
    private BlockEntityRegistry blockEntityRegistry;

    public  Rotation getRotation()
    {
        return  rotation;
    }

    public  Side getStart()
    {
        return  start;
    }

    public  Side getEnd()
    {
        return  end;
    }

    public RailBlockTrackSegment(CubicBezier[] curves,BlockEntityRegistry blockEntityRegistry, PathDescriptorComponent.Descriptor descriptor, Rotation rotation, WorldProvider worldProvider, RailBlockTrackSegmentSystem railBlockTrackSegmentSystem, Vector3f startingBinormal) {
        super(curves,  startingBinormal);
        this.rotation = rotation;
        this.start = rotation.rotate(descriptor.start);
        this.end = rotation.rotate(descriptor.end);
        this.worldProvider = worldProvider;
        this.blockEntityRegistry = blockEntityRegistry;
        this.railBlockTrackSegmentSystem = railBlockTrackSegmentSystem;
    }

    @Override
    public boolean invertSegment(TrackSegment previous, TrackSegment next) {

        if(((RailBlockTrackSegment)previous).end == Side.TOP) {
            if(((RailBlockTrackSegment)previous).start == ((RailBlockTrackSegment)next).end)
                return true;
        }
        if(((RailBlockTrackSegment)previous).start == Side.TOP) {
            if(((RailBlockTrackSegment)previous).start == ((RailBlockTrackSegment)next).end)
                return true;
        }

        if(((RailBlockTrackSegment)previous).end == ((RailBlockTrackSegment)next).end.reverse())
            return true;

        if(((RailBlockTrackSegment)previous).start == ((RailBlockTrackSegment)next).start.reverse())
            return true;
        return  false;
    }

    @Override
    public TrackSegmentPair getNextSegment(EntityRef ref) {
        Side direction = end;

        Vector3i blockPosition = new Vector3i(ref.getComponent(BlockComponent.class).getPosition()).add(end.getVector3i());
        EntityRef nextRef =  blockEntityRegistry.getBlockEntityAt(blockPosition);
        BlockComponent block = nextRef.getComponent(BlockComponent.class);

        if(end == Side.TOP)
        {
            nextRef =  blockEntityRegistry.getBlockEntityAt(new Vector3i(blockPosition).add(start.reverse().getVector3i()));
            block = nextRef.getComponent(BlockComponent.class);
            direction = start.reverse();
        }

        if(!(block.getBlock().getBlockFamily() instanceof  RailsUpdatesFamily)) {

            blockPosition.add(Vector3i.down());
            nextRef =  blockEntityRegistry.getBlockEntityAt(blockPosition);
            block = nextRef.getComponent(BlockComponent.class);
            if(!(block.getBlock().getBlockFamily() instanceof  RailsUpdatesFamily)) {
                return null;
            }
        }

        return  new TrackSegmentPair(railBlockTrackSegmentSystem.getSegment(block.getBlock().getURI(),direction),nextRef);
    }


    @Override
    public TrackSegmentPair getPreviousSegment(EntityRef ref) {
        Side direction = start;
        Vector3i blockPosition = new Vector3i(ref.getComponent(BlockComponent.class).getPosition()).add(start.getVector3i());
        EntityRef nextRef =  blockEntityRegistry.getBlockEntityAt(blockPosition);
        BlockComponent block = nextRef.getComponent(BlockComponent.class);

        if(start == Side.TOP)
        {
            direction = end.reverse();
            nextRef =  blockEntityRegistry.getBlockEntityAt(new Vector3i(blockPosition).add(end.reverse().getVector3i()));
            block = nextRef.getComponent(BlockComponent.class);

        }

        if(!(block.getBlock().getBlockFamily() instanceof  RailsUpdatesFamily)) {

            blockPosition.add(Vector3i.down());
            nextRef =  blockEntityRegistry.getBlockEntityAt(blockPosition);
            block = nextRef.getComponent(BlockComponent.class);
            if(!(block.getBlock().getBlockFamily() instanceof  RailsUpdatesFamily)) {
                return null;
            }
        }

        return  new TrackSegmentPair(railBlockTrackSegmentSystem.getSegment(block.getBlock().getURI(),direction),nextRef);
    }
}
