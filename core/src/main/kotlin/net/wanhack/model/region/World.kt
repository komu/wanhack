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

package net.wanhack.model.region

import net.wanhack.definitions.betweenLevels
import net.wanhack.definitions.weightedRandom
import net.wanhack.model.Game
import net.wanhack.model.region.generators.MazeRegionGenerator
import net.wanhack.model.region.generators.RoomFirstRegionGenerator
import net.wanhack.service.region.RegionLoader
import net.wanhack.utils.Probability
import net.wanhack.utils.randomInt
import java.util.*

class World(val game: Game) {
    private val loadedRegions = HashMap<String, Region>()
    private val regions = ArrayList<RegionInfo>()
    private val mazeProbability = Probability(5)

    init {
        addNonRandomRegion(0, "start")

        for (n in 1..30)
            addRandomRegion(n, "level$n")

        addNonRandomRegion(31, "end")

        var previous: RegionInfo? = null
        for (region in regions) {
            val prev = previous
            if (prev != null) {
                prev.next = region
                region.previous = prev
            }

            previous = region
        }
    }

    private fun addRandomRegion(level: Int, id: String) {
        regions.add(RegionInfo(id, level, true))
    }

    private fun addNonRandomRegion(level: Int, id: String) {
        regions.add(RegionInfo(id, level, false))
    }

    fun getRegion(name: String): Region =
        loadedRegions.getOrPut(name) {
            initRegion(name)
        }

    private fun initRegion(name: String): Region {
        val info = regions.find { it.id == name } ?: throw IllegalArgumentException("unknown region <$name>")
        val region = loadRegion(info)
        addRandomCreatures(region)
        addRandomItems(region)
        return region
    }

    private fun loadRegion(info: RegionInfo) =
        if (info.random) {
            val generator = if (mazeProbability.check()) MazeRegionGenerator else RoomFirstRegionGenerator
            generator.generate(this, info.id, info.level, info.previous?.id, info.next?.id)
        } else
            RegionLoader(this).loadRegion(info)

    private fun addRandomCreatures(region: Region) {
        if (region.level == 0)
            return

        val monsterCount = randomInt(1, 1 + 2 * region.level)
        val emptyCells = region.getCellsForItemsAndCreatures()

        for (i in 1..monsterCount) {
            val creatures = game.objectFactory.randomSwarm(region.level, game.player.level)
            if (emptyCells.isEmpty())
                return

            val cell = emptyCells.randomElement()
            val cells = cell.cellsNearestFirst().filter { it.canMoveInto(true) }.iterator()
            for (creature in creatures)
                if (cells.hasNext()) {
                    val target = cells.next()
                    emptyCells.remove(target)
                    creature.cell = target
                }
        }
    }

    private fun addRandomItems(region: Region) {
        val emptyCells = region.getCellsForItemsAndCreatures()
        if (emptyCells.isEmpty())
            return

        val items = game.objectFactory.instantiableItems.betweenLevels(0, region.level)
        repeat(randomInt(6)) {
            val item = items.weightedRandom().create()
            emptyCells.randomElement().items.add(item)
        }
    }
}
