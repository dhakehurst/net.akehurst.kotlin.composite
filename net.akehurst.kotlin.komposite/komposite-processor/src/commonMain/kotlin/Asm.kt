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
import net.akehurst.kotlinx.reflect.KotlinxReflect
import net.akehurst.kotlinx.reflect.reflect
import kotlin.reflect.KClass

typealias TypeResolver = (TypeReference) -> TypeDeclaration

class DatatypeModelSimple : DatatypeModel {

    companion object {
        //TODO: 'close' this set of instances
        fun ANY_TYPE_REF(resolver: TypeResolver) = TypeReferenceSimple(resolver, listOf("kotlin", "Any"), emptyList())

        fun ARRAY_TYPE(model: DatatypeModel) = CollectionTypeSimple(NamespaceSimple(model, ("kotlin.collections")), "Array", listOf("E"))
        fun LIST_TYPE(model: DatatypeModel) = CollectionTypeSimple(NamespaceSimple(model, ("kotlin.collections")), "List", listOf("E"))
        fun SET_TYPE(model: DatatypeModel) = CollectionTypeSimple(NamespaceSimple(model, ("kotlin.collections")), "Set", listOf("E"))
        fun MAP_TYPE(model: DatatypeModel) = CollectionTypeSimple(NamespaceSimple(model, ("kotlin.collections")), "Map", listOf("K", "V"))
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
    override val qualifiedName: String
) : Namespace {

    private val _declaration = mutableMapOf<String, TypeDeclaration>()

    override val path: List<String> by lazy { qualifiedName.split(".").toList() }
    override val declaration: Map<String, TypeDeclaration> = _declaration

    fun addDeclaration(value: TypeDeclaration) {
        this._declaration[value.name] = value
    }

    override fun qualifiedNameBy(separator: String): String {
        return this.path.joinToString(separator)
    }

    override fun hashCode(): Int {
        return this.path.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
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
    override val isPrimitive: Boolean get() = true
    override val isEnum: Boolean get() = false
    override val isCollection: Boolean get() = false
    override val isAny: Boolean get() = false
    override val qualifiedName: String = "${namespace.qualifiedName}.$name"
    override fun qualifiedNameBy(separator: String): String {
        return this.namespace.qualifiedNameBy(separator) + separator + this.name
    }

    override fun instance(arguments: List<TypeInstance>): TypeInstance = TypeInstanceSimple(this, emptyList<TypeInstance>())
}

data class EnumTypeSimple(
    override val namespace: Namespace,
    override val name: String
) : EnumType {
    override val isPrimitive: Boolean get() = false
    override val isEnum: Boolean get() = true
    override val isCollection: Boolean get() = false
    override val isAny: Boolean get() = false
    override val qualifiedName: String = "${namespace.qualifiedName}.$name"
    override fun qualifiedNameBy(separator: String): String {
        return this.namespace.qualifiedNameBy(separator) + separator + this.name
    }

    override fun instance(arguments: List<TypeInstance>): TypeInstance = TypeInstanceSimple(this, emptyList<TypeInstance>())

    override val clazz: KClass<Enum<*>> by lazy { KotlinxReflect.classForName(qualifiedName) as KClass<Enum<*>> }
}

data class CollectionTypeSimple(
    override val namespace: Namespace,
    override val name: String,
    override val parameters: List<String>
) : CollectionType {
    override val isPrimitive: Boolean get() = false
    override val isEnum: Boolean get() = false
    override val isCollection: Boolean get() = true
    override val isAny: Boolean get() = false
    override val isArray get() = this == DatatypeModelSimple.ARRAY_TYPE(this.namespace.model)
    override val isList get() = this == DatatypeModelSimple.LIST_TYPE(this.namespace.model)
    override val isSet get() = this == DatatypeModelSimple.SET_TYPE(this.namespace.model)
    override val isMap get() = this == DatatypeModelSimple.MAP_TYPE(this.namespace.model)
    override val qualifiedName: String = "${namespace.qualifiedName}.$name"
    override fun qualifiedNameBy(separator: String): String {
        return this.namespace.qualifiedNameBy(separator) + separator + this.name
    }

    override fun instance(arguments: List<TypeInstance>): TypeInstance = TypeInstanceSimple(this, arguments)
}

//data class TypeParameter(
//    val name: String
//)

data class DatatypeSimple(
    override val namespace: Namespace,
    override val name: String
) : Datatype {

    private val _typeParameters = mutableListOf<String>()
    private val _superTypes = mutableListOf<TypeReference>()
    private val _property = mutableMapOf<String, DatatypeProperty>()

    internal val resolver: TypeResolver = { tref ->
        if (1 == tref.typePath.size && _typeParameters.contains(tref.typePath[0])) {
            DatatypeModelSimple.ANY_TYPE_REF(this.namespace.model::resolve).type.declaration
        } else {
            this.namespace.model.resolve(tref)
        }
    }

    override val isPrimitive: Boolean get() = false
    override val isEnum: Boolean get() = false
    override val isCollection: Boolean get() = false
    override val isAny: Boolean get() = "kotlin.Any" == this.qualifiedName
    override val qualifiedName: String = "${namespace.qualifiedName}.$name"

    override val clazz: KClass<*> by lazy { KotlinxReflect.classForName(qualifiedName) }

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

    fun addTypeParameter(value: String) {
        this._typeParameters.add(value)
    }

    fun addProperty(value: DatatypeProperty) {
        _property[value.name] = value
    }

    override fun qualifiedNameBy(separator: String): String {
        return this.namespace.qualifiedNameBy(separator) + separator + this.name
    }

    override fun instance(arguments: List<TypeInstance>): TypeInstance = TypeInstanceSimple(this, arguments)

    override fun objectNonIdentityProperties(obj: Any): Set<DatatypeProperty> {
        val objProperties: Set<DatatypeProperty> = obj.reflect().allPropertyNames.map {
            if (this.property.containsKey(it)) {
                this.property[it]!!
            } else {
                DatatypePropertySimple(this, it, DatatypeModelSimple.ANY_TYPE_REF { tref -> this.namespace.model.resolve(tref) })
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
                DatatypePropertySimple(this, it, DatatypeModelSimple.ANY_TYPE_REF { tref -> this.namespace.model.resolve(tref) })
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

    override val propertyType: TypeInstance get() = typeReference.type
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

    override val isMutable: Boolean get() = isIdentity.not() && propertyType.declaration.isCollection.not() //get() = this.datatype.clazz.reflect().isPropertyMutable(this.name)

    override var ignore: Boolean = false

    fun setIdentityIndex(value: Int) {
        this._identityIndex = value
    }


}

data class TypeReferenceSimple(
    val resolver: TypeResolver,
    override val typePath: List<String>,
    override val typeArguments: List<TypeReference>
) : TypeReference {

    private fun resolve(ref: TypeReference): TypeInstance {
        val decl = this.resolver(ref)
        val args = ref.typeArguments.map {
            resolve(it)
        }
        return TypeInstanceSimple(decl, args)
    }

    override val type: TypeInstance get() = resolve(this)

}

data class TypeInstanceSimple(
    override val declaration: TypeDeclaration,
    override val arguments: List<TypeInstance>
) : TypeInstance {
}