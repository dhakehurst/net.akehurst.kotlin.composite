package net.akehurst.kotlin.komposite.processor

import net.akehurst.kotlin.komposite.api.*

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

    private val _declaration = mutableMapOf<String, Declaration>()

    override val declaration:Map<String,Declaration> = _declaration

    fun addDeclaration(value: Declaration) {
        this._declaration[value.name] = value
    }

}

data class PrimitiveSimple(
        override val namespace: Namespace,
        override val name: String
) : Primitive

data class DatatypeSimple(
        override val namespace: Namespace,
        override val name: String
) : Datatype {

    private val _superTypes = mutableListOf<Datatype>()
    private val _property = mutableMapOf<String, DatatypeProperty>()

    override val superTypes: List<Datatype> = _superTypes

    override val property: Map<String, DatatypeProperty> = _property

    override val identityProperties: List<DatatypeProperty>
        get() {
            return property.values.filter { it.isIdentity }
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

}

data class DatatypePropertySimple(
        override val datatype: Datatype,
        override val name: String
) : DatatypeProperty {
    private var _isComposite = false
    private var _identityIndex = -1

    override val isIdentity: Boolean get() = -1 == _identityIndex
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

}