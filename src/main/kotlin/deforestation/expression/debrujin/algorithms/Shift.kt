package deforestation.expression.debrujin.algorithms

import deforestation.expression.debrujin.*

fun DeBrujinExpression.shift(level: Int): DeBrujinExpression = shift(level, 0)

fun DeBrujinExpression.shift(level: Int, depth: Int): DeBrujinExpression = when (this) {
    is Constructor -> Constructor(name, arguments.map { it.shift(level, depth) })
    is FunctionCall -> FunctionCall(name, arguments.map { it.shift(level, depth) })
    is BoundedVariable -> if (free(depth)) BoundedVariable(index + level) else this
    is FreeVariable -> this
    is Case -> Case(
        scrutinee.shift(level, depth),
        branches.mapExpressions { it.expression.shift(level, depth + it.bounded) },
    )
}
