package lox

class LoxClass(
    val name: String,
    val methods: MutableMap<String, LoxFunction>
) : LoxCallable {

    override val arity: Int = 0

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val instance = LoxInstance(this)
        return instance
    }

    fun findMethod(name: String): LoxFunction? = methods[name]

    override fun toString(): String = "<class $name>"
}
