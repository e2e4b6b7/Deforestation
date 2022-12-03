package deforestation.expression.debrujin.algorithms

import deforestation.expression.debrujin.*

typealias Substitution = List<Pair<Identifier, DeBrujinExpression>>
private typealias MutableSubstitution = MutableList<Pair<Identifier, DeBrujinExpression>>

data class Generalization(
    val expression: DeBrujinExpression,
    val substitutionA: Substitution,
    val substitutionB: Substitution,
)

infix fun DeBrujinExpression.generalization(other: DeBrujinExpression): Generalization {
    val s1 = mutableListOf<Pair<Identifier, DeBrujinExpression>>()
    val s2 = mutableListOf<Pair<Identifier, DeBrujinExpression>>()
    val ge = generalization(this, s1, other, s2, 0)
    return Generalization(ge, s1, s2)
}

private fun generalization(
    e1: DeBrujinExpression,
    s1: MutableSubstitution,
    e2: DeBrujinExpression,
    s2: MutableSubstitution,
    depth: Int
): DeBrujinExpression = when {

    e1 is Constructor && e2 is Constructor && e1.name == e2.name && e1.arguments.size == e2.arguments.size ->
        Constructor(e1.name, e1.arguments.zip(e2.arguments).map { (a1, a2) -> generalization(a1, s1, a2, s2, depth) })

    e1 is FunctionCall && e2 is FunctionCall && e1.name == e2.name && e1.arguments.size == e2.arguments.size ->
        FunctionCall(e1.name, e1.arguments.zip(e2.arguments).map { (a1, a2) -> generalization(a1, s1, a2, s2, depth) })

    else -> {
        val eq = s1.zip(s2).find { (ss1, ss2) -> ss1.second == e1 && ss2.second == e2 }
        if (eq != null) {
            eq.first.first.variable()
        } else {
            FreeVariable("gen${s1.size}").also {
                s1.add(it.identifier(depth) to e1.shift(-depth))
                s2.add(it.identifier(depth) to e2.shift(-depth))
            }
        }
    }
}
