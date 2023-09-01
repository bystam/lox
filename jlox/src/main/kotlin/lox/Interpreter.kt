package lox

class Interpreter(
    private val printlnFunction: (String) -> Unit = ::println
) : Expr.Visitor<Any?>, Stmt.Visitor<Unit> {

    private val globals: Environment = Environment()
    private var environment: Environment = globals
    private val locals: MutableMap<Expr, Int> = mutableMapOf()

    init {
        globals.define("clock", NativeCallable.Clock)
    }

    fun interpret(statements: List<Stmt>) {
        try {
            statements.forEach { statement ->
                execute(statement)
            }
        } catch (error: RuntimeError) {
            Error.report(error)
        }
    }

    fun resolve(expr: Expr, depth: Int) {
        locals[expr] = depth
    }

    /// ----- Stmt.Visitor -----

    override fun visitBlockStmt(stmt: Stmt.Block) {
        executeBlock(stmt.statements, Environment(this.environment))
    }

    override fun visitClassStmt(stmt: Stmt.Class) {
        var superclass: LoxClass? = null
        if (stmt.superclass != null) {
            superclass = evaluate(stmt.superclass) as? LoxClass
            if (superclass == null) {
                throw RuntimeError(stmt.superclass.name, "Superclass must be a class.")
            }
        }

        environment.define(stmt.name.lexeme, null)

        stmt.superclass?.let {
            environment = Environment(environment)
            environment.define("super", superclass)
        }

        val methods = mutableMapOf<String, LoxFunction>()
        stmt.methods.forEach { method ->
            val function = LoxFunction(method, environment, isInitializer = method.name.lexeme == "init")
            methods[method.name.lexeme] = function
        }

        val cls = LoxClass(stmt.name.lexeme, superclass, methods)

        stmt.superclass?.let {
            environment = environment.enclosing!!
        }

        environment.assign(stmt.name, cls)
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        val initialValue = stmt.initializer?.let { evaluate(it) }
        environment.define(stmt.name.lexeme, initialValue)
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch)
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch)
        }
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body)
        }
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        val value = evaluate(stmt.expression)
        printlnFunction(stringify(value))
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        val value = stmt.value?.let { evaluate(it) }
        throw LoxFunction.Return(value)
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        evaluate(stmt.expression)
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        val function = LoxFunction(stmt, environment, false)
        environment.define(stmt.name.lexeme, function)
    }

    private fun execute(statement: Stmt) {
        statement.accept(this)
    }

    fun executeBlock(statements: List<Stmt>, environment: Environment) {
        val originalEnvironment = this.environment
        try {
            this.environment = environment
            statements.forEach { statement ->
                execute(statement)
            }
        } finally {
            this.environment = originalEnvironment
        }
    }

    /// ----- Expr.Visitor -----

    override fun visitAssignExpr(expr: Expr.Assign): Any? {
        val value = evaluate(expr.value)
        val distance = locals[expr]
        if (distance != null) {
            environment.assignAt(distance, expr.name, value)
        } else {
            globals.assign(expr.name, value)
        }
        return value
    }

    override fun visitBinaryExpr(expr: Expr.Binary): Any? {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            TokenType.MINUS -> {
                val (l, r) = castNumbers(expr.operator, left, right)
                l - r
            }
            TokenType.SLASH -> {
                val (l, r) = castNumbers(expr.operator, left, right)
                l / r
            }
            TokenType.STAR -> {
                val (l, r) = castNumbers(expr.operator, left, right)
                l * r
            }
            TokenType.PLUS -> when {
                left is Double && right is Double -> left + right
                left is String && right is String -> left + right
                else -> throw RuntimeError(expr.operator, "Operands must be two numbers or two strings")
            }

            TokenType.LESS -> {
                val (l, r) = castNumbers(expr.operator, left, right)
                l < r
            }
            TokenType.LESS_EQUAL -> {
                val (l, r) = castNumbers(expr.operator, left, right)
                l <= r
            }
            TokenType.GREATER -> {
                val (l, r) = castNumbers(expr.operator, left, right)
                l > r
            }
            TokenType.GREATER_EQUAL -> {
                val (l, r) = castNumbers(expr.operator, left, right)
                l >= r
            }

            TokenType.BANG_EQUAL -> left != right
            TokenType.EQUAL_EQUAL -> left == right

            else -> null
        }
    }

    override fun visitCallExpr(expr: Expr.Call): Any? {
        val callee = evaluate(expr.callee)
        val arguments = expr.arguments.map { evaluate(it) }
        if (callee !is LoxCallable) {
            throw RuntimeError(expr.paren, "Can only call functions and classes.")
        }
        val function = callee
        if (arguments.size != function.arity) {
            throw RuntimeError(expr.paren, "Expected ${function.arity} arguments but got ${arguments.size}.")
        }
        return function.call(this, arguments)
    }

    override fun visitGetExpr(expr: Expr.Get): Any? {
        val obj = evaluate(expr.obj)
        if (obj is LoxInstance) {
            return obj.get(expr.name)
        }
        throw RuntimeError(expr.name, "Only instances have properties.")
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Any? {
        return evaluate(expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): Any? = expr.value

    override fun visitLogicalExpr(expr: Expr.Logical): Any? {
        val left = evaluate(expr)
        if (expr.operator.type == TokenType.OR && isTruthy(left)) {
            return left
        }
        if (expr.operator.type == TokenType.AND && !isTruthy(left)) {
            return left
        }
        return evaluate(expr.right)
    }

    override fun visitSetExpr(expr: Expr.Set): Any? {
        val obj = evaluate(expr.obj)

        if (obj !is LoxInstance) {
            throw RuntimeError(expr.name, "Only instances have fields.")
        }

        val value = evaluate(expr.value)
        obj.set(expr.name, value)
        return value
    }

    override fun visitSuperExpr(expr: Expr.Super): Any? {
        val distance = locals[expr]!!
        val superclass = environment.getAt(distance, "super") as LoxClass
        val obj = environment.getAt(distance - 1, "this") as LoxInstance
        return superclass.findMethod(expr.method.lexeme)!!.bind(obj)
    }

    override fun visitThisExpr(expr: Expr.This): Any? {
        return lookUpVariable(expr.keyword, expr)
    }

    override fun visitUnaryExpr(expr: Expr.Unary): Any {
        val right = evaluate(expr.right)
        return when (expr.operator.type) {
            TokenType.MINUS -> -castNumbers(expr.operator, right).single()
            TokenType.BANG -> isTruthy(right)
            else -> TODO("WTF")
        }
    }

    override fun visitVariableExpr(expr: Expr.Variable): Any? {
        return lookUpVariable(expr.name, expr)
    }

    private fun isTruthy(value: Any?): Boolean {
        if (value == null) return false
        if (value is Boolean) return value
        return true
    }

    private fun lookUpVariable(name: Token, expr: Expr): Any? {
        val distance = locals[expr]
        return if (distance != null) {
            environment.getAt(distance, name.lexeme)
        } else {
            globals.get(name)
        }
    }

    private fun evaluate(expr: Expr): Any? {
        return expr.accept(this)
    }

    private fun castNumbers(operator: Token, vararg values: Any?): List<Double> {
         return values.map { value ->
            value as? Double ?: throw RuntimeError(operator, "Operand(s) must be a number")
        }
    }

    private fun stringify(value: Any?): String = when (value) {
        null -> "nil"
        is Double -> {
            value.toString().removeSuffix(".0")
        }
        else -> value.toString()
    }
}
