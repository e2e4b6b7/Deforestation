package deforestation.expression.debrujin

import deforestation.expression.CaseBranch
import deforestation.expression.DefaultCaseBranch

sealed interface DeBrujinTreelessExpression : DeBrujinExpression

sealed interface TreelessVariable : DeBrujinTreelessExpression, Variable

data class TreelessBoundedVariable(override val index: Int, override val name: String) : TreelessVariable, BoundedVariable

data class TreelessFreeVariable(override val name: String) : TreelessVariable, FreeVariable

data class TreelessCase(
     override val scrutinee: TreelessVariable,
     override val branches: List<CaseBranch<DeBrujinTreelessExpression>>,
     override val defaultBranch: DefaultCaseBranch<DeBrujinTreelessExpression>?
) : DeBrujinTreelessExpression, Case

data class TreelessConstructor(
     override val name: String,
     override val arguments: List<DeBrujinTreelessExpression>
) : DeBrujinTreelessExpression, Constructor


data class TreelessFunctionCall(
     override val name: String,
     override val arguments: List<TreelessVariable>
) : DeBrujinTreelessExpression, FunctionCall
