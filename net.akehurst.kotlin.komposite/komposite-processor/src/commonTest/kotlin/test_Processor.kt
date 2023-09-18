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

package net.akehurst.kotlin.komposite.processor

import net.akehurst.language.typemodel.api.PropertyCharacteristic
import net.akehurst.language.typemodel.api.typeModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class test_Processor() {

    private companion object {
        var processor = Komposite.processor()
        val komposite = """
            namespace colls {
                collection List<E>
            }
            namespace test {
              primitive String
              primitive Int
              primitive XXXX
              enum YYYY
              
              datatype TestDatatype {
                    composite-val id : String
                    composite-var prop1 : String
                    reference-var prop2 : Int
                    dis ignored : String
                    composite-val id2 : Int
              }
              
              datatype Dt2 {
              }
            
              datatype TestDatatype2 : TestDatatype, Dt2 {
            
              }
            
            }
        """.trimIndent()
    }

    @Test
    fun parse() {
        val result = processor.parse(komposite)
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { "$it" })
        //val resultStr = result.asString
        //assertEquals(original, resultStr)
    }

    @Test
    fun process() {

        val expected = typeModel("aTypeModel", false, emptyList()) {
            namespace("colls", mutableListOf()) {
                collectionType("List", listOf("E"))
            }
            namespace("test", mutableListOf()) {
                primitiveType("String")
                primitiveType("Int")
                primitiveType("XXXX")
                enumType("YYYY", emptyList())
                dataType("TestDatatype") {
                    propertyOf(setOf(COMPOSITE, IDENTITY), "id", "String")
                    propertyOf(setOf(COMPOSITE, MEMBER), "prop1", "String")
                    propertyOf(setOf(REFERENCE, MEMBER), "prop2", "Int")
                    propertyOf(setOf(), "ignored", "String")
                    propertyOf(setOf(COMPOSITE, IDENTITY), "id2", "Int")
                }
                dataType("Dt2")
                dataType("TestDatatype2") {
                    supertypes("TestDatatype", "Dt2")
                }

            }
        }

        val result = processor.process(komposite)
        assertNotNull(result.asm, result.issues.joinToString(separator = "\n") { "$it" })
        assertEquals(expected.asString(), result.asm?.asString())
    }

}
