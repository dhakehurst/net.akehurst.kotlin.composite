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

class DatatypeRegistry {

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
                collection Array
                collection Collection
                collection List
                collection Set
                collection Map
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
                collection Array
				collection Collection
				collection List
                collection Set
                collection Map
            }
        """.trimIndent()
	}

    private val _datatypes = mutableMapOf<String, Datatype>()
	private val _collection = mutableMapOf<String,CollectionType>()
	private val _primitive = mutableMapOf<String,PrimitiveType>()

	fun registerFromConfigString(datatypeModel:String) {
		try {
			//TODO: use qualified names when kotlin JS reflection supports qualifiedName
			val dtm:DatatypeModel = Komposite.process(datatypeModel)
			dtm.namespaces.forEach { ns->
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

	fun findDatatypeByName(name:String) : Datatype? {
		return this._datatypes[name]
	}
	fun findDatatypeByClass(cls:KClass<*>) : Datatype? {
		//TODO: use qualified name where possible
		return this._datatypes[cls.simpleName]
	}
	fun findCollectionTypeFor(value:Any) : CollectionType? {
		//TODO: use qualified name where possible
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
		//TODO: use qualified name where possible
		return this._primitive[cls.simpleName]
	}
}