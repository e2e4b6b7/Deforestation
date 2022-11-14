package deforestation.expression.debrujin.algorithms

import deforestation.expression.debrujin.*

infix fun DeBrujinExpression.alphaEq(other: DeBrujinExpression): Boolean = alphaEqMap(other) != null

infix fun DeBrujinExpression.alphaEqMap(other: DeBrujinExpression): Map<String, String>? {
    val freePairs = ArrayList<Pair<String, String>>()
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
    accum: MutableList<Pair<String, String>>
): Boolean = when (e1) {
    is BoundedVariable -> when (e2) {
        is BoundedVariable -> {
            if (e1.index >= depth && e2.index >= depth) {
                accum.add(e1.name to e2.name)
                true
            } else {
                e1.index == e2.index
            }
        }

        is FreeVariable -> {
            accum.add(e1.name to e2.name)
            e1.index >= depth
        }

        else -> false
    }

    is FreeVariable -> when (e2) {
        is BoundedVariable -> {
            accum.add(e1.name to e2.name)
            e2.index >= depth
        }

        is FreeVariable -> {
            accum.add(e1.name to e2.name)
            true
        }

        else -> false
    }

    is Constructor -> (e2 as? Constructor)?.let {
        e1.name == e2.name && e1.arguments.zip(e2.arguments).all { (a1, a2) -> alphaEq(a1, a2, depth, accum) }
    }

    is FunctionCall -> (e2 as? FunctionCall)?.let {
        e1.name == e2.name && e1.arguments.zip(e2.arguments).all { (a1, a2) -> alphaEq(a1, a2, depth, accum) }
    }

    is Case -> (e2 as? Case)?.let {
        alphaEq(e1.scrutinee, e2.scrutinee, depth, accum) &&

                e1.branches.size == e2.branches.size &&
                e1.branches.zip(e2.branches).all { (b1, b2) -> b1.pattern.constructor == b2.pattern.constructor } &&
                e1.branches.zip(e2.branches)
                    .all { (b1, b2) -> b1.pattern.variables.size == b2.pattern.variables.size } &&
                e1.branches.zip(e2.branches)
                    .all { (b1, b2) ->
                        alphaEq(b1.expression, b2.expression, depth + b1.pattern.variables.size, accum)
                    } &&

                (e1.defaultBranch == null) == (e2.defaultBranch == null) &&
                listOfNotNull(e1.defaultBranch).zip(listOfNotNull(e2.defaultBranch))
                    .all { (b1, b2) -> alphaEq(b1.expression, b2.expression, depth + 1, accum) }

    }
} ?: false
