package dev.komu.kraken.model.creature.monsters

import dev.komu.kraken.model.Game
import dev.komu.kraken.model.creature.Monster
import dev.komu.kraken.utils.rollDie

class KnightOfNi(name: String): Monster(name) {

    override fun onTick(game: Game) {
        val player = game.player
        if (seesCreature(player))
            when (rollDie(20)) {
                0, 1, 2 -> player.say(this, "Ni!")
                3       -> player.say(this, "Noo!")
                else    -> {  }
            }

        super.onTick(game)
    }
}
