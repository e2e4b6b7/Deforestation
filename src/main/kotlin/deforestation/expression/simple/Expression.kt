package deforestation.expression.simple

import deforestation.expression.branches.CaseBranch
import deforestation.expression.branches.DefaultCaseBranch

sealed interface Expression

data class Variable(val name: String) : Expression

data class Constructor(val name: String, val arguments: List<Expression>) : Expression

data class Case(
    val scrutinee: Expression,
    val patterns: List<CaseBranch<Expression>>,
    val defaultPattern: DefaultCaseBranch<Expression>?
) : Expression

data class FunctionCall(
    val name: String,
    val arguments: List<Expression>
) : Expression
