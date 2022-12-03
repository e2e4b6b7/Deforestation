package deforestation.expression.debrujin.algorithms

import deforestation.expression.branches.Branches
import deforestation.expression.branches.DefaultCaseBranch
import deforestation.expression.debrujin.*
import deforestation.expression.debrujin.Function

fun sc(expr: DeBrujinExpression, context: MutableMap<String, Function>) =
    (SCContext(context, ArrayList()).sc(expr) as SCSuccess).expr

private data class Frame(
    /** Previously processed expression */
    val expr: DeBrujinExpression,
    /** Reserved name for function which requested to generate from expression in that frame */
    var funcName: String? = null,
)

private data class SCContext(
    val functions: MutableMap<String, Function>,
    val stack: MutableList<Frame>
)

private sealed interface SCResult
private data class SCSuccess(val expr: DeBrujinExpression) : SCResult
private data class SCFailure(
    /** Frame that should be generalized */
    val frame: Frame,
    /** Generalized version of expression in that frame */
    val generalization: DeBrujinExpression,
    /** Substitution from generalized to local version */
    val substitution: Substitution
) : SCResult

private fun SCContext.sc(expr: DeBrujinExpression): SCResult {
    val alphaEqFrame = stack.find { expr alphaEq it.expr }
    if (alphaEqFrame != null) return SCSuccess(resolveCycle(expr, alphaEqFrame))

    val whistleFrame = stack.find { it.expr homeomorphicEmbedded expr }
    if (whistleFrame != null) return resolveWhistle(expr, whistleFrame)

    return scRec(expr)
}

private fun SCContext.scRec(expr: DeBrujinExpression): SCResult {
    val frame = Frame(expr)

    stack.add(frame)
    val result = scTransform(expr)
    stack.removeLast()

    when (result) {
        is SCSuccess -> {
            val scExpr = result.expr
            frame.funcName?.let { funcName ->
                val fvs = expr.freeVariables
                val funcArgs = fvs.mapIndexed { i, v -> (v as? NamedIdentifier)?.name ?: "arg$i" }
                val substitutions = fvs.asReversed().mapIndexed { i, fv -> fv to BoundedVariable(i) }
                val funcExpr = scExpr.subst(substitutions)
                functions[funcName] = Function(funcName, funcArgs, funcExpr)
                return SCSuccess(FunctionCall(funcName, expr.freeVariables.map { it.variable() }))
            }
            return SCSuccess(scExpr)
        }

        is SCFailure -> {
            if (frame != result.frame) return result
            val genResult = sc(result.generalization)
            if (genResult is SCFailure) return genResult
            val genExpr = (genResult as SCSuccess).expr
            return result.substitution
                .map { it.second }
                .mapM(this::sc) {
                    SCSuccess(it.zip(result.substitution).foldIndexed(genExpr) { i, e, (arg, s) ->
                        Case(
                            arg.shift(-i),
                            Branches(
                                listOf(),
                                DefaultCaseBranch("gen", e.subst(s.first, BoundedVariable(0)))
                            )
                        )
                    })
                }
        }
    }
}

private fun SCContext.resolveCycle(expr: DeBrujinExpression, frame: Frame): DeBrujinExpression {
    if (expr is FunctionCall && expr.arguments.all { it is Variable }) return expr
    createFunction(frame)
    return foldCycle(expr, frame)
}

private fun SCContext.foldCycle(expr: DeBrujinExpression, frame: Frame): DeBrujinExpression =
    FunctionCall(frame.funcName!!, expr.freeVariables.map { fv -> fv.variable() })

private fun SCContext.resolveWhistle(expr: DeBrujinExpression, frame: Frame): SCResult {
    val (e, s1, s2) = expr generalization frame.expr
    if (s2.all { it.second is Variable } && s2.map { it.second }.toSet().size == s2.size) {
        // Upper expression is more general
        createFunction(frame)
        return s1.mapM({ sc(it.second) }, { SCSuccess(FunctionCall(frame.funcName!!, it)) })
    }
    return SCFailure(frame, e, s2)
}

private fun SCContext.createFunction(frame: Frame) {
    if (frame.funcName == null) frame.funcName = generateNewFunctionName()
}

private fun SCContext.generateNewFunctionName(): String {
    repeat(Int.MAX_VALUE) {
        val name = "_$it"
        if (functions[name] == null) {
            functions[name] = Function(name, emptyList(), UNDEFINED) // reserve name
            return name
        }
    }
    error("Too much functions")
}

private fun SCContext.scTransform(expr: DeBrujinExpression): SCResult = when (expr) {
    is Constructor -> scConstructor(expr)
    is FunctionCall -> sc(unfoldFunction(expr.name, expr.arguments))
    is Case -> scCase(expr)
    is Variable -> SCSuccess(expr)
}

private fun SCContext.scConstructor(constructor: Constructor): SCResult =
    constructor.arguments.mapM(this::sc) { SCSuccess(constructor.copy(arguments = it)) }

private fun SCContext.scCase(case: Case): SCResult = when (val scrutinee = case.scrutinee) {
    is Variable -> case.branches.allBranches
        .map { it.expression.subst(scrutinee.identifier(-it.bounded), it.boundedExpression()) }
        .mapM(this::sc) { SCSuccess(Case(scrutinee, case.branches.withExpressions(it))) }

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

private fun SCContext.unfoldFunction(name: String, args: List<DeBrujinExpression>): DeBrujinExpression =
    functions[name]!!.let { f ->
        require(f.variables.size == args.size) { "Invalid arguments count in function call" }
        f.expression.resolve(args)
    }

private inline fun <T> List<T>.mapM(
    mapper: (T) -> SCResult,
    joiner: (List<DeBrujinExpression>) -> SCResult
): SCResult {
    val mapped = map(mapper)
    mapped.find { it is SCFailure }?.let { return it }
    val args = mapped.map { (it as SCSuccess).expr }
    return joiner(args)
}
