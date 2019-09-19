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
import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.processor.BranchHandler
import net.akehurst.language.processor.SyntaxAnalyserAbstract

class SyntaxAnalyser : SyntaxAnalyserAbstract() {

    class SyntaxAnalyserException : RuntimeException {
        constructor(message: String) : super(message)
    }

    init {
        // could autodetect these by reflection, for jvm, but faster if registered
        this.register("model", this::model as BranchHandler<DatatypeModel>)
        this.register("namespace", this::namespace as BranchHandler<Namespace>)
        this.register("path", this::path as BranchHandler<List<String>>)
        this.register("declaration", this::declaration as BranchHandler<TypeDeclaration>)
        this.register("primitive", this::primitive as BranchHandler<Datatype>)
        this.register("collection", this::collection as BranchHandler<Datatype>)
        this.register("datatype", this::datatype as BranchHandler<Datatype>)
        this.register("property", this::property as BranchHandler<DatatypeProperty>)
        this.register("typeDeclaration", this::characteristicValue as BranchHandler<Datatype>)
        this.register("typeArgumentList", this::characteristicValue as BranchHandler<Datatype>)
        this.register("characteristic", this::characteristic as BranchHandler<Datatype>)
    }

    override fun clear() {

    }

    override fun <T> transform(sppt: SharedPackedParseTree): T {
        return this.transform<T>(sppt.root.asBranch, "") as T
    }

    // model = namespace* ;
    fun model(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): DatatypeModel {
        val result = DatatypeModelSimple()

        val namespaces = children[0].branchNonSkipChildren.forEach {
            val ns = super.transform<Namespace>(it, arg)
            result.addNamespace(ns)
        }

        return result
    }

    // namespace = 'namespace' path '{' declaration* '}' ;
    fun namespace(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): Namespace {
        val path = super.transform<List<String>>(children[0], arg)
        val result = NamespaceSimple(path)
        children[1].branchNonSkipChildren.forEach {
            val dt = super.transform<TypeDeclaration>(it, result)
            result.addDeclaration(dt)
        }
        return result
    }

    // path = [ NAME / '.']+ ;
    fun path(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): List<String> {
        return children[0].branchNonSkipChildren.map { it.nonSkipMatchedText }
    }

    // declaration = primitive | collection | datatype ;
    fun declaration(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): TypeDeclaration {
        return super.transform(children[0], arg)
    }

    // primitive = 'primitive' NAME ;
    fun primitive(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): PrimitiveType {
        val namespace = arg as Namespace
        val name = children[0].nonSkipMatchedText
        val result = PrimitiveTypeSimple(namespace, name)
        return result
    }

    // collection = 'primitive' NAME ;
    fun collection(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): CollectionType {
        val namespace = arg as Namespace
        val name = children[0].nonSkipMatchedText
        val result = CollectionTypeSimple(namespace, name)
        return result
    }

    // datatype = 'datatype' NAME '{' property* '}' ;
    fun datatype(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): Datatype {
        val namespace = arg as Namespace
        val name = children[0].nonSkipMatchedText
        val result = DatatypeSimple(namespace, name)
        children[1].branchNonSkipChildren.forEach {
            val p = super.transform<DatatypeProperty>(it, result)
            result.addProperty(p)
        }
        return result
    }


    // property = characteristic NAME : typeDeclaration ;
    fun property(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): DatatypeProperty {
        val datatype = arg as Datatype
        val name = children[1].nonSkipMatchedText
        val result = DatatypePropertySimple(datatype, name)

        val char = children[0].nonSkipMatchedText

        when (char) {
            "val" -> {
                result.isReference = true
                result.setIdentityIndex(datatype.identityProperties.size)
            }
            "var" -> result.isReference = true
            "cal" -> {
                result.isComposite = true
                result.setIdentityIndex(datatype.identityProperties.size)
            }
            "car" -> result.isComposite = true
            "dis" -> result.ignore = true
            else ->throw SyntaxAnalyserException("unknown characteristic")
        }

        // TODO: typeDeclaration

        return result
    }

    // characteristic =  characteristicValue | identity ;
    fun characteristic(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): Any {
        return super.transform(children[0], arg)
    }

    // characteristicValue = 'composite' | 'reference' | 'ignore' ;
    fun characteristicValue(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): String {
        return target.nonSkipMatchedText
    }

    // identity = 'identity' '(' POSITIVE_INTEGER ')'
    fun identity(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): Int {
        return children[0].nonSkipMatchedText.toInt()
    }

}