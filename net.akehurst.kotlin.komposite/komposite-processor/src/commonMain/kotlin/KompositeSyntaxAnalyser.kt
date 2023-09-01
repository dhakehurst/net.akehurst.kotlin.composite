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
import net.akehurst.language.agl.collections.toSeparatedList
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.grammar.GrammarItem
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.SentenceContext
import net.akehurst.language.api.sppt.Sentence
import net.akehurst.language.api.sppt.SpptDataNodeInfo


class KompositeSyntaxAnalyser : SyntaxAnalyserByMethodRegistrationAbstract<DatatypeModel>() {

    class SyntaxAnalyserException : RuntimeException {
        constructor(message: String) : super(message)
    }

    override fun registerHandlers() {
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


    override val embeddedSyntaxAnalyser: Map<String, SyntaxAnalyser<DatatypeModel>> = emptyMap()

    override fun configure(configurationContext: SentenceContext<GrammarItem>, configuration: Map<String, Any>): List<LanguageIssue> {
        return emptyList()
    }

    // model = namespace* ;
    private fun model(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): DatatypeModel {
        val result = DatatypeModelSimple()
        val namespaces = (children as List<((model: DatatypeModel) -> Namespace)?>).filterNotNull()
        namespaces.forEach {
            val ns = it.invoke(result)
            result.addNamespace(ns)
        }
        return result
    }

    // namespace = 'namespace' path '{' declaration* '}' ;
    private fun namespace(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (model: DatatypeModel) -> Namespace {
        val path = children[1] as List<String>
        val declaration = (children[3] as List<((namespace: Namespace) -> TypeDeclaration)?>).filterNotNull()
        val qn = path.joinToString(separator = ".")

        val result = { model: DatatypeModel ->
            val ns = NamespaceSimple(model, qn)
            declaration.forEach {
                val dec = it.invoke(ns)
                ns.addDeclaration(dec)
            }
            ns//.also { locationMap[it] = nodeInfo.node.locationIn(sentence) }
        }
        return result
    }

    // path = [ NAME / '.']+ ;
    private fun path(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<String> {
        return children.toSeparatedList<String, String>().items
    }

    // declaration = primitive | enum | collection | datatype ;
    private fun declaration(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (namespace: Namespace) -> TypeDeclaration {
        return children[0] as (namespace: Namespace) -> TypeDeclaration
    }

    // primitive = 'primitive' NAME ;
    private fun primitive(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (namespace: Namespace) -> PrimitiveType {
        val name = children[1] as String
        val result = { namespace: Namespace ->
            PrimitiveTypeSimple(namespace, name)//.also { locationMap[it] = nodeInfo.node.locationIn(sentence) }
        }
        return result
    }

    // enum = 'enum' NAME ;
    private fun enum(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (namespace: Namespace) -> EnumType {
        val name = children[1] as String
        val result = { namespace: Namespace ->
            EnumTypeSimple(namespace, name)//.also { locationMap[it] = nodeInfo.node.locationIn(sentence) }
        }
        return result
    }

    // collection = 'collection' NAME '<' typeParameterList '>' ;
    private fun collection(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (namespace: Namespace) -> CollectionType {
        val name = children[1] as String
        val params = children[3] as List<String>
        val result = { namespace: Namespace ->
            CollectionTypeSimple(namespace, name, params)//.also { locationMap[it] = nodeInfo.node.locationIn(sentence) }
        }
        return result
    }

    // datatype = 'datatype' NAME supertypes? '{' property* '}' ;
    private fun datatype(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (namespace: Namespace) -> Datatype {
        val name = children[1] as String
        val supertypes = children[2] as List<TypeReference>? ?: emptyList()
        val property = (children[4] as List<((Datatype) -> DatatypeProperty)?>).filterNotNull()

        val result = { namespace: Namespace ->
            val dt = DatatypeSimple(namespace, name)
            supertypes.forEach {
                (it as TypeReferenceSimple).contextResolver = dt.resolver
                dt.addSuperType(it)
            }
            property.forEach {
                val p = it.invoke(dt)
                dt.addProperty(p)
            }
            dt//.also { locationMap[it] = nodeInfo.node.locationIn(sentence) }
        }
        return result
    }

    // supertypes = ':' [ typeReference / ',']+ ;
    private fun supertypes(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<TypeReference> {
        return (children[1] as List<*>).filterNotNull().toSeparatedList<TypeReference, String>().items
    }

    // property = characteristic NAME : typeReference ;
    private fun property(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (Datatype) -> DatatypeProperty {
        val char: DatatypePropertyCharacteristic = children[0] as DatatypePropertyCharacteristic
        val name = children[1] as String
        val typeReference = children[3] as TypeReferenceSimple

        val result = { datatype: Datatype ->
            typeReference.contextResolver = (datatype as DatatypeSimple).resolver
            val dt = DatatypePropertySimple(datatype, name, typeReference)
            when (char) {
                DatatypePropertyCharacteristic.reference_val -> {
                    dt.isReference = true
                    dt.setIdentityIndex(datatype.identityProperties.size)
                }

                DatatypePropertyCharacteristic.reference_var -> dt.isReference = true
                DatatypePropertyCharacteristic.composite_val -> {
                    dt.isComposite = true
                    dt.setIdentityIndex(datatype.identityProperties.size)
                }

                DatatypePropertyCharacteristic.composite_var -> dt.isComposite = true
                DatatypePropertyCharacteristic.dis -> dt.ignore = true
                else -> throw SyntaxAnalyserException("unknown characteristic")
            }
            dt//.also { locationMap[it] = nodeInfo.node.locationIn(sentence) }
        }
        return result
    }

    // characteristic  = 'val'    // reference, constructor argument
    //                 | 'var'    // reference mutable property
    //                 | 'cal'    // composite, constructor argument
    //                 | 'car'    // composite mutable property
    //                 | 'dis'    // disregard / ignore
    //                 ;
    private fun characteristic(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): DatatypePropertyCharacteristic {
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
    private fun typeReference(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TypeReference {
        val path = children[0] as List<String>
        val typeArgumentList = children[1] as List<TypeReference>? ?: emptyList()
        val tr = TypeReferenceSimple(path, typeArgumentList)//.also { locationMap[it] = nodeInfo.node.locationIn(sentence) }
        return tr
    }

    //typeArgumentList = '<' [ typeReference / ',']+ '>' ;
    private fun typeArgumentList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<TypeReference> {
        val list = (children[1] as List<*>).toSeparatedList<TypeReference, String>().items
        return list
    }

}