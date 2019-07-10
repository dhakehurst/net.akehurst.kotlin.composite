package net.akehurst.kotlin.komposite.common

import net.akehurst.kotlin.komposite.api.Datatype
import net.akehurst.kotlin.komposite.api.DatatypeProperty
import net.akehurst.kotlin.komposite.api.KompositeException
import net.akehurst.kotlin.komposite.processor.DatatypePropertySimple
import net.akehurst.kotlinx.reflect.reflect

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
        val objectBegin: (key: Any, info: WalkInfo<P, A>, obj: Any, datatype: Datatype) -> WalkInfo<P, A>,
        val objectEnd: (key: Any, info: WalkInfo<P, A>, obj: Any, datatype: Datatype) -> WalkInfo<P, A>,
        val propertyBegin: (key: Any, info: WalkInfo<P, A>, property: DatatypeProperty) -> WalkInfo<P, A>,
        val propertyEnd: (key: Any, info: WalkInfo<P, A>, property: DatatypeProperty) -> WalkInfo<P, A>,
        val mapBegin: (key: Any, info: WalkInfo<P, A>, map: Map<*, *>) -> WalkInfo<P, A>,
        val mapEntryBegin: (key: Any, info: WalkInfo<P, A>, entry: Map.Entry<*, *>) -> WalkInfo<P, A>,
        val mapEntryEnd: (key: Any, info: WalkInfo<P, A>, entry: Map.Entry<*, *>) -> WalkInfo<P, A>,
        val mapSeparate: (key: Any, info: WalkInfo<P, A>, map: Map<*, *>, previousEntry: Map.Entry<*, *>) -> WalkInfo<P, A>,
        val mapEnd: (key: Any, info: WalkInfo<P, A>, map: Map<*, *>) -> WalkInfo<P, A>,
        val collBegin: (key: Any, info: WalkInfo<P, A>, list: Collection<*>) -> WalkInfo<P, A>,
        val collElementBegin: (key: Any, info: WalkInfo<P, A>, element: Any?) -> WalkInfo<P, A>,
        val collElementEnd: (key: Any, info: WalkInfo<P, A>, element: Any?) -> WalkInfo<P, A>,
        val collSeparate: (key: Any, info: WalkInfo<P, A>, list: Collection<*>, previousElement: Any?) -> WalkInfo<P, A>,
        val collEnd: (key: Any, info: WalkInfo<P, A>, list: Collection<*>) -> WalkInfo<P, A>,
        val reference: (key: Any, info: WalkInfo<P, A>, value: Any, property: DatatypeProperty) -> WalkInfo<P, A>,
        val primitive: (key: Any, info: WalkInfo<P, A>, value: Any) -> WalkInfo<P, A>,
        val nullValue: (key: Any, info: WalkInfo<P, A>) -> WalkInfo<P, A>
) {

    companion object {
        val ROOT = object : Any() {
            override fun toString(): String {
                return ""
            }
        }
    }

    class Builder<P : Any?, A : Any?>() {
        private var _objectBegin: (key: Any, info: WalkInfo<P, A>, obj: Any, datatype: Datatype) -> WalkInfo<P, A> = { _, info, _, _ -> info }
        private var _objectEnd: (key: Any, info: WalkInfo<P, A>, obj: Any, datatype: Datatype) -> WalkInfo<P, A> = { _, info, _, _ -> info }
        private var _propertyBegin: (key: Any, info: WalkInfo<P, A>, property: DatatypeProperty) -> WalkInfo<P, A> = { _, info, _ -> info }
        private var _propertyEnd: (key: Any, info: WalkInfo<P, A>, property: DatatypeProperty) -> WalkInfo<P, A> = { _, info, _ -> info }
        private var _mapBegin: (key: Any, info: WalkInfo<P, A>, map: Map<*, *>) -> WalkInfo<P, A> = { _, info, _ -> info }
        private var _mapEntryBegin: (key: Any, info: WalkInfo<P, A>, entry: Map.Entry<*, *>) -> WalkInfo<P, A> = { _, info, _ -> info }
        private var _mapEntryEnd: (key: Any, info: WalkInfo<P, A>, entry: Map.Entry<*, *>) -> WalkInfo<P, A> = { _, info, _ -> info }
        private var _mapSeparate: (key: Any, info: WalkInfo<P, A>, map: Map<*, *>, previousEntry: Map.Entry<*, *>) -> WalkInfo<P, A> = { _, info, _, _ -> info }
        private var _mapEnd: (key: Any, info: WalkInfo<P, A>, map: Map<*, *>) -> WalkInfo<P, A> = { _, info, _ -> info }
        private var _collBegin: (key: Any, info: WalkInfo<P, A>, list: Collection<*>) -> WalkInfo<P, A> = { _, info, _ -> info }
        private var _collElementBegin: (key: Any, info: WalkInfo<P, A>, element: Any?) -> WalkInfo<P, A> = { _, info, _ -> info }
        private var _collElementEnd: (key: Any, info: WalkInfo<P, A>, element: Any?) -> WalkInfo<P, A> = { _, info, _ -> info }
        private var _collSeparate: (key: Any, info: WalkInfo<P, A>, list: Collection<*>, previousElement: Any?) -> WalkInfo<P, A> = { _, info, _, _ -> info }
        private var _collEnd: (key: Any, info: WalkInfo<P, A>, list: Collection<*>) -> WalkInfo<P, A> = { _, info, _ -> info }
        private var _reference: (key: Any, info: WalkInfo<P, A>, value: Any, property: DatatypeProperty) -> WalkInfo<P, A> = { _, info, _, _ -> info }
        private var _primitive: (key: Any, info: WalkInfo<P, A>, value: Any) -> WalkInfo<P, A> = { _, info, _ -> info }
        private var _nullValue: (key: Any, info: WalkInfo<P, A>) -> WalkInfo<P, A> = { _, info -> info }

        fun objectBegin(func: (key: Any, info: WalkInfo<P, A>, obj: Any, datatype: Datatype) -> WalkInfo<P, A>) {
            this._objectBegin = func
        }

        fun objectEnd(func: (key: Any, info: WalkInfo<P, A>, obj: Any, datatype: Datatype) -> WalkInfo<P, A>) {
            this._objectEnd = func
        }

        fun propertyBegin(func: (key: Any, info: WalkInfo<P, A>, property: DatatypeProperty) -> WalkInfo<P, A>) {
            this._propertyBegin = func
        }

        fun propertyEnd(func: (key: Any, info: WalkInfo<P, A>, property: DatatypeProperty) -> WalkInfo<P, A>) {
            this._propertyEnd = func
        }

        fun mapBegin(func: (key: Any, info: WalkInfo<P, A>, map: Map<*, *>) -> WalkInfo<P, A>) {
            this._mapBegin = func
        }

        fun mapEntryBegin(func: (key: Any, info: WalkInfo<P, A>, entry: Map.Entry<*, *>) -> WalkInfo<P, A>) {
            this._mapEntryBegin = func
        }

        fun mapEntryEnd(func: (key: Any, info: WalkInfo<P, A>, entry: Map.Entry<*, *>) -> WalkInfo<P, A>) {
            this._mapEntryEnd = func
        }

        fun mapSeparate(func: (key: Any, info: WalkInfo<P, A>, map: Map<*, *>, previousEntry: Map.Entry<*, *>) -> WalkInfo<P, A>) {
            this._mapSeparate = func
        }

        fun mapEnd(func: (key: Any, info: WalkInfo<P, A>, map: Map<*, *>) -> WalkInfo<P, A>) {
            this._mapEnd = func
        }

        fun collBegin(func: (key: Any, info: WalkInfo<P, A>, coll: Collection<*>) -> WalkInfo<P, A>) {
            this._collBegin = func
        }
        fun collElementBegin(func: (key: Any, info: WalkInfo<P, A>, element: Any?) -> WalkInfo<P, A>) {
            this._collElementBegin = func
        }
        fun collElementEnd(func: (key: Any, info: WalkInfo<P, A>, element: Any?) -> WalkInfo<P, A>) {
            this._collElementEnd = func
        }
        fun collSeparate(func: (key: Any, info: WalkInfo<P, A>, coll: Collection<*>, previousElement: Any?) -> WalkInfo<P, A>) {
            this._collSeparate = func
        }

        fun collEnd(func: (key: Any, info: WalkInfo<P, A>, coll: Collection<*>) -> WalkInfo<P, A>) {
            this._collEnd = func
        }

        fun reference(func: (key: Any, info: WalkInfo<P, A>, value: Any, property: DatatypeProperty) -> WalkInfo<P, A>) {
            this._reference = func
        }

        fun primitive(func: (key: Any, info: WalkInfo<P, A>, value: Any) -> WalkInfo<P, A>) {
            this._primitive = func
        }

        fun nullValue(func: (key: Any, info: WalkInfo<P, A>) -> WalkInfo<P, A>) {
            this._nullValue = func
        }

        fun build(registry: DatatypeRegistry): KompositeWalker<P, A> {
            return KompositeWalker(
                    registry,
                    _objectBegin, _objectEnd,
                    _propertyBegin, _propertyEnd,
                    _mapBegin, _mapEntryBegin, _mapEntryEnd, _mapSeparate, _mapEnd,
                    _collBegin, _collElementBegin, _collElementEnd, _collSeparate, _collEnd,
                    _reference, _primitive, _nullValue
            )
        }
    }

    fun walk(info: WalkInfo<P, A>, data: Any?): WalkInfo<P, A> {
        val key = null
        return walkValue(null, ROOT, info, data)
    }

    protected fun walkValue(owningProperty: DatatypeProperty?, key: Any, info: WalkInfo<P, A>, data: Any?): WalkInfo<P, A> {
        return when {
            null == data -> walkNull(key, info)
            registry.isPrimitive(data) -> walkPrimitive(key, info, data)
            registry.isCollection(data) -> walkCollection(owningProperty, key, info, data)
            registry.hasDatatypeInfo(data) -> walkObject(owningProperty, key, info, data)
            else -> throw KompositeException("Don't know how to walk object of class: ${data::class}")
        }
    }

    protected fun walkPropertyValue(owningProperty: DatatypeProperty, key: Any, info: WalkInfo<P, A>, propValue: Any?): WalkInfo<P, A> {
        return when {
            null == propValue -> walkNull(key, info)
            registry.isPrimitive(propValue) -> walkPrimitive(key, info, propValue)
            owningProperty.isComposite -> walkValue(owningProperty, owningProperty.name, info, propValue)
            owningProperty.isReference -> walkReference(owningProperty, key, info, propValue)
            else -> throw KompositeException("Don't know how to walk property $owningProperty = $propValue")
        }
    }


    protected fun walkObject(owningProperty: DatatypeProperty?, key: Any, info: WalkInfo<P, A>, obj: Any): WalkInfo<P, A> {
        //TODO: use qualified name when we can
        val cls = obj::class
        val dt = registry.findDatatypeByName(cls.simpleName!!)
        if (null == dt) {
            throw KompositeException("Cannot find datatype for ${cls}, is it in the datatype configuration")
        } else {
            val infoob = this.objectBegin(key, info, obj, dt)
            var acc = infoob.acc

            cls.reflect().allPropertyNames(obj).forEach { propName ->
                val prop = dt.allProperty[propName] ?: DatatypePropertySimple(dt, propName) //default is a reference property
                if (prop.ignore) {
                    //TODO: log!
                } else {
                    val propValue = prop.get(obj)
                    val infopb = this.propertyBegin(propName, WalkInfo(infoob.path, acc), prop)
                    val infowp = this.walkPropertyValue(prop, propName, WalkInfo(infoob.path, infopb.acc), propValue)
                    val infope = this.propertyEnd(propName, WalkInfo(infoob.path, infowp.acc), prop)
                    acc = infope.acc
                }
            }
            return this.objectEnd(key, WalkInfo(infoob.path, acc), obj, dt)
        }
    }

    protected fun walkCollection(owningProperty: DatatypeProperty?, key: Any, info: WalkInfo<P, A>, obj: Any): WalkInfo<P, A> {
        return when (obj) {
            is Collection<*> -> walkColl(owningProperty, key, info, obj)
            is Map<*, *> -> walkMap(owningProperty, key, info, obj)
            else -> throw KompositeException("Don't know how to walk collection of type ${obj::class.simpleName}")
        }
    }

    protected fun walkColl(owningProperty: DatatypeProperty?, key: Any, info: WalkInfo<P, A>, coll: Collection<*>): WalkInfo<P, A> {
        val infolb = this.collBegin(key, info, coll)
        var acc = infolb.acc
        coll.forEachIndexed { index, element ->
            val infobEl = WalkInfo(infolb.path, acc)
            val infoElb = this.collElementBegin(index, infobEl, element)
            val infoal = this.walkValue(owningProperty, index, infoElb, element)
            val infoEls = if (index < coll.size-1) {
                val infoas = this.collSeparate(key, infoal, coll, element)
                WalkInfo(infoas.path, infoas.acc)
            } else {
                //last one
                WalkInfo(infoal.path, infoal.acc)
            }
            val infoEle = this.collElementEnd(index, infoEls, element)
        }
        val infole = WalkInfo(infolb.path, acc)
        return this.collEnd(key, infole, coll)
    }

    protected fun walkMap(owningProperty: DatatypeProperty?, key: Any, info: WalkInfo<P, A>, map: Map<*, *>): WalkInfo<P, A> {
        val infolb = this.mapBegin(key, info, map)
        var acc = infolb.acc
        map.entries.forEachIndexed { index, entry ->
            val infobEl = WalkInfo(infolb.path, acc)
            val infopb = this.mapEntryBegin(entry.key!!, infobEl, entry)
            val infoal = this.walkMapEntryValue(owningProperty, index, infopb, entry.value)
            val infome = if (index < map.size-1) {
                val infoas = this.mapSeparate(key, infoal, map, entry)
                WalkInfo(infoas.path, infoas.acc)
            } else {
                //last one
                WalkInfo(infoal.path, infoal.acc)
            }

            val infope = this.mapEntryEnd(entry.key!!, infome, entry)
            acc = infope.acc
        }
        val infole = WalkInfo(infolb.path, acc)
        return this.mapEnd(key, infole, map)
    }

    protected fun walkMapEntryValue(owningProperty: DatatypeProperty?, key: Any, info: WalkInfo<P, A>, value: Any?): WalkInfo<P, A> {
        return when {
            null == value -> walkNull(key, info)
            registry.isPrimitive(value) -> walkPrimitive(key, info, value)
            null==owningProperty || owningProperty.isComposite -> walkValue(owningProperty, key, info, value)
            owningProperty.isReference -> walkReference(owningProperty, key, info, value)
            else -> throw KompositeException("Don't know how to walk Map element $owningProperty[${key}] = $value")
        }
    }

    protected fun walkReference(owningProperty: DatatypeProperty, key: Any, info: WalkInfo<P, A>, propValue: Any): WalkInfo<P, A> {
        return this.reference(key, info, propValue, owningProperty)
    }

    protected fun walkPrimitive(key: Any, info: WalkInfo<P, A>, primitive: Any): WalkInfo<P, A> {
        return this.primitive(key, info, primitive)
    }

    protected fun walkNull(key: Any, info: WalkInfo<P, A>): WalkInfo<P, A> {
        return this.nullValue(key, info)
    }
}