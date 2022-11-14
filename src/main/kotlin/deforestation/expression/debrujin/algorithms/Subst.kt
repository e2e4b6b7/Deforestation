package deforestation.expression.debrujin.algorithms

import deforestation.expression.debrujin.*

fun DeBrujinExpression.subst(expression: DeBrujinExpression): DeBrujinExpression =
    subst(0, expression)

fun DeBrujinExpression.subst(substIndex: Int, expression: DeBrujinExpression): DeBrujinExpression =
    subst(0, substIndex, expression)

private fun DeBrujinExpression.subst(
    curDepth: Int,
    substIndex: Int,
    expression: DeBrujinExpression
): DeBrujinExpression = when (this) {
    is FreeVariable -> this
    is BoundedVariable -> if (index == substIndex + curDepth) expression.shift(curDepth) else this
    is Constructor -> ConstructorImpl(name, arguments.map { it.subst(curDepth, substIndex, expression) })
    is FunctionCall -> FunctionCallImpl(name, arguments.map { it.subst(curDepth, substIndex, expression) })
    is Case -> CaseImpl(
        scrutinee.subst(curDepth, substIndex, expression),
        branches.map { it.copy(expression = it.expression.subst(curDepth + it.bounded, substIndex, expression)) },
        defaultBranch?.let { it.copy(expression = it.expression.subst(curDepth + it.bounded, substIndex, expression)) }
    )
}
