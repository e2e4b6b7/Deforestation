//package deforestation.expression.debrujin.algorithms
//
//import deforestation.expression.debrujin.*
//
//fun DeBrujinTreelessExpression.remap(from: TreelessVariable, to: TreelessVariable): DeBrujinTreelessExpression = when (from) {
//    is TreelessBoundedVariable -> remap(from, to)
//    is TreelessFreeVariable -> remap(from, to)
//}
//
//fun DeBrujinTreelessExpression.remap(from: TreelessBoundedVariable, to: TreelessVariable): DeBrujinTreelessExpression =
//    remap(0, from.index, to)
//
//fun DeBrujinTreelessExpression.remap(from: TreelessFreeVariable, to: TreelessVariable): DeBrujinTreelessExpression =
//    remapFree(0, from.name, to)
//
//private fun DeBrujinTreelessExpression.remap(
//    curDepth: Int,
//    remapIndex: Int,
//    to: TreelessVariable
//): DeBrujinTreelessExpression = when (this) {
//    is TreelessConstructor -> TreelessConstructor(name, arguments.map { it.remap(curDepth, remapIndex, to) })
//    is TreelessFunctionCall -> TreelessFunctionCall(name, arguments.map { it.remap(curDepth, remapIndex, to) })
//    is TreelessCase -> TreelessCase(
//        scrutinee.remap(curDepth, remapIndex, to),
//        branches.map { it.copy(expression = it.expression.remap(curDepth + it.bounded, remapIndex, to)) },
//        defaultBranch?.let { it.copy(expression = it.expression.remap(curDepth + it.bounded, remapIndex, to)) }
//    )
//
//    is TreelessVariable -> remap(curDepth, remapIndex, to)
//}
//
//private fun TreelessVariable.remap(
//    curDepth: Int,
//    remapIndex: Int,
//    to: TreelessVariable
//): TreelessVariable = when (this) {
//    is TreelessFreeVariable -> this
//    is TreelessBoundedVariable -> if (index == remapIndex + curDepth) to.shift(curDepth) else this
//}
//
//private fun DeBrujinTreelessExpression.remapFree(
//    curDepth: Int,
//    remapName: String,
//    to: TreelessVariable
//): DeBrujinTreelessExpression = when (this) {
//    is TreelessConstructor -> TreelessConstructor(name, arguments.map { it.remapFree(curDepth, remapName, to) })
//    is TreelessFunctionCall -> TreelessFunctionCall(name, arguments.map { it.remapFree(curDepth, remapName, to) })
//    is TreelessCase -> TreelessCase(
//        scrutinee.remapFree(curDepth, remapName, to),
//        branches.map { it.copy(expression = it.expression.remapFree(curDepth + it.bounded, remapName, to)) },
//        defaultBranch?.let { it.copy(expression = it.expression.remapFree(curDepth + it.bounded, remapName, to)) }
//    )
//
//    is TreelessVariable -> remapFree(curDepth, remapName, to)
//}
//
//private fun TreelessVariable.remapFree(
//    curDepth: Int,
//    remapName: String,
//    to: TreelessVariable
//): TreelessVariable = when (this) {
//    is TreelessFreeVariable -> if (name == remapName) to.shift(curDepth) else this
//    is TreelessBoundedVariable -> this
//}
