package dev.komu.kraken.model

import dev.komu.kraken.common.Direction
import dev.komu.kraken.definitions.Creatures
import dev.komu.kraken.definitions.Items
import dev.komu.kraken.definitions.Weapons
import dev.komu.kraken.model.GameConfiguration.PetType
import dev.komu.kraken.model.actions.*
import dev.komu.kraken.model.common.Console
import dev.komu.kraken.model.creature.Creature
import dev.komu.kraken.model.creature.Player
import dev.komu.kraken.model.creature.pets.Doris
import dev.komu.kraken.model.creature.pets.Lassie
import dev.komu.kraken.model.creature.pets.Pet
import dev.komu.kraken.model.events.GameEvent
import dev.komu.kraken.model.events.global.HungerEvent
import dev.komu.kraken.model.events.global.RegainHitPointsEvent
import dev.komu.kraken.model.events.region.CreateMonstersEvent
import dev.komu.kraken.model.item.Equipable
import dev.komu.kraken.model.item.Item
import dev.komu.kraken.model.item.ItemInfo
import dev.komu.kraken.model.item.food.Food
import dev.komu.kraken.model.region.*
import dev.komu.kraken.service.config.ObjectFactory
import dev.komu.kraken.utils.MaximumCounter
import dev.komu.kraken.utils.isFestivus
import dev.komu.kraken.utils.logger
import java.time.DayOfWeek
import java.time.LocalDate

class Game(val config: GameConfiguration, private val console: Console, val listener: () -> Unit) : ReadOnlyGame {
    private val globalClock = Clock()
    private val regionClock = Clock()
    val player = Player(config.name)
    private val world = World(this)

    val objectFactory = ObjectFactory().apply {
        addDefinitions(Weapons)
        addDefinitions(Items)
        addDefinitions(Creatures)
    }

    private val maximumDungeonLevel = MaximumCounter(0)
    var over = false

    override var selectedCell: Coordinate? = null

    override val inventoryItems: List<ItemInfo>
        get() {
            val infos = mutableListOf<ItemInfo>()

            player.activatedItems.mapTo(infos) {
                ItemInfo(it.title, it.description, it.letter, inUse = true)
            }

            player.inventory.items.mapTo(infos) {
                ItemInfo(it.title, it.description, it.letter, inUse = false)
            }

            return infos
        }

    override val statistics: GameStatistics
        get() = GameStatistics(player, globalClock.time)

    override val visibleCells: CellSet
        get() = player.visibleCells

    fun revealCurrentRegion() {
        currentRegion.reveal()
    }

    fun start() {
        player.wieldedWeapon = Weapons.dagger.create()
        player.inventory.add(Items.foodRation.create())
        player.inventory.add(Items.cyanideCapsule.create())
        enterRegion("start", "from up")

        if (config.pet == PetType.DORIS)
            putPetNextToPlayer(Doris("Doris"))
        else if (config.pet == PetType.LASSIE)
            putPetNextToPlayer(Lassie("Lassie"))

        currentRegion.updateLighting()
        player.act(this)
        player.updateVisiblePoints()
        currentRegion.updateSeenCells(player.visibleCells)
        player.message("Hello %s, welcome to Kraken!", player.name)

        val today = LocalDate.now()
        if (today.isFestivus) {
            player.message("Happy Festivus!")
            player.strength += 10
            player.luck = 2
            player.inventory.add(Weapons.aluminiumPole.create())
        }

        if (today.dayOfWeek == DayOfWeek.FRIDAY) {
            player.message("It is Friday, good luck!")
            player.luck = 1
        }

        addGlobalEvent(HungerEvent)
        addGlobalEvent(RegainHitPointsEvent)
    }

    private fun putPetNextToPlayer(pet: Creature) {
        val target = player.cell.adjacentCells.find { cell ->
            cell.isFloor && cell.creature == null
        }

        if (target != null) {
            pet.cell = target
            regionClock.schedule(pet.tickRate, pet)
        }
    }

    fun addGlobalEvent(event: GameEvent) {
        globalClock.schedule(event.rate, event)
    }

    private fun addRegionEvent(event: GameEvent) {
        regionClock.schedule(event.rate, event)
    }

    override val cellInFocus: Coordinate
        get() = player.cell.coordinate

