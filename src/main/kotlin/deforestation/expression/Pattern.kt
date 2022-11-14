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
