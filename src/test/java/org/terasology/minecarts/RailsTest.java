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

import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.terasology.engine.math.Side;
import org.terasology.engine.math.SideBitFlag;
import org.terasology.engine.registry.In;
import org.terasology.engine.world.BlockEntityRegistry;
import org.terasology.engine.world.WorldProvider;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.BlockManager;
import org.terasology.engine.world.block.BlockRegion;
import org.terasology.engine.world.block.BlockUri;
import org.terasology.engine.world.block.family.BlockFamily;
import org.terasology.engine.world.block.family.BlockPlacementData;
import org.terasology.moduletestingenvironment.MTEExtension;
import org.terasology.moduletestingenvironment.ModuleTestingHelper;
import org.terasology.moduletestingenvironment.extension.Dependencies;

@ExtendWith(MTEExtension.class)
@Dependencies({"Rails", "CoreAssets"})
@Disabled("Failing test with Reflection")
public class RailsTest {
    private static final String RAIL_BLOCKFAMILY_URI = "rails:rails";
    private Block dirtBlock;
    private Block airBlock;
    private BlockFamily railBlockFamily;

    @In
    BlockEntityRegistry blockEntityRegistry;
    @In
    BlockManager blockManager;
    @In
    ModuleTestingHelper helper;
    @In
    WorldProvider worldProvider;

    public void initialize() {
        airBlock = blockManager.getBlock("engine:air");
        dirtBlock = blockManager.getBlock("CoreAssets:Dirt");
        railBlockFamily = blockManager.getBlockFamily(RAIL_BLOCKFAMILY_URI);

        BlockRegion region = new BlockRegion(0, 0, 0).expand(5, 5, 5);

        for (Vector3ic pos : region) {
            helper.forceAndWaitForGeneration(pos);
            worldProvider.setBlock(pos, airBlock);
        }
        for (Vector3ic pos : region) {
            helper.forceAndWaitForGeneration(pos);
            worldProvider.setBlock(pos, dirtBlock);
        }
    }

    @Test
    public void singleRail() {
        this.initialize();
        worldProvider.setBlock(new Vector3i(0, 0, 0),
                railBlockFamily.getBlockForPlacement(new BlockPlacementData(new Vector3i(), Side.FRONT,
                        new Vector3f())));

        assertRailBlockAtConnectsTo(new Vector3i(), SideBitFlag.getSides());
    }

    @Test
    public void straightRail() throws Exception {
        this.initialize();

        setRail(new Vector3i(0, 0, 0));
        setRail(new Vector3i(0, 0, 1));

        assertRailBlockAtConnectsTo(new Vector3i(0, 0, 0), SideBitFlag.getSides(Side.BACK));
        assertRailBlockAtConnectsTo(new Vector3i(0, 0, 1), SideBitFlag.getSides(Side.FRONT));
    }

    @Test
    public void cornerRail() throws Exception {
        this.initialize();

        setRail(new Vector3i(0, 0, 0));
        setRail(new Vector3i(0, 0, 1));
        setRail(new Vector3i(1, 0, 0));

        assertRailBlockAtConnectsTo(new Vector3i(0, 0, 1), SideBitFlag.getSides(Side.FRONT));
        assertRailBlockAtConnectsTo(new Vector3i(), SideBitFlag.getSides(Side.BACK, Side.RIGHT));
        assertRailBlockAtConnectsTo(new Vector3i(1, 0, 0), SideBitFlag.getSides(Side.LEFT));
    }

    @Test
    public void teeRail() {
        this.initialize();

        setRail(new Vector3i(0, 0, 1));
        setRail(new Vector3i(0, 0, -1));
        setRail(new Vector3i(1, 0, 0));

        // Must be added last so that the tee is actually created
        setRail(new Vector3i());

        assertRailBlockAtConnectsTo(new Vector3i(), SideBitFlag.getSides(Side.FRONT, Side.BACK, Side.RIGHT));
    }

    @Test
    public void slopeRail() {
        this.initialize();
        worldProvider.setBlock(new Vector3i(0, 0, 1), dirtBlock);

        setRail(new Vector3i(0, 0, 0));
        setRail(new Vector3i(0, 1, 1));
        setRail(new Vector3i(0, 0, -1));

        assertRailBlockAtConnectsTo(new Vector3i(0, 1, 1), SideBitFlag.getSides(Side.FRONT));
        assertRailBlockAtConnectsTo(new Vector3i(), SideBitFlag.getSides(Side.TOP, Side.FRONT));
        assertRailBlockAtConnectsTo(new Vector3i(0, 0, -1), SideBitFlag.getSides(Side.BACK));
    }

    @Test
    public void doubleSlopeRail() {
        this.initialize();
        worldProvider.setBlock(new Vector3i(), dirtBlock);
        worldProvider.setBlock(new Vector3i(0, 0, 1), dirtBlock);
        worldProvider.setBlock(new Vector3i(0, 1, 1), dirtBlock);

        setRail(new Vector3i(0, 0, -1));
        setRail(new Vector3i(0, 1, 0));
        setRail(new Vector3i(0, 2, 1));

        assertRailBlockAtConnectsTo(new Vector3i(0, 1, 0), SideBitFlag.getSides(Side.TOP, Side.FRONT));
        assertRailBlockAtConnectsTo(new Vector3i(0, 0, -1), SideBitFlag.getSides(Side.TOP, Side.FRONT));
    }

    private void assertRailBlockAtConnectsTo(Vector3ic position, byte expectedConnectionSides) {
        BlockUri railsBlockUri = worldProvider.getBlock(position).getURI();
        String expectedIdentifier = String.valueOf(expectedConnectionSides);

        Assertions.assertEquals(RAIL_BLOCKFAMILY_URI, railsBlockUri.getFamilyUri().toString());
        Assertions.assertEquals(expectedIdentifier, railsBlockUri.getIdentifier().toString());
    }

    private void setRail(Vector3i position) {
        worldProvider.setBlock(position, railBlockFamily.getBlockForPlacement(new BlockPlacementData(position,
                Side.FRONT, new Vector3f())));
    }
}
