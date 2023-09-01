package lox

class LoxClass(
    val name: String,
    val superclass: LoxClass?,
    val methods: MutableMap<String, LoxFunction>
) : LoxCallable {

    override val arity: Int get() {
        return findMethod("init")?.arity ?: 0
    }

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val instance = LoxInstance(this)
        findMethod("init")?.let { initializer ->
            initializer.bind(instance).call(interpreter, arguments)
        }
        return instance
    }

    fun findMethod(name: String): LoxFunction? = methods[name] ?: superclass?.findMethod(name)

    override fun toString(): String = "<class $name>"
}
