/**
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

import net.akehurst.kotlin.komposite.api.*
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.agl.processor.SyntaxAnalysisResultDefault
import net.akehurst.language.agl.syntaxAnalyser.BranchHandler
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserAbstract
import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.grammar.GrammarItem
import net.akehurst.language.api.grammar.RuleItem
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.*
import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SharedPackedParseTree


class KompositeSemanticAnalyser : SemanticAnalyser<DatatypeModel,Any> {


    override fun clear() {

    }

    override fun configure(configurationContext: SentenceContext<GrammarItem>, configuration: Map<String, Any>): List<LanguageIssue> {
       return emptyList<LanguageIssue>()
    }

    override fun analyse(asm: DatatypeModel, locationMap: Map<Any, InputLocation>?, context: Any?, options: Map<String, Any>): SemanticAnalysisResult {
        return SemanticAnalysisResultDefault(IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS))
    }
}