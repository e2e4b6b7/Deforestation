import com.github.h0tk3y.betterParse.parser.parseToEnd
import com.github.h0tk3y.betterParse.st.liftToSyntaxTreeGrammar
import deforestation.expression.debrujin.BoundedVariable
import deforestation.expression.debrujin.Function
import deforestation.expression.debrujin.FunctionCall
import deforestation.expression.debrujin.Program
import deforestation.expression.debrujin.algorithms.treeless
import deforestation.expression.simple.algorithms.toDeBrujin
import deforestation.parser.LangParser
import deforestation.parser.print
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class MainTest {

    fun check(before: String, after: String) {
        val parsed = LangParser.liftToSyntaxTreeGrammar().parseToEnd(LangParser.myTokenizer.tokenize(before)).item
        val deBrujinProgram = Program(
            parsed.functions.map { Function(it.name, it.variables, it.expression.toDeBrujin(it.variables)) },
            parsed.expression.toDeBrujin
        )

        val context = buildMap { deBrujinProgram.functions.forEach { put(it.name, it) } }.toMutableMap()
        context.entries.toList().forEach { (k, f) ->
            context[k] = f.copy(
                expression = treeless(
                    FunctionCall(f.name,
                        List(f.variables.size) { i -> BoundedVariable(f.variables.size - i - 1) }), context
                )
            )
        }

        val finalExpression = treeless(deBrujinProgram.expression, context)
        val final = Program(context.values.toList(), finalExpression)

        assertEquals(after, final.print())
    }

    @Test
    fun `simple test`() = check(
        before = """
            fun _tmp a -> Box a;
            case _tmp(X) of
                Box x -> x,
                x -> x,
            esac
        """.trimIndent(), after = """
            fun _tmp a ->
                Box a(0);
            X
        """.trimIndent()
    )

    @Test
    fun `flip flip test`() = check(
        before = """
            fun _flip zt -> 
                case zt of
                    Leaf z -> Leaf z,
                    Branch xt yt -> Branch _flip(yt) _flip(xt),
                esac;
            _flip(_flip(zt))
        """.trimIndent(), after = """
            fun _flip zt ->
                case zt(0) of
                    Leaf z -> Leaf z(0),
                    Branch xt yt -> Branch _flip(yt(0)) _flip(xt(1)),
                esac;
            fun _0 arg0 arg1 ->
                Branch case arg1(0) of
                    Leaf z -> Leaf z(0),
                    Branch xt yt -> _0(yt(0), xt(1)),
                esac case arg0(1) of
                    Leaf z -> Leaf z(0),
                    Branch xt yt -> _0(yt(0), xt(1)),
                esac;
            case zt of
                Leaf z -> Leaf z(0),
                Branch xt yt -> _0(yt(0), xt(1)),
            esac
        """.trimIndent()
    )


    @Test
    fun `flatten test`() = check(
        before = """
            fun _append xs ys -> 
                case xs of
                    Nil -> ys,
                    Cons x xs -> Cons x _append(xs, ys),
                esac;
            fun _flatten xss -> case xss of
                Nil -> Nil,
                Cons xs xss -> _append(xs, _flatten(xss)),
            esac;
            _flatten(xss)
        """.trimIndent(), after = """
            fun _append xs ys ->
                case xs(1) of
                    Nil -> ys(0),
                    Cons x xs -> Cons x(1) _append(xs(0), ys(2)),
                esac;
            fun _flatten xss ->
                case xss(0) of
                    Nil -> Nil,
                    Cons xs xss -> _0(xs(1), xss(0)),
                esac;
            fun _0 arg0 arg1 ->
                case arg0(1) of
                    Nil -> _flatten(arg1(0)),
                    Cons x xs -> Cons x(1) _0(xs(0), arg1(2)),
                esac;
            case xss of
                Nil -> Nil,
                Cons xs xss -> case xs(1) of
                    Nil -> _flatten(xss(0)),
                    Cons x xs -> Cons x(1) _0(xs(0), xss(2)),
                esac,
            esac
        """.trimIndent()
    )
}
