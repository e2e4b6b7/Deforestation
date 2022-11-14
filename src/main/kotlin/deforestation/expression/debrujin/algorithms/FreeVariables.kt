package deforestation.expression.debrujin.algorithms

import deforestation.expression.debrujin.*

val DeBrujinExpression.freeVariables: List<Variable>
    get() = ArrayList<Variable>().apply { freeVariables(0, this) }

fun DeBrujinExpression.freeVariables(depth: Int, variables: MutableList<Variable>) {
    when (this) {
        is FreeVariable -> variables.add(this)
        is BoundedVariable -> if (index >= depth) variables.add(BoundedVariableImpl(index - depth, name))
        is Constructor -> arguments.forEach { it.freeVariables(depth, variables) }
        is FunctionCall -> arguments.forEach { it.freeVariables(depth, variables) }
        is Case -> {
            scrutinee.freeVariables(depth, variables)
            allBranches.forEach { it.expression.freeVariables(depth + it.bounded, variables) }
        }
    }
}
