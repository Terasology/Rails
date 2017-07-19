/*
 * Copyright 2014 MovingBlocks
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
package org.terasology.minecarts.blocks;

import gnu.trove.map.TByteObjectMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.console.Console;
import org.terasology.math.Rotation;
import org.terasology.math.Side;
import org.terasology.math.SideBitFlag;
import org.terasology.math.geom.Vector3i;
import org.terasology.segmentedpaths.blocks.PathFamily;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.BlockUri;
import org.terasology.world.block.family.AbstractBlockFamily;
import org.terasology.world.block.family.ConnectionCondition;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class RailsUpdateFamily extends AbstractBlockFamily implements PathFamily {
    private static final Logger logger = LoggerFactory.getLogger(RailsUpdateFamily.class);

    private ConnectionCondition connectionCondition;
    private Block archetypeBlock;

    private TByteObjectMap<Block> blocks;
    private TByteObjectMap<Rotation> rotation;
    private byte connectionSides;

    public RailsUpdateFamily(ConnectionCondition connectionCondition, BlockUri blockUri,
                             List<String> categories, Block archetypeBlock, TByteObjectMap<Block> blocks, byte connectionSides, TByteObjectMap<Rotation> rotation) {

        super(blockUri, categories);
        this.connectionCondition = connectionCondition;
        this.archetypeBlock = archetypeBlock;
        this.blocks = blocks;
        this.connectionSides = connectionSides;
        this.rotation = rotation;

        for (Block block : blocks.valueCollection()) {
            block.setBlockFamily(this);
        }
    }

    @Override
    public Block getArchetypeBlock() {
        return archetypeBlock;
    }

    @Override
    public Block getBlockForPlacement(WorldProvider worldProvider, BlockEntityRegistry blockEntityRegistry, Vector3i location, Side attachmentSide, Side direction) {
        return blocks.get(getByteConnections(worldProvider, blockEntityRegistry, location));
    }

    public Block getBlockForNeighborRailUpdate(WorldProvider worldProvider, BlockEntityRegistry blockEntityRegistry, Vector3i location, Block oldBlock) {
        return blocks.get(getByteConnections(worldProvider, blockEntityRegistry, location));
    }


    public Rotation getRotationFor(BlockUri blockUri) {
        if (getURI().equals(blockUri.getFamilyUri())) {
            try {
                byte connections = Byte.parseByte(blockUri.getIdentifier().toString());
                return rotation.get(connections);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    @Override
    public Block getBlockFor(BlockUri blockUri) {
        if (getURI().equals(blockUri.getFamilyUri())) {
            try {
                byte connections = Byte.parseByte(blockUri.getIdentifier().toString());
                return blocks.get(connections);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    @Override
    public Iterable<Block> getBlocks() {
        return blocks.valueCollection();
    }


    private boolean isFullyConnected(Vector3i location, Side connectSide, WorldProvider worldProvider, BlockEntityRegistry blockEntityRegistry)
    {
        if (connectionCondition.isConnectingTo(location, connectSide, worldProvider, blockEntityRegistry)) {
            Vector3i neighborLocation = new Vector3i(location);
            neighborLocation.add(connectSide.getVector3i());
            EnumSet<Side> sides = SideBitFlag.getSides(Byte.parseByte(worldProvider.getBlock(neighborLocation).getURI().getIdentifier().toString()));

            for(Side side : sides)
            {
                if(side == Side.TOP || side == Side.BOTTOM)
                    continue;
                if (new Vector3i(neighborLocation).add(side.getVector3i()).equals(location)) {
                    return false;
                }
            }
            if(sides.size() > 1)
                return  true;
        }
        return false;
    }

    private byte getByteConnections(WorldProvider worldProvider, BlockEntityRegistry blockEntityRegistry, Vector3i location) {
        byte connections = 0;
        byte fullConnectedEdges = 0;
        int countConnetions = 0;
        boolean hasTopBlock = false;
        ArrayList<Side> skipSides = new ArrayList<Side>();
        Vector3i upLocation = new Vector3i(location);
        upLocation.y += 1;
        Block block = worldProvider.getBlock(upLocation);

        if (block.getURI() != BlockManager.AIR_ID && !block.isPenetrable() && block.isLiquid()) {
            hasTopBlock = true;
        }

        for (Side connectSide : SideBitFlag.getSides(connectionSides)) {
            if (connectionCondition.isConnectingTo(location, connectSide, worldProvider, blockEntityRegistry)) {
                if(isFullyConnected(location, connectSide, worldProvider, blockEntityRegistry))
                    fullConnectedEdges += SideBitFlag.getSide(connectSide);
                else
                    connections += SideBitFlag.getSide(connectSide);

            } else if (hasTopBlock) {
                block = worldProvider.getBlock(location);

                if (block.getURI() != BlockManager.AIR_ID && !block.isPenetrable() && block.isLiquid()) {
                    skipSides.add(connectSide);
                }
            }
        }
        if(connections == 0)
            connections = fullConnectedEdges;

        countConnetions = SideBitFlag.getSides(connections).size();

        upLocation.y -= 2;
        for (Side connectSide : SideBitFlag.getSides(connectionSides)) {
            if (connectionCondition.isConnectingTo(upLocation, connectSide, worldProvider, blockEntityRegistry)) {
                connections += SideBitFlag.getSide(connectSide);
            }
        }
        upLocation.y += 2;
        switch (countConnetions) {
            case 0:
                for (Side connectSide : SideBitFlag.getSides(connectionSides)) {
                    if (skipSides.contains(connectSide)) {
                        continue;
                    }
                    if (connectionCondition.isConnectingTo(upLocation, connectSide, worldProvider, blockEntityRegistry)) {
                        connections = 0;
                        connections += SideBitFlag.getSide(connectSide);
                        connections += SideBitFlag.getSide(Side.TOP);
                        break;
                    }
                }
                break;
            case 1:
                EnumSet<Side> sides = SideBitFlag.getSides(connections);
                Side connectSide = (Side) sides.toArray()[0];
                connectSide = connectSide.reverse();
                if (skipSides.contains(connectSide)) {
                    break;
                }
                if (connectionCondition.isConnectingTo(upLocation, connectSide, worldProvider, blockEntityRegistry)) {
                    connections = 0;
                    connections += SideBitFlag.getSide(connectSide);
                    connections += SideBitFlag.getSide(Side.TOP);
                    break;
                }
                break;
        }
        return connections;
    }
}
