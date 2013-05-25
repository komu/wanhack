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

package net.wanhack.test.files

import org.junit.Test as test
import net.wanhack.definitions.*

class DefinitionsTest {

    test fun loadDefinitions() {
        for (def in Weapons.itemDefinitions)
            def.create()

        for (def in Items.itemDefinitions)
            def.create()

        for (def in Creatures.creatureDefinitions)
            def.create()
    }
}