    private fun enterRegion(name: String, location: String) {
        val region = world.getRegion(name)
        val oldCell = player.cellOrNull
        val oldRegion = oldCell?.region
        region.setPlayerLocation(player, location)
        if (region != oldRegion) {
            regionClock.clear()
            maximumDungeonLevel.update(region.level)
            if (oldCell != null) {
                val pet = oldCell.findAdjacentPet()
                if (pet != null)
                    putPetNextToPlayer(pet)
            }

            addRegionEvent(CreateMonstersEvent(region))
            for (creature in region.creatures)
                regionClock.schedule(creature.tickRate, creature)

            selectedCell = null
        }
    }

    private fun Cell.findAdjacentPet(): Pet? {
        return adjacentCells
            .asSequence()
            .mapNotNull { it.creature as? Pet }
            .firstOrNull()
    }

    fun addCreature(creature: Creature, target: Cell) {
        creature.cell = target
        if (target.region == currentRegion) {
            regionClock.schedule(creature.tickRate, creature)
        }
    }

    fun talk() = gameAction {
        val adjacent = player.adjacentCreatures
        if (adjacent.size == 1) {
            TalkAction(adjacent.first(), player)
        } else if (adjacent.isEmpty()) {
            player.message("There's no-one to talk to.")
            null
        } else {
            val dir = console.selectDirection()
            if (dir != null) {
                val creature = player.cell.getCellTowards(dir).creature
                if (creature != null) {
                    TalkAction(adjacent.first(), player)
                } else {
                    player.message("There's nobody in selected direction.")
                    null
                }
            } else {
                null
            }
        }
    }

    fun openDoor() = gameAction {
        val closedDoors = player.cell.adjacentCells.filter { it.cellType == CellType.CLOSED_DOOR }
        if (closedDoors.isEmpty()) {
            player.message("There are no closed doors around you.")
            null
        } else if (closedDoors.size == 1) {
            OpenDoorAction(closedDoors[0], player)
        } else {
            val dir = console.selectDirection()
            if (dir != null) {
                val cell = player.cell.getCellTowards(dir)
                if (cell.cellType == CellType.CLOSED_DOOR) {
                    OpenDoorAction(cell, player)
                } else {
                    player.message("No closed door in selected direction.")
                    null
                }
            } else {
                null
            }
        }
    }

    fun closeDoor() = gameAction {
        val openDoors = player.cell.adjacentCells.filter { it.cellType == CellType.OPEN_DOOR }
        if (openDoors.isEmpty()) {
            player.message("There are no open doors around you.")
            null
        } else if (openDoors.size == 1) {
            CloseDoorAction(openDoors[0], player)
        } else {
            val dir = console.selectDirection()
            if (dir != null) {
                val cell = player.cell.getCellTowards(dir)
                if (cell.cellType == CellType.OPEN_DOOR)
                    CloseDoorAction(cell, player)
                else {
                    player.message("No open door in selected direction.")
                    null
                }
            } else {
                null
            }
        }
    }

    fun pickup() = gameAction {
        val cell = player.cell
        val items = cell.items
        when {
            items.isEmpty() -> { player.message("There's nothing to pick up."); null }
            items.size == 1 -> PickupAction(items.first(), player)
            else ->
                console.selectItem("Select item to pick up", items)?.let { PickupAction(it, player) }
        }
    }

    fun equip() = gameAction {
        console.selectItem("Select item to equip", player.inventory.byType<Equipable>())?.let { EquipAction(it, player) }
    }

    fun drop() = gameAction {
        console.selectItem("Select item to drop", player.inventory.items)?.let { DropAction(it, player) }
    }

    fun drop(item: Item) = gameAction {
        DropAction(item, player)
    }

    fun eat() = gameAction {
        console.selectItem("Select food to eat", player.inventory.byType<Food>())?.let { EatAction(it, player) }
    }

    fun search() = gameAction {
        SearchAction(player)
    }

    fun throwItem() = gameAction {
        val projectile = console.selectItem("Select item to throw", player.inventory.byType<Item>())
        if (projectile != null) {
            console.selectDirection()?.let { ThrowAction(projectile, it, player) }
        } else {
            null
        }
    }

    override val currentRegionOrNull: Region?
        get() = player.cellOrNull?.region

    private val currentRegion: Region
        get() = player.region

    fun movePlayer(direction: Direction) = gameAction {
        val creatureInCell = player.cell.getCellTowards(direction).creature
        if (creatureInCell != null) {
            if (creatureInCell.alive && (!creatureInCell.friendly || ask("Really attack %s?", creatureInCell.name)))
                AttackAction(creatureInCell, player)
            else
                null

        } else {
            MoveAction(player, direction)
        }
    }

