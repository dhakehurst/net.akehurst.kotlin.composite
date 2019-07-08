package net.akehurst.kotlin.komposite.processor

import net.akehurst.kotlin.komposite.api.*
import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.api.sppt2ast.UnableToTransformSppt2AstExeception
import net.akehurst.language.processor.BranchHandler
import net.akehurst.language.processor.SyntaxAnalyserAbstract

class SyntaxAnalyser : SyntaxAnalyserAbstract() {

    class SyntaxAnalyserException : RuntimeException {
        constructor(message: String) : super(message)
    }

    init {
        // could autodetect these by reflection, for jvm, but faster if registered
        this.register("model", this::model as BranchHandler<DatatypeModel>)
        this.register("namespace", this::namespace as BranchHandler<Namespace>)
        this.register("path", this::path as BranchHandler<List<String>>)
        this.register("declaration", this::declaration as BranchHandler<Declaration>)
        this.register("primitive", this::primitive as BranchHandler<Datatype>)
        this.register("collection", this::collection as BranchHandler<Datatype>)
        this.register("datatype", this::datatype as BranchHandler<Datatype>)
        this.register("property", this::property as BranchHandler<DatatypeProperty>)
        this.register("characteristic", this::characteristic as BranchHandler<Datatype>)
        this.register("characteristicValue", this::characteristicValue as BranchHandler<Datatype>)
        this.register("identity", this::identity as BranchHandler<Datatype>)
    }

    override fun clear() {

    }

    override fun <T> transform(sppt: SharedPackedParseTree): T {
        return this.transform<T>(sppt.root.asBranch, "") as T
    }

    // model = namespace* ;
    fun model(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): DatatypeModel {
        val result = DatatypeModelSimple()

        val namespaces = children[0].branchNonSkipChildren.forEach {
            val ns = super.transform<Namespace>(it, arg)
            result.addNamespace(ns)
        }

        return result
    }

    // namespace = 'namespace' path '{' declaration* '}' ;
    fun namespace(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): Namespace {
        val path = super.transform<List<String>>(children[0], arg)
        val result = NamespaceSimple(path)
        children[1].branchNonSkipChildren.forEach {
            val dt = super.transform<Declaration>(it, result)
            result.addDeclaration(dt)
        }
        return result
    }

    // path = [ NAME / '.']+ ;
    fun path(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): List<String> {
        return children[0].branchNonSkipChildren.map { it.nonSkipMatchedText }
    }

    // declaration = primitive | collection | datatype ;
    fun declaration(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): Declaration {
        return super.transform(children[0], arg)
    }

    // primitive = 'primitive' NAME ;
    fun primitive(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): PrimitiveType {
        val namespace = arg as Namespace
        val name = children[0].nonSkipMatchedText
        val result = PrimitiveTypeSimple(namespace, name)
        return result
    }

    // collection = 'primitive' NAME ;
    fun collection(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): CollectionType {
        val namespace = arg as Namespace
        val name = children[0].nonSkipMatchedText
        val result = CollectionTypeSimple(namespace, name)
        return result
    }

    // datatype = 'datatype' NAME '{' property* '}' ;
    fun datatype(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): Datatype {
        val namespace = arg as Namespace
        val name = children[0].nonSkipMatchedText
        val result = DatatypeSimple(namespace, name)
        children[1].branchNonSkipChildren.forEach {
            val p = super.transform<DatatypeProperty>(it, result)
            result.addProperty(p)
        }
        return result
    }


    // property = NAME '{' characteristic+ '}' ;
    fun property(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): DatatypeProperty {
        val datatype = arg as Datatype
        val name = children[0].nonSkipMatchedText
        val result = DatatypePropertySimple(datatype, name)

        val charList = children[1].branchNonSkipChildren.map {
            super.transform<Any>(it, arg)
        }

        charList.forEach {
            when (it) {
                "composite" -> result.isComposite = true
                "reference" -> result.isReference = true
                "ignore" -> result.ignore = true
                else -> {
                    if (it is Int) {
                        result.setIdentityIndex(it)
                    } else {
                        throw SyntaxAnalyserException("unknown characteristic")
                    }
                }
            }


        }

        return result
    }

    // characteristic =  characteristicValue | identity ;
    fun characteristic(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): Any {
        return super.transform(children[0], arg)
    }

    // characteristicValue = 'composite' | 'reference' | 'ignore' ;
    fun characteristicValue(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): String {
        return target.nonSkipMatchedText
    }

    // identity = 'identity' '(' POSITIVE_INTEGER ')'
    fun identity(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): Int {
        return children[0].nonSkipMatchedText.toInt()
    }

}