package deforestation.expression.branches.algorithms

import deforestation.expression.branches.Branches

infix fun <E> Branches<E>.sameStructure(other: Branches<E>): Boolean =
    commonBranches.size == other.commonBranches.size &&
            commonBranches.zip(commonBranches).all { (b1, b2) ->
                b1.pattern.constructor == b2.pattern.constructor &&
                        b1.pattern.variables.size == b2.pattern.variables.size
            } &&
            (defaultBranch == null) == (other.defaultBranch == null)
