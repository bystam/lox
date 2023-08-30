package lox

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AstPrinterTest {

    @Test
    fun `test stuff`() {
        val expression: Expr = Expr.Binary(
            Expr.Unary(
                Token(TokenType.MINUS, "-", null, 1),
                Expr.Literal(123)
            ),
            Token(TokenType.STAR, "*", null, 1),
            Expr.Grouping(
                Expr.Literal(45.67)
            )
        )

        assertEquals("(* (- 123) (group 45.67))", AstPrinter().print(expression))
    }
}
