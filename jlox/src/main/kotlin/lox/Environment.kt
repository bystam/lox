package lox

class Environment(
    val enclosing: Environment? = null
) {
    private val values: MutableMap<String, Any?> = mutableMapOf()

    fun define(name: String, value: Any?) {
        values[name] = value
    }

    fun assign(name: Token, value: Any?) {
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme] = value
            return
        }
        enclosing?.let { enc ->
            enc.assign(name, value)
            return
        }
        throw RuntimeError(name, "Undefined variable: ${name.lexeme}.")
    }

    fun get(name: Token): Any? {
        if (values.containsKey(name.lexeme)) {
            return values[name.lexeme]
        }
        enclosing?.let { enc ->
            return enc.get(name)
        }
        throw RuntimeError(name, "Undefined variable: ${name.lexeme}.")
    }
}
