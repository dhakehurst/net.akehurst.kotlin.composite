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
import net.akehurst.kotlinx.reflect.ModuleRegistry
import net.akehurst.kotlinx.reflect.reflect
import kotlin.reflect.KClass

class DatatypeModelSimple : DatatypeModel {
    private val _namespaces = mutableListOf<Namespace>()

    override val namespaces: List<Namespace> = _namespaces

    fun addNamespace(value: Namespace) {
        this._namespaces += value
    }

}

data class NamespaceSimple(
        override val path: List<String>
) : Namespace {

    private val _declaration = mutableMapOf<String, TypeDeclaration>()

    override val declaration: Map<String, TypeDeclaration> = _declaration

    fun addDeclaration(value: TypeDeclaration) {
        this._declaration[value.name] = value
    }

    override fun qualifiedName(separator: String): String {
        return this.path.joinToString(separator)
    }
}

data class PrimitiveTypeSimple(
        override val namespace: Namespace,
        override val name: String
) : PrimitiveType {
    override fun qualifiedName(separator: String): String {
        return this.namespace.qualifiedName(separator) + separator + this.name
    }
}

data class CollectionTypeSimple(
        override val namespace: Namespace,
        override val name: String
) : CollectionType {

    companion object {
        //TODO: 'close' this set of instances
        val ARRAY_TYPE = CollectionTypeSimple(NamespaceSimple(listOf("kotlin", "collections")), "Array")
        val LIST_TYPE = CollectionTypeSimple(NamespaceSimple(listOf("kotlin", "collections")), "List")
        val SET_TYPE = CollectionTypeSimple(NamespaceSimple(listOf("kotlin", "collections")), "Set")
        val MAP_TYPE = CollectionTypeSimple(NamespaceSimple(listOf("kotlin", "collections")), "Map")
    }

    override val isArray = this== ARRAY_TYPE
    override val isList = this== LIST_TYPE
    override val isSet = this== SET_TYPE
    override val isMap = this== MAP_TYPE

    override fun qualifiedName(separator: String): String {
        return this.namespace.qualifiedName(separator) + separator + this.name
    }
}

data class DatatypeSimple(
        override val namespace: Namespace,
        override val name: String
) : Datatype {

    private val _superTypes = mutableListOf<Datatype>()
    private val _property = mutableMapOf<String, DatatypeProperty>()

    override val clazz: KClass<*>
        get() = ModuleRegistry.classForName(this.qualifiedName("."))

    override val superTypes: List<Datatype> = _superTypes

    override val property: Map<String, DatatypeProperty> = _property

    override val identityProperties: List<DatatypeProperty>
        get() {
            return property.values.filter { it.isIdentity }.sortedBy { it.identityIndex }
        }
    override val nonIdentityProperties: Set<DatatypeProperty>
        get() {
            return property.values.filter { it.isIdentity.not() }.toSet()
        }
    override val compositeProperties: Set<DatatypeProperty>
        get() {
            return property.values.filter { it.isComposite }.toSet()
        }

    override val referenceProperties: Set<DatatypeProperty>
        get() {
            return property.values.filter { it.isReference }.toSet()
        }


    override val allProperty: Map<String, DatatypeProperty>
        get() {
            return superTypes.flatMap { it.allProperty.values }.associate { Pair(it.name, it) } + this.property
        }

    fun addSuperType(value: Datatype) {
        _superTypes += value
    }

    fun addProperty(value: DatatypeProperty) {
        _property[value.name] = value
    }

    override fun qualifiedName(separator: String): String {
        return this.namespace.qualifiedName(separator) + separator + this.name
    }
}

data class DatatypePropertySimple(
        override val datatype: Datatype,
        override val name: String
) : DatatypeProperty {
    private var _isComposite = false
    private var _identityIndex = -1

    override val isIdentity: Boolean get() = -1 != _identityIndex
    override val identityIndex: Int get() = _identityIndex

    override var isComposite: Boolean
        get() = _isComposite
        set(value) {
            this._isComposite = value

        }
    override var isReference: Boolean
        get() = this.isComposite.not()
        set(value) {
            this._isComposite = value.not()
        }

    override var ignore: Boolean = false

    fun setIdentityIndex(value: Int) {
        this._identityIndex = value
    }

    val type: TypeDeclaration get() {
        TODO("needs kotlin JS reflection")
    }
}