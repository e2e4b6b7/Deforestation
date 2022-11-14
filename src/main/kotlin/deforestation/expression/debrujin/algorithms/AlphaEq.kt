package deforestation.expression.debrujin.algorithms

import deforestation.expression.debrujin.*

infix fun DeBrujinExpression.alphaEq(other: DeBrujinExpression): Boolean = alphaEqMap(other) != null

infix fun DeBrujinExpression.alphaEqMap(other: DeBrujinExpression): Map<Identifier, Identifier>? {
    val freePairs = ArrayList<Pair<Identifier, Identifier>>()
    val equalStructure = alphaEq(this, other, 0, freePairs)
    if (!equalStructure) return null
    val optimisticFreeVariablesMap = freePairs.toMap()
    val freeVariablesUsageEqual = freePairs.all { (v1, v2) -> optimisticFreeVariablesMap[v1] == v2 }
    if (!freeVariablesUsageEqual) return null
    return optimisticFreeVariablesMap
}

private fun alphaEq(
    e1: DeBrujinExpression,
    e2: DeBrujinExpression,
    depth: Int,
    accum: MutableList<Pair<Identifier, Identifier>>
): Boolean = when {
    e1 is BoundedVariable && e2 is BoundedVariable ->
        if (e1.free(depth) && e2.free(depth)) {
            accum.link(e1, e2, depth)
            true
        } else {
            e1.index == e2.index
        }

    e1 is Variable && e2 is Variable ->
        if (e1.free(depth) && e2.free(depth)) {
            accum.link(e1, e2, depth)
            true
        } else {
            false
        }

    e1 is Constructor && e2 is Constructor ->
        e1.name == e2.name && e1.arguments.zip(e2.arguments).all { (a1, a2) -> alphaEq(a1, a2, depth, accum) }

    e1 is FunctionCall && e2 is FunctionCall ->
        e1.name == e2.name && e1.arguments.zip(e2.arguments).all { (a1, a2) -> alphaEq(a1, a2, depth, accum) }

    e1 is Case && e2 is Case ->
        alphaEq(e1.scrutinee, e2.scrutinee, depth, accum) &&

                e1.branches.commonBranches.size == e2.branches.commonBranches.size &&
                e1.branches.commonBranches.zip(e2.branches.commonBranches)
                    .all { (b1, b2) -> b1.pattern.constructor == b2.pattern.constructor } &&
                e1.branches.commonBranches.zip(e2.branches.commonBranches)
                    .all { (b1, b2) -> b1.pattern.variables.size == b2.pattern.variables.size } &&

                e1.branches.size == e2.branches.size &&
                e1.branches.zip(e2.branches)
                    .all { (b1, b2) -> alphaEq(b1.expression, b2.expression, depth + b1.bounded, accum) }

    else -> false
}

fun MutableList<Pair<Identifier, Identifier>>.link(e1: Variable, e2: Variable, depth: Int) {
    add(e1.identifier(depth) to e2.identifier(depth))
}
