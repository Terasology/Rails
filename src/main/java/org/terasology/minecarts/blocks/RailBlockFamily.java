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
import org.joml.Vector3ic;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.math.Rotation;
import org.terasology.math.Side;
import org.terasology.math.SideBitFlag;
import org.terasology.math.geom.Vector3i;
import org.terasology.naming.Name;
import org.terasology.segmentedpaths.blocks.PathFamily;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockBuilderHelper;
import org.terasology.world.block.BlockUri;
import org.terasology.world.block.family.BlockSections;
import org.terasology.world.block.family.MultiConnectFamily;
import org.terasology.world.block.family.RegisterBlockFamily;
import org.terasology.world.block.loader.BlockFamilyDefinition;
import org.terasology.world.block.shapes.BlockShape;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RegisterBlockFamily("rails")
@BlockSections({"no_connections", "one_connection", "one_connection_slope", "line_connection", "2d_corner", "2d_t", "cross"})
public class RailBlockFamily extends MultiConnectFamily implements PathFamily {
    public static final String NO_CONNECTIONS = "no_connections";
    public static final String ONE_CONNECTION = "one_connection";
    public static final String ONE_CONNECTIONS_SLOPE = "one_connection_slope";
    public static final String TWO_CONNECTIONS_LINE = "line_connection";
    public static final String TWO_CONNECTIONS_CORNER = "2d_corner";
    public static final String THREE_CONNECTIONS_T = "2d_t";
    public static final String FOUR_CONNECTIONS_CROSS = "cross";

    private TByteObjectMap<Rotation> rotationMap = new TByteObjectHashMap<>();
    private Map<String, Byte> baseSideBitMap = new HashMap<>();

    public RailBlockFamily(BlockFamilyDefinition definition, BlockShape shape, BlockBuilderHelper blockBuilder) {
        super(definition, shape, blockBuilder);
        initSideBitMap();
    }

    public RailBlockFamily(BlockFamilyDefinition definition, BlockBuilderHelper blockBuilder) {
        super(definition, blockBuilder);

        BlockUri blockUri = new BlockUri(definition.getUrn());

        initSideBitMap();

        for (String k : baseSideBitMap.keySet()) {
            this.registerBlock(blockUri, definition, blockBuilder, k, baseSideBitMap.get(k),
                Rotation.horizontalRotations());
        }
    }

    private void initSideBitMap() {
        baseSideBitMap.put(NO_CONNECTIONS, (byte) 0);
        baseSideBitMap.put(ONE_CONNECTION, (byte) 0b010000);
        baseSideBitMap.put(ONE_CONNECTIONS_SLOPE, (byte) 0b000101);
        baseSideBitMap.put(TWO_CONNECTIONS_LINE, (byte) 0b010010);
        baseSideBitMap.put(TWO_CONNECTIONS_CORNER, (byte) 0b000110);
        baseSideBitMap.put(THREE_CONNECTIONS_T, (byte) 0b010110);
        baseSideBitMap.put(FOUR_CONNECTIONS_CROSS, (byte) 0b110110);
    }

    @Override
    public Set<Block> registerBlock(BlockUri root, BlockFamilyDefinition definition, BlockBuilderHelper blockBuilder,
                                    String name, byte sides, Iterable<Rotation> rotations) {
        Set<Block> result = Sets.newLinkedHashSet();
        for (Rotation rotation : rotations) {
            byte sideBits = 0;
            for (Side side : SideBitFlag.getSides(sides)) {
                sideBits += SideBitFlag.getSide(rotation.rotate(side));
            }
            Block block = blockBuilder.constructTransformedBlock(definition, name, rotation, new BlockUri(root,
                new Name(String.valueOf(sideBits))), this);
            rotationMap.put(sideBits, rotation);
            blocks.put(sideBits, block);
            result.add(block);
        }
        return result;
    }


