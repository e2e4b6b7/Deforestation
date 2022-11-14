package deforestation.expression.debrujin

import deforestation.expression.CaseBranch
import deforestation.expression.DefaultCaseBranch

sealed interface DeBrujinExpression

sealed interface Variable : DeBrujinExpression {
//    val name: String
}

sealed interface BoundedVariable : Variable {
    val index: Int
}

sealed interface FreeVariable : Variable {
    val name: String
}

sealed interface Case : DeBrujinExpression {
    val scrutinee: DeBrujinExpression
    val branches: List<CaseBranch<DeBrujinExpression>>
    val defaultBranch: DefaultCaseBranch<DeBrujinExpression>?
}

sealed interface Constructor : DeBrujinExpression {
    val name: String
    val arguments: List<DeBrujinExpression>
}

sealed interface FunctionCall : DeBrujinExpression {
    val name: String
    val arguments: List<DeBrujinExpression>
}

/** Impls */

data class BoundedVariableImpl(override val index: Int, override val name: String) : BoundedVariable

data class FreeVariableImpl(override val name: String) : FreeVariable

data class CaseImpl(
    override val scrutinee: DeBrujinExpression,
    override val branches: List<CaseBranch<DeBrujinExpression>>,
    override val defaultBranch: DefaultCaseBranch<DeBrujinExpression>?
) : Case

data class ConstructorImpl(
    override val name: String,
    override val arguments: List<DeBrujinExpression>
) : Constructor

data class FunctionCallImpl(
    override val name: String,
    override val arguments: List<DeBrujinExpression>
) : FunctionCall

/** functions */

val Case.allBranches
    get() = defaultBranch?.let { branches.asSequence() + sequenceOf(it) } ?: branches.asSequence()
