package deforestation.parser

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.DefaultTokenizer
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.Parser
import deforestation.expression.CaseBranch
import deforestation.expression.DefaultCaseBranch
import deforestation.expression.Pattern
import deforestation.expression.simple.*

object LangParser : Grammar<Program>() {
    val ws by regexToken("\\s+", ignore = true)

    val kwArrow = literalToken("->")
    val kwCase = literalToken("case")
    val kwEsac = literalToken("esac")
    val kwFun = literalToken("fun")
    val kwComma = literalToken(",")
    val kwOf = literalToken("of")
    val kwLBrace = literalToken("(")
    val kwRBrace = literalToken(")")

    val variableId by regexToken("[a-z][\\w_]*")
    val functionId by regexToken("_[\\w_]*")
    val constructorId by regexToken("[A-Z][\\w_]*")

    val myTokenizer = DefaultTokenizer(
        listOf(
            ws,
            kwArrow,
            kwCase,
            kwEsac,
            kwFun,
            kwComma,
            kwOf,
            kwLBrace,
            kwRBrace,
            variableId,
            functionId,
            constructorId,
        )
    )

    val expr = parser(this::expression)

    val variable = variableId map { Variable(it.text) }

    val constructorCall = constructorId and zeroOrMore(expr) map
            { (name, args) -> Constructor(name.text, args) }

    val functionCall = functionId and -kwLBrace and separatedTerms(expr, kwComma) and -kwRBrace map
            { (name, args) -> FunctionCall(name.text, args) }

    val pattern = constructorId and zeroOrMore(variableId) map
            { (constructor, args) -> Pattern(constructor.text, args.map { it.text }) }

    val caseBranch = pattern and -kwArrow and expr and -kwComma map
            { (pat, expr) -> CaseBranch(pat, expr) }

    val defaultCaseBranch = variableId and -kwArrow and expr and -kwComma map
            { (name, expr) -> DefaultCaseBranch(name.text, expr) }

    val caseExpression =
        -kwCase and expr and -kwOf and zeroOrMore(caseBranch) and optional(defaultCaseBranch) and -kwEsac map
                { (expr, branches, defaultBranch) -> Case(expr, branches, defaultBranch) }

    val bracedExpression = -kwLBrace and expr and -kwRBrace

    val expression: Parser<Expression> =
        variable or constructorCall or functionCall or caseExpression or bracedExpression

    val funDecl = -kwFun and functionId and zeroOrMore(variableId) and -kwArrow and expression map
            { (name, args, expr) -> Function(name.text, args.map { it.text }, expr) }

    override val rootParser by zeroOrMore(funDecl and -kwComma) and expression map
            { (functions, ex) -> Program(functions, ex) }
}
