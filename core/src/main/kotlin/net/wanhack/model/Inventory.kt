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

import net.wanhack.model.item.Item
import java.util.*

class Inventory {

    val items = HashSet<Item>()

    inline fun <reified T : Any> byType() = items.filterIsInstance<T>()

    fun add(item: Item) {
        items.add(item)
    }

    fun remove(item: Item) {
        items.remove(item)
    }

    val weight: Int
        get() = items.sumBy(Item::weight)

    val lighting: Int
        get() = items.sumBy(Item::lighting)
}
