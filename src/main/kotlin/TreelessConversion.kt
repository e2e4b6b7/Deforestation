fun treeless(expr: Expression, context: ComputationContext) =
    TreelessConversionContext(context, emptySet(), emptyMap()).treeless(expr)

private data class TreelessConversionContext(
    val context: ComputationContext,
    val stack: Set<Expression>,
    val cycleResolvers: Map<Expression, Expression>
)

private fun TreelessConversionContext.treeless(expr: Expression): TreelessExpression {
    if (stack.contains(expr)) error("Cycle resolution was not implemented")
    val newStack = stack.plus(expr)
    return TreelessConversionContext(context, newStack, cycleResolvers).treelessNoCycle(expr)
}

private fun TreelessConversionContext.treelessNoCycle(expr: Expression): TreelessExpression = when (expr) {
    is Variable -> expr
    is Constructor -> Constructor(expr.name, expr.arguments.map { treeless(it) })
    is FunctionCall -> treeless(context.unfoldFunction(expr.name, expr.arguments))
    is Case -> treelessNoCycleCase(expr)
    else -> error("unreachable")
}

private fun TreelessConversionContext.treelessNoCycleCase(case: Case) = when (val scrutinee = case.scrutinee) {
    is Variable -> TreelessCaseImpl(
        scrutinee,
        case.patterns.map { CaseBranch(it.pattern, treeless(it.expression)) },
        case.defaultPattern?.let { DefaultCaseBranch(it.name, treeless(it.expression)) }
    )

    is Constructor -> {
        case.patterns.find { it.pattern.constructor == scrutinee.name }?.let { pat ->
            /// Match with branch
            treeless(pat.expression.subst(pat.pattern.variables, scrutinee.arguments))
        } ?: case.defaultPattern?.let { pat ->
            /// Match with default branch
            treeless(pat.name?.let { pat.expression.subst(it, scrutinee) } ?: pat.expression)
        } ?: error("Matching failed")
    }

    is FunctionCall -> treeless(
        CaseImpl(
            treeless(context.unfoldFunction(scrutinee.name, scrutinee.arguments)),
            case.patterns,
            case.defaultPattern
        )
    )

    is Case -> treeless(CaseImpl(
        scrutinee.scrutinee,
        scrutinee.patterns.map {
            CaseBranch(
                it.pattern,
                CaseImpl(it.expression, case.patterns, case.defaultPattern)
            )
        },
        scrutinee.defaultPattern?.let {
            DefaultCaseBranch(
                it.name,
                CaseImpl(it.expression, case.patterns, case.defaultPattern)
            )
        }
    ))

    else -> error("unreachable")
}

private fun ComputationContext.unfoldFunction(name: String, arguments: List<Expression>): Expression {
    return functions[name]!!.let { (_, funVariables, funExpression) ->
        funExpression.subst(funVariables, arguments)
    }
}

private fun Expression.subst(variables: List<String>, expressions: List<Expression>): Expression {
    require(variables.size == expressions.size) { "Invalid arguments count" }
    return variables.zip(expressions).fold(this) { expr, (v, arg) -> expr.subst(v, arg) }
}
