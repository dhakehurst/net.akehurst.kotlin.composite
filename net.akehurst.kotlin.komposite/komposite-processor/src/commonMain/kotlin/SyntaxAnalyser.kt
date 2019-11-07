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
        this.register("characteristic", this::characteristic as BranchHandler<DatatypePropertyCharacteristic>)
        this.register("typeReference", this::typeReference as BranchHandler<TypeReference>)
        this.register("typeArgumentList", this::typeArgumentList as BranchHandler<List<TypeReference>>)
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
            val ns = super.transform<Namespace>(it, result)
            result.addNamespace(ns)
        }

        return result
    }

    // namespace = 'namespace' path '{' declaration* '}' ;
    fun namespace(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): Namespace {
        val model = arg as DatatypeModel
        val path = super.transform<List<String>>(children[0], arg)
        val result = NamespaceSimple(model, path)
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

    // collection = collection = 'collection' NAME '<' typeParameterList '>' ;
    fun collection(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): CollectionType {
        val namespace = arg as Namespace
        val name = children[0].nonSkipMatchedText
        val params = children[1].branchNonSkipChildren[0].branchNonSkipChildren.map { it.nonSkipMatchedText }
        val result = CollectionTypeSimple(namespace, name, params)
        return result
    }

    // datatype = 'datatype' NAME supertypes? '{' property* '}' ;
    // supertypes = ':' [ typeReference / ',']+ ;
    fun datatype(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): Datatype {
        val namespace = arg as Namespace
        val name = children[0].nonSkipMatchedText
        val result = DatatypeSimple(namespace, name)
        if (children[1].isEmptyMatch.not()) {
            children[1].branchNonSkipChildren[0].branchNonSkipChildren[0].branchNonSkipChildren.forEach {
                val st = super.transform<TypeReference>(it, result)
                result.addSuperType(st)
            }
        }
        children[2].branchNonSkipChildren.forEach {
            val p = super.transform<DatatypeProperty>(it, result)
            result.addProperty(p)
        }
        return result
    }

    // property = characteristic NAME : typeReference ;
    fun property(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): DatatypeProperty {
        val datatype = arg as Datatype
        val char:DatatypePropertyCharacteristic = super.transform(children[0],arg)
        val name = children[1].nonSkipMatchedText
        val typeReference:TypeReference = super.transform(children[2], arg)

        val result = DatatypePropertySimple(datatype, name, typeReference)
        when (char) {
            DatatypePropertyCharacteristic.`val` -> {
                result.isReference = true
                result.setIdentityIndex(datatype.identityProperties.size)
            }
            DatatypePropertyCharacteristic.`var` -> result.isReference = true
            DatatypePropertyCharacteristic.cal -> {
                result.isComposite = true
                result.setIdentityIndex(datatype.identityProperties.size)
            }
            DatatypePropertyCharacteristic.car -> result.isComposite = true
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
    fun characteristic(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): DatatypePropertyCharacteristic {
        return DatatypePropertyCharacteristic.valueOf(target.nonSkipMatchedText)
    }

    // typeReference = path typeArgumentList? ;
    fun typeReference(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): TypeReference {
        val path = children[0]
        val typeArgumentList = children[1]
        val typePath:List<String> = super.transform(path, arg)
        val typeArgs = if (typeArgumentList.isEmptyMatch) {
            emptyList<TypeReference>()
        } else {
            super.transform(typeArgumentList.branchNonSkipChildren[0], arg)
        }
        val dt = arg as Datatype
        return TypeReferenceSimple({ tref->dt.namespace.model.resolve(tref)}, typePath, typeArgs)
    }

    //typeArgumentList = '<' [ typeReference / ',']+ '>' ;
    fun typeArgumentList(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): List<TypeReference> {
        val list = children[0]
        return if (list.isEmptyMatch) {
            emptyList()
        } else {
            val dt = arg as Datatype
            val elems = list.branchNonSkipChildren
            elems.map {
                transform<TypeReference>(it, arg)
            }
        }
    }

}