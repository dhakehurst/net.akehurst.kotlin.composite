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

import net.akehurst.language.api.processor.LanguageProcessor
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.*
import kotlin.test.Test
import kotlin.test.assertNotNull


@RunWith(Parameterized::class)
class test_Parser(val data:Data) {

    class Data(val sourceFileName: String, val fileContent:String) {

        // --- Object ---
        override fun toString(): String {
            return this.sourceFileName
        }
    }

    companion object {
        var processor: LanguageProcessor = Komposite.processor()

        val sourceFiles = listOf(
                "testFiles/empty.komposite",
                "testFiles/emptyNamespace.komposite",
                "testFiles/emptyDatatype.komposite",
                "testFiles/datatypes.komposite",
                "testFiles/generictypes.komposite"
        )


        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Data> {
            val col = ArrayList<Data>()
            for (sourceFile in sourceFiles) {
                var fileContent = ClassLoader.getSystemClassLoader().getResourceAsStream(sourceFile).reader().readText()
                col.add(Data(sourceFile, fileContent))
            }
            return col
        }
    }


    @Test
    fun test() {
        val result = processor.parse("model", this.data.fileContent)
        assertNotNull(result)
        //val resultStr = result.asString
        //assertEquals(original, resultStr)
    }

}