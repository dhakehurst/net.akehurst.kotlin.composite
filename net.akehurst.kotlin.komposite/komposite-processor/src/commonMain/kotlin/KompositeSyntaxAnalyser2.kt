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

import net.akehurst.kotlin.komposite.api.*
import net.akehurst.kotlin.komposite.api.Namespace
import net.akehurst.language.agl.collections.toSeparatedList
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.SyntaxAnalysisResultDefault
import net.akehurst.language.agl.sppt.SPPTFromTreeData
import net.akehurst.language.agl.syntaxAnalyser.BranchHandler
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserAbstract
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserFromTreeDataAbstract
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.grammar.*
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.processor.SentenceContext
import net.akehurst.language.api.processor.SyntaxAnalysisResult
import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.api.sppt.SpptDataNodeInfo
import net.akehurst.language.typemodel.api.PropertyDeclaration


class KompositeSyntaxAnalyser2 : SyntaxAnalyserFromTreeDataAbstract<DatatypeModel>() {

    class SyntaxAnalyserException : RuntimeException {
        constructor(message: String) : super(message)
    }

    init {
        super.register(this::model)
        super.register(this::namespace)
        super.register(this::path)
        super.register(this::declaration)
        super.register(this::primitive)
        super.register(this::enum)
        super.register(this::collection)
        super.register(this::datatype)
        super.register(this::supertypes)
        super.register(this::property)
        super.register(this::characteristic)
        super.register(this::typeReference)
        super.register(this::typeArgumentList)
    }

    private val _localStore = mutableMapOf<String, Any>()

    override fun configure(configurationContext: SentenceContext<GrammarItem>, configuration: Map<String, Any>): List<LanguageIssue> {
        return emptyList()
    }

    // model = namespace* ;
    private fun model(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: String): DatatypeModel {
        val result = DatatypeModelSimple()
        val namespaces = (children as List<Namespace?>).filterNotNull()
        namespaces.forEach { result.addNamespace(it) }
        return result
    }

    // namespace = 'namespace' path '{' declaration* '}' ;
    private fun namespace(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: String): Namespace {
        val model = arg as DatatypeModel
        val path = children[1] as List<String>
        val qn = path.joinToString(separator = ".")
        val result = NamespaceSimple(model, qn)
        val declaration = (children[3] as List<TypeDeclaration?>).filterNotNull()
        declaration.forEach { result.addDeclaration(it) }
        return result
    }

    // path = [ NAME / '.']+ ;
    private fun path(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: String): List<String> {
        return children.toSeparatedList<String,String>().items
    }

    // declaration = primitive | enum | collection | datatype ;
    private fun declaration(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: String): TypeDeclaration {
        return children[0] as TypeDeclaration
    }

    // primitive = 'primitive' NAME ;
    private fun primitive(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: String): PrimitiveType {
        val namespace = arg as Namespace
        val name = children[1] as String
        val result = PrimitiveTypeSimple(namespace, name)
        return result
    }

    // enum = 'enum' NAME ;
    private fun enum(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: String): EnumType {
        val namespace = arg as Namespace
        val name = children[0].nonSkipMatchedText
        val result = EnumTypeSimple(namespace, name)
        return result
    }

    // collection = collection = 'collection' NAME '<' typeParameterList '>' ;
    private fun collection(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: String): CollectionType {
        val namespace = arg as Namespace
        val name = children[0].nonSkipMatchedText
        val params = children[1].branchNonSkipChildren.map { it.nonSkipMatchedText }
        val result = CollectionTypeSimple(namespace, name, params)
        return result
    }

    // datatype = 'datatype' NAME supertypes? '{' property* '}' ;
    private fun datatype(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: String): Datatype {
        val namespace = arg as Namespace
        val name = children[1] as String
        val supertypes = children[2] as List<TypeReference>? ?: emptyList()
        val property = (children[4] as List<DatatypeProperty?>).filterNotNull()
        val result = DatatypeSimple(namespace, name)
        supertypes.forEach { result.addSuperType(it) }
        property.forEach { result.addProperty(it) }
        return result
    }

    // supertypes = ':' [ typeReference / ',']+ ;
    private fun supertypes(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: String): List<TypeReference> {
        return (children[1] as List<*>).toSeparatedList<TypeReference, String>().items
    }

    // property = characteristic NAME : typeReference ;
    private fun property(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: String): DatatypeProperty {
        val datatype = arg as Datatype
        val char: DatatypePropertyCharacteristic = children[0] as DatatypePropertyCharacteristic
        val name = children[1] as String
        val typeReference: TypeReference = children[3] as TypeReference

        val result = DatatypePropertySimple(datatype, name, typeReference)
        when (char) {
            DatatypePropertyCharacteristic.reference_val -> {
                result.isReference = true
                result.setIdentityIndex(datatype.identityProperties.size)
            }

            DatatypePropertyCharacteristic.reference_var -> result.isReference = true
            DatatypePropertyCharacteristic.composite_val -> {
                result.isComposite = true
                result.setIdentityIndex(datatype.identityProperties.size)
            }

            DatatypePropertyCharacteristic.composite_var -> result.isComposite = true
            DatatypePropertyCharacteristic.dis -> result.ignore = true
            else -> throw SyntaxAnalyserException("unknown characteristic")
        }
        return result
    }

    // characteristic  = 'val'    // reference, constructor argument
    //                 | 'var'    // reference mutable property
    //                 | 'cal'    // composite, constructor argument
    //                 | 'car'    // composite mutable property
    //                 | 'dis'    // disregard / ignore
    //                 ;
    private fun characteristic(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: String): DatatypePropertyCharacteristic {
        return when (children[0] as String) {
            "reference-val" -> DatatypePropertyCharacteristic.reference_val
            "reference-var" -> DatatypePropertyCharacteristic.reference_var
            "composite-val" -> DatatypePropertyCharacteristic.composite_val
            "composite-var" -> DatatypePropertyCharacteristic.composite_var
            "dis" -> DatatypePropertyCharacteristic.dis
            else -> error("Value not allowed '${children[0]}'")
        }
    }

    // typeReference = path typeArgumentList? '?'?;
    private fun typeReference(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: String): TypeReference {
        val path = children[0] as List<String>
        val typeArgumentList = children[1] as List<TypeReference>? ?: emptyList()
        val dt = arg as DatatypeSimple
        return TypeReferenceSimple(dt.resolver, path, typeArgumentList)
    }

    //typeArgumentList = '<' [ typeReference / ',']+ '>' ;
    private fun typeArgumentList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: String): List<TypeReference> {
        val list = (children[1] as List<*>).toSeparatedList<TypeReference, String>().items
        return list
    }

}