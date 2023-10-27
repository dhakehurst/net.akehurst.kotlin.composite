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

import net.akehurst.kotlin.komposite.api.KompositeException
import net.akehurst.kotlin.komposite.api.PrimitiveMapper
import net.akehurst.kotlin.komposite.processor.Komposite
import net.akehurst.kotlinx.reflect.KotlinxReflect
import net.akehurst.kotlinx.reflect.reflect
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.simple.TypeModelSimpleAbstract
import kotlin.reflect.KClass

class DatatypeRegistry2 : TypeModelSimpleAbstract("registry") {

    companion object {
        val KOTLIN_STD_STRING = """
            namespace kotlin {
                primitive Boolean
                primitive Byte
                primitive Short
                primitive Int
                primitive Long
                primitive Float
                primitive Double
                primitive String
            }
            namespace kotlin.collections {
                collection Array<E>
                collection Collection<E>
                collection List<E>
                collection Set<E>
                collection Map<K,V>
            }
        """.trimIndent()

        val KOTLIN_STD_MODEL = typeModel("kotlin-std",false, emptyList()) {
            namespace("kotlin", emptyList()) {
                primitiveType("Boolean")
                primitiveType("Byte")
                primitiveType("Short")
                primitiveType("Int")
                primitiveType("Long")
                primitiveType("Float")
                primitiveType("Double")
                primitiveType("String")
                dataType("Any") {

                }
            }
            namespace("kotlin.collections", emptyList()) {
                collectionType("Array", listOf("E"))
                collectionType("Collection", listOf("E"))
                collectionType("List", listOf("E"))
                collectionType("Set", listOf("E"))
                collectionType("Map", listOf("K", "V"))
            }
        }

        val JAVA_STD = """
            namespace java.lang {
                primitive Boolean
                primitive Integer
                primitive Long
                primitive Float
                primitive Double
                primitive String
            }
    
            namespace java.util {
                collection Array<T>
				collection Collection<T>
				collection List<T>
                collection Set<T>
                collection Map<K,V>
            }
        """.trimIndent()
    }

    private val _primitiveMappers = mutableMapOf<KClass<*>, PrimitiveMapper<*, *>>()

    fun registerPrimitiveMapper(mapper: PrimitiveMapper<*, *>) {
        this._primitiveMappers[mapper.primitiveKlass] = mapper
    }

    fun registerFromConfigString(kompositeModel: String, primitiveMappers: Map<KClass<*>, PrimitiveMapper<*, *>>) {
        try {
            val result = Komposite.process(kompositeModel)
            if (null == result.asm) {
                throw KompositeException("Error processing config string", result.issues.errors, null)
            } else {
                this.registerFromKompositeModel(result.asm!!, primitiveMappers)
            }
        } catch (e: Exception) {
            throw KompositeException("Error trying to register datatypes from config string - ${e.message}", e)
        }
    }

    fun registerFromKompositeModel(kompositeModel: TypeModel, primitiveMappers: Map<KClass<*>, PrimitiveMapper<*, *>>) {
        this._primitiveMappers.putAll(primitiveMappers)
        kompositeModel.allNamespace.forEach {
                this.addNamespace(it)
        }
    }

    fun findTypeDeclarationByKClass(cls: KClass<*>): TypeDeclaration? {
        //TODO: use qualified name when possible (i.e. when JS reflection supports qualified names)
        //val qname = cls.qualifiedName ?: error("class does not have a qualifiedName!")
        val qname = cls.simpleName ?: error("class does not have a simple name!")
        return this.findByQualifiedNameOrNull(qname)
    }

    fun findPrimitiveMapperByKClass(cls: KClass<*>): PrimitiveMapper<*, *>? {
        return this._primitiveMappers[cls]
    }

    fun findPrimitiveMapperBySimpleName(clsName: String): PrimitiveMapper<*, *>? {
        return this._primitiveMappers.values.firstOrNull {
            it.primitiveKlass.simpleName == clsName //FIXME: use qualified name when JS supports it!
        }
    }

    fun isPrimitive(value: Any): Boolean {
        return this.findFirstByNameOrNull(value::class.simpleName!!) is PrimitiveType
    }

    fun isEnum(value: Any): Boolean {
        return this.findFirstByNameOrNull(value::class.simpleName!!) is EnumType
    }

    fun isCollection(value: Any): Boolean {
        //TODO: use type hierachy so we can e.g. register List rather than ArrayList
        return when (value) {
            is List<*> -> this.findFirstByNameOrNull("List") is CollectionType
            is Set<*> -> this.findFirstByNameOrNull("Set") is CollectionType
            is Map<*, *> -> this.findFirstByNameOrNull("Map") is CollectionType
            is Collection<*> -> this.findFirstByNameOrNull("Collection") is CollectionType
            is Array<*> -> this.findFirstByNameOrNull("Array") is CollectionType
            else -> this.findFirstByNameOrNull(value::class.simpleName!!) is CollectionType
        }
    }

    fun isDatatype(value: Any): Boolean {
        return this.findFirstByNameOrNull(value::class.simpleName!!) is DataType
    }

    fun findCollectionTypeFor(value: Any): CollectionType? {
        //TODO: use qualified name when possible
        return when (value) {
            is List<*> -> this.findFirstByNameOrNull("List") as CollectionType?
            is Set<*> -> this.findFirstByNameOrNull("Set") as CollectionType?
            is Map<*, *> -> this.findFirstByNameOrNull("Map") as CollectionType?
            is Collection<*> -> this.findFirstByNameOrNull("Collection") as CollectionType?
            is Array<*> -> this.findFirstByNameOrNull("Array") as CollectionType?
            else -> this.findFirstByNameOrNull(value::class.simpleName!!) as CollectionType?
        }
    }

    fun checkPublicAndReflectable() : List<String> {
        val issues = mutableListOf<String>()
        for (ns in super.allNamespace) {
            when(ns.qualifiedName) {
                "kotlin" -> Unit //don't check kotlin namespace
                else -> {
                    for (t in ns.elementType) {
                        when {
                            //cls.reflect().exists -> Unit //OK
                            KotlinxReflect.registeredClasses.containsKey(t.qualifiedName) -> Unit // OK gegistered
                            else -> issues.add("Type '${t.qualifiedName}' is not registered with kotlinxReflect")
                        }
                    }
                }
            }
        }
        return issues
    }
}