package deforestation.expression.debrujin.algorithms

import deforestation.expression.debrujin.*
import deforestation.expression.debrujin.Function

fun treeless(expr: DeBrujinExpression, context: MutableMap<String, Function>) =
    TreelessConversionContext(context, ArrayList(), ArrayList(), ArrayList()).sc(expr)

private data class TreelessConversionContext(
    val context: MutableMap<String, Function>,
    val stack: MutableList<DeBrujinExpression>,
    val cycleResolvers: MutableList<Pair<DeBrujinExpression, String>>,
    val cycleResolversRoots: MutableList<Pair<DeBrujinExpression, String>>,
)

private fun TreelessConversionContext.sc(expr: DeBrujinExpression): DeBrujinExpression {
    /// check if there is already a known function call for such an expression
    val resolver = cycleResolvers.find { it.first alphaEq expr }
    if (resolver != null) return foldCycle(expr, resolver)

    /// check if there was already such an expression in the stack
    if (stack.any { it alphaEq expr }) return resolveCycle(expr)

    /// transform
    stack.add(expr)
    val result = scTransform(expr)
    stack.removeLast()

    /// check if that expression used as a folding prototype
    val asRoot = cycleResolversRoots.find { it.first === expr }
    return if (asRoot != null) {
        val fvs = expr.freeVariables
        val substitutions = fvs.asReversed().mapIndexed { i, fv -> fv to BoundedVariable(i) }
        context[asRoot.second] = Function(
            asRoot.second,
            fvs.mapIndexed { i, v -> (v as? NamedIdentifier)?.name ?: "arg$i" },
            result.subst(substitutions)
        )
        FunctionCall(asRoot.second, expr.freeVariables.map { it.variable() })
    } else {
        result
    }
}

private fun TreelessConversionContext.foldCycle(
    expr: DeBrujinExpression,
    resolver: Pair<DeBrujinExpression, String>
): DeBrujinExpression = FunctionCall(
    resolver.second,
    expr.freeVariables.map { fv -> fv.variable() }
)

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

private fun TreelessConversionContext.scTransform(expr: DeBrujinExpression): DeBrujinExpression =
    when (expr) {
        is Constructor -> Constructor(expr.name, expr.arguments.map { sc(it) })
        is FunctionCall -> sc(unfoldFunction(expr.name, expr.arguments))
        is Case -> scCase(expr)
        is Variable -> expr
    }

private fun TreelessConversionContext.scCase(case: Case) = when (val scrutinee = case.scrutinee) {
    is Variable -> case.copy(branches = case.branches.mapExpressions { sc(it.expression) })

    is Constructor -> {
        case.branches.commonBranches.find { it.pattern.constructor == scrutinee.name }?.let { pat ->
            /// Match with common branch
            require(pat.pattern.variables.size == scrutinee.arguments.size) { "Invalid variables count in pattern" }
            sc(pat.expression.resolve(scrutinee.arguments))
        } ?: case.branches.defaultBranch?.let {
            /// Match with default branch
            sc(it.expression.resolve(scrutinee))
        } ?: error("Pattern matching failed")
    }

    is FunctionCall -> sc(case.copy(scrutinee = unfoldFunction(scrutinee.name, scrutinee.arguments)))

    is Case -> sc(Case(
        scrutinee.scrutinee,
        scrutinee.branches.mapExpressions {
            Case(
                it.expression,
                case.branches.mapExpressions { br -> br.expression.shift(it.bounded, br.bounded) },
            )
        }
    ))
}

private fun TreelessConversionContext.unfoldFunction(name: String, args: List<DeBrujinExpression>): DeBrujinExpression =
    context[name]!!.let { f ->
        require(f.variables.size == args.size) { "Invalid arguments count in function call" }
        f.expression.resolve(args)
    }
