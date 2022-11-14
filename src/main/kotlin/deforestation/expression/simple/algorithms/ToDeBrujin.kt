package deforestation.expression.simple.algorithms

import deforestation.expression.CaseBranch
import deforestation.expression.DefaultCaseBranch
import deforestation.expression.debrujin.*
import deforestation.expression.simple.*
import deforestation.expression.simple.Case
import deforestation.expression.simple.Constructor
import deforestation.expression.simple.FunctionCall
import deforestation.expression.simple.Variable

val Expression.toDeBrujin: DeBrujinExpression get() = toDeBrujin(HashMap())

fun Expression.toDeBrujin(initialVariables: List<String>): DeBrujinExpression =
    toDeBrujin(buildMap { initialVariables.forEachIndexed { i, s -> put(s, initialVariables.size - i - 1) } })

private fun Expression.toDeBrujin(variables: Map<String, Int>): DeBrujinExpression = when (this) {
    is Variable -> variables[name]?.let { BoundedVariableImpl(it, name) } ?: FreeVariableImpl(name)
    is Constructor -> ConstructorImpl(name, arguments.map { it.toDeBrujin(variables) })
    is FunctionCall -> FunctionCallImpl(name, arguments.map { it.toDeBrujin(variables) })
    is Case -> CaseImpl(
        scrutinee.toDeBrujin(variables),
        patterns.map { branch ->
            val newVariables = variables.mapValuesTo(HashMap()) { (_, v) -> v + branch.bounded }
            branch.pattern.variables.forEachIndexed { i, v -> newVariables[v] = branch.bounded - i - 1 }
            CaseBranch(branch.pattern, branch.expression.toDeBrujin(newVariables))
        },
        defaultPattern?.let { branch ->
            val newVariables = variables.mapValuesTo(HashMap()) { (_, v) -> v + branch.bounded }
            newVariables[branch.name] = 0
            DefaultCaseBranch(branch.name, branch.expression.toDeBrujin(newVariables))
        }
    )
}
