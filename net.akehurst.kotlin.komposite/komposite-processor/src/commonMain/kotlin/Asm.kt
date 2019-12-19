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

    companion object {
        //TODO: 'close' this set of instances
        fun ANY_TYPE_REF(resolver: (TypeReference)->TypeDeclaration) = TypeReferenceSimple(resolver, listOf("kotlin", "Any"), emptyList())

        fun ARRAY_TYPE(model: DatatypeModel) = CollectionTypeSimple(NamespaceSimple(model, listOf("kotlin", "collections")), "Array", listOf("T"))
        fun LIST_TYPE(model: DatatypeModel) = CollectionTypeSimple(NamespaceSimple(model, listOf("kotlin", "collections")), "List", listOf("T"))
        fun SET_TYPE(model: DatatypeModel) = CollectionTypeSimple(NamespaceSimple(model, listOf("kotlin", "collections")), "Set", listOf("T"))
        fun MAP_TYPE(model: DatatypeModel) = CollectionTypeSimple(NamespaceSimple(model, listOf("kotlin", "collections")), "Map", listOf("K", "V"))
    }

    private val _namespaces = mutableListOf<Namespace>()

    override val namespaces: List<Namespace> = _namespaces

    fun addNamespace(value: Namespace) {
        this._namespaces += value
    }

    fun findFirstByName(typeName: String): TypeDeclaration {
        return this.namespaces.mapNotNull {
            it.declaration[typeName]
        }.firstOrNull() ?: throw KompositeException("TypeDeclaration $typeName not found in any namespace")
    }

    override fun resolve(typeReference: TypeReference): TypeDeclaration {
        val nsPath = typeReference.typePath.dropLast(1)
        val typeName = typeReference.typePath.last()
        return if (nsPath.isEmpty()) {
            this.findFirstByName(typeName)
        } else {
            val ns = this.namespaces.firstOrNull { it.path == nsPath } ?: throw KompositeException("Namespace $nsPath not found")
            ns.declaration[typeName] ?: throw KompositeException("TypeDeclaration $typeName not found in namespace $nsPath")
        }
    }
}

class NamespaceSimple(
        override var model: DatatypeModel,
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

    override fun hashCode(): Int {
        return this.path.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return when(other) {
            is Namespace -> this.path == other.path
            else -> false
        }
    }

    override fun toString(): String {
        return "Namespace{$path}"
    }
}

data class PrimitiveTypeSimple(
        override val namespace: Namespace,
        override val name: String
) : PrimitiveType {
    override val isPrimitive: Boolean = true
    override val isCollection: Boolean = false
    override fun qualifiedName(separator: String): String {
        return this.namespace.qualifiedName(separator) + separator + this.name
    }
}

data class CollectionTypeSimple(
        override val namespace: Namespace,
        override val name: String,
        override val parameters: List<String>
) : CollectionType {

    override val isPrimitive: Boolean = false
    override val isCollection: Boolean = true
    override val isArray get() = this == DatatypeModelSimple.ARRAY_TYPE(this.namespace.model)
    override val isList get() = this == DatatypeModelSimple.LIST_TYPE(this.namespace.model)
    override val isSet get() = this == DatatypeModelSimple.SET_TYPE(this.namespace.model)
    override val isMap get() = this == DatatypeModelSimple.MAP_TYPE(this.namespace.model)

    override fun qualifiedName(separator: String): String {
        return this.namespace.qualifiedName(separator) + separator + this.name
    }
}

