package deforestation.expression.debrujin.algorithms

import deforestation.expression.branches.algorithms.sameStructure
import deforestation.expression.debrujin.*

infix fun DeBrujinExpression.homeomorphicEmbedded(other: DeBrujinExpression): Boolean =
    homeomorphicEmbeddedEqual(other)

private infix fun DeBrujinExpression.homeomorphicEmbeddedAny(other: DeBrujinExpression): Boolean =
    homeomorphicEmbeddedDecrease(other) || homeomorphicEmbeddedEqual(other)


private infix fun DeBrujinExpression.homeomorphicEmbeddedEqual(other: DeBrujinExpression): Boolean = when (this) {
    is FunctionCall -> when (other) {
        is FunctionCall -> other.name == name &&
                arguments.zip(other.arguments).all { (f, s) -> f homeomorphicEmbeddedAny s }

        else -> false
    }

    is Variable -> when (other) {
        is Variable -> true
        else -> false
    }

    is Case -> when (other) {
        is Case -> scrutinee homeomorphicEmbeddedEqual other.scrutinee &&
                branches sameStructure other.branches &&
                branches.zip(other.branches).all { (b1, b2) -> b1.expression homeomorphicEmbeddedAny b2.expression }

        else -> false
    }

    is Constructor -> when (other) {
        is Constructor -> other.name == name &&
                arguments.zip(other.arguments).all { (f, s) -> f homeomorphicEmbeddedAny s }

        else -> false
    }
}

private fun DeBrujinExpression.homeomorphicEmbeddedDecrease(other: DeBrujinExpression): Boolean = when (other) {
    is Case -> other.branches.allBranches.any { homeomorphicEmbedded(it.expression) }
    is Constructor -> other.arguments.any { homeomorphicEmbeddedAny(it) }
    is FunctionCall -> other.arguments.any { homeomorphicEmbeddedAny(it) }
    else -> false
}
