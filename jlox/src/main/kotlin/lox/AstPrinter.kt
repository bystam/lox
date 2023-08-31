package lox

class AstPrinter : Expr.Visitor<String> {

    fun print(expr: Expr): String = expr.accept(this)

    override fun visitAssignExpr(expr: Expr.Assign): String = parenthesize("${expr.name.lexeme} =", expr.value)

    override fun visitBinaryExpr(expr: Expr.Binary): String = parenthesize(expr.operator.lexeme, expr.left, expr.right)

    override fun visitGroupingExpr(expr: Expr.Grouping): String = parenthesize("group", expr.expression)

    override fun visitLiteralExpr(expr: Expr.Literal): String = expr.value?.toString() ?: "nil"

    override fun visitUnaryExpr(expr: Expr.Unary): String = parenthesize(expr.operator.lexeme, expr.right)
    override fun visitVariableExpr(expr: Expr.Variable): String = expr.name.lexeme

    private fun parenthesize(name: String, vararg exprs: Expr): String {
        val builder = StringBuilder()

        builder.append("(").append(name)
        for (expr in exprs) {
            builder.append(" ")
            builder.append(expr.accept(this))
        }
        builder.append(")")

        return builder.toString()
    }
}
