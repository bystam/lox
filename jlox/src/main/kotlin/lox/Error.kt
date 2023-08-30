package lox


object Error {

    var hadError: Boolean = false
    var hadRuntimeError: Boolean = false

    fun report(line: Int, message: String, where: String = "") {
        System.err.println("[line $line] lox.Error$where: $message")
        hadError = true
    }

    fun report(token: Token, message: String) {
        if (token.type === TokenType.EOF) {
            report(token.line, " at end", message)
        } else {
            report(token.line, " at '" + token.lexeme + "'", message)
        }
    }

    fun report(error: RuntimeError) {
        System.err.println("${error.message}\n[line ${error.token.line}]")
        hadRuntimeError = true
    }
}
