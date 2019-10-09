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

package net.akehurst.kotlin.komposite.api

import kotlin.reflect.KClass

class KompositeException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}

interface DatatypeModel {
    val namespaces: List<Namespace>
    fun resolve(typeReference: TypeReference) : TypeDeclaration
}

interface Namespace {
    var model: DatatypeModel
    val path: List<String>
    val declaration: Map<String, TypeDeclaration>
    fun qualifiedName(separator: String): String
}

interface TypeDeclaration {
    val namespace: Namespace
    val name: String
    val isPrimitive : Boolean
    val isCollection : Boolean

    fun qualifiedName(separator: String): String
}

interface PrimitiveType : TypeDeclaration {
}

interface CollectionType : TypeDeclaration {
    val isArray: Boolean
    val isList: Boolean
    val isSet: Boolean
    val isMap: Boolean

    val parameters : List<String>
}

interface Datatype : TypeDeclaration {

    val clazz: KClass<*>

    val superTypes: List<Datatype>

    val property: Map<String, DatatypeProperty>

    /**
     * properties that are marked as 'identity'
     */
    val identityProperties: List<DatatypeProperty>

    /**
     * properties that are not marked as 'identity', but are defined explicitly
     */
    val explicitNonIdentityProperties: Set<DatatypeProperty>

    /**
     * properties that are marked as 'composite' (composite has to be explicitly defined)
     */
    val compositeProperties: Set<DatatypeProperty>

    /**
     * properties that are explicitly marked as 'reference'
     */
    val explicitReferenceProperties: Set<DatatypeProperty>

    /**
     * all properties (from this and its supertypes) that are explicitly defined
     */
    val allExplicitProperty: Map<String, DatatypeProperty>

    /**
     * properties that are explicitly marked as 'ignore'
     */
    val ignoredProperties: Set<DatatypeProperty>

    /**
     * all properties found on the object minus (excluding) those marked as identity or ignore
     * <p> (required because Kotlin-Javascript reflection not yet supported)
     */
    fun objectNonIdentityProperties(obj:Any): Set<DatatypeProperty>

    /**
     * all mutable properties found on the object minus (excluding) those marked as identity or ignore
     * <p> (required because Kotlin-Javascript reflection not yet supported)
     */
    fun objectNonIdentityMutableProperties(obj:Any): Set<DatatypeProperty>

    /**
     * all properties found on the object minus (excluding) those marked as composite or ignore
     * <p> (required because Kotlin-Javascript reflection not yet supported)
     */
    fun objectReferenceProperties(obj:Any): Set<DatatypeProperty>
}

interface DatatypeProperty {
    val datatype: Datatype
    val name: String
    val typeReference: TypeReference
    val propertyType: TypeInstance
    val isIdentity: Boolean
    val identityIndex: Int
    val isComposite: Boolean
    val isReference: Boolean
    val isMutable: Boolean
    val ignore: Boolean
}

enum class DatatypePropertyCharacteristic {
    `val`, // reference, constructor argument
    `var`, // reference mutable property
    cal,   // composite, constructor argument
    car,   // composite mutable property
    dis    // disregard / ignore
}

interface TypeReference {
    val typePath:List<String>
    val typeArguments: List<TypeReference>
    val type : TypeInstance
}

interface TypeInstance {
    val declaration: TypeDeclaration
    val arguments: List<TypeInstance>
}

class PrimitiveMapper(
        val primitiveKlass: KClass<*>,
        val rawKlass: KClass<*>,
        val toRaw: (Any) -> Any,
        val fromRaw: (Any) -> Any
) {
    companion object {
        fun <P : Any, R : Any> create(
                primitiveKlass: KClass<P>,
                rawKlass: KClass<R>,
                toRaw: (P) -> R,
                fromRaw: (R) -> P): PrimitiveMapper {
            return PrimitiveMapper(primitiveKlass, rawKlass, toRaw as (Any) -> Any, fromRaw as (Any) -> Any)

        }
    }
}