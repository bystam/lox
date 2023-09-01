package lox

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach

abstract class BaseTest {

    private val printedLines = mutableListOf<String>()

    @BeforeEach
    fun setUp() {
        printedLines.clear()
    }

    protected fun executeLox(script: String) {
        val scanner = Scanner(script)
        val tokens = scanner.scanTokens()
        val parser = Parser(tokens)
        val statements = parser.parse()

        val interpreter = Interpreter(
            printlnFunction = { printedLines += it }
        )
        val resolver = Resolver(interpreter)
        resolver.resolve(statements)

        interpreter.interpret(statements)
    }

    protected fun assertPrintedLines(vararg lines: String) {
        Assertions.assertEquals(listOf(*lines), printedLines)
    }
}
