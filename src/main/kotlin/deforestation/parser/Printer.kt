package deforestation.parser

import deforestation.expression.branches.Pattern
import deforestation.expression.debrujin.*
import deforestation.expression.debrujin.Function

private data class PrintingContext(val level: Int, val bounded: List<String>) {
    fun up(levels: Int = 1) = PrintingContext(level + levels, bounded)
    fun bind(names: List<String>) = PrintingContext(level, names.asReversed() + bounded)
    fun bind(name: String) = PrintingContext(level, listOf(name) + bounded)
}

fun Program.print(): String =
    functions.joinToString("\n") { it.print() } + "\n" + PrintingContext(0, emptyList()).print(expression)

fun Function.print(): String =
    "fun $name ${variables.joinToString(" ")} ->\n    ${PrintingContext(1, variables.asReversed()).print(expression)};"

fun DeBrujinExpression.print(): String = PrintingContext(0, emptyList()).print(this)

private fun PrintingContext.print(expr: DeBrujinExpression): String = when (expr) {
    is Case -> print(expr)
    is Constructor -> print(expr)
    is FunctionCall -> print(expr)
    is Variable -> print(expr)
}

private fun PrintingContext.print(expr: Variable): String = when (expr) {
    is BoundedVariable -> "${bounded.getOrNull(expr.index) ?: "??"}(${expr.index})"
    is FreeVariable -> expr.name
}

private fun PrintingContext.print(expr: Constructor): String =
    if (expr.arguments.isEmpty()) expr.name else expr.name + " " + expr.arguments.joinToString(" ") { print(it) }

private fun PrintingContext.print(expr: FunctionCall): String =
    expr.name + "(" + expr.arguments.joinToString { print(it) } + ")"

private fun PrintingContext.print(expr: Case): String =
    if (expr.branches.commonBranches.isEmpty()) printLet(expr) else printCase(expr)

private fun PrintingContext.printLet(expr: Case) =
    expr.branches.defaultBranch!!.let {
        "let ${it.name} = ${print(expr.scrutinee)} in" + slb + bind(it.name).print(it.expression)
    }

private fun PrintingContext.printCase(expr: Case) =
    "case ${print(expr.scrutinee)} of" + lb +
            expr.branches.commonBranches.joinToString(lb) {
                "${print(it.pattern)} -> ${up().bind(it.pattern.variables).print(it.expression)},"
            } +
            (expr.branches.defaultBranch?.let { lb + "${it.name} -> ${up().bind(it.name).print(it.expression)}," }
                ?: "") +
            slb +
            "esac"

private fun print(pat: Pattern): String =
    if (pat.variables.isEmpty()) pat.constructor else pat.constructor + " " + pat.variables.joinToString(" ")

private val PrintingContext.lb: String get() = "\n" + " ".repeat(4 * (level + 1))
private val PrintingContext.slb: String get() = "\n" + " ".repeat(4 * level)
