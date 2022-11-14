package deforestation.expression.debrujin.algorithms

import deforestation.expression.debrujin.*

fun DeBrujinExpression.subst(expression: DeBrujinExpression): DeBrujinExpression =
    subst(0, expression)

fun DeBrujinExpression.subst(substIndex: Int, expression: DeBrujinExpression): DeBrujinExpression =
    subst(0, substIndex, expression)

private fun DeBrujinExpression.subst(
    depth: Int,
    substIndex: Int,
    expression: DeBrujinExpression
): DeBrujinExpression = when (this) {
    is FreeVariable -> this
    is BoundedVariable -> if (index == substIndex + depth) expression.shift(depth) else this
    is Constructor -> Constructor(name, arguments.map { it.subst(depth, substIndex, expression) })
    is FunctionCall -> FunctionCall(name, arguments.map { it.subst(depth, substIndex, expression) })
    is Case -> Case(
        scrutinee.subst(depth, substIndex, expression),
        branches.mapExpressions { it.expression.subst(depth + it.bounded, substIndex, expression) },
    )
}
