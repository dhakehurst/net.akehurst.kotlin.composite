/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

@DslMarker
annotation class KompositeDslMarker

fun komposite(init: KompositeBuilder.() -> Unit): DatatypeModel {
    val b = KompositeBuilder()
    b.init()
    return b.build()
}

@KompositeDslMarker
class KompositeBuilder {
    val model = DatatypeModelSimple()
    fun namespace(qualifiedName: String, init: KompositeNamespaceBuilder.() -> Unit): Namespace {
        val b = KompositeNamespaceBuilder(model, qualifiedName)
        b.init()
        return b.build()
    }

    fun build(): DatatypeModel {
        return model
    }
}

@KompositeDslMarker
class KompositeNamespaceBuilder(
    private val model: DatatypeModelSimple,
    private val qualifiedName: String
) {
    private val namespace = NamespaceSimple(model, qualifiedName)

    fun enumType(name: String): EnumType {
        val et = EnumTypeSimple(namespace, name)
        this.namespace.addDeclaration(et)
        return et
    }

    fun primitiveType(name: String): PrimitiveType {
        val pt = PrimitiveTypeSimple(namespace, name)
        this.namespace.addDeclaration(pt)
        return pt
    }

    fun collectionType(name: String, typeParameterNames: List<String>): CollectionType {
        val ct = CollectionTypeSimple(namespace, name, typeParameterNames)
        this.namespace.addDeclaration(ct)
        return ct
    }

    fun dataType(name: String, init: KompositeDatatypeBuilder.() -> Unit): Datatype {
        val b = KompositeDatatypeBuilder(namespace, name)
        b.init()
        return b.build()
    }

    fun build(): Namespace {
        model.addNamespace(namespace)
        return namespace
    }
}

@KompositeDslMarker
class KompositeDatatypeBuilder(
    private val namespace: NamespaceSimple,
    private val name: String
) {

    private val datatype = DatatypeSimple(namespace, name)

    fun superTypes(vararg typeNames:String) {
        typeNames.forEach {
            val typePath = it.split(".").toList()
            val superTypeRef = TypeReferenceSimple({ tref -> datatype.namespace.model.resolve(tref) }, typePath, emptyList())
            datatype.addSuperType(superTypeRef)
        }
    }

    fun constructorArguments(init: KompositePropertySetBuilder.() -> Unit) {
        val b = KompositePropertySetBuilder(datatype, true)
        b.init()
        b.build()
    }

    fun mutableProperties(init: KompositePropertySetBuilder.() -> Unit) {
        val b = KompositePropertySetBuilder(datatype, false)
        b.init()
        b.build()
    }

    fun build(): Datatype {
        namespace.addDeclaration(datatype)
        return datatype
    }
}

@KompositeDslMarker
class KompositePropertySetBuilder(
    private val datatype: DatatypeSimple,
    private val isConstructorArgs:Boolean
) {
    private val props = mutableSetOf<DatatypeProperty>()
    fun composite(name: String, qualifiedTypeName: String, nullable:Boolean=false, typeArguments: KompositeTypeArgumentBuilder.() -> Unit={}): DatatypeProperty {
        val typePath = qualifiedTypeName.split(".").toList()
        val tab = KompositeTypeArgumentBuilder(datatype.namespace.model)
        tab.typeArguments()
        val typeArgs = tab.build()
        val typeReference = TypeReferenceSimple({ tref -> datatype.namespace.model.resolve(tref) }, typePath, typeArgs)
        val prop = DatatypePropertySimple(datatype, name, typeReference)
        prop.isComposite = true
        if (isConstructorArgs) prop.setIdentityIndex(props.size)
        props.add(prop)
        return prop
    }

    fun reference(name: String, qualifiedTypeName: String, nullable:Boolean=false, typeArguments: KompositeTypeArgumentBuilder.() -> Unit={}): DatatypeProperty {
        val typePath = qualifiedTypeName.split(".").toList()
        val tab = KompositeTypeArgumentBuilder(datatype.namespace.model)
        tab.typeArguments()
        val typeArgs = tab.build()
        val typeReference = TypeReferenceSimple({ tref -> datatype.namespace.model.resolve(tref) }, typePath, typeArgs)
        val prop = DatatypePropertySimple(datatype, name, typeReference)
        prop.isReference = true
        if (isConstructorArgs) prop.setIdentityIndex(props.size)
        props.add(prop)
        return prop
    }

    fun build(): Set<DatatypeProperty> {
        props.forEach { this.datatype.addProperty(it) }
        return props
    }
}

@KompositeDslMarker
class KompositeTypeArgumentBuilder(
    private val model: DatatypeModel
) {
    private val list = mutableListOf<TypeReference>()
    fun typeArgument(qualifiedTypeName:String,typeArguments: KompositeTypeArgumentBuilder.() -> Unit={}) : TypeReference {
        val typePath = qualifiedTypeName.split(".").toList()
        val tab = KompositeTypeArgumentBuilder(model)
        tab.typeArguments()
        val typeArgs = tab.build()
        val tref = TypeReferenceSimple({model.resolve(it)},typePath, typeArgs)
        list.add(tref)
        return tref
    }

    fun build(): List<TypeReference> {
        return list
    }
}