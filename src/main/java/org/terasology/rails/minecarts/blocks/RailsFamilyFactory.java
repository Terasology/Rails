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
package org.terasology.rails.minecarts.blocks;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import gnu.trove.iterator.TByteObjectIterator;
import gnu.trove.map.TByteObjectMap;
import gnu.trove.map.hash.TByteObjectHashMap;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.math.Rotation;
import org.terasology.math.Side;
import org.terasology.math.SideBitFlag;
import org.terasology.math.geom.Vector3i;
import org.terasology.naming.Name;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockBuilderHelper;
import org.terasology.world.block.BlockUri;
import org.terasology.world.block.family.BlockFamily;
import org.terasology.world.block.family.BlockFamilyFactory;
import org.terasology.world.block.family.ConnectionCondition;
import org.terasology.world.block.family.RegisterBlockFamilyFactory;
import org.terasology.world.block.loader.BlockFamilyDefinition;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@RegisterBlockFamilyFactory(value = "Rails:rails")
public class RailsFamilyFactory implements BlockFamilyFactory  {
    public static final String NO_CONNECTIONS = "no_connections";
    public static final String ONE_CONNECTION = "one_connection";
    public static final String ONE_CONNECTIONS_SLOPE = "one_connection_slope";
    public static final String TWO_CONNECTIONS_LINE = "line_connection";
    public static final String TWO_CONNECTIONS_CORNER = "2d_corner";
    public static final String THREE_CONNECTIONS_T = "2d_t";
    public static final String FOUR_CONNECTIONS_CROSS = "cross";
    private static final Map<String, Byte> RAILS_MAPPING =
            new HashMap<String, Byte>() { {
                put(NO_CONNECTIONS, (byte) 0);
                put(ONE_CONNECTION, SideBitFlag.getSides(Side.RIGHT));
                put(ONE_CONNECTIONS_SLOPE, SideBitFlag.getSides(Side.BACK, Side.TOP));
                put(TWO_CONNECTIONS_LINE, SideBitFlag.getSides(Side.LEFT, Side.RIGHT));
                put(TWO_CONNECTIONS_CORNER, SideBitFlag.getSides(Side.LEFT, Side.FRONT));
                put(THREE_CONNECTIONS_T, SideBitFlag.getSides(Side.LEFT, Side.RIGHT, Side.FRONT));
                put(FOUR_CONNECTIONS_CROSS, SideBitFlag.getSides(Side.RIGHT, Side.LEFT, Side.BACK, Side.FRONT));
            } };

    private ConnectionCondition connectionCondition;
    private byte connectionSides;

    TByteObjectMap<Rotation> rotations = new TByteObjectHashMap<>();

    public RailsFamilyFactory() {
        connectionCondition = new RailsConnectionCondition();
        connectionSides = SideBitFlag.getSides(Side.BACK,Side.FRONT,Side.RIGHT,Side.LEFT,Side.TOP);
    }

    @Override
    public Set<String> getSectionNames() {

        return ImmutableSet.<String>builder()
                .add(NO_CONNECTIONS)
                .add(ONE_CONNECTION)
                .add(ONE_CONNECTIONS_SLOPE)
                .add(TWO_CONNECTIONS_LINE)
                .add(TWO_CONNECTIONS_CORNER)
                .add(THREE_CONNECTIONS_T)
                .add(FOUR_CONNECTIONS_CROSS)
                .build();
    }

