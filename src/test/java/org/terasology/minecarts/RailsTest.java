// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.minecarts;

import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.math.Region3i;
import org.terasology.engine.math.Side;
import org.terasology.engine.math.SideBitFlag;
import org.terasology.engine.world.BlockEntityRegistry;
import org.terasology.engine.world.WorldProvider;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.BlockManager;
import org.terasology.engine.world.block.BlockUri;
import org.terasology.engine.world.block.family.BlockFamily;
import org.terasology.math.geom.Vector3i;
import org.terasology.moduletestingenvironment.ModuleTestingEnvironment;

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
        return Sets.newHashSet("Rails", "CoreAssets");
    }

    @Before
    public void initialize() {
        worldProvider = getHostContext().get(WorldProvider.class);
        blockEntityRegistry = getHostContext().get(BlockEntityRegistry.class);

        BlockManager blockManager = getHostContext().get(BlockManager.class);
        dirtBlock = blockManager.getBlock("CoreAssets:Dirt");
        railBlockFamily = blockManager.getBlockFamily(RAIL_BLOCKFAMILY_URI);

        fillRegion(
                Region3i.createFromCenterExtents(Vector3i.up().negate(), new Vector3i(25, 0, 25)),
                dirtBlock
        );
    }

    @Test
    public void singleRail() {
        setRailBlock(Vector3i.zero());

        assertRailBlockAtConnectsTo(Vector3i.zero(), SideBitFlag.getSides());
    }

    @Test
    public void straightRail() throws Exception {
        setRailBlock(Vector3i.zero());
        setRailBlock(Vector3i.north());

        assertRailBlockAtConnectsTo(Vector3i.zero(), SideBitFlag.getSides(Side.BACK));
        assertRailBlockAtConnectsTo(Vector3i.north(), SideBitFlag.getSides(Side.FRONT));
    }

    @Test
    public void cornerRail() throws Exception {
        setRailBlock(Vector3i.zero());
        setRailBlock(Vector3i.north());
        setRailBlock(Vector3i.west());

        assertRailBlockAtConnectsTo(Vector3i.north(), SideBitFlag.getSides(Side.FRONT));
        assertRailBlockAtConnectsTo(Vector3i.zero(), SideBitFlag.getSides(Side.BACK, Side.RIGHT));
        assertRailBlockAtConnectsTo(Vector3i.west(), SideBitFlag.getSides(Side.LEFT));
    }

    @Test
    public void teeRail() throws Exception {
        setRailBlock(Vector3i.north());
        setRailBlock(Vector3i.south());
        setRailBlock(Vector3i.west());
        // Must be added last so that the tee is actually created
        setRailBlock(Vector3i.zero());

        assertRailBlockAtConnectsTo(Vector3i.zero(), SideBitFlag.getSides(Side.FRONT, Side.BACK, Side.RIGHT));
    }

    @Test
    public void slopeRail() throws Exception {
        setBlock(Vector3i.north(), dirtBlock);

        setRailBlock(Vector3i.zero());
        setRailBlock(Vector3i.north().add(Vector3i.up()));
        setRailBlock(Vector3i.south());

        assertRailBlockAtConnectsTo(Vector3i.north().add(Vector3i.up()), SideBitFlag.getSides(Side.FRONT));
        assertRailBlockAtConnectsTo(Vector3i.zero(), SideBitFlag.getSides(Side.BACK, Side.TOP));
        assertRailBlockAtConnectsTo(Vector3i.south(), SideBitFlag.getSides(Side.BACK));
    }

    @Test
    public void doubleSlopeRail() throws Exception {
        setBlock(Vector3i.zero(), dirtBlock);
        setBlock(Vector3i.north(), dirtBlock);
        setBlock(Vector3i.north().add(Vector3i.up()), dirtBlock);

        setRailBlock(Vector3i.south());
        setRailBlock(Vector3i.zero().add(Vector3i.up()));
        setRailBlock(Vector3i.north().add(Vector3i.up().scale(2)));

        assertRailBlockAtConnectsTo(Vector3i.zero().add(Vector3i.up()), SideBitFlag.getSides(Side.BACK, Side.TOP));
        assertRailBlockAtConnectsTo(Vector3i.south(), SideBitFlag.getSides(Side.BACK, Side.TOP));
    }

    private void assertRailBlockAtConnectsTo(Vector3i position, byte expectedConnectionSides) {
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
        setBlock(position, blockFamily.getBlockForPlacement(position, Side.TOP, Side.TOP));
    }

    private void setRailBlock(Vector3i position) {
        setBlockForFamily(position, railBlockFamily);
    }
}
