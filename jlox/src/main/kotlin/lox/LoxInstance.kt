package lox

class LoxInstance(val cls: LoxClass) {

    private val fields: MutableMap<String, Any?> = mutableMapOf()

    override fun toString(): String = "<instanceof $cls>"

    fun get(name: Token): Any? {
        if (fields.containsKey(name.lexeme))
            return fields[name.lexeme]

        cls.findMethod(name.lexeme)?.let { method ->
            return method.bind(this)
        }

        throw RuntimeError(name, "Undefined property ${name.lexeme}.")
    }

    fun set(name: Token, value: Any?) {
        fields[name.lexeme] = value
    }
}
