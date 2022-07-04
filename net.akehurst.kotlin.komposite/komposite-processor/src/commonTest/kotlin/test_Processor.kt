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

import net.akehurst.kotlin.komposite.api.DatatypeModel
import net.akehurst.language.api.processor.LanguageProcessor
import kotlin.test.Test
import kotlin.test.assertNotNull


class test_Processor() {

    companion object {
        var processor: LanguageProcessor = Komposite.processor()
        val komposite = """
            namespace kotlin.collections {
                collection List<E>
            }
            namespace net.akehurst.kotlin.composite.api {
              
              primitive XXXX
              enum YYYY
              
              datatype TestDatatype {
                    composite-val id : String
                    composite-var prop1 : String
                    reference-var prop2 : Int
                    dis prop4 : String
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

        val (sppt,issues) = processor.parse( komposite,"model")
        assertNotNull(sppt,issues.joinToString(separator = "\n"){"$it"})
        //val resultStr = result.asString
        //assertEquals(original, resultStr)
    }

    @Test
    fun process() {
        val (result,issues) = processor.process<DatatypeModel,Any>( komposite,"model")
        assertNotNull(result,issues.joinToString(separator = "\n"){"$it"})
        //val resultStr = result.asString
        //assertEquals(original, resultStr)
    }

}