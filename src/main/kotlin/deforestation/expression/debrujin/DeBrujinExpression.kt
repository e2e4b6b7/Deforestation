package deforestation.expression.debrujin

import deforestation.expression.Branches

sealed interface DeBrujinExpression

sealed interface Variable : DeBrujinExpression

data class BoundedVariable(val index: Int) : Variable

data class FreeVariable(val name: String) : Variable

data class Case(
    val scrutinee: DeBrujinExpression,
    val branches: Branches<DeBrujinExpression>
) : DeBrujinExpression

data class Constructor(
    val name: String,
    val arguments: List<DeBrujinExpression>
) : DeBrujinExpression

data class FunctionCall(
    val name: String,
    val arguments: List<DeBrujinExpression>
) : DeBrujinExpression

val UNDEFINED = FreeVariable("undefined")
