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

import net.wanhack.model.common.Direction
import net.wanhack.model.creature.Creature
import net.wanhack.model.creature.Player
import net.wanhack.model.item.Item
import net.wanhack.utils.countOfCellsAtDistance
import java.util.*
import java.util.Collections.emptyList
import java.lang.Math.*
import net.wanhack.model.common.Directions
import kotlin.support.AbstractIterator
import net.wanhack.utils.collections.maximumBy

class Cell(val region: Region, val coordinate: Coordinate, var state: CellState) {

    var hasBeenSeen = false
    val items = HashSet<Item>()
    var creature: Creature? = null
    var portal: Portal? = null
    var defaultLighting = 100
    var lighting = defaultLighting
    var lightPower = 0

    fun enter(creature: Creature) {
        creature.cell = this

        if (state.cellType.isStairs)
            creature.message("You see stairs here.")

        if (items.size == 1)
            creature.message("You see here %s.", items.first().title)
        else if (items.size > 1)
            creature.message("You see multiple items here.")
    }

    fun openDoor(opener: Creature) {
        (state as? Door)?.open(opener)
    }

    fun closeDoor(closer: Creature): Boolean {
        val door = state as? Door
        if (door != null && door.isOpen) {
            if (creature != null || !items.empty) {
                closer.message("Something blocks the door.")
                return false
            }

            door.close(closer)
            return true
        }

        return false
    }

    val largestItem: Item?
        get() = items.maximumBy { it.weight }

    fun isReachable(goal: Cell) = this == goal || region.findPath(this, goal) != null

    fun getCellTowards(direction: Direction) =
        region[coordinate.x + direction.dx, coordinate.y + direction.dy]

    fun getJumpTarget(up: Boolean) = portal?.getTarget(up)

    fun isFloor() = state.cellType.isFloor

    fun isInRoom() = state.cellType.isRoomFloor

    fun isClosedDoor() = state.cellType == CellType.CLOSED_DOOR

    fun isAdjacent(cell: Cell) = cell != this && abs(coordinate.x - cell.coordinate.x) < 2 && abs(coordinate.y - cell.coordinate.y) < 2

    fun isRoomCorner(): Boolean {
        if (countPassableMainNeighbours() != 2)
            return false

        var previousPassable = 0

        for (cell in adjacentCells) {
            if (cell.isPassable) {
                if (previousPassable == 2)
                    return true
                else
                    previousPassable++
            } else {
                previousPassable = 0
            }
        }

        return false
    }

    fun search(player: Player) =
        state.search(player)

    val cellType: CellType
        get() = state.cellType

    fun setType(cellType: CellType) {
        state = DefaultCellState(cellType)
    }

    val isFloor: Boolean
        get() = state.cellType.isFloor

    val isInRoom: Boolean
        get() = state.cellType.isRoomFloor

    val isClosedDoor: Boolean
        get() = state.cellType == CellType.CLOSED_DOOR

    val canDropItemToCell: Boolean
        get() = state.cellType.canDropItem

    val canSeeThrough: Boolean
        get() = state.cellType.canSeeThrough

    val isPassable: Boolean
        get() = state.cellType.passable

    val isInteresting: Boolean
        get() = !state.cellType.isFloor || !items.empty

    val isDeadEnd: Boolean
        get() = isPassable && countPassableMainNeighbours() == 1

    fun canMoveInto(corporeal: Boolean) = creature == null && state.cellType.canMoveInto(corporeal)

    fun distance(cell: Cell) = coordinate.distance(cell.coordinate)

    fun matchingCellsNearestFirst(predicate: (Cell) -> Boolean): Iterator<Cell> =
        object : AbstractIterator<Cell>() {
            private val maxDistance = max(max(coordinate.x, region.width - coordinate.x), max(coordinate.y, region.height - coordinate.y))
            private var distance = 0
            private var pos = 0
            private var cellsAtCurrentDistance: List<Cell> = Collections.emptyList()

