package deforestation.expression.debrujin.algorithms

import deforestation.expression.debrujin.DeBrujinExpression

fun DeBrujinExpression.resolve(expression: DeBrujinExpression): DeBrujinExpression =
    subst(expression.shift(1)).shift(-1)

fun DeBrujinExpression.resolve(expressions: List<DeBrujinExpression>): DeBrujinExpression =
    expressions.asReversed().foldIndexed(this) { i, acc, arg -> acc.subst(i, arg.shift(expressions.size)) }
        .shift(-expressions.size)
