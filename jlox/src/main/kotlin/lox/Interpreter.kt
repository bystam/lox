package lox

class Interpreter : Expr.Visitor<Any?>, Stmt.Visitor<Unit> {

    private var environment = Environment()

    fun interpret(statements: List<Stmt>) {
        try {
            statements.forEach { statement ->
                execute(statement)
            }
        } catch (error: RuntimeError) {
            Error.report(error)
        }
    }

    /// ----- Stmt.Visitor -----

    override fun visitBlockStmt(stmt: Stmt.Block) {
        executeBlock(stmt.statements)
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
        println(stringify(value))
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        evaluate(stmt.expression)
    }

    private fun execute(statement: Stmt) {
        statement.accept(this)
    }

    private fun executeBlock(statements: List<Stmt>) {
        val originalEnvironment = environment
        try {
            this.environment = Environment(originalEnvironment)
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
        environment.assign(expr.name, value)
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

    override fun visitUnaryExpr(expr: Expr.Unary): Any {
        val right = evaluate(expr.right)
        return when (expr.operator.type) {
            TokenType.MINUS -> -castNumbers(expr.operator, right).single()
            TokenType.BANG -> isTruthy(right)
            else -> TODO("WTF")
        }
    }

    override fun visitVariableExpr(expr: Expr.Variable): Any? {
        return environment.get(expr.name)
    }

    private fun isTruthy(value: Any?): Boolean {
        if (value == null) return false
        if (value is Boolean) return value
        return true
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
