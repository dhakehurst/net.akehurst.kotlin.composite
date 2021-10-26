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

import net.akehurst.kotlin.komposite.api.*
import kotlin.reflect.KClass
import net.akehurst.kotlin.komposite.processor.Komposite

class DatatypeRegistry : DatatypeModel {

	companion object {
		val KOTLIN_STD = """
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

	private val _namespaces = mutableListOf<Namespace>()
	private val _datatypes = mutableMapOf<String, Datatype>()
	private val _collection = mutableMapOf<String,CollectionType>()
	private val _primitive = mutableMapOf<String,PrimitiveType>()
	private val _primitiveMappers = mutableMapOf<KClass<*>,PrimitiveMapper<*,*>>()

	override val namespaces: List<Namespace>
		get() {
			return this._namespaces
		}

	fun registerPrimitiveMapper(mapper:PrimitiveMapper<*,*>) {
		this._primitiveMappers[mapper.primitiveKlass] = mapper
	}

	fun registerFromConfigString(datatypeModel:String, primitiveMappers:Map<KClass<*>,PrimitiveMapper<*,*>>) {
		try {
			this._primitiveMappers.putAll(primitiveMappers)
			//TODO: use qualified names when kotlin JS reflection supports qualifiedName
			val dtm:DatatypeModel = Komposite.process(datatypeModel)
			dtm.namespaces.forEach { ns->
				ns.model = this
				this._namespaces.add(ns)
				_primitive += ns.declaration.values.filterIsInstance<PrimitiveType>().associate { Pair(it.name, it) }
				_collection += ns.declaration.values.filterIsInstance<CollectionType>().associate { Pair(it.name, it) }
				_datatypes += ns.declaration.values.filterIsInstance<Datatype>().associate { Pair(it.name, it) }
			}
		} catch (e:Exception) {
			throw  KompositeException("Error trying to register datatypes from config string", e);
		}

	}

	fun isPrimitive(value:Any) : Boolean {
		return this._primitive.containsKey(value::class.simpleName)
	}

	fun isCollection(value:Any) : Boolean {
		//TODO: use type hierachy so we can e.g. register List rather than ArrayList
		return when (value) {
			is List<*> -> this._collection.containsKey("List")
			is Set<*> -> this._collection.containsKey("Set")
			is Map<*,*> -> this._collection.containsKey("Map")
			is Collection<*> -> this._collection.containsKey("Collection")
			is Array<*> -> this._collection.containsKey("Array")
			else -> this._collection.containsKey(value::class.simpleName)
		}
	}

	fun hasDatatypeInfo(value:Any) :Boolean {
		return this._datatypes.containsKey(value::class.simpleName)
	}

	fun findTypeDeclarationByName(name:String) : TypeDeclaration? {
		return this._datatypes[name] ?: this._primitive[name] ?: this._collection[name]
	}
	fun findTypeDeclarationByClass(cls:KClass<*>) : TypeDeclaration? {
		//TODO: use qualified name when possible (i.e. when JS reflection supports qualified names)
		return this._datatypes[cls.simpleName] ?: this._primitive[cls.simpleName] ?: this._collection[cls.simpleName]
	}

	fun findDatatypeByName(name:String) : Datatype? {
		return this._datatypes[name]
	}
	fun findDatatypeByClass(cls:KClass<*>) : Datatype? {
		//TODO: use qualified name when possible (i.e. when JS reflection supports qualified names)
		return this._datatypes[cls.simpleName]
	}
	fun findCollectionTypeFor(value:Any) : CollectionType? {
		//TODO: use qualified name when possible
		return when(value) {
			is List<*> -> this._collection["List"]
			is Set<*> -> this._collection["Set"]
			is Map<*,*> -> this._collection["Map"]
			is Collection<*> -> this._collection["Collection"]
			is Array<*> -> this._collection["Array"]
			else -> this._collection[value::class.simpleName]
		}
	}
	fun findPrimitiveByName(name:String) : PrimitiveType? {
		return this._primitive[name]
	}
	fun findPrimitiveByClass(cls:KClass<*>) : PrimitiveType? {
		//TODO: use qualified name when possible (i.e. when JS reflection supports qualified names)
		return this._primitive[cls.simpleName]
	}

	fun findPrimitiveMapperFor(cls:KClass<*>) : PrimitiveMapper<*,*>? {
		return this._primitiveMappers[cls]
	}
	fun findPrimitiveMapperFor(clsName:String) : PrimitiveMapper<*,*>? {
		return this._primitiveMappers.values.firstOrNull {
			it.primitiveKlass.simpleName == clsName //FIXME: use qualified name when JS supports it!
		}
	}
	fun findFirstByName(typeName: String): TypeDeclaration {
		return this.namespaces.mapNotNull {
			it.declaration[typeName]
		}.firstOrNull() ?: throw KompositeException("TypeDeclaration $typeName not found in any namespace")
	}

	override fun resolve(typeReference: TypeReference): TypeDeclaration {
		val nsPath = typeReference.typePath.dropLast(1)
		val typeName = typeReference.typePath.last()
		return if (nsPath.isEmpty()) {
			this.findFirstByName(typeName)
		} else {
			val ns = this.namespaces.firstOrNull { it.path == nsPath } ?: throw KompositeException("Namespace $nsPath not found")
			ns.declaration[typeName] ?: throw KompositeException("TypeDeclaration $typeName not found in namespace $nsPath")
		}
	}
}