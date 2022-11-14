package deforestation.expression.debrujin

sealed interface Identifier {
    fun refers(variable: Variable, depth: Int): Boolean
    fun variable(): Variable
}

data class NamedIdentifier(val name: String) : Identifier {
    override fun refers(variable: Variable, depth: Int) = (variable as? FreeVariable)?.name == name
    override fun variable() = FreeVariable(name)
}

data class IndexedIdentifier(val index: Int) : Identifier {
    override fun refers(variable: Variable, depth: Int) = (variable as? BoundedVariable)?.index == index + depth
    override fun variable(): Variable = BoundedVariable(index)
}


fun Variable.identifier(depth: Int): Identifier = when (this) {
    is BoundedVariable -> identifier(depth)
    is FreeVariable -> identifier(depth)
}

fun FreeVariable.identifier(depth: Int) = NamedIdentifier(name)

fun BoundedVariable.identifier(depth: Int) = IndexedIdentifier(index - depth)


fun Variable.free(depth: Int): Boolean = when (this) {
    is BoundedVariable -> free(depth)
    is FreeVariable -> free(depth)
}

fun FreeVariable.free(depth: Int) = true

fun BoundedVariable.free(depth: Int) = index >= depth

