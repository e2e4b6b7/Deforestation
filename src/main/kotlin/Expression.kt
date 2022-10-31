sealed interface Expression
sealed interface TreelessExpression : Expression

data class Variable(val name: String) : Expression, TreelessExpression

data class Constructor(val name: String, val arguments: List<Expression>) : Expression, TreelessExpression

interface FunctionCall : Expression {
    val name: String
    val arguments: List<Expression>
}

interface TreelessFunctionCall : FunctionCall, Expression {
    override val name: String
    override val arguments: List<Variable>
}

interface Case : Expression {
    val scrutinee: Expression
    val patterns: List<CaseBranch>
    val defaultPattern: DefaultCaseBranch?
}

interface TreelessCase : Case, TreelessExpression {
    override val scrutinee: Variable
    override val patterns: List<CaseBranch>
    override val defaultPattern: DefaultCaseBranch?
}

/*** Impls */

data class CaseImpl(
    override val scrutinee: Expression,
    override val patterns: List<CaseBranch>,
    override val defaultPattern: DefaultCaseBranch?
) : Case

data class TreelessCaseImpl(
    override val scrutinee: Variable,
    override val patterns: List<CaseBranch>,
    override val defaultPattern: DefaultCaseBranch?
) : TreelessCase

data class FunctionCallImpl(
    override val name: String,
    override val arguments: List<Expression>
) : FunctionCall

data class TreelessFunctionCallImpl(
    override val name: String,
    override val arguments: List<Variable>
) : TreelessFunctionCall
