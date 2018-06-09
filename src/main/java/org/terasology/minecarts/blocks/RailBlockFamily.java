/*
 * Copyright 2018 MovingBlocks
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

import com.google.common.collect.Sets;
import gnu.trove.map.TByteObjectMap;
import gnu.trove.map.hash.TByteObjectHashMap;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.math.Rotation;
import org.terasology.math.Side;
import org.terasology.math.SideBitFlag;
import org.terasology.math.geom.Vector3i;
import org.terasology.naming.Name;
import org.terasology.segmentedpaths.blocks.PathFamily;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockBuilderHelper;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.BlockUri;
import org.terasology.world.block.family.BlockSections;
import org.terasology.world.block.family.MultiConnectFamily;
import org.terasology.world.block.family.RegisterBlockFamily;
import org.terasology.world.block.loader.BlockFamilyDefinition;
import org.terasology.world.block.shapes.BlockShape;

import javax.print.attribute.standard.Sides;
import java.util.*;

@RegisterBlockFamily("rails")
@BlockSections({"no_connections", "one_connection", "one_connection_slope", "line_connection", "2d_corner", "2d_t", "cross"})
public class RailBlockFamily extends MultiConnectFamily  implements PathFamily {
    public static final String NO_CONNECTIONS = "no_connections";
    public static final String ONE_CONNECTION = "one_connection";
    public static final String ONE_CONNECTIONS_SLOPE = "one_connection_slope";
    public static final String TWO_CONNECTIONS_LINE = "line_connection";
    public static final String TWO_CONNECTIONS_CORNER = "2d_corner";
    public static final String THREE_CONNECTIONS_T = "2d_t";
    public static final String FOUR_CONNECTIONS_CROSS = "cross";

    private TByteObjectMap<Rotation> rotationMap  = new TByteObjectHashMap<>();

    public RailBlockFamily(BlockFamilyDefinition definition, BlockShape shape, BlockBuilderHelper blockBuilder) {
        super(definition, shape, blockBuilder);
    }

    public RailBlockFamily(BlockFamilyDefinition definition, BlockBuilderHelper blockBuilder) {
        super(definition, blockBuilder);

        BlockUri blockUri = new BlockUri(definition.getUrn());

        this.registerBlock(blockUri,definition,blockBuilder,NO_CONNECTIONS, (byte) 0, Rotation.horizontalRotations());
        this.registerBlock(blockUri,definition,blockBuilder,ONE_CONNECTION, SideBitFlag.getSides(Side.RIGHT), Rotation.horizontalRotations());
        this.registerBlock(blockUri,definition,blockBuilder,ONE_CONNECTIONS_SLOPE, SideBitFlag.getSides(Side.FRONT, Side.TOP), Rotation.horizontalRotations());
        this.registerBlock(blockUri,definition,blockBuilder,TWO_CONNECTIONS_LINE, SideBitFlag.getSides(Side.LEFT, Side.RIGHT), Rotation.horizontalRotations());
        this.registerBlock(blockUri,definition,blockBuilder,TWO_CONNECTIONS_CORNER, SideBitFlag.getSides(Side.LEFT, Side.FRONT), Rotation.horizontalRotations());
        this.registerBlock(blockUri,definition,blockBuilder,THREE_CONNECTIONS_T, SideBitFlag.getSides(Side.LEFT, Side.RIGHT, Side.FRONT), Rotation.horizontalRotations());
        this.registerBlock(blockUri,definition,blockBuilder,FOUR_CONNECTIONS_CROSS, SideBitFlag.getSides(Side.RIGHT, Side.LEFT, Side.BACK, Side.FRONT), Rotation.horizontalRotations());
   }

    @Override
    public Set<Block> registerBlock(BlockUri root, BlockFamilyDefinition definition, BlockBuilderHelper blockBuilder, String name, byte sides, Iterable<Rotation> rotations) {
        Set<Block> result = Sets.newLinkedHashSet();
        for (Rotation rotation: rotations) {
            byte sideBits = 0;
            for (Side side : SideBitFlag.getSides(sides)) {
                sideBits += SideBitFlag.getSide(rotation.rotate(side));
            }
            Block block = blockBuilder.constructTransformedBlock(definition, name, rotation, new BlockUri(root, new Name(String.valueOf(sideBits))), this);
            rotationMap.put(sideBits,rotation);
            blocks.put(sideBits, block);
            result.add(block);
        }
        return result;
    }

    @Override
    public Block getBlockForPlacement(Vector3i location, Side attachmentSide, Side direction) {
        byte connections = 0;
        for (Side connectSide : SideBitFlag.getSides(getConnectionSides())) {
            if (this.connectionCondition(location, connectSide) && !isFullyConnected(location,connectSide)) {
                connections |= SideBitFlag.getSide(connectSide);
            }
        }

        for (Side connectSide : SideBitFlag.getSides(getConnectionSides())) {
            if (this.connectionCondition(new Vector3i(location).add(Vector3i.down()), connectSide)) {
                connections |= SideBitFlag.getSide(connectSide);
            }
        }

        for (Side connectSide : SideBitFlag.getSides(getConnectionSides())) {
            if (this.connectionCondition(new Vector3i(location).add(Vector3i.up()), connectSide)) {
                connections |= SideBitFlag.getSide(Side.TOP);
                if(SideBitFlag.getSides(connections).size() == 1){
                    connections |= SideBitFlag.getSide(connectSide.reverse());
                }
                break;
            }
        }
        return blocks.get(connections);
    }



    @Override
    public Block getBlockForNeighborUpdate(Vector3i location, Block oldBlock) {
        return oldBlock;
    }



    /**
     * a fully connected tile has more then 1 connected edge and is not attached to the reference tile
     *
     * @param location
     * @param connectSide
     * @return
     */
    private boolean isFullyConnected(Vector3i location, Side connectSide) {
        if (connectionCondition(location, connectSide)) {
            Vector3i neighborLocation = new Vector3i(location);
            neighborLocation.add(connectSide.getVector3i());
            EnumSet<Side> sides = SideBitFlag.getSides(Byte.parseByte(worldProvider.getBlock(neighborLocation).getURI().getIdentifier().toString()));

            for (Side side : sides) {
                if (side == Side.TOP || side == Side.BOTTOM)
                    continue;
                if (new Vector3i(neighborLocation).add(side.getVector3i()).equals(location)) {
                    return false;
                }
            }
            if (sides.size() > 1)
                return true;
        }
        return false;
    }


    @Override
    protected boolean connectionCondition(Vector3i blockLocation, Side connectSide) {
        Vector3i neighborLocation = new Vector3i(blockLocation);
        neighborLocation.add(connectSide.getVector3i());
        EntityRef neighborEntity = blockEntityRegistry.getEntityAt(neighborLocation);
        return neighborEntity != null && neighborEntity.hasComponent(RailComponent.class);
    }

    @Override
    public byte getConnectionSides() {
        return SideBitFlag.getSides(Side.LEFT,Side.FRONT,Side.BACK,Side.RIGHT);
    }

    @Override
    public Block getArchetypeBlock() {
        return blocks.get(SideBitFlag.getSides(Side.RIGHT,Side.LEFT));
    }

    public Block getBlockByConnection(byte connectionSides) {
        return blocks.get(connectionSides);
    }


    @Override
    public Rotation getRotationFor(BlockUri blockUri) {
        if (getURI().equals(blockUri.getFamilyUri())) {
            try {
                byte connections = Byte.parseByte(blockUri.getIdentifier().toString());
                return rotationMap.get(connections);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

}
