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
package org.terasology.rails.blocks;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.math.Side;
import org.terasology.math.SideBitFlag;
import org.terasology.math.Vector3i;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.family.ConnectionCondition;
import org.terasology.world.block.family.RegisterBlockFamilyFactory;
import org.terasology.world.block.family.UpdatesWithNeighboursFamilyFactory;

import java.util.HashMap;
import java.util.Map;


@RegisterBlockFamilyFactory(value = "Rails:rails")
public class RailsFamilyFactory extends UpdatesWithNeighboursFamilyFactory {

    private static final Map<String, Byte> RAILS_MAPPING =
            new HashMap<String, Byte>() {{
                put(NO_CONNECTIONS, (byte) 0);

                put(ONE_CONNECTION, SideBitFlag.getSides(Side.RIGHT));

                put(TWO_CONNECTIONS_LINE, SideBitFlag.getSides(Side.LEFT, Side.RIGHT));
                put(TWO_CONNECTIONS_CORNER, SideBitFlag.getSides(Side.LEFT, Side.FRONT));


                put(THREE_CONNECTIONS_T, SideBitFlag.getSides(Side.LEFT, Side.RIGHT, Side.FRONT));

                put(FOUR_CONNECTIONS_CROSS, SideBitFlag.getSides(Side.RIGHT, Side.LEFT, Side.BACK, Side.FRONT));
            }};

    public RailsFamilyFactory() {
        super(new RailsConnectionCondition(), (byte) 0b110110, RAILS_MAPPING, true);
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
            return neighborEntity.hasComponent(ConnectsToRailsComponent.class);
        }
    }
}
