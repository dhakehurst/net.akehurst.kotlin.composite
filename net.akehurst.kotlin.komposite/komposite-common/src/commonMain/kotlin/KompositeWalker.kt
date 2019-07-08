package net.akehurst.kotlin.komposite.common

import net.akehurst.kotlin.komposite.api.Datatype
import net.akehurst.kotlin.komposite.api.DatatypeProperty
import net.akehurst.kotlin.komposite.api.KompositeException
import net.akehurst.kotlin.komposite.processor.DatatypePropertySimple
import net.akehurst.kotlinx.reflect.reflect

inline fun <P : Any?, A : Any?> kompositeWalker(registry: DatatypeRegistry, init: KompositeWalker.Builder<P,A>.() -> Unit): KompositeWalker<P,A> {
    val builder = KompositeWalker.Builder<P,A>()
    builder.init()
    return builder.build(registry)
}

data class WalkInfo<P, A>(
        val path: P,
        val acc: A
)

class KompositeWalker<P : Any?, A : Any?>(
        val registry: DatatypeRegistry,
        val objectBegin: (key:Any,info: WalkInfo<P, A>, obj: Any, datatype: Datatype) -> WalkInfo<P, A>,
        val objectEnd: (key:Any,info: WalkInfo<P, A>, obj: Any, datatype: Datatype) -> WalkInfo<P, A>,
        val propertyBegin: (key:Any,info: WalkInfo<P, A>, property: DatatypeProperty) -> WalkInfo<P, A>,
        val propertyEnd: (key:Any,info: WalkInfo<P, A>, property: DatatypeProperty) -> WalkInfo<P, A>,
        val collBegin: (key:Any,info: WalkInfo<P, A>, list: Collection<*>) -> WalkInfo<P, A>,
        val collSeparate: (key:Any,info: WalkInfo<P, A>, list: Collection<*>, previousElement: Any?) -> WalkInfo<P, A>,
        val collEnd: (key:Any,info: WalkInfo<P, A>, list: Collection<*>) -> WalkInfo<P, A>,
        val reference: (key:Any,info: WalkInfo<P, A>, value: Any?, property: DatatypeProperty) -> WalkInfo<P, A>,
        val primitive: (key:Any,info: WalkInfo<P, A>, value: Any) -> WalkInfo<P, A>,
        val nullValue: (key:Any, info: WalkInfo<P, A>) -> WalkInfo<P, A>
) {

    companion object {
        val ROOT = object:Any() {
            override fun toString(): String {
                return ""
            }
        }
    }

    class Builder<P : Any?, A : Any?>() {
        private var _objectBegin: (key:Any,info: WalkInfo<P, A>, obj: Any, datatype: Datatype) -> WalkInfo<P, A> = { _,info, _, _ -> info }
        private var _objectEnd: (key:Any,info: WalkInfo<P, A>, obj: Any, datatype: Datatype) -> WalkInfo<P, A> = { _,info, _, _ -> info }
        private var _propertyBegin: (key:Any,info: WalkInfo<P, A>, property: DatatypeProperty) -> WalkInfo<P, A> = { _,info, _ -> info }
        private var _propertyEnd: (key:Any,info: WalkInfo<P, A>, property: DatatypeProperty) -> WalkInfo<P, A> = { _,info, _ -> info }
        private var _collBegin: (key:Any,info: WalkInfo<P, A>, list: Collection<*>) -> WalkInfo<P, A>  = { _,info, _ -> info }
        private var _collSeparate: (key:Any,info: WalkInfo<P, A>, list: Collection<*>, previousElement: Any?) -> WalkInfo<P, A> = { _,info, _, _ -> info }
        private var _collEnd: (key:Any,info: WalkInfo<P, A>, list: Collection<*>) -> WalkInfo<P, A>  = { _,info, _ -> info }
        private var _reference: (key:Any,info: WalkInfo<P, A>, value: Any?, property: DatatypeProperty) -> WalkInfo<P, A> = { _,info, _, _ -> info }
        private var _primitive: (key:Any,info: WalkInfo<P, A>, value: Any) -> WalkInfo<P, A> = { _,info, _ -> info }
        private var _nullValue: (key:Any,info: WalkInfo<P, A>) -> WalkInfo<P, A> = { _,info -> info }

        fun objectBegin(func: (key:Any,info: WalkInfo<P, A>, obj: Any, datatype: Datatype) -> WalkInfo<P, A> ) {
            this._objectBegin = func
        }

        fun objectEnd(func: (key:Any,info: WalkInfo<P, A>, obj: Any, datatype: Datatype) -> WalkInfo<P, A> ) {
            this._objectEnd = func
        }

        fun propertyBegin(func: (key:Any,info: WalkInfo<P, A>, property: DatatypeProperty) -> WalkInfo<P, A> ) {
            this._propertyBegin = func
        }

        fun propertyEnd(func: (key:Any,info: WalkInfo<P, A>, property: DatatypeProperty) -> WalkInfo<P, A> ) {
            this._propertyEnd = func
        }

        fun collBegin(func: (key:Any,info: WalkInfo<P, A>, coll: Collection<*>) -> WalkInfo<P, A> ) {
            this._collBegin = func
        }

        fun collSeparate(func: (key:Any,info: WalkInfo<P, A>, coll: Collection<*>, previousElement: Any?) -> WalkInfo<P, A> ) {
            this._collSeparate = func
        }

        fun collEnd(func: (key:Any,info: WalkInfo<P, A>, coll: Collection<*>) -> WalkInfo<P, A> ) {
            this._collEnd = func
        }

        fun reference(func: (key:Any,info: WalkInfo<P, A>, value: Any?, property: DatatypeProperty) -> WalkInfo<P, A> ) {
            this._reference = func
        }

        fun primitive(func: (key:Any,info: WalkInfo<P, A>, value: Any) -> WalkInfo<P, A> ) {
            this._primitive = func
        }

        fun nullValue(func: (key:Any,info: WalkInfo<P, A> ) -> WalkInfo<P, A> ) {
            this._nullValue = func
        }

        fun build(registry: DatatypeRegistry): KompositeWalker<P,A> {
            return KompositeWalker(
                    registry,
                    _objectBegin, _objectEnd,
                    _propertyBegin, _propertyEnd,
                    _collBegin, _collSeparate, _collEnd,
                    _reference, _primitive, _nullValue
            )
        }
    }

    fun walk(info: WalkInfo<P, A>, data: Any?): WalkInfo<P, A>  {
        val key = null
        return walkElement(ROOT, info, data)
    }

    protected fun walkElement(key:Any, info: WalkInfo<P, A>, data: Any?): WalkInfo<P, A>  {
        return when {
            null == data -> walkNull(key, info)
            registry.isPrimitive(data) -> walkPrimitive(key,info, data)
            registry.isCollection(data) -> walkCollection(key,info, data as Collection<*> )
            registry.hasDatatypeInfo(data) -> walkObject(key,info, data)
            else -> throw KompositeException("Don't know how to walk object: $data")
        }
    }

    protected fun walkPropertyValue(key:Any,info: WalkInfo<P, A>, property: DatatypeProperty, propValue: Any?): WalkInfo<P, A> {
        return when {
            null == propValue -> walkNull(key,info)
            registry.isPrimitive(propValue) -> walkPrimitive(key,info, propValue)
            property.isComposite -> walkElement(property.name, info, propValue)
            property.isReference -> walkReference(key,info, property, propValue)
            else -> throw KompositeException("Don't know how to walk property $property = $propValue")
        }
    }

    protected fun walkObject(key:Any,info: WalkInfo<P, A>, obj: Any): WalkInfo<P, A> {
        //TODO: use qualified name when we can
        val cls = obj::class
        val dt = registry.findDatatypeByName(cls.simpleName!!)
        if (null==dt) {
            throw KompositeException("Cannot find datatype for ${cls}, is it in the datatype configuration")
        } else {
            val infoob = this.objectBegin(key, info, obj, dt)
            var acc = infoob.acc

            cls.reflect().allPropertyNames(obj).forEach {
                val prop = dt.allProperty[it] ?: DatatypePropertySimple(dt, it) //default is a reference property
                val propValue = prop.get(obj)
                val infopb = this.propertyBegin(it, WalkInfo(infoob.path, acc), prop)
                val infowp = this.walkPropertyValue(it, WalkInfo(infoob.path, infopb.acc), prop, propValue)
                val infope = this.propertyEnd(it, WalkInfo(infoob.path, infowp.acc), prop)
                acc = infope.acc
            }
            return this.objectEnd(key, WalkInfo(infoob.path, acc), obj, dt)
        }
    }

    protected fun walkCollection(key:Any,info: WalkInfo<P, A>, coll: Collection<*>): WalkInfo<P, A>{

        val infolb = this.collBegin(key,info, coll)
        var acc = infolb.acc
        coll.forEachIndexed { index, element ->
            val infobEl = WalkInfo(infolb.path, acc)
            val infoal= this.walkElement(index, infobEl, element)
            //TODO: handle last item, should not call separate after last item!
            val infoas = this.collSeparate(key,infoal, coll, element)
            acc = infoas.acc
        }
        val infole = WalkInfo(infolb.path, acc)
        return this.collEnd(key,infole, coll)
    }

    protected fun walkReference(key:Any,info:WalkInfo<P, A>, property: DatatypeProperty, propValue: Any?): WalkInfo<P, A> {
        return this.reference(key,info, propValue, property)
    }

    protected fun walkPrimitive(key:Any,info:WalkInfo<P, A>, primitive: Any): WalkInfo<P, A> {
        return this.primitive(key,info, primitive)
    }

    protected fun walkNull(key:Any, info:WalkInfo<P, A>): WalkInfo<P, A> {
        return this.nullValue(key, info)
    }
}