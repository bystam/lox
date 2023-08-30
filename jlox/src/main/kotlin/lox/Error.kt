package lox

object Error {

    var hadError: Boolean = false

    fun report(line: Int, message: String, where: String = "") {
        System.err.println("[line $line] lox.Error$where: $message")
        hadError = true
    }
}