    @Override
    public BlockFamily createBlockFamily(BlockFamilyDefinition definition, BlockBuilderHelper blockBuilder) {
        TByteObjectMap<String>[] basicBlocks = new TByteObjectMap[5];
        TByteObjectMap<Block> blocksForConnections = new TByteObjectHashMap<>();

        addConnections(basicBlocks, 0, NO_CONNECTIONS);
        addConnections(basicBlocks, 1, ONE_CONNECTION);
        addConnections(basicBlocks, 2, ONE_CONNECTIONS_SLOPE);
        addConnections(basicBlocks, 2, TWO_CONNECTIONS_LINE);
        addConnections(basicBlocks, 2, TWO_CONNECTIONS_CORNER);
        addConnections(basicBlocks, 3, THREE_CONNECTIONS_T);
        addConnections(basicBlocks, 4, FOUR_CONNECTIONS_CROSS);

        BlockUri blockUri = new BlockUri(definition.getUrn());

        // Now make sure we have all combinations based on the basic set (above) and rotations
        for (byte connections = 0; connections < 60; connections++) {
            // Only the allowed connections should be created
            if ((connections & connectionSides) == connections) {
                Block block = constructBlockForConnections(connections,blockUri, blockBuilder, definition, basicBlocks);
                if (block != null) {
                    block.setUri(new BlockUri(blockUri, new Name(String.valueOf(connections))));
                    blocksForConnections.put(connections, block);
                }
            }
        }

        final Block archetypeBlock = blocksForConnections.get(SideBitFlag.getSides(Side.RIGHT, Side.LEFT));
        return new RailsUpdatesFamily(connectionCondition, blockUri, definition.getCategories(),
                archetypeBlock, blocksForConnections, (byte) (connectionSides & 0b111110),rotations);
    }

    protected void addConnections(TByteObjectMap<String>[] basicBlocks, int index, String connections) {
        if (basicBlocks[index] == null) {
            basicBlocks[index] = new TByteObjectHashMap<>();
        }
        Byte val = RAILS_MAPPING.get(connections);
        if (val != null) {
            basicBlocks[index].put(RAILS_MAPPING.get(connections), connections);
        }
    }

    protected Block constructBlockForConnections(final byte connections,BlockUri uri, final BlockBuilderHelper blockBuilder,
                                                 BlockFamilyDefinition definition, TByteObjectMap<String>[] basicBlocks) {
        int connectionCount = SideBitFlag.getSides(connections).size();
        if(connectionCount >  basicBlocks.length -1 )
            return  null;
        TByteObjectMap<String> possibleBlockDefinitions = basicBlocks[connectionCount];
        final TByteObjectIterator<String> blockDefinitionIterator = possibleBlockDefinitions.iterator();
        while (blockDefinitionIterator.hasNext()) {
            blockDefinitionIterator.advance();
            final byte originalConnections = blockDefinitionIterator.key();
            final String section = blockDefinitionIterator.value();
            Rotation rot = getRotationToAchieve(originalConnections, connections);
            if (rot != null) {

                rotations.put(connections,rot);
                return blockBuilder.constructTransformedBlock(definition, section, rot);
            }
        }
        return null;
    }

    protected Rotation getRotationToAchieve(byte source, byte target) {
        Collection<Side> originalSides = SideBitFlag.getSides(source);

        Iterable<Rotation> rotations =  Rotation.horizontalRotations() ;
        for (Rotation rot : rotations) {
            Set<Side> transformedSides = Sets.newHashSet();
            transformedSides.addAll(originalSides.stream().map(rot::rotate).collect(Collectors.toList()));

            byte transformedSide = SideBitFlag.getSides(transformedSides);
            if (transformedSide == target) {
                return rot;
            }
        }
        return null;
    }

    public static class RailsConnectionCondition implements ConnectionCondition {
        @Override
        public boolean isConnectingTo(Vector3i blockLocation, Side connectSide, WorldProvider worldProvider, BlockEntityRegistry blockEntityRegistry) {
            Vector3i neighborLocation = new Vector3i(blockLocation);
            neighborLocation.add(connectSide.getVector3i());

            EntityRef neighborEntity = blockEntityRegistry.getEntityAt(neighborLocation);

            return neighborEntity != null && connectsToNeighbor(neighborEntity, connectSide.reverse());
        }

        private boolean connectsToNeighbor(EntityRef neighborEntity, Side side) {
            return neighborEntity.hasComponent(RailComponent.class);
        }
    }
}