/*
 * Copyright 2013 The Releasers of Kraken
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

package dev.komu.kraken.model.item.food

import dev.komu.kraken.model.creature.Player
import dev.komu.kraken.utils.exp.Expression
import dev.komu.kraken.utils.rollDie

class Corpse(name: String): Food(name) {

    var poisonDamage: Expression? = null
    var taste = Taste.CHICKEN

    override fun onEatenBy(eater: Player) {
        eater.decreaseHungriness(effectiveness)
        val poisonDamage = calculatePoisonDamage()
        if (poisonDamage > 0) {
            eater.takeDamage(poisonDamage, eater)
            eater.message("This %s tastes terrible, it must have been poisonous!", title)
            if (!eater.alive) {
                eater.message("%s %s.", eater.You(), eater.verb("die"))
                eater.die("poisonous corpse")
            }
        } else {
            eater.message("This %s tastes %s.", title, taste)
        }
    }

    private fun calculatePoisonDamage(): Int {
        val baseDamage = poisonDamage?.evaluate() ?: 0
        return if (baseDamage > 0)
            rollDie(baseDamage * level)
        else
            0
    }
}