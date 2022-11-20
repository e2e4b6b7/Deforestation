package deforestation.expression.debrujin.algorithms

import deforestation.expression.debrujin.*

fun DeBrujinExpression.subst(expression: DeBrujinExpression): DeBrujinExpression =
    subst(0, expression)

fun DeBrujinExpression.subst(substIndex: Int, expression: DeBrujinExpression): DeBrujinExpression =
    subst(IndexedIdentifier(substIndex), expression)

fun DeBrujinExpression.subst(identifier: Identifier, expression: DeBrujinExpression): DeBrujinExpression =
    subst(listOf(identifier to expression))

fun DeBrujinExpression.subst(substitutions: List<Pair<Identifier, DeBrujinExpression>>): DeBrujinExpression =
    subst(0, substitutions)

private fun DeBrujinExpression.subst(
    depth: Int,
    substitutions: List<Pair<Identifier, DeBrujinExpression>>
): DeBrujinExpression = when (this) {
    is Variable -> substitutions.find { it.first.refers(this, depth) }?.second?.shift(depth) ?: this
    is Constructor -> Constructor(name, arguments.map { it.subst(depth, substitutions) })
    is FunctionCall -> FunctionCall(name, arguments.map { it.subst(depth, substitutions) })
    is Case -> Case(
        scrutinee.subst(depth, substitutions),
        branches.mapExpressions { it.expression.subst(depth + it.bounded, substitutions) },
    )
}
