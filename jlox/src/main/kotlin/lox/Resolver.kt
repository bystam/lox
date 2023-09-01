package lox

/**
 * This runs on an entirely parsed syntax tree, which enables us to make semantic analysis of the content
 * and "massage" the script and state before running it. For instance:
 * - for every reference to a variable, store how many scopes away from current one (0...) it actually was declared
 * - check that return statements don't happen at the top level
 * - that we don't re-declare the same variable in the same scope
 *
 * You can think of the [Parser] as parsing things statement by statement, expression by expression, and the [Resolver]
 * as something that analyses the program more in its entirety, making decisions based on multiple statements.
 */
class Resolver(
    private val interpreter: Interpreter
) : Expr.Visitor<Unit>, Stmt.Visitor<Unit> {

    private val scopes: MutableList<MutableMap<String, Boolean>> = mutableListOf()
    private var currentFunction: FunctionType = FunctionType.NONE

    fun resolve(statements: List<Stmt>) = statements.forEach { resolve(it) }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        beginScope()
        resolve(stmt.statements)
        endScope()
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        declare(stmt.name)
        stmt.initializer?.let { resolve(it) }
        define(stmt.name)
    }

    override fun visitVariableExpr(expr: Expr.Variable) {
        if (scopes.isNotEmpty() && scopes.last()[expr.name.lexeme] == false) {
            Error.report(expr.name, "Can't read local variable in its own initializer.")
        }

        resolveLocal(expr, expr.name)
    }

    override fun visitAssignExpr(expr: Expr.Assign) {
        resolve(expr.value)
        resolveLocal(expr, expr.name)
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        declare(stmt.name)
        define(stmt.name)

        resolveFunction(stmt, FunctionType.FUNCTION)
    }

// --- BORING ONES ----

    override fun visitExpressionStmt(stmt: Stmt.Expression) = resolve(stmt.expression)
    override fun visitIfStmt(stmt: Stmt.If) {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)
        stmt.elseBranch?.let { resolve(it) }
    }

    override fun visitPrintStmt(stmt: Stmt.Print) = resolve(stmt.expression)
    override fun visitReturnStmt(stmt: Stmt.Return) {
        if (currentFunction == FunctionType.NONE) {
            Error.report(stmt.keyword, "Can't return from top-level code.")
        }
        stmt.value?.let { resolve(it) }
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        resolve(stmt.condition)
        resolve(stmt.body)
    }

    override fun visitBinaryExpr(expr: Expr.Binary) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitCallExpr(expr: Expr.Call) {
        resolve(expr.callee)
        expr.arguments.forEach { resolve(it) }
    }

    override fun visitGroupingExpr(expr: Expr.Grouping) {
        resolve(expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal) {
    }

    override fun visitLogicalExpr(expr: Expr.Logical) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitUnaryExpr(expr: Expr.Unary) {
        resolve(expr.right)
    }

// --- BORING ONES END ----


    private fun resolve(stmt: Stmt) = stmt.accept(this)
    private fun resolve(expr: Expr) = expr.accept(this)
    private fun resolveLocal(expr: Expr, name: Token) {
// wtf
        scopes.indices.reversed().forEach { i ->
            if (scopes[i].containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size - 1 - i)
                return
            }
        }
    }

    private fun resolveFunction(function: Stmt.Function, type: FunctionType) {
        val enclosingFunction = currentFunction
        currentFunction = type

        beginScope()
        function.params.forEach { param ->
            declare(param)
            define(param)
        }
        resolve(function.body)
        endScope()

        currentFunction = enclosingFunction
    }

    private fun beginScope() {
        scopes.add(mutableMapOf())
    }

    private fun endScope() {
        scopes.removeLast()
    }

    private fun declare(name: Token) {
        if (scopes.isEmpty()) return
        val scope = scopes.last()
        if (scope.containsKey(name.lexeme)) {
            Error.report(name, "Already a variable declared with name '${name.lexeme}'.")
        }
        scope[name.lexeme] = false
    }

    private fun define(name: Token) {
        if (scopes.isEmpty()) return
        scopes.last()[name.lexeme] = true
    }

    private enum class FunctionType {
        NONE, FUNCTION
    }
}