    fun runTowards(direction: Direction) = gameAction {
        val target = player.cell.getCellTowards(direction)
        if (isInCorridor(target))
            runInCorridor(direction)
        else
            runInRoom(direction)
        null // TODO: implement running properly
    }

    fun runTowards(end: Coordinate) = gameAction {
        val endCell = currentRegion[end]
        val path = currentRegion.findPath(player.cell, endCell)

        if (path != null) {
            val cells = path.drop(1)

            for (cell in cells) {
                if (!cell.canMoveInto(player.corporeal))
                    break

                cell.enter(player)
                tick()

                if (cell.isInteresting)
                    break
            }
        }

        null // TODO: implement running properly
    }

    private fun isInCorridor(cell: Cell) =
        cell.countPassableMainNeighbours() == 2 && !cell.isRoomCorner()

    private fun runInCorridor(initialDirection: Direction) {
        val first = player.cell.getCellTowards(initialDirection)
        if (first.canMoveInto(player.corporeal)) {
            var previous = player.cell

            first.enter(player)
            tick()

            while (!player.cell.isInteresting(true)) {
                val nextCandidates = player.cell.adjacentCellsInMainDirections.filter { c ->
                    c != previous && c.canMoveInto(player.corporeal)
                }

                if (nextCandidates.size == 1) {
                    previous = player.cell
                    nextCandidates.first().enter(player)
                    tick()

                } else {
                    return
                }
            }
        }
    }

    private fun runInRoom(direction: Direction) {
        val first = player.cell.getCellTowards(direction)
        if (first.canMoveInto(player.corporeal)) {
            first.enter(player)
            tick()

            while (!player.cell.isInteresting(false)) {
                val target = player.cell.getCellTowards(direction)
                if (target.canMoveInto(player.corporeal)) {
                    val previous = player.cell
                    target.enter(player)
                    tick()

                    if (previous.countPassableMainNeighbours() != target.countPassableMainNeighbours())
                        break
                } else
                    break
            }
        }
    }

    private fun Cell.isInteresting(corridor: Boolean) =
        isInteresting || player.seesNonFriendlyCreatures() || (corridor && countPassableMainNeighbours() > 2)

    fun movePlayerVertically(up: Boolean) = gameAction {
        val target = player.cell.getJumpTarget(up)
        if (target != null) {
            if (target.isExit) {
                if (ask("Really escape from the dungeon?"))
                    gameOver("Escaped the dungeon.")

            } else {
                log.fine("Found portal to target $target")
                enterRegion(target.region, target.location)
                tick()
            }
        } else {
            log.fine("No matching portal at current location")
        }
        null // TODO: proper action
    }

    fun skipTurn() {
        if (!over)
            tick()
    }

    fun rest(maxTurns: Int) = gameAction {
        val startTime = globalClock.time
        if (maxTurns == -1 && player.hitPoints == player.maximumHitPoints) {
            player.message("You don't feel like you need to rest.")
            null
        } else {
            player.message("Resting...")
            loop@ while (maxTurns == -1 || maxTurns < startTime - globalClock.time) {
                when {
                    player.hitPoints == player.maximumHitPoints -> {
                        player.message("You feel rested!")
                        break@loop
                    }
                    player.hungerLevel.hungry -> {
                        player.message("You wake up feeling hungry.")
                        break@loop
                    }
                    player.seesNonFriendlyCreatures() -> {
                        player.message("Your rest is interrupted.")
                        break@loop
                    }
                    else ->
                        tick()
                }
            }
            null // TODO: proper behavior
        }
    }

    private fun tick() {
        if (player.alive) {
            do {
                val ticks = player.tickRate

                globalClock.tick(ticks, this)
                regionClock.tick(ticks, this)
            } while (player.alive && player.fainted)

            currentRegion.updateLighting()
            player.updateVisiblePoints()
            currentRegion.updateSeenCells(player.visibleCells)

            listener()
        }
    }

    fun message(message: String, vararg args: Any?) {
        console.message(message.format(*args))
    }

    fun ask(question: String, vararg args: Any?): Boolean =
        console.ask(question.format(*args))

    fun gameOver(reason: String) {
        over = true
    }

    private fun gameAction(callback: () -> Action?) {
        if (!over) {
            if (!player.paralyzed) {
                val action = callback()
                if (action != null && player.perform(action))
                    tick()
            } else {
                message("You are paralyzed.")
            }
        }
    }

    companion object {
        private val log = Game::class.java.logger()
    }
}