    @Override
    public Block getBlockForPlacement(Vector3i location, Side attachmentSide, Side direction) {
        byte connections = 0;
        for (Side connectSide : SideBitFlag.getSides(getConnectionSides())) {
            if (this.connectionCondition(location, connectSide) && !isFullyConnected(location, connectSide)) {
                connections |= SideBitFlag.getSide(connectSide);
            }
        }

        for (Side connectSide : SideBitFlag.getSides(getConnectionSides())) {
            if (this.connectionCondition(new Vector3i(location).add(Vector3i.down()), connectSide)) {
                connections |= SideBitFlag.getSide(connectSide);
            }
        }

        Side topSide = Side.BOTTOM;
        for (Side connectSide : SideBitFlag.getSides(getConnectionSides())) {
            if (this.connectionCondition(new Vector3i(location).add(Vector3i.up()), connectSide)) {
                connections |= SideBitFlag.getSide(Side.TOP);
                topSide = connectSide;
                if (SideBitFlag.getSides(connections).size() == 1) {
                    connections |= SideBitFlag.getSide(connectSide.reverse());
                }
                break;
            }
        }

        Block result = blocks.get(connections);
        if (result != null) {
            return result;
        } else {
            return getClosestMatch(connections, topSide);
        }
    }

    private Block getClosestMatch(byte connections, Side topSide) {
        EnumSet<Side> sides = SideBitFlag.getSides(connections);

        // Indices represent priorities, 0 being the highest and 6 being the lowest
        String[] keys = new String[baseSideBitMap.size()];
        keys[0] = FOUR_CONNECTIONS_CROSS;
        keys[1] = THREE_CONNECTIONS_T;
        keys[2] = ONE_CONNECTIONS_SLOPE;
        keys[3] = TWO_CONNECTIONS_CORNER;
        keys[4] = TWO_CONNECTIONS_LINE;
        keys[5] = ONE_CONNECTION;
        keys[6] = NO_CONNECTIONS;

        for (String k : keys) {
            Block result = checkConnection(sides, k, topSide);
            if (result != null) {
                return result;
            }
        }

        return blocks.get((byte) 0); // default block, simple no connection block
    }

    private Block checkConnection(EnumSet<Side> sides, String connection, Side topSide) {

        // if building slopes, make sure it is in the direction of the top block
        if (connection.equals(ONE_CONNECTIONS_SLOPE)) {
            if (sides.contains(Side.TOP) && sides.contains(topSide.reverse())) {
                byte b = SideBitFlag.getSides(topSide.reverse(), Side.TOP);
                return blocks.get(b);
            } else {
                return null;
            }
        }

        Set<Byte> arrangements = new HashSet<>();

        for (Rotation rotation : Rotation.horizontalRotations()) {
            byte sideBits = 0;
            for (Side side : SideBitFlag.getSides(baseSideBitMap.get(connection))) {
                sideBits += SideBitFlag.getSide(rotation.rotate(side));
            }
            arrangements.add(sideBits);
        }

        for (byte b : arrangements) {
            if (sides.containsAll(SideBitFlag.getSides(b))) {
                return blocks.get(b);
            }
        }

        return null;
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
            EnumSet<Side> sides =
                SideBitFlag.getSides(Byte.parseByte(worldProvider.getBlock(neighborLocation).getURI().getIdentifier().toString()));

            for (Side side : sides) {
                if (side == Side.TOP || side == Side.BOTTOM) {
                    continue;
                }
                if (new Vector3i(neighborLocation).add(side.getVector3i()).equals(location)) {
                    return false;
                }
            }
            if (sides.size() > 1) {
                return true;
            }

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
    protected boolean connectionCondition(Vector3ic blockLocation, Side connectSide) {
        org.joml.Vector3i neighborLocation = new org.joml.Vector3i(blockLocation);
        neighborLocation.add(connectSide.direction());
        EntityRef neighborEntity = blockEntityRegistry.getEntityAt(neighborLocation);
        return neighborEntity != null && neighborEntity.hasComponent(RailComponent.class);
    }

    @Override
    public byte getConnectionSides() {
        return SideBitFlag.getSides(Side.LEFT, Side.FRONT, Side.BACK, Side.RIGHT);
    }

    @Override
    public Block getArchetypeBlock() {
        return blocks.get(SideBitFlag.getSides(Side.RIGHT, Side.LEFT));
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
