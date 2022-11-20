package deforestation.expression.debrujin.algorithms

import deforestation.expression.debrujin.DeBrujinExpression
import deforestation.expression.debrujin.IndexedIdentifier

fun DeBrujinExpression.resolve(expression: DeBrujinExpression): DeBrujinExpression =
    subst(expression.shift(1)).shift(-1)

fun DeBrujinExpression.resolve(expressions: List<DeBrujinExpression>): DeBrujinExpression =
    subst(expressions.asReversed().mapIndexed { i, e -> IndexedIdentifier(i) to e.shift(expressions.size) })
        .shift(-expressions.size)
