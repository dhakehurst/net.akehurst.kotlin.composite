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

package net.akehurst.kotlin.komposite.common

import net.akehurst.kotlin.komposite.api.CollectionType
import net.akehurst.kotlin.komposite.api.Datatype
import net.akehurst.kotlin.komposite.api.DatatypeProperty
import net.akehurst.kotlin.komposite.api.KompositeException
import net.akehurst.kotlin.komposite.processor.DatatypePropertySimple
import net.akehurst.kotlinx.collections.Stack
import net.akehurst.kotlinx.reflect.reflect
import kotlin.reflect.KClass

inline fun <P : Any?, A : Any?> kompositeWalker(registry: DatatypeRegistry, init: KompositeWalker.Builder<P, A>.() -> Unit): KompositeWalker<P, A> {
    val builder = KompositeWalker.Builder<P, A>()
    builder.init()
    return builder.build(registry)
}

data class WalkInfo<P, A>(
        val path: P,
        val acc: A
)

class KompositeWalker<P : Any?, A : Any?>(
        val registry: DatatypeRegistry,
        val objectBegin: (path: List<String>, info: WalkInfo<P, A>, obj: Any, datatype: Datatype) -> WalkInfo<P, A>,
        val objectEnd: (path: List<String>, info: WalkInfo<P, A>, obj: Any, datatype: Datatype) -> WalkInfo<P, A>,
        val propertyBegin: (path: List<String>, info: WalkInfo<P, A>, property: DatatypeProperty) -> WalkInfo<P, A>,
        val propertyEnd: (path: List<String>, info: WalkInfo<P, A>, property: DatatypeProperty) -> WalkInfo<P, A>,
        val mapBegin: (path: List<String>, info: WalkInfo<P, A>, map: Map<*, *>) -> WalkInfo<P, A>,
        val mapEntryKeyBegin: (path: List<String>, info: WalkInfo<P, A>, entry: Map.Entry<*, *>) -> WalkInfo<P, A>,
        val mapEntryKeyEnd: (path: List<String>, info: WalkInfo<P, A>, entry: Map.Entry<*, *>) -> WalkInfo<P, A>,
        val mapEntryValueBegin: (path: List<String>, info: WalkInfo<P, A>, entry: Map.Entry<*, *>) -> WalkInfo<P, A>,
        val mapEntryValueEnd: (path: List<String>, info: WalkInfo<P, A>, entry: Map.Entry<*, *>) -> WalkInfo<P, A>,
        val mapSeparate: (path: List<String>, info: WalkInfo<P, A>, map: Map<*, *>, previousEntry: Map.Entry<*, *>) -> WalkInfo<P, A>,
        val mapEnd: (path: List<String>, info: WalkInfo<P, A>, map: Map<*, *>) -> WalkInfo<P, A>,
        val collBegin: (path: List<String>, info: WalkInfo<P, A>, type: CollectionType, coll: Collection<*>) -> WalkInfo<P, A>,
        val collElementBegin: (path: List<String>, info: WalkInfo<P, A>, element: Any?) -> WalkInfo<P, A>,
        val collElementEnd: (path: List<String>, info: WalkInfo<P, A>, element: Any?) -> WalkInfo<P, A>,
        val collSeparate: (path: List<String>, info: WalkInfo<P, A>, type: CollectionType, coll: Collection<*>, previousElement: Any?) -> WalkInfo<P, A>,
        val collEnd: (path: List<String>, info: WalkInfo<P, A>, type:CollectionType, coll: Collection<*>) -> WalkInfo<P, A>,
        val reference: (path: List<String>, info: WalkInfo<P, A>, value: Any, property: DatatypeProperty) -> WalkInfo<P, A>,
        val primitive: (path: List<String>, info: WalkInfo<P, A>, value: Any) -> WalkInfo<P, A>,
        val nullValue: (path: List<String>, info: WalkInfo<P, A>) -> WalkInfo<P, A>
) {

    class Builder<P : Any?, A : Any?>() {
        private var _objectBegin: (path: List<String>, info: WalkInfo<P, A>, obj: Any, datatype: Datatype) -> WalkInfo<P, A> = { _, info, _, _ -> info }
        private var _objectEnd: (path: List<String>, info: WalkInfo<P, A>, obj: Any, datatype: Datatype) -> WalkInfo<P, A> = { _, info, _, _ -> info }
        private var _propertyBegin: (path: List<String>, info: WalkInfo<P, A>, property: DatatypeProperty) -> WalkInfo<P, A> = { _, info, _ -> info }
        private var _propertyEnd: (path: List<String>, info: WalkInfo<P, A>, property: DatatypeProperty) -> WalkInfo<P, A> = { _, info, _ -> info }
        private var _mapBegin: (path: List<String>, info: WalkInfo<P, A>, map: Map<*, *>) -> WalkInfo<P, A> = { _, info, _ -> info }
        private var _mapEntryKeyBegin: (path: List<String>, info: WalkInfo<P, A>, entry: Map.Entry<*, *>) -> WalkInfo<P, A> = { _, info, _ -> info }
        private var _mapEntryKeyEnd: (path: List<String>, info: WalkInfo<P, A>, entry: Map.Entry<*, *>) -> WalkInfo<P, A> = { _, info, _ -> info }
        private var _mapEntryValueBegin: (path: List<String>, info: WalkInfo<P, A>, entry: Map.Entry<*, *>) -> WalkInfo<P, A> = { _, info, _ -> info }
        private var _mapEntryValueEnd: (path: List<String>, info: WalkInfo<P, A>, entry: Map.Entry<*, *>) -> WalkInfo<P, A> = { _, info, _ -> info }
        private var _mapSeparate: (path: List<String>, info: WalkInfo<P, A>, map: Map<*, *>, previousEntry: Map.Entry<*, *>) -> WalkInfo<P, A> = { _, info, _, _ -> info }
        private var _mapEnd: (path: List<String>, info: WalkInfo<P, A>, map: Map<*, *>) -> WalkInfo<P, A> = { _, info, _ -> info }
        private var _collBegin: (path: List<String>, info: WalkInfo<P, A>, type: CollectionType, coll: Collection<*>) -> WalkInfo<P, A> = { _, info, _, _ -> info }
        private var _collElementBegin: (path: List<String>, info: WalkInfo<P, A>, element: Any?) -> WalkInfo<P, A> = { _, info, _ -> info }
        private var _collElementEnd: (path: List<String>, info: WalkInfo<P, A>, element: Any?) -> WalkInfo<P, A> = { _, info, _ -> info }
        private var _collSeparate: (path: List<String>, info: WalkInfo<P, A>, type: CollectionType, coll: Collection<*>, previousElement: Any?) -> WalkInfo<P, A> = { _, info, _, _, _ -> info }
        private var _collEnd: (path: List<String>, info: WalkInfo<P, A>, type: CollectionType, coll: Collection<*>) -> WalkInfo<P, A> = { _, info, _, _ -> info }
        private var _reference: (path: List<String>, info: WalkInfo<P, A>, value: Any, property: DatatypeProperty) -> WalkInfo<P, A> = { _, info, _, _ -> info }
        private var _primitive: (path: List<String>, info: WalkInfo<P, A>, value: Any) -> WalkInfo<P, A> = { _, info, _ -> info }
        private var _nullValue: (path: List<String>, info: WalkInfo<P, A>) -> WalkInfo<P, A> = { _, info -> info }

        fun objectBegin(func: (path: List<String>, info: WalkInfo<P, A>, obj: Any, datatype: Datatype) -> WalkInfo<P, A>) {
            this._objectBegin = func
        }

        fun objectEnd(func: (path: List<String>, info: WalkInfo<P, A>, obj: Any, datatype: Datatype) -> WalkInfo<P, A>) {
            this._objectEnd = func
        }

        fun propertyBegin(func: (path: List<String>, info: WalkInfo<P, A>, property: DatatypeProperty) -> WalkInfo<P, A>) {
            this._propertyBegin = func
        }

        fun propertyEnd(func: (path: List<String>, info: WalkInfo<P, A>, property: DatatypeProperty) -> WalkInfo<P, A>) {
            this._propertyEnd = func
        }

        fun mapBegin(func: (path: List<String>, info: WalkInfo<P, A>, map: Map<*, *>) -> WalkInfo<P, A>) {
            this._mapBegin = func
        }

        fun mapEntryKeyBegin(func: (path: List<String>, info: WalkInfo<P, A>, entry: Map.Entry<*, *>) -> WalkInfo<P, A>) {
            this._mapEntryKeyBegin = func
        }

        fun mapEntryKeyEnd(func: (path: List<String>, info: WalkInfo<P, A>, entry: Map.Entry<*, *>) -> WalkInfo<P, A>) {
            this._mapEntryKeyEnd = func
        }

        fun mapEntryValueBegin(func: (path: List<String>, info: WalkInfo<P, A>, entry: Map.Entry<*, *>) -> WalkInfo<P, A>) {
            this._mapEntryValueBegin = func
        }

        fun mapEntryValueEnd(func: (path: List<String>, info: WalkInfo<P, A>, entry: Map.Entry<*, *>) -> WalkInfo<P, A>) {
            this._mapEntryValueEnd = func
        }

        fun mapSeparate(func: (path: List<String>, info: WalkInfo<P, A>, map: Map<*, *>, previousEntry: Map.Entry<*, *>) -> WalkInfo<P, A>) {
            this._mapSeparate = func
        }

        fun mapEnd(func: (path: List<String>, info: WalkInfo<P, A>, map: Map<*, *>) -> WalkInfo<P, A>) {
            this._mapEnd = func
        }

        fun collBegin(func: (path: List<String>, info: WalkInfo<P, A>, type:CollectionType, coll: Collection<*>) -> WalkInfo<P, A>) {
            this._collBegin = func
        }

        fun collElementBegin(func: (path: List<String>, info: WalkInfo<P, A>, element: Any?) -> WalkInfo<P, A>) {
            this._collElementBegin = func
        }

        fun collElementEnd(func: (path: List<String>, info: WalkInfo<P, A>, element: Any?) -> WalkInfo<P, A>) {
            this._collElementEnd = func
        }

        fun collSeparate(func: (path: List<String>, info: WalkInfo<P, A>, type:CollectionType, coll: Collection<*>, previousElement: Any?) -> WalkInfo<P, A>) {
            this._collSeparate = func
        }

        fun collEnd(func: (path: List<String>, info: WalkInfo<P, A>, type:CollectionType, coll: Collection<*>) -> WalkInfo<P, A>) {
            this._collEnd = func
        }

        fun reference(func: (path: List<String>, info: WalkInfo<P, A>, value: Any, property: DatatypeProperty) -> WalkInfo<P, A>) {
            this._reference = func
        }

        fun primitive(func: (path: List<String>, info: WalkInfo<P, A>, value: Any) -> WalkInfo<P, A>) {
            this._primitive = func
        }

        fun nullValue(func: (path: List<String>, info: WalkInfo<P, A>) -> WalkInfo<P, A>) {
            this._nullValue = func
        }

        fun build(registry: DatatypeRegistry): KompositeWalker<P, A> {
            return KompositeWalker(
                    registry,
                    _objectBegin, _objectEnd,
                    _propertyBegin, _propertyEnd,
                    _mapBegin, _mapEntryKeyBegin, _mapEntryKeyEnd, _mapEntryValueBegin, _mapEntryValueEnd, _mapSeparate, _mapEnd,
                    _collBegin, _collElementBegin, _collElementEnd, _collSeparate, _collEnd,
                    _reference, _primitive, _nullValue
            )
        }
    }

    fun walk(info: WalkInfo<P, A>, data: Any?): WalkInfo<P, A> {
        val path = Stack<String>()
        return walkValue(null, path, info, data)
    }

    protected fun walkValue(owningProperty: DatatypeProperty?, path: Stack<String>, info: WalkInfo<P, A>, data: Any?): WalkInfo<P, A> {
        return when {
            null == data -> walkNull(path, info)
            registry.isPrimitive(data) -> walkPrimitive(path, info, data)
            registry.isCollection(data) -> walkCollection(owningProperty, path, info, data)
            registry.hasDatatypeInfo(data) -> walkObject(owningProperty, path, info, data)
            else -> throw KompositeException("Don't know how to walk object of class: ${data::class}")
        }
    }

    protected fun walkPropertyValue(owningProperty: DatatypeProperty, path: Stack<String>, info: WalkInfo<P, A>, propValue: Any?): WalkInfo<P, A> {
        return when {
            null == propValue -> walkNull(path, info)
            registry.isPrimitive(propValue) -> walkPrimitive(path, info, propValue)
            owningProperty.isComposite -> {
                path.push(owningProperty.name)
                val wi = walkValue(owningProperty, path, info, propValue)
                path.pop()
                wi
            }
            owningProperty.isReference -> walkReference(owningProperty, path, info, propValue)
            else -> throw KompositeException("Don't know how to walk property $owningProperty = $propValue")
        }
    }

    protected fun walkObject(owningProperty: DatatypeProperty?, path: Stack<String>, info: WalkInfo<P, A>, obj: Any): WalkInfo<P, A> {
        //TODO: use qualified name when we can
        val cls = obj::class
        val dt = registry.findDatatypeByName(cls.simpleName!!)
        if (null == dt) {
            throw KompositeException("Cannot find datatype for ${cls}, is it in the datatype configuration")
        } else {
            val infoob = this.objectBegin(path.elements, info, obj, dt)
            var acc = infoob.acc

            cls.reflect().allPropertyNames(obj).forEach { propName ->
                val prop = dt.allProperty[propName] ?: DatatypePropertySimple(dt, propName) //default is a reference property
                if (prop.ignore) {
                    //TODO: log!
                } else {
                    val propValue = prop.get(obj)
                    path.push(propName)
                    val infopb = this.propertyBegin(path.elements, WalkInfo(infoob.path, acc), prop)
                    val infowp = this.walkPropertyValue(prop, path, WalkInfo(infoob.path, infopb.acc), propValue)
                    val infope = this.propertyEnd(path.elements, WalkInfo(infoob.path, infowp.acc), prop)
                    path.pop()
                    acc = infope.acc
                }
            }
            return this.objectEnd(path.elements, WalkInfo(infoob.path, acc), obj, dt)
        }
    }

    protected fun walkCollection(owningProperty: DatatypeProperty?, path: Stack<String>, info: WalkInfo<P, A>, obj: Any): WalkInfo<P, A> {
        val collDt = this.registry.findCollectionTypeFor(obj) ?: throw KompositeException("CollectionType not found for ${obj::class}")
        return when (obj) {
            is Array<*> -> walkColl(owningProperty, path, info, collDt, obj.toList())
            is Collection<*> -> walkColl(owningProperty, path, info, collDt, obj)
            is Map<*, *> -> walkMap(owningProperty, path, info, collDt, obj)
            else -> throw KompositeException("Don't know how to walk collection of type ${obj::class.simpleName}")
        }
    }

    protected fun walkColl(owningProperty: DatatypeProperty?, path: Stack<String>, info: WalkInfo<P, A>, type: CollectionType, coll: Collection<*>): WalkInfo<P, A> {
        val infolb = this.collBegin(path.elements, info, type, coll)
        var acc = infolb.acc
        coll.forEachIndexed { index, element ->
            path.push(index.toString())
            val infobEl = WalkInfo(infolb.path, acc)
            val infoElb = this.collElementBegin(path.elements, infobEl, element)
            val infoElv = this.walkCollValue(owningProperty, path, infoElb, element)
            val infoEle = this.collElementEnd(path.elements, infoElv, element)
            val infoEls = if (index < coll.size - 1) {
                val infoas = this.collSeparate(path.elements, infoEle, type, coll, element)
                WalkInfo(infoas.path, infoas.acc)
            } else {
                //last one
                WalkInfo(infoEle.path, infoEle.acc)
            }
            acc = infoEls.acc
            path.pop()
        }
        val infole = WalkInfo(infolb.path, acc)
        return this.collEnd(path.elements, infole, type, coll)
    }

    protected fun walkCollValue(owningProperty: DatatypeProperty?, path: Stack<String>, info: WalkInfo<P, A>, value: Any?): WalkInfo<P, A> {
        return when {
            null == value -> walkNull(path, info)
            registry.isPrimitive(value) -> walkPrimitive(path, info, value)
            null == owningProperty || owningProperty.isComposite -> walkValue(owningProperty, path, info, value)
            owningProperty.isReference -> walkReference(owningProperty, path, info, value)
            else -> throw KompositeException("Don't know how to walk Collection element $owningProperty[${path.peek()}] = $value")
        }
    }

    protected fun walkMap(owningProperty: DatatypeProperty?, path: Stack<String>, info: WalkInfo<P, A>, type: CollectionType, map: Map<*, *>): WalkInfo<P, A> {
        val infolb = this.mapBegin(path.elements, info, map)
        var acc = infolb.acc
        map.entries.forEachIndexed { index, entry ->
            val infobEl = WalkInfo(infolb.path, acc)
            path.push(index.toString())
            val infomekb = this.mapEntryKeyBegin(path.elements, infobEl, entry)
            val infomekv = this.walkMapEntryKey(owningProperty, path, infomekb, entry.key)
            val infomeke = this.mapEntryKeyEnd(path.elements, infomekv, entry)
            val infomevb = this.mapEntryValueBegin(path.elements, infobEl, entry)
            val infomev = this.walkMapEntryValue(owningProperty, path, infomevb, entry.value)
            val infomeve = this.mapEntryValueEnd(path.elements, infomev, entry)
            val infomes = if (index < map.size - 1) {
                val infoas = this.mapSeparate(path.elements, infomeve, map, entry)
                WalkInfo(infoas.path, infoas.acc)
            } else {
                //last one
                WalkInfo(infomeve.path, infomeve.acc)
            }
            acc = infomes.acc
            path.pop()
        }
        val infole = WalkInfo(infolb.path, acc)
        return this.mapEnd(path.elements, infole, map)
    }

    protected fun walkMapEntryKey(owningProperty: DatatypeProperty?, path: Stack<String>, info: WalkInfo<P, A>, value: Any?): WalkInfo<P, A> {
        //key should always be a primitive or a reference, unless owning property is null! (i.e. map is the root)...I think !
        return when {
            null == value -> walkNull(path, info)
            registry.isPrimitive(value) -> walkPrimitive(path, info, value)
            null == owningProperty -> walkValue(owningProperty, path, info, value)
            else -> walkReference(owningProperty, path, info, value)
        }
    }

    protected fun walkMapEntryValue(owningProperty: DatatypeProperty?, path: Stack<String>, info: WalkInfo<P, A>, value: Any?): WalkInfo<P, A> {
        return when {
            null == value -> walkNull(path, info)
            registry.isPrimitive(value) -> walkPrimitive(path, info, value)
            null == owningProperty || owningProperty.isComposite -> walkValue(owningProperty, path, info, value)
            owningProperty.isReference -> walkReference(owningProperty, path, info, value)
            else -> throw KompositeException("Don't know how to walk Map value $owningProperty[${path.peek()}] = $value")
        }
    }

    protected fun walkReference(owningProperty: DatatypeProperty, path: Stack<String>, info: WalkInfo<P, A>, propValue: Any?): WalkInfo<P, A> {
        return when {
            null == propValue -> walkNull(path, info)
            registry.isCollection(propValue) -> walkCollection(owningProperty, path, info, propValue)
            else -> this.reference(path.elements, info, propValue, owningProperty)
        }
    }


    protected fun walkPrimitive(path: Stack<String>, info: WalkInfo<P, A>, primitive: Any): WalkInfo<P, A> {
        return this.primitive(path.elements, info, primitive)
    }

    protected fun walkNull(path: Stack<String>, info: WalkInfo<P, A>): WalkInfo<P, A> {
        return this.nullValue(path.elements, info)
    }
}