data class DatatypeSimple(
        override val namespace: Namespace,
        override val name: String
) : Datatype {

    private val _superTypes = mutableListOf<TypeReference>()
    private val _property = mutableMapOf<String, DatatypeProperty>()

    override val isPrimitive: Boolean = false
    override val isCollection: Boolean = false

    override val clazz: KClass<*>
        get() = ModuleRegistry.classForName(this.qualifiedName("."))

    override val superTypes: List<TypeReference> = _superTypes

    override val property: Map<String, DatatypeProperty> = _property

    override val identityProperties: List<DatatypeProperty>
        get() {
            return property.values.filter { it.isIdentity }.sortedBy { it.identityIndex }
        }
    override val explicitNonIdentityProperties: Set<DatatypeProperty>
        get() {
            return property.values.filter { it.isIdentity.not() }.toSet()
        }
    override val compositeProperties: Set<DatatypeProperty>
        get() {
            return property.values.filter { it.isComposite }.toSet()
        }

    override val explicitReferenceProperties: Set<DatatypeProperty>
        get() {
            return property.values.filter { it.isReference }.toSet()
        }

    override val allSuperTypes: List<TypeReference>
        get() {
            return superTypes + superTypes.flatMap {
                val decl = it.type.declaration
                if (decl is Datatype) {
                    decl.allSuperTypes
                } else {
                    emptyList()
                }
            }
        }

    override val allExplicitProperty: Map<String, DatatypeProperty>
        get() {
            return superTypes.flatMap {
                val decl = it.type.declaration
                if (decl is Datatype) {
                    decl.allExplicitProperty.values
                } else {
                    emptyList()
                }
            }.associate { Pair(it.name, it) } + this.property
        }

    override val allExplicitNonIdentityProperties: Set<DatatypeProperty>
        get() {
            return superTypes.flatMap {
                val decl = it.type.declaration
                if (decl is Datatype) {
                    decl.allExplicitNonIdentityProperties
                } else {
                    emptySet()
                }
            }.toSet() + this.explicitNonIdentityProperties
        }

    override val ignoredProperties: Set<DatatypeProperty>
        get() {
            return property.values.filter { it.ignore }.toSet()
        }

    fun addSuperType(value: TypeReference) {
        _superTypes += value
    }

    fun addProperty(value: DatatypeProperty) {
        _property[value.name] = value
    }

    override fun qualifiedName(separator: String): String {
        return this.namespace.qualifiedName(separator) + separator + this.name
    }

    override fun objectNonIdentityProperties(obj: Any): Set<DatatypeProperty> {
        val objProperties: Set<DatatypeProperty> = obj.reflect().allPropertyNames.map {
            if (this.property.containsKey(it)) {
                this.property[it]!!
            } else {
                DatatypePropertySimple(this, it, DatatypeModelSimple.ANY_TYPE_REF { tref->this.namespace.model.resolve(tref)})
            }
        }.toSet()
        return (property.values.toSet() + objProperties) - this.identityProperties - this.ignoredProperties
    }

    override fun objectNonIdentityMutableProperties(obj: Any): Set<DatatypeProperty> {
        val objProperties: Set<DatatypeProperty> = objectNonIdentityProperties(obj).filter { it.isMutable }.toSet()
        return (property.values.toSet() + objProperties) - this.identityProperties - this.ignoredProperties
    }

    override fun objectReferenceProperties(obj: Any): Set<DatatypeProperty> {
        val objProperties: Set<DatatypeProperty> = obj.reflect().allPropertyNames.map {
            if (this.property.containsKey(it)) {
                this.property[it]!!
            } else {
                DatatypePropertySimple(this, it, DatatypeModelSimple.ANY_TYPE_REF { tref->this.namespace.model.resolve(tref)})
            }
        }.toSet()
        return (property.values.toSet() + objProperties) - this.compositeProperties - this.ignoredProperties
    }
}

data class DatatypePropertySimple(
        override val datatype: Datatype,
        override val name: String,
        override val typeReference: TypeReference
) : DatatypeProperty {
    private var _isComposite = false
    private var _identityIndex = -1

    override val propertyType: TypeInstance
        get() {
            return typeReference.type
        }
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

    override val isMutable: Boolean get() = this.datatype.clazz.reflect().isPropertyMutable(this.name)

    override var ignore: Boolean = false

    fun setIdentityIndex(value: Int) {
        this._identityIndex = value
    }


}

data class TypeReferenceSimple(
        val resolver: (TypeReference)->TypeDeclaration,
        override val typePath: List<String>,
        override val typeArguments: List<TypeReference>
) : TypeReference {

    private fun resolve(ref:TypeReference) : TypeInstance {
        val decl = this.resolver(ref)
        val args = ref.typeArguments.map {
            resolve(it)
        }
        return TypeInstanceSimple(decl, args)
    }

    override val type: TypeInstance
        get() = resolve(this)

}

data class TypeInstanceSimple(
        override val declaration: TypeDeclaration,
        override val arguments: List<TypeInstance>
) : TypeInstance {
}