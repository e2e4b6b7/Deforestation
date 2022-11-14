package deforestation.expression.debrujin.algorithms

import deforestation.expression.debrujin.*
import deforestation.expression.debrujin.Function

fun treeless(expr: DeBrujinExpression, context: MutableMap<String, Function>) =
    TreelessConversionContext(context, ArrayList(), ArrayList(), ArrayList()).treeless(expr)

private data class TreelessConversionContext(
    val context: MutableMap<String, Function>,
    val stack: MutableList<DeBrujinExpression>,
    val cycleResolvers: MutableList<Pair<DeBrujinExpression, String>>,
    val cycleResolversRoots: MutableList<Pair<DeBrujinExpression, String>>,
)

private fun TreelessConversionContext.treeless(expr: DeBrujinExpression): DeBrujinExpression {
    /// check if there is already a known function call for such an expression
    val resolver = cycleResolvers.find { it.first alphaEq expr }
    if (resolver != null) return foldCycle(expr, resolver)

    /// check if there was already such an expression in the stack
    if (stack.any { it alphaEq expr }) return resolveCycle(expr)

    /// transform
    stack.add(expr)
    val result = treelessNoCycle(expr)
    stack.removeLast()

    /// check if that expression used as a folding prototype
    val asRoot = cycleResolversRoots.find { it.first === expr }
    return if (asRoot != null) {
        context[asRoot.second] = Function(
            asRoot.second,
            expr.freeVariables.mapIndexed { i, v -> (v as? NamedIdentifier)?.name ?: "arg$i" },
            result
        )
        FunctionCall(asRoot.second, expr.freeVariables.map { it.variable() })
    } else {
        result
    }
}

private fun TreelessConversionContext.foldCycle(
    expr: DeBrujinExpression,
    resolver: Pair<DeBrujinExpression, String>
): DeBrujinExpression {
    val arguments = resolver.first.freeVariables
    val freeVariables = expr.freeVariables
    val argMapping = (resolver.first alphaEqMap expr)!!

    return FunctionCall(
        resolver.second,
        arguments.map { arg -> freeVariables.find { it == argMapping[arg]!! }!!.variable() })
}

private fun TreelessConversionContext.resolveCycle(expr: DeBrujinExpression): DeBrujinExpression {
    if (expr is FunctionCall && expr.arguments.all { it is Variable }) {
        return FunctionCall(expr.name, expr.arguments.map { (it as Variable) })
    }

    val root = stack.find { it alphaEq expr }!!
    val name = generateNewFunctionName()
    val resolver = root to name
    cycleResolversRoots.add(resolver)
    cycleResolvers.add(resolver)

    return foldCycle(expr, resolver)
}

private fun TreelessConversionContext.generateNewFunctionName(): String {
    repeat(Int.MAX_VALUE) {
        val name = "_$it"
        if (context[name] == null) {
            context[name] = Function(name, emptyList(), UNDEFINED) // reserve name
            return name
        }
    }
    error("Too much functions")
}

private fun TreelessConversionContext.treelessNoCycle(expr: DeBrujinExpression): DeBrujinExpression =
    when (expr) {
        is Constructor -> Constructor(expr.name, expr.arguments.map { treeless(it) })
        is FunctionCall -> treeless(unfoldFunction(expr.name, expr.arguments))
        is Case -> treelessNoCycleCase(expr)
        is Variable -> expr
    }

private fun TreelessConversionContext.treelessNoCycleCase(case: Case) = when (val scrutinee = case.scrutinee) {
    is Variable -> case.copy(branches = case.branches.mapExpressions { treeless(it.expression) })

    is Constructor -> {
        case.branches.commonBranches.find { it.pattern.constructor == scrutinee.name }?.let { pat ->
            /// Match with common branch
            require(pat.pattern.variables.size == scrutinee.arguments.size) { "Invalid variables count in pattern" }
            treeless(pat.expression.resolve(scrutinee.arguments))
        } ?: case.branches.defaultBranch?.let {
            /// Match with default branch
            treeless(it.expression.resolve(scrutinee))
        } ?: error("Pattern matching failed")
    }

    is FunctionCall -> treeless(case.copy(scrutinee = unfoldFunction(scrutinee.name, scrutinee.arguments)))

    is Case -> treeless(Case(
        scrutinee.scrutinee,
        scrutinee.branches.mapExpressions {
            Case(
                it.expression,
                case.branches.mapExpressions { br -> br.expression.shift(it.bounded) },
            )
        }
    ))
}

private fun TreelessConversionContext.unfoldFunction(name: String, args: List<DeBrujinExpression>): DeBrujinExpression =
    context[name]!!.let { f ->
        require(f.variables.size == args.size) { "Invalid arguments count in function call" }
        f.expression.resolve(args)
    }
