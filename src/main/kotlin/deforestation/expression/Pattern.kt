package deforestation.expression

data class Pattern(val constructor: String, val variables: List<String>)

interface AnyCaseBranch<out Expression> {
    val expression: Expression
    val bounded: Int
}

data class CaseBranch<out Expression>(val pattern: Pattern, override val expression: Expression) :
    AnyCaseBranch<Expression> {
    override val bounded: Int get() = pattern.variables.size
}

data class DefaultCaseBranch<out Expression>(val name: String, override val expression: Expression) :
    AnyCaseBranch<Expression> {
    override val bounded: Int get() = 1
}

data class Branches<Expression>(
    val commonBranches: List<CaseBranch<Expression>>,
    val defaultBranch: DefaultCaseBranch<Expression>?,
    val allBranches: List<AnyCaseBranch<Expression>> = commonBranches + listOfNotNull(defaultBranch)
) : List<AnyCaseBranch<Expression>> by allBranches {
    inline fun mapExpressions(mapper: (AnyCaseBranch<Expression>) -> Expression) = Branches(
        commonBranches.map { it.copy(expression = mapper(it)) },
        defaultBranch?.let { it.copy(expression = mapper(it)) }
    )
}