package net.akehurst.kotlin.komposite.common

import net.akehurst.kotlin.komposite.api.Datatype
import net.akehurst.kotlin.komposite.api.DatatypeProperty
import net.akehurst.kotlin.komposite.api.KompositeException
import net.akehurst.kotlin.komposite.processor.DatatypePropertySimple
import net.akehurst.kotlinx.reflect.reflect

fun DatatypeProperty.call(obj: Any): Any? {
    val cls = obj::class
    val reflect = cls.reflect()
    return reflect.callProperty(this.name, obj)
}

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
        val objectBegin: (info: WalkInfo<P, A>, obj: Any, datatype: Datatype) -> WalkInfo<P, A>,
        val objectEnd: (info: WalkInfo<P, A>, obj: Any, datatype: Datatype) -> WalkInfo<P, A>,
        val propertyBegin: (info: WalkInfo<P, A>, property: DatatypeProperty) -> WalkInfo<P, A>,
        val propertyEnd: (info: WalkInfo<P, A>, property: DatatypeProperty) -> WalkInfo<P, A>,
        val listBegin: (info: WalkInfo<P, A>, list: List<*>) -> WalkInfo<P, A>,
        val listSeparate: (info: WalkInfo<P, A>, list: List<*>, previousElement: Any?) -> WalkInfo<P, A>,
        val listEnd: (info: WalkInfo<P, A>, list: List<*>) -> WalkInfo<P, A>,
        val reference: (info: WalkInfo<P, A>, value: Any?, property: DatatypeProperty) -> WalkInfo<P, A>,
        val primitive: (info: WalkInfo<P, A>, value: Any) -> WalkInfo<P, A>,
        val nullValue: (info: WalkInfo<P, A>) -> WalkInfo<P, A>
) {

    class Builder<P : Any?, A : Any?>() {
        private var _objectBegin: (info: WalkInfo<P, A>, obj: Any, datatype: Datatype) -> WalkInfo<P, A> = { info, _, _ -> info }
        private var _objectEnd: (info: WalkInfo<P, A>, obj: Any, datatype: Datatype) -> WalkInfo<P, A> = { info, _, _ -> info }
        private var _propertyBegin: (info: WalkInfo<P, A>, property: DatatypeProperty) -> WalkInfo<P, A> = { info, _ -> info }
        private var _propertyEnd: (info: WalkInfo<P, A>, property: DatatypeProperty) -> WalkInfo<P, A> = { info, _ -> info }
        private var _listBegin: (info: WalkInfo<P, A>, list: List<*>) -> WalkInfo<P, A>  = { info, _ -> info }
        private var _listSeparate: (info: WalkInfo<P, A>, list: List<*>, previousElement: Any?) -> WalkInfo<P, A> = { info, _, _ -> info }
        private var _listEnd: (info: WalkInfo<P, A>, list: List<*>) -> WalkInfo<P, A>  = { info, _ -> info }
        private var _reference: (info: WalkInfo<P, A>, value: Any?, property: DatatypeProperty) -> WalkInfo<P, A> = { info, _, _ -> info }
        private var _primitive: (info: WalkInfo<P, A>, value: Any) -> WalkInfo<P, A> = { info, _ -> info }
        private var _nullValue: (info: WalkInfo<P, A>) -> WalkInfo<P, A> = { info -> info }

        fun objectBegin(func: (info: WalkInfo<P, A>, obj: Any, datatype: Datatype) -> WalkInfo<P, A> ) {
            this._objectBegin = func
        }

        fun objectEnd(func: (info: WalkInfo<P, A>, obj: Any, datatype: Datatype) -> WalkInfo<P, A> ) {
            this._objectEnd = func
        }

        fun propertyBegin(func: (info: WalkInfo<P, A>, property: DatatypeProperty) -> WalkInfo<P, A> ) {
            this._propertyBegin = func
        }

        fun propertyEnd(func: (info: WalkInfo<P, A>, property: DatatypeProperty) -> WalkInfo<P, A> ) {
            this._propertyEnd = func
        }

        fun listBegin(func: (info: WalkInfo<P, A>, list: List<*>) -> WalkInfo<P, A> ) {
            this._listBegin = func
        }

        fun listSeparate(func: (info: WalkInfo<P, A>, list: List<*>, previousElement: Any?) -> WalkInfo<P, A> ) {
            this._listSeparate = func
        }

        fun listEnd(func: (info: WalkInfo<P, A>, list: List<*>) -> WalkInfo<P, A> ) {
            this._listEnd = func
        }

        fun reference(func: (info: WalkInfo<P, A>, value: Any?, property: DatatypeProperty) -> WalkInfo<P, A> ) {
            this._reference = func
        }

        fun primitive(func: (info: WalkInfo<P, A>, value: Any) -> WalkInfo<P, A> ) {
            this._primitive = func
        }

        fun nullValue(func: (info: WalkInfo<P, A> ) -> WalkInfo<P, A> ) {
            this._nullValue = func
        }

        fun build(registry: DatatypeRegistry): KompositeWalker<P,A> {
            return KompositeWalker(
                    registry,
                    _objectBegin, _objectEnd,
                    _propertyBegin, _propertyEnd,
                    _listBegin, _listSeparate, _listEnd,
                    _reference, _primitive, _nullValue
            )
        }
    }

    fun walk(info: WalkInfo<P, A>, data: Any?): WalkInfo<P, A>  {
        return when {
            null == data -> walkNull(info)
            registry.isPrimitive(data) -> walkPrimitive(info, data)
            data is List<*> -> walkList(info, data)
            registry.hasDatatypeInfo(data) -> walkObject(info, data)
            else -> throw KompositeException("Don't know how to walk object: $data")
        }
    }

    protected fun walkPropertyValue(info: WalkInfo<P, A>, property: DatatypeProperty, propValue: Any?): WalkInfo<P, A> {
        return when {
            null == propValue -> walkNull(info)
            registry.isPrimitive(propValue) -> walkPrimitive(info, propValue)
            property.isComposite -> walk(info, propValue)
            property.isReference -> walkReference(info, property, propValue)
            else -> throw KompositeException("Don't know how to walk property $property = $propValue")
        }
    }

    protected fun walkObject(info: WalkInfo<P, A>, obj: Any): WalkInfo<P, A> {
        //TODO: use qualified name when we can
        val cls = obj::class
        val dt: Datatype = registry.findDatatypeByName(cls.simpleName!!)

        val infoob = this.objectBegin(info, obj, dt)
        var acc = infoob.acc

        cls.reflect().allPropertyNames.forEach {
            val prop = dt.allProperty[it] ?: DatatypePropertySimple(dt, it) //default is a reference property
            val propValue = prop.call(obj)
            val infopb = this.propertyBegin(WalkInfo(infoob.path, acc), prop)
            val infowp = this.walkPropertyValue(WalkInfo(infoob.path, infopb.acc), prop, propValue)
            val infope = this.propertyEnd(WalkInfo(infoob.path, infowp.acc), prop)
            acc = infope.acc
        }
        return this.objectEnd(WalkInfo(infoob.path, acc), obj, dt)
    }

    protected fun walkList(info: WalkInfo<P, A>, list: List<*>): WalkInfo<P, A>{

        val infolb = this.listBegin(info, list)
        var acc = infolb.acc
        list.forEach { element ->
            val infobEl = WalkInfo(infolb.path, acc)
            val infoal= this.walk(infobEl, element)
            //TODO: handle last item, should not call separate after last item!
            val infoas = this.listSeparate(infoal, list, element)
            acc = infoas.acc
        }
        val infole = WalkInfo(infolb.path, acc)
        return this.listEnd(infole, list)
    }

    protected fun walkReference(info:WalkInfo<P, A>, property: DatatypeProperty, propValue: Any?): WalkInfo<P, A> {
        return this.reference(info, propValue, property)
    }

    protected fun walkPrimitive(info:WalkInfo<P, A>, primitive: Any): WalkInfo<P, A> {
        return this.primitive(info, primitive)
    }

    protected fun walkNull(info:WalkInfo<P, A>): WalkInfo<P, A> {
        return this.nullValue(info)
    }
}