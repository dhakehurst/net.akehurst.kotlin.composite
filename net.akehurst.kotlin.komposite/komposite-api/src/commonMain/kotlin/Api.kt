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
    val declaration: Map<String, Declaration>
    fun qualifiedName(separator: String): String
}

interface Declaration {
    val namespace: Namespace
    val name: String
}

interface PrimitiveType : Declaration {

}

interface CollectionType : Declaration {

}

interface Datatype : Declaration {

    val clazz: KClass<*>

    val superTypes: List<Datatype>

    val property: Map<String, DatatypeProperty>

    val identityProperties: List<DatatypeProperty>

    val nonIdentityProperties: Set<DatatypeProperty>

    val compositeProperties: Set<DatatypeProperty>

    val referenceProperties: Set<DatatypeProperty>

    val allProperty: Map<String, DatatypeProperty>

    fun qualifiedName(separator: String): String
}

interface DatatypeProperty {
    val datatype: Datatype
    val name: String
    val isIdentity: Boolean
    val identityIndex: Int
    val isComposite: Boolean
    val isReference: Boolean
    val ignore: Boolean
}