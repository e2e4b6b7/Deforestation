fun Expression.print(level: Int): String = when (this) {
    is Case -> print(level)
    is Constructor -> print(level)
    is FunctionCall -> print(level)
    is Variable -> print()
    else -> error("unreachable")
}

fun Variable.print(): String = name

fun Constructor.print(level: Int): String =
    if (arguments.isEmpty())
        name
    else
        name + " " + arguments.joinToString(" ") { it.print(level) }

fun FunctionCall.print(level: Int): String =
    name + "(" + arguments.joinToString { it.print(level) } + ")"

fun Case.print(level: Int): String =
    "case " + scrutinee.print(level) + " of\n" +
            "\t".repeat(level+1) + patterns.joinToString(postfix = "\n") { it.pattern.print() + " -> " + it.expression.print(level + 1) } +
            (defaultPattern?.let { "\t".repeat(level+1) + it.name + " -> " + it.expression.print(level + 1) + "\n" } ?: "") +
            "esac"

private fun Pattern.print(): String = constructor + " " + variables.joinToString(" ")
