package lox

interface LoxCallable {
    val arity: Int
    fun call(interpreter: Interpreter, arguments: List<Any?>): Any?
}

sealed class NativeCallable : LoxCallable {
    override fun toString(): String = "<native fn>"

    object Clock : NativeCallable() {
        override val arity: Int = 0
        override fun call(interpreter: Interpreter, arguments: List<Any?>): Any = System.currentTimeMillis().toDouble() / 1000.0
    }
}

class LoxFunction(
    private val declaration: Stmt.Function,
    private val closure: Environment
) : LoxCallable {

    override val arity: Int = declaration.params.size

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(closure)
        declaration.params.zip(arguments).forEach { (param, arg) ->
            environment.define(param.lexeme, arg)
        }

        return try {
            interpreter.executeBlock(declaration.body, environment)
            null // implicit nil return
        } catch (r: Return) {
            r.value // explicit return
        }
    }

    override fun toString(): String = "<fn ${declaration.name.lexeme}>"

    class Return(val value: Any?) : RuntimeException(null, null, false, false)
}
