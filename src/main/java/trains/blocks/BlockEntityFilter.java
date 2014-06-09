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

import com.google.common.base.Predicate;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.world.block.BlockComponent;

/**
 * Created by adeon on 09.06.14.
 */
public class BlockEntityFilter implements Predicate<EntityRef> {
    @Override
    public boolean apply(EntityRef entity) {
        BlockComponent component = entity.getComponent(BlockComponent.class);
        return !component.getBlock().isPenetrable() && !component.getBlock().isLiquid();
    }
}
