/**
 * Copyright (C) 2019 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.kotlin.komposite.common

import kotlin.test.Test
import kotlin.test.assertEquals


class test_KompositeWalker {

    data class A (
        val prop1:String = "hello"
    )

    @Test
    fun walk_null() {
        val reg = DatatypeRegistry()
        val sut = kompositeWalker<String?, String>(reg) {
            nullValue() { path,info->
                WalkInfo(path.lastOrNull(), "null")
            }
        }

        val actual = sut.walk(WalkInfo(null, ""), null)

        val expected = WalkInfo<String?, String>(null, "null")

        assertEquals( expected, actual)
    }

    @Test
    fun walk_Map() {

        val map = mapOf<String, Int>(
                "a" to 1,
                "b" to 2,
                "c" to 3
        )

        var result = ""
        val reg = DatatypeRegistry()
        reg.registerFromConfigString("""
            namespace kotlin {
                primitive Int
                primitive String
            }
            namespace kotlin.collections {
               collection Map<K,V>
            }
        """, emptyMap())
        val sut = kompositeWalker<String, String>(reg) {
            primitive { path,info, value, m ->
                when(value) {
                    is Int -> result += "${value}"
                    is String -> result += "'${value}'"
                }
                info
            }
            mapBegin { path, info, map ->
                result += "Map { "
                info
            }
            mapEntryKeyBegin { path, info, entry ->
                result += "["
                info
            }
            mapEntryKeyEnd { path, info, entry ->
                result += "]"
                info
            }
            mapEntryValueBegin { path, info, entry ->
                result += " = "
                info
            }
            mapEntryValueEnd { path, info, entry ->  info}
            mapSeparate { path, info, map, previousEntry ->
                result += ", "
                info
            }
            mapEnd { path, info, map ->
                result += " }"
                info
            }
        }

        val actual = sut.walk(WalkInfo("", ""), map)
        val expected = "Map { ['a'] = 1, ['b'] = 2, ['c'] = 3 }"
        assertEquals(expected, result)
    }

    @Test
    fun walk_object() {

        val obj1 = A()

        val reg = DatatypeRegistry()

        reg.registerFromConfigString("""
            namespace net.akehurst.kotlin.komposite.common {
               primitive String
               datatype A {
                 composite-val prop1:String
               }
            }
        """, emptyMap())

        var result = ""
        val sut = kompositeWalker<String, String>(reg) {
            objectBegin { path,info, obj, datatype ->
                result += "${datatype.name} { "
                info
            }
            objectEnd { path,info, obj, datatype ->
                result += " }"
                info
            }
            propertyBegin { path,info, property ->
                result += "${property.name} = "
                info
            }
            primitive { path,info, value, m ->
                when(value) {
                    is String -> result += "'${value}'"
                }
                info
            }
        }

        val actual = sut.walk(WalkInfo("", ""), obj1)
        val expected = "A { prop1 = 'hello' }"
        assertEquals(expected, result)
    }

    //TODO
}