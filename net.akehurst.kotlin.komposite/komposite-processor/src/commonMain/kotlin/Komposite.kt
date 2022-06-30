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
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.processor.LanguageProcessor

object Komposite {

    private var _processor: LanguageProcessor? = null

    internal fun processor(): LanguageProcessor {
        if (null == _processor) {
            val grammarStr = fetchGrammarStr()
            _processor = Agl.processorFromString(
                grammarDefinitionStr = grammarStr,
                syntaxAnalyser = KompositeSyntaxAnalyser(),
                formatter = Formatter()
            ).buildFor("model")
        }
        return _processor!!
    }

    internal fun fetchGrammarStr(): String {
        return """
            namespace net.akehurst.kotlin.komposite
            
            grammar Composite {
                skip WHITE_SPACE = "\s+" ;
                skip COMMENT = MULTI_LINE_COMMENT | SINGLE_LINE_COMMENT ;
                     MULTI_LINE_COMMENT = "[/][*](?:.|\n)*?[*][/]" ;
                     SINGLE_LINE_COMMENT = "//[^\n\r]*" ;
            
                model = namespace* ;
                namespace = 'namespace' path '{' declaration* '}' ;
                path = [ NAME / '.']+ ;
                declaration = primitive | enum | collection | datatype ;
                primitive = 'primitive' NAME ;
                enum = 'enum' NAME ;
                collection = 'collection' NAME '<' typeParameterList '>' ;
                typeParameterList = [ NAME / ',']+ ;
                datatype = 'datatype' NAME supertypes? '{' property* '}' ;
                supertypes = ':' [ typeReference / ',']+ ;
                property = characteristic NAME ':' typeReference ;
                typeReference = path typeArgumentList? '?'?;
                typeArgumentList = '<' [ typeReference / ',']+ '>' ;
                characteristic
                   = 'reference-val'    // reference, constructor argument
                   | 'reference-var'    // reference mutable property
                   | 'composite-val'    // composite, constructor argument
                   | 'composite-var'    // composite mutable property
                   | 'dis'    // disregard / ignore
                   ;
            
                NAME = "[a-zA-Z_][a-zA-Z0-9_]*" ;
                POSITIVE_INTEGER = "[0-9]+" ;
            
            }
            
            """.trimIndent()
    }

    fun process(datatypeModel: String): DatatypeModel {
        //TODO: handle issues
        return this.processor().process<DatatypeModel, Any>(datatypeModel, "model").first!!
    }
}