package lox

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

    @Test
    fun `handle this`() {
        executeLox("""
            class Cake {
              taste() {
                var adjective = "delicious";
                print "The " + this.flavor + " cake is " + adjective + "!";
              }
            }
            
            var cake = Cake();
            cake.flavor = "German chocolate";
            cake.taste(); // Prints "The German chocolate cake is delicious!".
        """.trimIndent())

        assertPrintedLines("The German chocolate cake is delicious!")
    }


    @Test
    fun `handle inheritance`() {
        executeLox("""
            class Food {
              init(name) {
                this.name = name;
              }
              
              operation() {
                return "Eat me!";
              }
              
              toString() {
                return this.name;
              }
            }
            
            class DescribedFood < Food {
              init(name, description) {
                super.init(name);
                this.description = description;
              }
              
              toString() {
                return this.description + " " + super.toString();
              }
            }
            
            var fries = DescribedFood("Fries", "Tasty");
            print fries.operation();
            print fries.toString();
        """.trimIndent())

        assertPrintedLines("Eat me!", "Tasty Fries")
    }

    private fun executeLox(script: String) {
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

    private fun assertPrintedLines(vararg lines: String) {
        assertEquals(listOf(*lines), printedLines)
    }
}
