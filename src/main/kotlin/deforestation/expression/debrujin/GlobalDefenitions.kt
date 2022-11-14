package deforestation.expression.debrujin

interface Function {
    val name: String
    val variables: List<String>
    val expression: DeBrujinExpression
}
interface Program {
    val functions: List<Function>
    val expression: DeBrujinExpression
}

data class ProgramImpl(override val functions: List<Function>, override val expression: DeBrujinExpression) : Program

data class TreelessProgram(
    override val functions: List<TreelessFunction>,
    override val expression: DeBrujinTreelessExpression
) : Program

data class FunctionImpl(
    override val name: String,
    override val variables: List<String>,
    override val expression: DeBrujinExpression
) : Function

data class TreelessFunction(
    override val name: String,
    override val variables: List<String>,
    override val expression: DeBrujinTreelessExpression
) : Function

