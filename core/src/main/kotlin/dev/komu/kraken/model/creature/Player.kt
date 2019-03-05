package dev.komu.kraken.model.creature

import dev.komu.kraken.model.Game
import dev.komu.kraken.model.common.Attack
import dev.komu.kraken.model.common.Color
import dev.komu.kraken.model.events.OneTimeEvent
import dev.komu.kraken.model.item.Item
import dev.komu.kraken.model.item.armor.Armor
import dev.komu.kraken.model.item.weapon.NaturalWeapon
import dev.komu.kraken.model.item.weapon.WeaponClass
import dev.komu.kraken.model.region.Cell
import dev.komu.kraken.model.region.CellSet
import dev.komu.kraken.model.skill.Proficiency
import dev.komu.kraken.model.skill.SkillSet
import dev.komu.kraken.utils.randomEnum
import dev.komu.kraken.utils.randomInt
import dev.komu.kraken.utils.rollDie
import kotlin.properties.Delegates

class Player(name: String): Creature(name) {

    var maximumHitPoints = 0
    var experience = 0
    val hit: Attack = NaturalWeapon("hit", "0", "randint(1, 3)")
    val skills = SkillSet()
    var visibleCells: CellSet by Delegates.notNull()
    var hunger = 2000
    var fainted = false
    var regenerated = false
    var sight = 20

    private val game: Game
        get() = region.world.game

    override var tickRate: Int = 90
        get() = Math.max(1, super.tickRate + weightPenalty)

    init {
        letter = '@'
        color = Color.BLUE
        maximumHitPoints = 8 + rollDie(5)
        hitPoints = maximumHitPoints
        skills.setWeaponProficiency(WeaponClass.SWORD, Proficiency.BASIC)
    }

    fun getThrowRange(weight: Int): Int =
        when {
            weight < 1000  -> 30
            weight < 2000  -> 20
            weight < 3000  -> 15
            weight < 5000  -> 10
            weight < 10000 -> 8
            weight < 15000 -> 5
            weight < 20000 -> 3
            weight < 25000 -> 2
            weight < 50000 -> 1
            else           -> 0
        }

    val weightPenalty: Int
        get() {
            val carriedWeightInKilograms = weightOfCarriedItems / 1000
            val factor = 30
            return factor * carriedWeightInKilograms / strength
        }

    fun replaceArmor(armor: Armor): Armor? =
        armoring.replaceArmor(armor)

    val activatedItems: List<Item>
        get() {
            val result = mutableListOf<Item>()

            val weapon = wieldedWeapon
            if (weapon != null)
                result.add(weapon)

            result += armoring

            return result
        }

    override fun die(killer: String) {
        if (game.config.wizardMode && !game.ask("Die?")) {
            regenerate()
            game.message("You regenerate.")
        } else {
            game.gameOver(killer)
        }
    }

    private fun regenerate() {
        regenerated = true

        hitPoints = maximumHitPoints
        hunger = 1000
    }

    override fun getProficiency(weaponClass: WeaponClass) =
        skills.getWeaponProficiency(weaponClass).bonus

    override val isPlayer = true

    override fun onSuccessfulHit(target: Creature, weapon: Attack) {
        skills.exerciseSkill(weapon.weaponClass, this)
    }

    override fun onKilledCreature(target: Creature) {
        addExperience(target.killExperience)
    }

    fun decreaseHungriness(effectiveness: Int) {
        hunger += effectiveness
    }

    fun increaseHungriness(game: Game) {
        if (fainted)
            return

        hunger--
        if (hunger < 0) {
            message("You faint.")
            hitPoints = 1
            fainted = true
            val ticks = 100 * randomInt(5, 50)
            game.addGlobalEvent(OneTimeEvent(ticks) {
                if (fainted) {
                    message("You wake up.")
                    if (hunger <= 0)
                        hunger = 10

                    fainted = false
                }
            })
        }

    }

    val hungerLevel: HungerLevel
        get() = hunger.toHungerLevel()

    private fun addExperience(exp: Int) {
        experience += exp
        if (experience >= experienceNeededForNextLevel)
            gainLevel()
    }

    private fun gainLevel() {
        val newHp = 3 + rollDie(3)
        maximumHitPoints += newHp
        hitPoints = Math.min(hitPoints + newHp, maximumHitPoints)
        level += 1
    }

    val experienceNeededForNextLevel: Int
        get() = when {
            level < 10 -> 10 * (1 shl level)
            level < 20 -> 10000 * (1 shl (level - 10))
            else       -> 10000000 * (level - 19)
        }

    fun seesCreatures() =
        visibleCells.any { cell ->
            val creature = cell.creature
            creature != null && creature != this
        }

    fun seesNonFriendlyCreatures() =
        visibleCells.any { cell ->
            val creature = cell.creature
            creature != null && creature != this && !creature.friendly
        }

    override fun onTick(game: Game) {
        updateVisiblePoints()
    }

    fun regainHitPoint() {
        hitPoints = Math.min(maximumHitPoints, hitPoints + 1)
    }

    override fun canSee(target: Cell) =
        target in visibleCells

    private fun updateVisiblePoints() {
        visibleCells = cell.getVisibleCells(sight)
    }

    fun getInvisibleCells(): CellSet {
        val cells = region.getCells()
        cells.removeAll(visibleCells)
        return cells
    }

    override fun you() = "you"

    override val naturalAttack: Attack
        get() = hit

    override fun verb(verb: String) = verb

    override fun message(pattern: String, vararg args: Any?) =
        game.message(pattern, *args)

    override fun say(talker: Creature, message: String, vararg args: Any?) =
        game.message("\"$message\"", *args)

    override fun ask(defaultValue: Boolean, question: String, vararg args: Any?) =
        game.ask(question, *args)

    override fun createCorpse() = null
}
