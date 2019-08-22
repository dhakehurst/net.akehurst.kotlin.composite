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
}

interface Namespace {
    val path: List<String>
    val declaration: Map<String, TypeDeclaration>
    fun qualifiedName(separator: String): String
}

interface TypeDeclaration {
    val namespace: Namespace
    val name: String
}

interface PrimitiveType : TypeDeclaration {
    fun qualifiedName(separator: String): String
}

interface CollectionType : TypeDeclaration {
    val isArray: Boolean
    val isList: Boolean
    val isSet: Boolean
    val isMap: Boolean
    fun qualifiedName(separator: String): String
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

    fun qualifiedName(separator: String): String

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
    val isIdentity: Boolean
    val identityIndex: Int
    val isComposite: Boolean
    val isReference: Boolean
    val isMutable: Boolean
    val ignore: Boolean
}