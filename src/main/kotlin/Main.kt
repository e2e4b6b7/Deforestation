import com.github.h0tk3y.betterParse.parser.parseToEnd
import com.github.h0tk3y.betterParse.st.liftToSyntaxTreeGrammar

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
        Branch xt yt -> Branch (_flip(yt)) (_flip(xt)),
    esac,
    _flip(_flip(zt))
""".trimIndent()


fun main() {
    transform(simpleCode)
    transform(flipCode)
}

private fun transform(code: String) {
    println("-----------------From---------------")
    val parsed = LangParser.liftToSyntaxTreeGrammar().parseToEnd(LangParser.myTokenizer.tokenize(code)).item
    println(code)
    val context = ComputationContext(buildMap { parsed.functions.forEach { put(it.name, it) } })
    println("-----------------To-----------------")
    val treel = treeless(parsed.expression, context)
    println(treel.print(0))
    println("------------------------------------")
}