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

package org.terasology.minecarts;

import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.math.Region3i;
import org.terasology.math.Side;
import org.terasology.math.SideBitFlag;
import org.terasology.math.geom.Vector3i;
import org.terasology.minecarts.blocks.RailsFamilyFactory;
import org.terasology.moduletestingenvironment.ModuleTestingEnvironment;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.BlockUri;
import org.terasology.world.block.family.BlockFamily;

import java.util.Set;

public class RailsTest extends ModuleTestingEnvironment {
    private static final Logger LOGGER = LoggerFactory.getLogger(RailsTest.class);
    private static final String RAIL_BLOCKFAMILY_URI = "rails:rails";
    private WorldProvider worldProvider;
    private BlockEntityRegistry blockEntityRegistry;
    private Block dirtBlock;
    private BlockFamily railBlockFamily;

    @Override
    public Set<String> getDependencies() {
        return Sets.newHashSet("Rails");
    }

    @Before
    public void initialize() {
        worldProvider = getHostContext().get(WorldProvider.class);
        blockEntityRegistry = getHostContext().get(BlockEntityRegistry.class);

        BlockManager blockManager = getHostContext().get(BlockManager.class);
        dirtBlock = blockManager.getBlock("core:dirt");
        railBlockFamily = blockManager.getBlockFamily(RAIL_BLOCKFAMILY_URI);

        fillRegion(
                Region3i.createFromCenterExtents(Vector3i.up().negate(), new Vector3i(25, 0, 25)),
                dirtBlock
        );
    }

    @Test
    public void singleRail() {
        setRailBlock(Vector3i.zero());

        assertRailsBlockAt(Vector3i.zero(), SideBitFlag.getSides());
    }

    @Test
    public void straightRail() throws Exception {
        setRailBlock(Vector3i.zero());
        setRailBlock(Vector3i.north());

        assertRailsBlockAt(Vector3i.zero(), SideBitFlag.getSides(Side.BACK));
        assertRailsBlockAt(Vector3i.north(), SideBitFlag.getSides(Side.FRONT));
    }

    @Test
    public void cornerRail() throws Exception {
        setRailBlock(Vector3i.zero());
        setRailBlock(Vector3i.north());
        setRailBlock(Vector3i.west());

        assertRailsBlockAt(Vector3i.north(), SideBitFlag.getSides(Side.FRONT));
        assertRailsBlockAt(Vector3i.zero(), SideBitFlag.getSides(Side.BACK, Side.RIGHT));
        assertRailsBlockAt(Vector3i.west(), SideBitFlag.getSides(Side.LEFT));
    }

    @Test
    public void teeRail() throws Exception {
        setRailBlock(Vector3i.north());
        setRailBlock(Vector3i.south());
        setRailBlock(Vector3i.west());
        // Must be added last so that the tee is actually created
        setRailBlock(Vector3i.zero());

        assertRailsBlockAt(Vector3i.zero(), SideBitFlag.getSides(Side.FRONT, Side.BACK, Side.RIGHT));
    }

    @Test
    public void slopeRail() throws Exception {
        setBlock(Vector3i.north(), dirtBlock);

        setRailBlock(Vector3i.zero());
        setRailBlock(Vector3i.north().add(Vector3i.up()));
        setRailBlock(Vector3i.south());

        assertRailsBlockAt(Vector3i.north().add(Vector3i.up()), SideBitFlag.getSides(Side.FRONT));
        assertRailsBlockAt(Vector3i.zero(), SideBitFlag.getSides(Side.BACK, Side.TOP));
        assertRailsBlockAt(Vector3i.south(), SideBitFlag.getSides(Side.BACK));
    }

    private void assertRailsBlockAt(Vector3i position, byte expectedConnectionSides) {
        BlockUri railsBlockUri = worldProvider.getBlock(position).getURI();
        String expectedIdentifier = String.valueOf(expectedConnectionSides);

        Assert.assertEquals(RAIL_BLOCKFAMILY_URI, railsBlockUri.getFamilyUri().toString());
        Assert.assertEquals(expectedIdentifier, railsBlockUri.getIdentifier().toString());
    }

    /**
     * Fills the given region with the specified block.
     * <p>
     * Also ensures that all blocks in and adjacent to the region are loaded.
     */
    private void fillRegion(Region3i region, Block material) {
        Region3i loadRegion = region.expand(1);
        for (Vector3i pos : loadRegion) {
            forceAndWaitForGeneration(pos);
        }

        for (Vector3i pos : region) {
            worldProvider.setBlock(pos, material);
        }
    }

    /**
     * Sets the block at the given position.
     * <p>
     * Also ensures that all blocks adjacent to the position are loaded.
     */
    private void setBlock(Vector3i position, Block material) {
        fillRegion(Region3i.createFromCenterExtents(position, 0), material);
    }

    private void setBlockForFamily(Vector3i position, BlockFamily blockFamily) {
        setBlock(position, blockFamily.getBlockForPlacement(worldProvider, blockEntityRegistry, position,
                Side.TOP, Side.TOP));
    }

    private void setRailBlock(Vector3i position) {
        setBlockForFamily(position, railBlockFamily);
    }
}
