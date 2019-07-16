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
            nullValue() { key,info->
                WalkInfo(key.toString(), "null")
            }
        }

        val actual = sut.walk(WalkInfo(null, ""), null)

        val expected = WalkInfo<String?, String>("", "null")

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
               collection LinkedHashMap
            }
        """)
        val sut = kompositeWalker<String, String>(reg) {
            primitive { key,info, value ->
                when(value) {
                    is Int -> result += "${value}"
                    is String -> result += "'${value}'"
                }
                info
            }
            mapBegin { key, info, map ->
                result += "Map { "
                info
            }
            mapEntryKeyBegin { key, info, entry ->
                result += "["
                info
            }
            mapEntryKeyEnd { key, info, entry ->
                result += "]"
                info
            }
            mapEntryValueBegin { key, info, entry ->
                result += " = "
                info
            }
            mapEntryValueEnd { key, info, entry ->  info}
            mapSeparate { key, info, map, previousEntry ->
                result += ", "
                info
            }
            mapEnd { key, info, map ->
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
                 
               }
            }
        """)

        var result = ""
        val sut = kompositeWalker<String, String>(reg) {
            objectBegin { key,info, obj, datatype ->
                result += "${datatype.name} { "
                info
            }
            objectEnd { key,info, obj, datatype ->
                result += " }"
                info
            }
            propertyBegin { key,info, property ->
                result += "${property.name} = "
                info
            }
            primitive { key,info, value ->
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

}