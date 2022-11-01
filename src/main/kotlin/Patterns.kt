data class Pattern(val constructor: String, val variables: List<String>) {
    fun binds(name: String): Boolean = variables.contains(name)
}

data class CaseBranch(val pattern: Pattern, val expression: Expression) {
    fun subst(name: String, expression: Expression) =
        if (pattern.binds(name)) this else CaseBranch(pattern, expression.subst(name, expression))
}

data class DefaultCaseBranch(val name: String, val expression: Expression) {
    fun subst(name: String, expression: Expression) =
        if (this.name == name) this else DefaultCaseBranch(name, expression.subst(name, expression))
}
