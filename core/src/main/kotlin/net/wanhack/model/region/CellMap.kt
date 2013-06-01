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

import java.util.AbstractMap
import java.util.AbstractSet
import java.util.NoSuchElementException
import java.util.Objects

class CellMap<V : Any>(private val region: Region): AbstractMap<Cell, V>() {

    private val mappings = Array<V?>(region.width * region.height) { null }

    override fun get(key: Any?): V? =
        mappings[index((key as Cell).coordinate)]

    override fun put(key: Cell, value: V): V? {
        val index = index(key.coordinate)
        val old = mappings[index]
        mappings[index] = value
        return old
    }

    override fun remove(key: Any?): V? =
        if (key is Cell) {
            val index = index(key.coordinate)
            val old = mappings[index]
            mappings[index] = null
            old
        } else
            null

    override fun isEmpty() = size() != 0

    override fun size(): Int {
        var size = 0
        for (value in mappings)
            if (value != null)
                size++
        return size
    }

    override fun clear() {
        for (i in mappings.indices)
            mappings[i] = null
    }

    override fun entrySet() = EntrySet<V>(this)

    private fun index(c: Coordinate): Int =
        c.x + c.y * region.width

    private fun cell(index: Int): Cell =
        region[index % region.width, index / region.width]

    private class EntrySet<V : Any>(val map: CellMap<V>): AbstractSet<MutableMap.MutableEntry<Cell, V>>() {

        override fun iterator() =
            EntryIterator<V>(map)

        override fun add(e: MutableMap.MutableEntry<Cell, V>): Boolean {
            map[e.key] = e.value
            return true
        }

        public override fun remove(o: Any?): Boolean {
            if (o is Map.Entry<Any?, Any?>) {
                val key = o.key as Cell
                val value = map.get(key)
                if (value == o.value)
                    return map.remove(key) != null
                else
                    return false
            }

            return false
        }

        override fun contains(o: Any?) =
            (o is Map.Entry<Any?, Any?>) && (map.get(o.key as Cell) == o.value)

        override fun size() = map.size
    }

    private class CellMapEntry<V : Any>(val map: CellMap<V>, val index: Int): MutableMap.MutableEntry<Cell, V> {

        override fun getKey() = map.cell(index)
        override fun getValue() = map.mappings[index]!!

        override fun setValue(value: V): V {
            val old = map.mappings[index]
            map.mappings[index] = value
            return old!!
        }

        override fun equals(other: Any?): Boolean {
            if (other == this)
                return true

            if (other is Map.Entry<Any?, Any?>) {
                val rhs = other as Map.Entry<Any?, Any?>

                return getKey() == rhs.key && getValue() == rhs.value
            }

            return false
        }

        override fun hashCode() = Objects.hash(getKey(), getValue())
    }

    private class EntryIterator<V : Any>(val map: CellMap<V>): MutableIterator<MutableMap.MutableEntry<Cell, V>> {
        private var index = 0
        private var previous = -1

        override fun hasNext(): Boolean {
            while (index < map.mappings.size && map.mappings[index] == null)
                index++

            return index < map.mappings.size
        }

        override fun next(): MutableMap.MutableEntry<Cell, V> {
            if (hasNext()) {
                previous = index++
                return CellMapEntry(map, previous)
            }
            else
                throw NoSuchElementException()
        }

        override fun remove() {
            if (previous == -1)
                throw IllegalStateException()
            map.mappings[previous] = null
        }
    }
}
