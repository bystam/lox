package lox

import java.util.Arrays

interface LoxInstance {
    fun get(name: Token): Any?
    fun set(name: Token, value: Any?)
}

class BuiltinArray(
    private val array: Array<Any?>
) : LoxInstance {

    override fun toString(): String = "<native array>"

    override fun get(name: Token): Any? = when (name.lexeme) {
        "length" -> array.size
        "get" -> object : LoxCallable {
            override val arity: Int = 1
            override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
                val index = arguments.single() as? Double ?: throw RuntimeError(name, "Index must be a number.")
                return array[index.toInt()]
            }
        }
        "set" -> object : LoxCallable {
            override val arity: Int = 2
            override fun call(interpreter: Interpreter, arguments: List<Any?>): Any {
                val index = arguments.first() as? Double ?: throw RuntimeError(name, "Index must be a number.")
                val value = arguments.last()
                array[index.toInt()] = value
                return Unit
            }
        }
        else -> throw RuntimeError(name, "Unknown method on array: ${name.lexeme}.")
    }

    override fun set(name: Token, value: Any?) {
        throw RuntimeError(name, "Can't set properties on arrays.")
    }
}

class LoxObject(val cls: LoxClass) : LoxInstance {

    private val fields: MutableMap<String, Any?> = mutableMapOf()

    override fun toString(): String = "<instanceof $cls>"

    override fun get(name: Token): Any? {
        if (fields.containsKey(name.lexeme))
            return fields[name.lexeme]

        cls.findMethod(name.lexeme)?.let { method ->
            return method.bind(this)
        }

        throw RuntimeError(name, "Undefined property ${name.lexeme}.")
    }

    override fun set(name: Token, value: Any?) {
        fields[name.lexeme] = value
    }
}
