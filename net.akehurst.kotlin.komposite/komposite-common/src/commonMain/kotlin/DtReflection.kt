package net.akehurst.kotlin.komposite.common

import net.akehurst.kotlin.komposite.api.Datatype
import net.akehurst.kotlin.komposite.api.DatatypeProperty
import net.akehurst.kotlinx.reflect.reflect

fun Datatype.construct(vararg constructorArgs: Any?): Any {
    val cls = this.clazz
    val obj = cls.reflect().construct(*constructorArgs)
    return obj
}

fun DatatypeProperty.get(obj: Any): Any? {
    val cls = obj::class
    val reflect = cls.reflect()
    return reflect.getProperty(this.name, obj)
}

fun DatatypeProperty.set(obj: Any, value: Any?) {
    val cls = obj::class
    val reflect = cls.reflect()
    reflect.setProperty(this.name, obj, value)
}