            override fun computeNext() {
                while (pos == cellsAtCurrentDistance.size) {
                    if (distance >= maxDistance) {
                        done()
                        return
                    }

                    cellsAtCurrentDistance = getMatchingCellsAtDistance(++distance, predicate)
                    pos = 0
                }
                setNext(cellsAtCurrentDistance[pos++])
            }
        }

    fun getMatchingCellsAtDistance(distance: Int, predicate: (Cell) -> Boolean): List<Cell> {
        if (distance == 0)
            return if (predicate(this)) listOf(this) else emptyList()

        val cells = ArrayList<Cell>(countOfCellsAtDistance(distance))
        val x1 = max(0, coordinate.x - distance)
        val y1 = max(0, coordinate.y - distance)
        val x2 = min(region.width-1, coordinate.x + distance)
        val y2 = min(region.height-1, coordinate.y + distance)

        for (xx in x1..x2) {
            val cell = region[xx, y1]
            if (predicate(cell))
                cells.add(cell)
        }

        for (yy in y1 + 1..y2 - 1) {
            val left = region[x1, yy]
            if (predicate(left))
                cells.add(left)

            val right = region[x2, yy]
            if (predicate(right))
                cells.add(right)
        }

        for (xx in x1..x2) {
            val cell = region[xx, y2]
            if (predicate(cell))
                cells.add(cell)
        }

        return cells
    }

    val adjacentCells: List<Cell>
        get() = adjacentCells(Directions.allDirections)

    val adjacentCellsInMainDirections: List<Cell>
        get() = adjacentCells(Directions.mainDirections)

    private fun adjacentCells(directions: Collection<Direction>): List<Cell> {
        val adjacent = ArrayList<Cell>(directions.size)
        for (d in directions) {
            val xx = coordinate.x + d.dx
            val yy = coordinate.y + d.dy
            if (region.containsPoint(xx, yy))
                adjacent.add(region[xx, yy])
        }
        return adjacent
    }

    fun countPassableMainNeighbours() =
        Directions.mainDirections.count { getCellTowards(it).isPassable }

    fun getDirection(cell: Cell) = coordinate.directionOf(cell.coordinate)

    fun hasLineOfSight(target: Cell) =
        getCellsBetween(target).all { it.canSeeThrough }

    fun getCellsBetween(target: Cell): List<Cell> {
        val cells = ArrayList<Cell>(distance(target))
        var x0 = coordinate.x
        var y0 = coordinate.y
        var x1 = target.coordinate.x
        var y1 = target.coordinate.y

        val steep = abs(y1 - y0) > abs(x1 - x0)
        if (steep) {
            var t0 = x0
            x0 = y0
            y0 = t0
            var t1 = x1
            x1 = y1
            y1 = t1
        }

        val reverse = x0 > x1
        if (reverse) {
            var t0 = x0
            x0 = x1
            x1 = t0
            var t1 = y0
            y0 = y1
            y1 = t1
        }

        val deltaX = x1 - x0
        val deltaY = abs(y1 - y0)
        var error = 0
        val deltaError = deltaY
        var y = y0
        val yStep = if ((y0 < y1)) 1 else -1

        for (x in x0..x1 - 1) {
            if ((x != x0 || y != y0) && (x != x1 || y != y1))
                cells.add(if (steep) region[y, x] else region[x, y])

            error += deltaError
            if (2 * error >= deltaX) {
                y += yStep
                error -= deltaX
            }
        }

        if (reverse)
            Collections.reverse(cells)

        return cells
    }

    fun resetLighting() {
        lighting = defaultLighting
    }

    fun updateLighting() {
        val lightSourceEffectiveness = calculateLightSourceEffectiveness()
        if (lightSourceEffectiveness > 0) {
            val cells = VisibilityChecker.getVisibleCells(this, lightSourceEffectiveness / 10)
            for (cell in cells)
                cell.lighting += max(lightSourceEffectiveness - 10 * distance(cell), 0)
        }
    }

    private fun calculateLightSourceEffectiveness(): Int {
        var effectiveness = 0

        for (item in items)
            effectiveness += item.lighting

        effectiveness += creature?.lighting ?: 0

        return lightPower + effectiveness
    }

    fun toString() = coordinate.toString()
}
