package deforestation.expression.debrujin.algorithms

import deforestation.expression.CaseBranch
import deforestation.expression.DefaultCaseBranch
import deforestation.expression.debrujin.Function
import deforestation.expression.debrujin.*
import deforestation.expression.debrujin.DeBrujinTreelessExpression
import deforestation.expression.debrujin.TreelessBoundedVariable
import deforestation.expression.debrujin.TreelessConstructor
import deforestation.expression.debrujin.TreelessFreeVariable

fun treeless(expr: DeBrujinExpression, context: MutableMap<String, Function>) =
    TreelessConversionContext(context, ArrayList(), ArrayList(), ArrayList()).treeless(expr)

private data class TreelessConversionContext(
    val context: MutableMap<String, Function>,
    val stack: MutableList<DeBrujinExpression>,
    val cycleResolvers: MutableList<Pair<DeBrujinExpression, String>>,
    val cycleResolversRoots: MutableList<Pair<DeBrujinExpression, String>>,
)

private fun TreelessConversionContext.treeless(expr: DeBrujinExpression): DeBrujinTreelessExpression {
    val resolver = cycleResolvers.find { it.first alphaEq expr }
    if (resolver != null) return foldCycle(expr, resolver)

    if (stack.any { it alphaEq expr }) return resolveCycle(expr)

    stack.add(expr)
    val result = treelessNoCycle(expr)
    stack.removeLast()

    val asRoot = cycleResolversRoots.find { it.first === expr }
    return if (asRoot != null) {
//        val fvs = expr.freeVariables
//        fvs.asReversed().foldIndexed(result.shift(fvs.size)) { i, acc, arg ->
//            acc.remap(arg.treeless().shift(fvs.size), TreelessBoundedVariable(i, arg.name))
//        }
        context[asRoot.second] = TreelessFunction(asRoot.second, expr.freeVariables.map { it.name }, result)
        TreelessFunctionCall(asRoot.second, expr.freeVariables.map { it.treeless() })
    } else {
        result
    }
}

private fun TreelessConversionContext.foldCycle(
    expr: DeBrujinExpression,
    resolver: Pair<DeBrujinExpression, String>
): DeBrujinTreelessExpression {
    val arguments = resolver.first.freeVariables
    val freeVariables = expr.freeVariables
    val argMapping = (resolver.first alphaEqMap expr)!!

    return TreelessFunctionCall(
        resolver.second,
        arguments.map { arg -> freeVariables.find { it.name == argMapping[arg.name]!! }!!.treeless() })
}

private fun TreelessConversionContext.resolveCycle(expr: DeBrujinExpression): DeBrujinTreelessExpression {
    if (expr is FunctionCall && expr.arguments.all { it is Variable }) {
        return TreelessFunctionCall(expr.name, expr.arguments.map { (it as Variable).treeless() })
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
            context[name] = FunctionImpl("tmp", emptyList(), FreeVariableImpl("tmp"))
            return name
        }
    }
    error("Too much functions")
}

private fun TreelessConversionContext.treelessNoCycle(expr: DeBrujinExpression): DeBrujinTreelessExpression =
    when (expr) {
        is Constructor -> TreelessConstructor(expr.name, expr.arguments.map { treeless(it) })
        is FunctionCall -> treeless(unfoldFunction(expr.name, expr.arguments))
        is Case -> treelessNoCycleCase(expr)
        is BoundedVariable -> TreelessBoundedVariable(expr.index, expr.name)
        is FreeVariable -> TreelessFreeVariable(expr.name)
    }

private fun TreelessConversionContext.treelessNoCycleCase(case: Case) = when (val scrutinee = case.scrutinee) {
    is FreeVariable -> TreelessCase(
        TreelessFreeVariable(scrutinee.name),
        case.branches.map { CaseBranch(it.pattern, treeless(it.expression)) },
        case.defaultBranch?.let { DefaultCaseBranch(it.name, treeless(it.expression)) }
    )

    is BoundedVariable -> TreelessCase(
        TreelessBoundedVariable(scrutinee.index, scrutinee.name),
        case.branches.map { CaseBranch(it.pattern, treeless(it.expression)) },
        case.defaultBranch?.let { DefaultCaseBranch(it.name, treeless(it.expression)) }
    )

    is Constructor -> {
        case.branches.find { it.pattern.constructor == scrutinee.name }?.let { pat ->
            /// Match with branch
            require(pat.pattern.variables.size == scrutinee.arguments.size) { "Invalid variables count in pattern" }
            treeless(pat.expression.resolve(scrutinee.arguments))
        } ?: case.defaultBranch?.let { pat ->
            /// Match with default branch
            treeless(pat.expression.resolve(scrutinee))
        } ?: error("Matching failed")
    }

    is FunctionCall -> treeless(
        CaseImpl(
            unfoldFunction(scrutinee.name, scrutinee.arguments),
            case.branches,
            case.defaultBranch
        )
    )

    is Case -> treeless(CaseImpl(
        scrutinee.scrutinee,
        scrutinee.branches.map {
            CaseBranch(
                it.pattern,
                CaseImpl(
                    it.expression,
                    case.branches.map { br -> br.copy(expression = br.expression.shift(it.bounded)) },
                    case.defaultBranch?.let { br -> br.copy(expression = br.expression.shift(it.bounded)) }
                )
            )
        },
        scrutinee.defaultBranch?.let {
            DefaultCaseBranch(
                it.name,
                CaseImpl(
                    it.expression,
                    case.branches.map { br -> br.copy(expression = br.expression.shift(it.bounded)) },
                    case.defaultBranch?.let { br -> br.copy(expression = br.expression.shift(it.bounded)) }
                )
            )
        }
    ))
}

private fun TreelessConversionContext.unfoldFunction(name: String, args: List<DeBrujinExpression>): DeBrujinExpression =
    context[name]!!.let { f ->
        require(f.variables.size == args.size) { "Invalid arguments count in function call" }
        f.expression.resolve(args)
    }

private fun Variable.treeless(): TreelessVariable = when (this) {
    is BoundedVariableImpl -> TreelessBoundedVariable(index, name)
    is TreelessBoundedVariable -> this
    is FreeVariableImpl -> TreelessFreeVariable(name)
    is TreelessFreeVariable -> this
}
