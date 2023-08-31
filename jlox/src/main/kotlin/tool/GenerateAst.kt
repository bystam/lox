package tool

import java.io.PrintWriter


object GenerateAst {

    @JvmStatic
    fun main(args: Array<String>) {
        val projectDirectory = System.getProperty("user.dir")
        val outputDir = "$projectDirectory/src/main/kotlin/lox"

        defineAst(outputDir, "Expr", listOf(
            "Assign   : Token name, Expr value",
            "Binary   : Expr left, Token operator, Expr right",
            "Call     : Expr callee, Token paren, List<Expr> arguments",
            "Grouping : Expr expression",
            "Literal  : Any? value",
            "Logical  : Expr left, Token operator, Expr right",
            "Unary    : Token operator, Expr right",
            "Variable : Token name",
        ))

        defineAst(outputDir, "Stmt", listOf(
            "Block      : List<Stmt> statements",
            "Expression : Expr expression",
            "Function   : Token name, List<Token> params, List<Stmt> body",
            "If         : Expr condition, Stmt thenBranch, Stmt? elseBranch",
            "Print      : Expr expression",
            "Return     : Token keyword, Expr? value",
            "Var        : Token name, Expr? initializer",
            "While      : Expr condition, Stmt body",
        ))
    }

    private fun defineAst(
        outputDir: String,
        baseName: String,
        types: List<String>
    ) {
        val path = "$outputDir/$baseName.kt"
        val writer = PrintWriter(path, "UTF-8")
        writer.println("package lox")
        writer.println()
        writer.println()
        writer.println("sealed interface $baseName {")

        writer.println()
        defineVisitor(writer, baseName, types)
        writer.println()

        writer.println("    fun <R> accept(visitor: Visitor<R>): R")
        writer.println()

        for (type in types) {
            val pieces = type.split(":").map { piece -> piece.trim() }

            val (className, fields) = pieces
            defineType(writer, baseName, className, fields)
        }
        writer.println("}")
        writer.close()
    }

    private fun defineType(
        writer: PrintWriter,
        baseName: String,
        className: String,
        fieldList: String
    ) {
        writer.println(
            "    data class $className("
        )

        // Store parameters in fields.
        val fields = fieldList.split(", ")
        for (field in fields) {
            val (type, name) = field.split(" ")
            writer.println("        val $name: $type,")
        }
        writer.println("    ) : $baseName {")
        writer.println("""
            override fun <R> accept(visitor: Visitor<R>): R = visitor.visit$className$baseName(this)
        """.trimIndent().prependIndent("        "))
        writer.println("    }")
        writer.println()
    }

    private fun defineVisitor(
        writer: PrintWriter,
        baseName: String,
        types: List<String>
    ) {
        writer.println("    interface Visitor<R> {")

        for (type in types) {
            val typeName = type.split(":").first().trim()
            writer.println("        fun visit$typeName$baseName(${baseName.lowercase()}: $typeName): R")
        }

        writer.println("    }")
    }
}
