package deforestation.expression.debrujin.algorithms

import deforestation.expression.debrujin.*

fun DeBrujinExpression.shift(level: Int): DeBrujinExpression = shift(level, 0)

private fun DeBrujinExpression.shift(level: Int, curDepth: Int): DeBrujinExpression = when (this) {
    is Constructor -> ConstructorImpl(name, arguments.map { it.shift(level, curDepth) })
    is FunctionCall -> FunctionCallImpl(name, arguments.map { it.shift(level, curDepth) })
    is BoundedVariable -> if (index >= curDepth) BoundedVariableImpl(index + level, name) else this
    is FreeVariable -> this
    is Case -> CaseImpl(
        scrutinee.shift(level, curDepth),
        branches.map { it.copy(expression = it.expression.shift(level, curDepth + it.bounded)) },
        defaultBranch?.let { it.copy(expression = it.expression.shift(level, curDepth + it.bounded)) }
    )
}

fun DeBrujinTreelessExpression.shift(level: Int): DeBrujinTreelessExpression = shift(level, 0)

private fun DeBrujinTreelessExpression.shift(level: Int, curDepth: Int): DeBrujinTreelessExpression = when (this) {
    is TreelessConstructor -> TreelessConstructor(name, arguments.map { it.shift(level, curDepth) })
    is TreelessFunctionCall -> TreelessFunctionCall(name, arguments.map { it.shift(level, curDepth) })
    is TreelessVariable -> shift(level, curDepth)
    is TreelessCase -> TreelessCase(
        scrutinee.shift(level, curDepth),
        branches.map { it.copy(expression = it.expression.shift(level, curDepth + it.bounded)) },
        defaultBranch?.let { it.copy(expression = it.expression.shift(level, curDepth + it.bounded)) }
    )
}

fun TreelessVariable.shift(level: Int, curDepth: Int = 0): TreelessVariable = when (this) {
    is TreelessBoundedVariable -> if (index >= curDepth) TreelessBoundedVariable(index + level, name) else this
    is TreelessFreeVariable -> this
}
