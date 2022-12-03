package deforestation.expression.branches

import deforestation.expression.debrujin.BoundedVariable
import deforestation.expression.debrujin.Constructor
import deforestation.expression.debrujin.DeBrujinExpression

data class Pattern(val constructor: String, val variables: List<String>)

interface AnyCaseBranch<out Expression> {
    val expression: Expression
    val bounded: Int

    fun boundedExpression(): DeBrujinExpression
}

data class CaseBranch<out Expression>(val pattern: Pattern, override val expression: Expression) :
    AnyCaseBranch<Expression> {
    override val bounded: Int get() = pattern.variables.size
    override fun boundedExpression(): DeBrujinExpression =
        Constructor(pattern.constructor, List(bounded) { BoundedVariable(it) }.asReversed())
}

data class DefaultCaseBranch<out Expression>(val name: String, override val expression: Expression) :
    AnyCaseBranch<Expression> {
    override val bounded: Int get() = 1
    override fun boundedExpression(): DeBrujinExpression = BoundedVariable(0)
}

data class Branches<Expression>(
    val commonBranches: List<CaseBranch<Expression>>,
    val defaultBranch: DefaultCaseBranch<Expression>?,
    val allBranches: List<AnyCaseBranch<Expression>> = commonBranches + listOfNotNull(defaultBranch)
) : List<AnyCaseBranch<Expression>> by allBranches {
    inline fun mapExpressions(mapper: (AnyCaseBranch<Expression>) -> Expression) =
        Branches(commonBranches.map { it.copy(expression = mapper(it)) },
            defaultBranch?.let { it.copy(expression = mapper(it)) })

    fun withExpressions(expressions: List<Expression>): Branches<Expression> {
        require(expressions.size == size)
        return Branches(
            commonBranches.zip(expressions).map { (br, e) -> br.copy(expression = e) },
            defaultBranch?.copy(expression = expressions.last())
        )
    }
}
