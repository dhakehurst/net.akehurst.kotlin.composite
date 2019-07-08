package net.akehurst.kotlin.komposite.common

import kotlin.test.Test
import kotlin.test.assertEquals


class test_KompositeWalker {

    data class A (
        val prop1:String = "hello"
    )

    @Test
    fun walk_null() {
        val reg = DatatypeRegistry()
        val sut = kompositeWalker<String?, String>(reg) {
            nullValue() { key,info->
                WalkInfo(key.toString(), "null")
            }
        }

        val actual = sut.walk(WalkInfo(null, ""), null)

        val expected = WalkInfo<String?, String>("", "null")

        assertEquals( expected, actual)
    }

    @Test
    fun walk_object() {

        val obj1 = A()

        val reg = DatatypeRegistry()
        reg.registerFromConfigString("""
            namespace net.akehurst.kotlin.komposite.common {
               primitive String
               datatype A {
                 
               }
            }
        """)

        var result = ""
        val sut = kompositeWalker<String, String>(reg) {
            objectBegin { key,info, obj, datatype ->
                result += "${datatype.name} { "
                info
            }
            objectEnd { key,info, obj, datatype ->
                result += "}"
                info
            }
            propertyBegin { key,info, property ->
                result += "${property.name} = "
                info
            }
            primitive { key,info, value ->
                when(value) {
                    is String -> result += "'${value.toString()}' "
                }

                info
            }
        }

        val actual = sut.walk(WalkInfo("", ""), obj1)
        val expected = "A { prop1 = 'hello' }"
        assertEquals(expected, result)
    }

}