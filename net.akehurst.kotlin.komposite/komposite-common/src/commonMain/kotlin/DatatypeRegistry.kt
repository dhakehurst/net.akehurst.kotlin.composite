package net.akehurst.kotlin.komposite.common

import net.akehurst.kotlin.komposite.api.*
import kotlin.reflect.KClass
import net.akehurst.kotlin.komposite.processor.Komposite

class DatatypeRegistry {

	companion object {
		val KOTLIN_STD = """
            namespace kotlin {
                primitive Boolean
                primitive Int
                primitive Long
                primitive Float
                primitive Double
                primitive String
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
                collection List
                collection ArrayList
                collection Set
                collection HashSet
                collection Map
                collection HashMap
            }
        """.trimIndent()
	}

    private val _datatypes = mutableMapOf<String, Datatype>()
	private val _collection = mutableMapOf<String,CollectionType>()
	private val _primitive = mutableMapOf<String,PrimitiveType>()

	fun registerFromConfigString(datatypeModel:String) {
		try {
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
		return this._collection.containsKey(value::class.simpleName)
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
	fun findPrimitiveByName(name:String) : PrimitiveType? {
		return this._primitive[name]
	}
	fun findPrimitiveByClass(cls:KClass<*>) : PrimitiveType? {
		//TODO: use qualified name where possible
		return this._primitive[cls.simpleName]
	}
}