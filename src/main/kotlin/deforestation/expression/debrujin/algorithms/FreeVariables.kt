package deforestation.expression.debrujin.algorithms

import deforestation.expression.debrujin.*

val DeBrujinExpression.freeVariables: List<Identifier>
    get() = ArrayList<Identifier>().apply { freeVariables(0, this) }

fun DeBrujinExpression.freeVariables(depth: Int, variables: MutableList<Identifier>) {
    when (this) {
        is Variable -> if (free(depth)) variables.add(identifier(depth))
        is Constructor -> arguments.forEach { it.freeVariables(depth, variables) }
        is FunctionCall -> arguments.forEach { it.freeVariables(depth, variables) }
        is Case -> {
            scrutinee.freeVariables(depth, variables)
            branches.forEach { it.expression.freeVariables(depth + it.bounded, variables) }
        }
    }
}
