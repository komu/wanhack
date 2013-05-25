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

package net.wanhack.model.item.food

import net.wanhack.model.creature.Player

class CyanideCapsule: Food("a cyanide capsule") {

    public override fun onEatenBy(eater: Player) {
        if (eater.ask(false, "Really take %s?", title)) {
            eater.message("You swallow %s.", title)
            eater.die(title)
        } else {
            eater.message("You change your mind.")
            eater.inventoryItems.add(this)
        }
    }
}