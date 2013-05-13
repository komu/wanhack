/*
 * Copyright 2013 The Wanhack Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.wanhack.model.events.region

import java.util.Random
import net.wanhack.model.Game
import net.wanhack.model.creature.Creature
import net.wanhack.model.creature.Player
import net.wanhack.model.events.PersistentEvent
import net.wanhack.model.region.Cell
import net.wanhack.model.region.CellSet
import net.wanhack.model.region.Region
import net.wanhack.service.creature.CreatureService
import org.apache.commons.logging.LogFactory

class CreateMonstersEvent(val region: Region): PersistentEvent(500 * 100) {

    private val random  = Random()

    override fun fire(game: Game) {
        val creatureService = CreatureService.instance
        val player = game.player
        val creatures = creatureService.randomSwarm(region.level, player.level)
        log.debug("Created new random creatures: " + creatures)

        for (creature in creatures) {
            val target = getTargetCell(player, creature)
            if (target != null)
                game.addCreature(creature, target)
            else
                log.warn("No empty cells available, creature not added.")
        }
    }

    private fun getTargetCell(player: Player, creature: Creature): Cell? {
        val cell = selectRandomTargetCell(player.getInvisibleCells(), creature)
        if (cell != null)
            return cell
        else
            return selectRandomTargetCell(region.getCells(), creature)
    }

    private fun selectRandomTargetCell(candidates: CellSet, creature: Creature): Cell? {
        while (!candidates.isEmpty()) {
            val cell = candidates[random.nextInt(candidates.size)]
            if (cell.creature == null && cell.canMoveInto(creature.corporeal))
                return cell
        }
        return null
    }

    class object {
        private val log = LogFactory.getLog(javaClass<CreateMonstersEvent>())!!
    }
}
