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

package dev.komu.kraken.model.item.armor

import java.util.*

class Armoring : Iterable<Armor> {
    private val armors = EnumMap<BodyPart, Armor>(BodyPart::class.java)

    override fun iterator(): Iterator<Armor> =
        armors.values.iterator()

    fun removeAllArmors(): Collection<Armor> {
        val result = armors.values.toList()
        armors.clear()
        return result
    }

    val weight: Int
        get() = armors.values.sumBy { it.weight }

    val totalArmorBonus: Int
        get() = armors.values.sumBy { it.armorBonus }

    fun replaceArmor(armor: Armor): Armor? =
        armors.put(armor.bodyPart, armor)

    override fun toString() = armors.values.toString()
}