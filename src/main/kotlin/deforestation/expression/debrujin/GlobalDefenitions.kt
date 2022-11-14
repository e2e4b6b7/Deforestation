package deforestation.expression.debrujin

data class Program(val functions: List<Function>, val expression: DeBrujinExpression)

data class Function(val name: String, val variables: List<String>, val expression: DeBrujinExpression)
