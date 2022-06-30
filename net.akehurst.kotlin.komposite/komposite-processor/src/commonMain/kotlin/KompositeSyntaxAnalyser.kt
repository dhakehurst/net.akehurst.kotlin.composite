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
import net.akehurst.language.agl.syntaxAnalyser.BranchHandler
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserAbstract
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.grammar.RuleItem
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.SentenceContext
import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SharedPackedParseTree


class KompositeSyntaxAnalyser : SyntaxAnalyser<DatatypeModel,Any> {

    class SyntaxAnalyserException : RuntimeException {
        constructor(message: String) : super(message)
    }

    private val issues= mutableListOf<LanguageIssue>()

    override val locationMap: Map<Any, InputLocation> = mutableMapOf()

    override fun clear() {

    }

    override fun configure(configurationContext: SentenceContext, configuration: String): List<LanguageIssue> {
        return emptyList()
    }

    override fun transform(sppt: SharedPackedParseTree, mapToGrammar: (Int, Int) -> RuleItem, context: Any?): Pair<DatatypeModel, List<LanguageIssue>> {
        val asm = this.transformBranch<DatatypeModel>(sppt.root.asBranch, "")
        return Pair(asm,issues)
    }

    private fun <T:Any> transformBranch(branch:SPPTBranch, arg: Any):T {
        return when(branch.name) {
            "model" -> transformModel(branch, branch.branchNonSkipChildren, arg) as T
            "namespace" -> namespace(branch, branch.branchNonSkipChildren, arg) as T
            "path" -> path(branch, branch.branchNonSkipChildren, arg) as T
            "declaration" -> declaration(branch, branch.branchNonSkipChildren, arg) as T
            "primitive" -> primitive(branch, branch.branchNonSkipChildren, arg) as T
            "enum" -> enum(branch, branch.branchNonSkipChildren, arg) as T
            "collection" -> collection(branch, branch.branchNonSkipChildren, arg) as T
            "datatype" -> datatype(branch, branch.branchNonSkipChildren, arg) as T
            "property" -> property(branch, branch.branchNonSkipChildren, arg) as T
            "characteristic" -> characteristic(branch, branch.branchNonSkipChildren, arg) as T
            "typeReference" -> typeReference(branch, branch.branchNonSkipChildren, arg) as T
            "typeArgumentList" -> typeArgumentList(branch, branch.branchNonSkipChildren, arg) as T
            else -> error("SPPTBranch with name '${branch.name}' not handled")
        }
    }

    // model = namespace* ;
    private fun transformModel(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): DatatypeModel {
        val result = DatatypeModelSimple()

        val namespaces = children.forEach {
            val ns = this.transformBranch<Namespace>(it, result)
            result.addNamespace(ns)
        }

        return result
    }

    // namespace = 'namespace' path '{' declaration* '}' ;
    private fun namespace(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): Namespace {
        val model = arg as DatatypeModel
        val path = this.transformBranch<List<String>>(children[0], arg)
        val qn = path.joinToString(separator = ".")
        val result = NamespaceSimple(model, qn)
        children[1].branchNonSkipChildren.forEach {
            val dt = this.transformBranch<TypeDeclaration>(it, result)
            result.addDeclaration(dt)
        }
        return result
    }

    // path = [ NAME / '.']+ ;
    private fun path(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): List<String> {
        return children.map { it.nonSkipMatchedText }
    }

    // declaration = primitive | enum | collection | datatype ;
    private fun declaration(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): TypeDeclaration {
        return this.transformBranch(children[0], arg)
    }

    // primitive = 'primitive' NAME ;
    private fun primitive(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): PrimitiveType {
        val namespace = arg as Namespace
        val name = children[0].nonSkipMatchedText
        val result = PrimitiveTypeSimple(namespace, name)
        return result
    }

    // enum = 'enum' NAME ;
    private fun enum(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): EnumType {
        val namespace = arg as Namespace
        val name = children[0].nonSkipMatchedText
        val result = EnumTypeSimple(namespace, name)
        return result
    }

    // collection = collection = 'collection' NAME '<' typeParameterList '>' ;
    private fun collection(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): CollectionType {
        val namespace = arg as Namespace
        val name = children[0].nonSkipMatchedText
        val params = children[1].branchNonSkipChildren.map { it.nonSkipMatchedText }
        val result = CollectionTypeSimple(namespace, name, params)
        return result
    }

    // datatype = 'datatype' NAME supertypes? '{' property* '}' ;
    // supertypes = ':' [ typeReference / ',']+ ;
    private fun datatype(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): Datatype {
        val namespace = arg as Namespace
        val name = children[0].nonSkipMatchedText
        val result = DatatypeSimple(namespace, name)
        if (children[1].isEmptyMatch.not()) {
            children[1].branchNonSkipChildren[0].branchNonSkipChildren[0].branchNonSkipChildren.forEach {
                val st = this.transformBranch<TypeReference>(it, result)
                result.addSuperType(st)
            }
        }
        children[2].branchNonSkipChildren.forEach {
            val p = this.transformBranch<DatatypeProperty>(it, result)
            result.addProperty(p)
        }
        return result
    }

    // property = characteristic NAME : typeReference ;
    private fun property(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): DatatypeProperty {
        val datatype = arg as Datatype
        val char:DatatypePropertyCharacteristic = this.transformBranch(children[0],arg)
        val name = children[1].nonSkipMatchedText
        val typeReference:TypeReference = this.transformBranch(children[2], arg)

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
            else ->throw SyntaxAnalyserException("unknown characteristic")
        }
        return result
    }

    // characteristic  = 'val'    // reference, constructor argument
    //                 | 'var'    // reference mutable property
    //                 | 'cal'    // composite, constructor argument
    //                 | 'car'    // composite mutable property
    //                 | 'dis'    // disregard / ignore
    //                 ;
    private fun characteristic(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): DatatypePropertyCharacteristic {
        return when (target.nonSkipMatchedText) {
            "reference-val"->DatatypePropertyCharacteristic.reference_val
            "reference-var"->DatatypePropertyCharacteristic.reference_var
            "composite-val"->DatatypePropertyCharacteristic.composite_val
            "composite-var"->DatatypePropertyCharacteristic.composite_var
            "dis"->DatatypePropertyCharacteristic.dis
            else -> error("Value not allowed '${target.nonSkipMatchedText}'")
        }
    }

    // typeReference = path typeArgumentList? '?'?;
    private fun typeReference(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): TypeReference {
        val path = children[0]
        val typeArgumentList = children[1]
        val typePath:List<String> = this.transformBranch(path, arg)
        val typeArgs = if (typeArgumentList.isEmptyMatch) {
            emptyList<TypeReference>()
        } else {
            this.transformBranch(typeArgumentList.branchNonSkipChildren[0], arg)
        }
        val dt = arg as Datatype
        return TypeReferenceSimple({ tref->dt.namespace.model.resolve(tref)}, typePath, typeArgs)
    }

    //typeArgumentList = '<' [ typeReference / ',']+ '>' ;
    private fun typeArgumentList(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): List<TypeReference> {
        val list = children[0]
        return if (list.isEmptyMatch) {
            emptyList()
        } else {
            val dt = arg as Datatype
            val elems = list.branchNonSkipChildren
            elems.map {
                this.transformBranch<TypeReference>(it, arg)
            }
        }
    }

}