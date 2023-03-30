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

import net.akehurst.kotlin.komposite.api.Datatype
import net.akehurst.kotlin.komposite.api.DatatypeProperty
import net.akehurst.kotlin.komposite.api.EnumType
import net.akehurst.kotlin.komposite.api.KompositeException
import net.akehurst.kotlinx.reflect.reflect

fun Datatype.construct(vararg constructorArgs: Any?): Any {
    try {
        val cls = this.clazz
        val obj = cls.reflect().construct(*constructorArgs)
        return obj
    } catch (t: Throwable) {
        throw KompositeException("Unable to construct ${this.name} from ${constructorArgs.toList()} due to ${t.message ?: "Unknown"}")
    }
}

fun DatatypeProperty.get(obj: Any): Any? {
    val reflect = obj.reflect()
    return reflect.getProperty(this.name)
}
/*
fun DatatypeProperty.set(obj: Any, value: Any?) {
    try {
        val reflect = obj.reflect()

        if (reflect.isPropertyMutable(this.name)) {
            try {
                reflect.setProperty(this.name, value)
                return
            } catch (_: Throwable) {

            }
        }

        val existingValue = reflect.getProperty(this.name)
        if (existingValue is MutableCollection<*> && value is Collection<*>) {
            existingValue.clear()
            (existingValue as MutableCollection<Any>).addAll(value as Collection<Any>)
        } else if (existingValue is MutableMap<*, *> && value is Map<*, *>) {
            existingValue.clear()
            (existingValue as MutableMap<Any, Any>).putAll(value as Map<Any, Any>)
        } else {
            error("Cannot set property ${this.datatype.name}.${this.name} to ${value} because it is not a mutable property or Mutable collection")
        }

    } catch (t: Throwable) {
        throw KompositeException("Unable to set property ${this.datatype.name}.${this.name} to ${value} due to ${t.message ?: "Unknown"}")
    }
}
*/
fun DatatypeProperty.set(obj: Any, value: Any?) {
    try{
        val reflect = obj.reflect()
        if (this.isMutable) {
            reflect.setProperty(this.name, value)
        } else {
            val existingValue = reflect.getProperty(this.name)
            if (existingValue is MutableCollection<*> && value is Collection<*>) {
                existingValue.clear()
                (existingValue as MutableCollection<Any>).addAll(value as Collection<Any>)
            } else if (existingValue is MutableMap<*, *> && value is Map<*, *>) {
                existingValue.clear()
                (existingValue as MutableMap<Any, Any>).putAll(value as Map<Any, Any>)
            } else {
                error("Cannot set property ${this.datatype.name}.${this.name} to ${value} because it is not a mutable property or Mutable collection")
            }
        }

    } catch (t: Throwable) {
        throw KompositeException("Unable to set property ${this.datatype.name}.${this.name} to ${value} due to ${t.message ?: "Unknown"}")
    }
}

fun <E : Enum<E>> EnumType.valueOf(name: String): Enum<E>? {
    return this.clazz.reflect().enumValueOf<E>(name)
}