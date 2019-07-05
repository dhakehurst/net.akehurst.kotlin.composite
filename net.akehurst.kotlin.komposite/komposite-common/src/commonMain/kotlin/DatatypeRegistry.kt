package net.akehurst.kotlin.komposite.common

import kotlin.reflect.KClass
import net.akehurst.kotlin.komposite.api.Datatype
import net.akehurst.kotlin.komposite.api.KompositeException
import net.akehurst.kotlin.komposite.api.DatatypeModel
import net.akehurst.kotlin.komposite.api.Primitive
import net.akehurst.kotlin.komposite.processor.Komposite

class DatatypeRegistry {

    private val _datatypes = mutableMapOf<String, Datatype>()
	private val _primitive = mutableMapOf<String,Primitive>()

	fun registerFromConfigString(datatypeModel:String) {
		try {
			val dtm:DatatypeModel = Komposite.process(datatypeModel)
			dtm.namespaces.forEach { ns->
				_primitive += ns.declaration.values.filterIsInstance<Primitive>().associate { Pair(it.name, it) }
				_datatypes += ns.declaration.values.filterIsInstance<Datatype>().associate { Pair(it.name, it) }
			}

		} catch (e:Exception) {
			throw  KompositeException("Error trying to register datatypes from config string", e);
		}

	}

	fun isPrimitive(value:Any) : Boolean {
		return this._primitive.containsKey(value::class.simpleName)
	}

	fun hasDatatypeInfo(value:Any) :Boolean {
		return this._datatypes.containsKey(value::class.simpleName)
	}

	fun findDatatypeByName(name:String) : Datatype {
		return this._datatypes[name]!!
	}

}