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
            report(token.line, message, " at end")
        } else {
            report(token.line, message, " at '" + token.lexeme + "'")
        }
    }

    fun report(error: RuntimeError) {
        System.err.println("${error.message}\n[line ${error.token.line}]")
        hadRuntimeError = true
    }
}
