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
    val cls = obj::class
    val reflect = cls.reflect()
    return reflect.getProperty(this.name, obj)
}

fun DatatypeProperty.set(obj: Any, value: Any?) {
    try {
        val cls = obj::class
        val reflect = cls.reflect()
        reflect.setProperty(this.name, obj, value)
    } catch (t: Throwable) {
        throw KompositeException("Unable to set property ${this.name} to ${value} due to ${t.message ?: "Unknown"}")
    }
}