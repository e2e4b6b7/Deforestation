fun Expression.freeVariables(): Set<String> = when (this) {
    is Case -> freeVariables()
    is Constructor -> freeVariables()
    is FunctionCall -> freeVariables()
    is Variable -> freeVariables()
    else -> error("unreachable")
}

fun Variable.freeVariables() = setOf(name)

fun Constructor.freeVariables() = arguments.map { it.freeVariables() }.merge

fun FunctionCall.freeVariables() = arguments.map { it.freeVariables() }.merge

fun Case.freeVariables() =
    scrutinee.freeVariables() +
            patterns.map { it.expression.freeVariables().minus(it.pattern.variables.toSet()) }.merge +
            (defaultPattern?.expression?.freeVariables()?.minus(listOfNotNull(defaultPattern?.name)) ?: emptySet())

val List<Set<String>>.merge get() = foldRight(mutableSetOf<String>()) { a, b -> b.apply { addAll(a) } }