package lox

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ProgramTest {

    private val printedLines = mutableListOf<String>()

    @BeforeEach
    fun setUp() {
        printedLines.clear()
    }

    @Test
    fun `recursive fibonacci`() {
        executeLox("""
            fun fib(n) {
              if (n <= 1) return n;
              return fib(n - 2) + fib(n - 1);
            }

            for (var i = 0; i < 7; i = i + 1) {
              print fib(i);
            }
        """.trimIndent())
        assertPrintedLines("0", "1", "1", "2", "3", "5", "8")
    }

    @Test
    fun `function closures`() {
        executeLox("""
            fun makeCounter() {
              var i = 0;
              fun count() {
                i = i + 1;
                print i;
              }
            
              return count;
            }
            
            var counter = makeCounter();
            counter(); // "1".
            counter(); // "2".
        """.trimIndent())

        assertPrintedLines("1", "2")
    }

    @Test
    fun `parse class`() {
        executeLox("""
            class DevonshireCream {
              serveOn() {
                return "Scones";
              }
            }

            print DevonshireCream; // Prints "DevonshireCream".
        """.trimIndent())

        assertPrintedLines("<class DevonshireCream>")
    }

    @Test
    fun `run method`() {
        executeLox("""
            class Bacon {
              eat() {
                print "Crunch crunch crunch!";
              }
            }

            Bacon().eat(); // Prints "Crunch crunch crunch!".
        """.trimIndent())

        assertPrintedLines("Crunch crunch crunch!")
    }

    private fun executeLox(script: String) {
        val scanner = Scanner(script)
        val tokens = scanner.scanTokens()
        val parser = Parser(tokens)
        val statements = parser.parse()

        val interpreter = Interpreter(
            printlnFunction = { printedLines += it }
        )
        interpreter.interpret(statements)
    }

    private fun assertPrintedLines(vararg lines: String) {
        assertEquals(listOf(*lines), printedLines)
    }
}
