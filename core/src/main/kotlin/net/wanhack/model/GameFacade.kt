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

package net.wanhack.model

import net.wanhack.common.Direction
import net.wanhack.model.common.Console
import net.wanhack.model.item.Item
import net.wanhack.model.region.Coordinate
import net.wanhack.utils.relinquish
import net.wanhack.utils.yieldLock
import java.util.concurrent.Executors
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

/**
 * All commands from UI to game go through this facade.
 */
class GameFacade(config: GameConfiguration, console: Console, val listener: (Boolean) -> Unit) {

    private val gameExecutor = Executors.newSingleThreadExecutor { Thread(it, "game") }
    private val lock = ReentrantReadWriteLock(true)
    private val game = Game(config, LockRelinquishingConsole(console, lock.writeLock())) {
        listener(true)
        lock.writeLock().yieldLock()
    }

    fun <T> query(callback: (ReadOnlyGame) -> T): T =
        lock.readLock().withLock { callback(game) }

    fun start() = gameAction {
        game.start()
    }

    fun movePlayer(direction: Direction) = gameAction {
        game.movePlayer(direction)
    }

    fun runTowards(direction: Direction) = gameAction {
        game.runTowards(direction)
    }

    fun runTowards(c: Coordinate) = gameAction {
        game.runTowards(c)
    }

    fun movePlayerVertically(up: Boolean) = gameAction {
        game.movePlayerVertically(up)
    }

    fun skipTurn() = gameAction {
        game.skipTurn()
    }

    fun revealCurrentRegion() = gameAction {
        game.revealCurrentRegion()
    }

    fun drop(item: Item) = gameAction {
        game.drop(item)
    }

    fun equip() = gameAction {
        game.equip()
    }

    fun rest(maxTurns: Int) = gameAction {
        game.rest(maxTurns)
    }

    fun talk() = gameAction {
        game.talk()
    }

    fun openDoor() = gameAction {
        game.openDoor()
    }

    fun closeDoor() = gameAction {
        game.closeDoor()
    }

    fun pickup() = gameAction {
        game.pickup()
    }

    fun drop() = gameAction {
        game.drop()
    }

    fun eat() = gameAction {
        game.eat()
    }

    fun fling() = gameAction {
        game.fling()
    }

    fun search() = gameAction {
        game.search()
    }

    fun focus(coordinate: Coordinate) = gameAction {
        game.selectedCell = coordinate
    }

    private fun gameAction(body: () -> Unit) {
        gameExecutor.execute {
            lock.writeLock().withLock {
                if (!game.over)
                    body()
            }

            listener(false)
        }
    }

    class LockRelinquishingConsole(val console: Console, val lock: Lock) : Console {
        override fun message(message: String) =
            console.message(message)

        override fun ask(question: String) =
            lock.relinquish { console.ask(question) }

        override fun selectDirection() =
            lock.relinquish { console.selectDirection() }

        override fun <T: Item> selectItem(message: String, items: Collection<T>) =
            lock.relinquish { console.selectItem(message, items) }

        override fun <T: Item> selectItems(message: String, items: Collection<T>) =
            lock.relinquish { console.selectItems(message, items) }
    }
}
