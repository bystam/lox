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

    object Array : NativeCallable() {
        override val arity: Int = 1

        override fun call(interpreter: Interpreter, arguments: List<Any?>): Any {
            val length = arguments.single() as? Double ?: TODO("Handle goodly")
            return BuiltinArray(arrayOfNulls(length.toInt()))
        }
    }
}

class LoxFunction(
    private val declaration: Stmt.Function,
    private val closure: Environment,
    private val isInitializer: Boolean
) : LoxCallable {

    override val arity: Int = declaration.params.size

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(closure)
        declaration.params.zip(arguments).forEach { (param, arg) ->
            environment.define(param.lexeme, arg)
        }

        return try {
            interpreter.executeBlock(declaration.body, environment)

            // implicit return
            if (isInitializer)
                closure.getAt(0, "this") // init always returns this
            else
                null // implicit nil return when not declared in function
        } catch (r: Return) {  // explicit return
            if (isInitializer)
                closure.getAt(0, "this") // init always returns this
            else
                r.value
        }
    }

    override fun toString(): String = "<fn ${declaration.name.lexeme}>"

    fun bind(instance: LoxObject): LoxFunction {
        val environment = Environment(closure)
        environment.define("this", instance)
        return LoxFunction(declaration, environment, isInitializer)
    }

    class Return(val value: Any?) : RuntimeException(null, null, false, false)
}
