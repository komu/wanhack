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

package dev.komu.kraken.service.config

import dev.komu.kraken.definitions.betweenLevels
import dev.komu.kraken.definitions.weightedRandom
import dev.komu.kraken.model.creature.Creature
import java.util.*

class ObjectFactory {
    private val creatures = HashMap<String, dev.komu.kraken.definitions.CreatureDefinition<*>>()
    private val items = HashMap<String, dev.komu.kraken.definitions.ItemDefinition<*>>()

    val instantiableItems: Collection<dev.komu.kraken.definitions.ItemDefinition<*>>
        get() = items.values.filter { it.instantiable }

    val instantiableCreatures: Collection<dev.komu.kraken.definitions.CreatureDefinition<*>>
        get() = creatures.values.filter { it.instantiable }

    fun addDefinitions(definitions: dev.komu.kraken.definitions.Definitions) {
        for (definition in definitions.itemDefinitions)
            items[definition.name] = definition

        for (definition in definitions.creatureDefinitions)
            creatures[definition.name] = definition
    }

    fun createCreature(name: String) =
        getCreatureDefinition(name).create()

    fun createItem(name: String) =
        getItemDefinition(name).create()

    fun randomSwarm(regionLevel: Int, playerLevel: Int): Collection<Creature> {
        val minLevel = regionLevel / 6
        val maxLevel = (regionLevel + playerLevel) / 2

        return instantiableCreatures.betweenLevels(minLevel, maxLevel).weightedRandom().createSwarm()
    }

    private fun getItemDefinition(name: String) =
        items[name] ?: throw ConfigurationException("No such item <$name>")

    private fun getCreatureDefinition(name: String) =
        creatures[name] ?: throw ConfigurationException("No such creature <$name>")
}