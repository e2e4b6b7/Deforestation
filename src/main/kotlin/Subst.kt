fun Expression.subst(name: String, expression: Expression): Expression = when (this) {
    is Case -> subst(name, expression)
    is Constructor -> subst(name, expression)
    is FunctionCall -> subst(name, expression)
    is Variable -> subst(name, expression)
    else -> error("unreachable")
}

fun Variable.subst(name: String, expression: Expression) =
    if (this.name == name) expression else this

fun Constructor.subst(name: String, expression: Expression) =
    Constructor(this.name, arguments.map { it.subst(name, expression) })

fun FunctionCall.subst(name: String, expression: Expression) =
    FunctionCallImpl(this.name, arguments.map { it.subst(name, expression) })

fun Case.subst(name: String, expression: Expression) =
    CaseImpl(
        scrutinee.subst(name, expression),
        patterns.map { it.subst(name, expression) },
        defaultPattern?.subst(name, expression)
    )
