package deforestation.expression.simple.algorithms

import deforestation.expression.branches.Branches
import deforestation.expression.branches.CaseBranch
import deforestation.expression.branches.DefaultCaseBranch
import deforestation.expression.debrujin.BoundedVariable
import deforestation.expression.debrujin.DeBrujinExpression
import deforestation.expression.debrujin.FreeVariable
import deforestation.expression.simple.*

val Expression.toDeBrujin: DeBrujinExpression get() = toDeBrujin(HashMap())

fun Expression.toDeBrujin(initialVariables: List<String>): DeBrujinExpression =
    toDeBrujin(buildMap { initialVariables.forEachIndexed { i, s -> put(s, initialVariables.size - i - 1) } })

private fun Expression.toDeBrujin(variables: Map<String, Int>): DeBrujinExpression = when (this) {
    is Variable -> variables[name]?.let { BoundedVariable(it) } ?: FreeVariable(name)
    is Constructor -> deforestation.expression.debrujin.Constructor(name, arguments.map { it.toDeBrujin(variables) })
    is FunctionCall -> deforestation.expression.debrujin.FunctionCall(name, arguments.map { it.toDeBrujin(variables) })
    is Case -> deforestation.expression.debrujin.Case(
        scrutinee.toDeBrujin(variables),
        Branches(
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
    )
}
