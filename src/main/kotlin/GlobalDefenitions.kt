data class ComputationContext(val functions: Map<String, Function>)

data class Program(val functions: List<Function>, val expression: Expression)

data class Function(val name: String, val variables: List<String>, val expression: Expression)
