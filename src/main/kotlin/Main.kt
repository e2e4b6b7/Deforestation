import com.github.h0tk3y.betterParse.parser.parseToEnd
import com.github.h0tk3y.betterParse.st.liftToSyntaxTreeGrammar
import deforestation.expression.debrujin.*
import deforestation.expression.debrujin.algorithms.shift
import deforestation.expression.debrujin.algorithms.treeless
import deforestation.expression.simple.algorithms.toDeBrujin
import deforestation.parser.LangParser

val simpleCode = """
    fun _tmp a -> Box a,
    case _tmp(X) of
        Box x -> x,
        x -> x,
    esac
""".trimIndent()

val flipCode = """
    fun _flip zt -> case zt of
        Leaf z -> Leaf z,
        Branch xt yt -> Branch _flip(yt) _flip(xt),
    esac,
    _flip(_flip(zt))
""".trimIndent()

val flattenCode = """
    fun _append xs ys -> case xs of
        Nil -> ys,
        Cons x xs -> Cons x _append(xs, ys),
    esac,
    fun _flatten xss -> case xss of
        Nil -> Nil,
        Cons xs xss -> _append(xs, _flatten(xss)),
    esac,
    _flatten(xss)
""".trimIndent()

val testCode = """
    fun _append xs ys -> case xs of
        Nil -> ys,
        Cons x xs -> Cons x _append(xs, ys),
    esac,
    fun _flatten xss -> case xss of
        Nil -> Nil,
        Cons xs xss -> _append(xs, _flatten(xss)),
    esac,
    X
""".trimIndent()


fun main() {
    transform(simpleCode)
    transform(flipCode)
    transform(flattenCode)
//    transform(testCode)
}

private fun transform(code: String) {
    println("-------------------From-------------------")
    println(code)
    val parsed = LangParser.liftToSyntaxTreeGrammar().parseToEnd(LangParser.myTokenizer.tokenize(code)).item
    val deBrujinProgram = ProgramImpl(
        parsed.functions.map { FunctionImpl(it.name, it.variables, it.expression.toDeBrujin(it.variables)) },
        parsed.expression.toDeBrujin
    )
//    println("-------------------Internal---------------")
//    println(deBrujinProgram.print())
    val context = buildMap { deBrujinProgram.functions.forEach { put(it.name, it) } }.toMutableMap()
    context.entries.toList().forEach { (k, f) ->
        context[k] = TreelessFunction(
            f.name,
            f.variables,
            treeless(
                FunctionCallImpl(
                    f.name,
                    f.variables.mapIndexed { i, name -> BoundedVariableImpl(f.variables.size - i - 1, name) }),
                context
            )
        )
    }
    val finalExpression = treeless(deBrujinProgram.expression, context)
    val final = TreelessProgram(context.values.map { it as TreelessFunction }, finalExpression)
    println("--------------------To--------------------")
    println(final.print())
}