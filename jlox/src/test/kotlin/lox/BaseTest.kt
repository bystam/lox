package lox

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import java.lang.Exception

abstract class BaseTest {

    private val printedLines = mutableListOf<String>()

    @BeforeEach
    fun setUp() {
        printedLines.clear()
    }

    protected fun executeLox(script: String) {
        val entireScript = Stdlib + script;
        val scanner = Scanner(entireScript)
        val tokens = scanner.scanTokens()
        val parser = Parser(tokens)
        val statements = parser.parse()

        val interpreter = Interpreter(
            printlnFunction = { printedLines += it }
        )
        val resolver = Resolver(interpreter)
        resolver.resolve(statements)

        try {
            interpreter.interpret(statements)
        } catch (e: Exception) {
            println("Prints:")
            printedLines.forEach {
                println("  $it")
            }
            throw e
        }
    }

    protected fun assertPrintedLines(vararg lines: String) {
        Assertions.assertEquals(listOf(*lines), printedLines)
    }
}
