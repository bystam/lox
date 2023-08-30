package lox

import java.nio.charset.Charset
import java.nio.file.Paths
import kotlin.io.path.readBytes
import kotlin.system.exitProcess

fun main(args: Array<String>): Unit = when {
    args.size > 1 -> {
        print("Usage: jlox [script]")
        exitProcess(64)
    }
    args.size == 1 -> {
        runFile(args.single())
    }
    else -> {
        runPrompt()
    }
}

private fun runFile(path: String) {
    val bytes = Paths.get(path).readBytes()
    val script = String(bytes, Charset.defaultCharset())
    run(script)
}

private fun runPrompt() {
    val prompt = generateSequence(::readLine)
    prompt.forEach { line ->
        run(line)
        Error.hadError = false
    }
}

private fun run(script: String) {
    val scanner = Scanner(script)
    val tokens = scanner.scanTokens()

    tokens.forEach { println(it) }
}
