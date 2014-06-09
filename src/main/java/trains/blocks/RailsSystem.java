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
package trains.blocks;


import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.multiBlock.*;
import org.terasology.multiBlock.recipe.UniformMultiBlockFormItemRecipe;
import org.terasology.registry.In;
import org.terasology.world.block.BlockManager;

public class RailsSystem extends BaseComponentSystem {
    @In
    private MultiBlockFormRecipeRegistry multiBlockRecipeRegistry;
    @In
    private BlockManager blockManager;

    public void initialise() {
        addMultiblockRails();
    }

    private void addMultiblockRails() {
        multiBlockRecipeRegistry.addMultiBlockFormItemRecipe(
                new UniformMultiBlockFormItemRecipe(
                        new ToolTypeEntityFilter("rails"), new UseOnTopFilter(),
                        new BlockEntityFilter(), new Basic3DSizeFilter(10, 10, 1, 1),
                        "CopperAndBronze:BasicMetalcrafting",
                        new UniformBlockReplacementCallback<Void>(blockManager.getBlock("CopperAndBronze:BasicMetalStation"))));
    }
}
