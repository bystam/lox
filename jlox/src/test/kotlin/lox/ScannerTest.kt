package lox

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ScannerTest {

    @Test
    fun `dumb test`() {
        val program = """
            var a = 1
            var b = 2.3
            var c = a + b
            print c
        """.trimIndent()

        val expected = listOf(
            Token(TokenType.VAR, "var", null, 1),
            Token(TokenType.IDENTIFIER, "a", null, 1),
            Token(TokenType.EQUAL, "=", null, 1),
            Token(TokenType.NUMBER, "1", 1.0, 1),

            Token(TokenType.VAR, "var", null, 2),
            Token(TokenType.IDENTIFIER, "b", null, 2),
            Token(TokenType.EQUAL, "=", null, 2),
            Token(TokenType.NUMBER, "2.3", 2.3, 2),

            Token(TokenType.VAR, "var", null, 3),
            Token(TokenType.IDENTIFIER, "c", null, 3),
            Token(TokenType.EQUAL, "=", null, 3),
            Token(TokenType.IDENTIFIER, "a", null, 3),
            Token(TokenType.PLUS, "+", null, 3),
            Token(TokenType.IDENTIFIER, "b", null, 3),

            Token(TokenType.PRINT, "print", null, 4),
            Token(TokenType.IDENTIFIER, "c", null, 4),

            Token(TokenType.EOF, "", null, 4)
        )

        val tokens = Scanner(program).scanTokens()
        assertEquals(expected, tokens)
    }
}
