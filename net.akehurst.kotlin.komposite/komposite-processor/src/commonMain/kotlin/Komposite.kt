package net.akehurst.kotlin.komposite.processor

import net.akehurst.kotlin.komposite.api.DatatypeModel
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.processor.Agl
import kotlin.js.JsName

object Komposite {

    private var _processor:LanguageProcessor? = null

    internal fun processor(): LanguageProcessor {
        if (null== _processor) {
            val grammarStr = fetchGrammarStr()
            _processor = Agl.processor(
                    grammarStr,
                    SyntaxAnalyser(),
                    Formatter()
            )
        }
        return _processor!!
    }

    internal fun fetchGrammarStr(): String {
        return """
            namespace net.akehurst.kotlin.komposite
            
            grammar Composite {
                skip WHITE_SPACE = "\s+" ;
                skip COMMENT = MULTI_LINE_COMMENT | SINGLE_LINE_COMMENT ;
                     MULTI_LINE_COMMENT = "[/][*](?:.|\n)*?[*][/]" ;
                     SINGLE_LINE_COMMENT = "//(?:.*)?$" ;
            
                model = namespace* ;
                namespace = 'namespace' path '{' declaration* '}' ;
                path = [ NAME / '.']+ ;
                declaration = primitive | datatype ;
                primitive = 'primitive' NAME ;
                datatype = 'datatype' NAME '{' property* '}' ;
                property = NAME '{' characteristic+ '}' ;
                characteristic =  characteristicValue | identity ;
                characteristicValue = 'composite' | 'reference' | 'ignore' ;
                identity = 'identity' '(' POSITIVE_INTEGER ')' ;
            
                NAME = "[a-zA-Z_-][a-zA-Z0-9_-]*" ;
                POSITIVE_INTEGER = "[0-9]+" ;
            
            }
            
            """.trimIndent()


    }


    @JsName("process")
    fun process(datatypeModel:String) : DatatypeModel {
        return this.processor().process("model", datatypeModel)
    }
}