import deforestation.expression.Pattern
import deforestation.expression.debrujin.*
import deforestation.expression.debrujin.Function

fun Program.print(): String = functions.joinToString(separator = "\n") { it.print() } + "\n" + expression.print(0)

fun Function.print(): String = "fun $name ${variables.joinToString(separator = " ")} ->\n\t${expression.print(1)},"

fun DeBrujinExpression.print(level: Int): String = when (this) {
    is Case -> print(level)
    is Constructor -> print(level)
    is FunctionCall -> print(level)
    is Variable -> print()
}

fun Variable.print(): String = when (this) {
    is BoundedVariable -> "$name($index)"
    is FreeVariable -> name
}

fun Constructor.print(level: Int): String =
    if (arguments.isEmpty())
        name
    else
        name + " " + arguments.joinToString(" ") { it.print(level) }

fun FunctionCall.print(level: Int): String =
    name + "(" + arguments.joinToString { it.print(level) } + ")"

fun Case.print(level: Int): String =
    "case ${scrutinee.print(level)} of" +
            lb(level + 1) +
            branches.joinToString(",${lb(level + 1)}") { "${it.pattern.print()} -> ${it.expression.print(level + 1)}" } +
            (defaultBranch?.let { "${lb(level + 1)}${it.name} -> ${it.expression.print(level + 1)}" } ?: "") +
            lb(level) +
            "esac"

private fun Pattern.print(): String = constructor + " " + variables.joinToString(" ")

private fun lb(level: Int): String = "\n" + "\t".repeat(level)
