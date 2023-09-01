package lox

import java.nio.charset.Charset
import java.nio.file.Paths
import kotlin.io.path.readBytes
import kotlin.system.exitProcess

object Lox {

    private val interpreter = Interpreter()

    @JvmStatic
    fun main(args: Array<String>): Unit = when {
        args.size > 1 -> {
            print("Usage: jlox [script]")
            exitProcess(64)
        }
        args.size == 1 -> {
            runFile(args.single())
        }
        else -> {
            val projectDirectory = System.getProperty("user.dir")
            val path = "$projectDirectory/src/main/resources/program.lox"
            runFile(path)
//            runPrompt()
        }
    }

    private fun runFile(path: String) {
        val bytes = Paths.get(path).readBytes()
        val script = String(bytes, Charset.defaultCharset())
        run(script)

        if (Error.hadError) exitProcess(65)
        if (Error.hadRuntimeError) exitProcess(70)
    }

    private fun runPrompt() {
        val prompt = generateSequence(::readLine)
        prompt.forEach { line ->
            run(line)
            Error.hadError = false
            Error.hadRuntimeError = false
        }
    }

    private fun run(script: String) {
        val scanner = Scanner(script)
        val tokens = scanner.scanTokens()
        val parser = Parser(tokens)
        val statements = parser.parse()

        if (Error.hadError) return

        val resolver = Resolver(interpreter)
        resolver.resolve(statements)

        if (Error.hadError) return

        interpreter.interpret(statements)
    }
}
