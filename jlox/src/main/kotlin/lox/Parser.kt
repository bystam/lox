package lox

import kotlin.math.exp

class Parser(
    private val tokens: List<Token>
) {
    private var current: Int = 0

    fun parse(): List<Stmt> {
        val statements = mutableListOf<Stmt>()
        while (!isAtEnd()) {
            declaration()?.let { statements += it }
        }
        return statements
    }

    // declaration    → funDecl
    //                | varDecl
    //                | statement ;
    private fun declaration(): Stmt? {
        return try {
            when {
                match(TokenType.FUN) -> function("function")
                match(TokenType.VAR) -> varDeclaration()
                else -> statement()
            }
        } catch (e: ParseError) {
            synchronize()
            null
        }
    }

    // funDecl        → "fun" function ;
    // function       → IDENTIFIER "(" parameters? ")" block ;
    // parameters     → IDENTIFIER ( "," IDENTIFIER )* ;
    private fun function(kind: String): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expected $kind name.")

        // parameters
        consume(TokenType.LEFT_PAREN, "Expected '(' after $kind declaration")
        val parameters = mutableListOf<Token>()
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }

                parameters += consume(TokenType.IDENTIFIER, "Expected parameter name.")

            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RIGHT_PAREN, "Expected ') after $kind parameters")

        // body
        consume(TokenType.LEFT_BRACE, "Expected '{' before $kind body")
        val body = block()
        return Stmt.Function(name, parameters, body)
    }

    // varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
    private fun varDeclaration(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expected variable name")
        var initializer: Expr? = null
        if (match(TokenType.EQUAL)) {
            initializer = expression()
        }
        consume(TokenType.SEMICOLON, "Expected ';' after declaration.")
        return Stmt.Var(name, initializer)
    }

    // statement      → exprStmt
    //                | forStmt
    //                | ifStmt
    //                | printStmt
    //                | returnStmt
    //                | whileStmt
    //                | block;
    private fun statement(): Stmt {
        return when {
            match(TokenType.FOR) -> forStatement()
            match(TokenType.IF) -> ifStatement()
            match(TokenType.PRINT) -> printStatement()
            match(TokenType.RETURN) -> returnStatement()
            match(TokenType.WHILE) -> whileStatement()
            match(TokenType.LEFT_BRACE) -> Stmt.Block(block())
            else -> expressionStatement()
        }
    }

    // forStmt        → "for" "(" ( varDecl | exprStmt | ";" )
    //                   expression? ";"
    //                   expression? ")" statement ;
    private fun forStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expected '(' after if.")

        val initializer: Stmt? = when {
            match(TokenType.SEMICOLON) -> null
            match(TokenType.VAR) -> varDeclaration()
            else -> expressionStatement()
        }

        val condition: Expr? = when {
            check(TokenType.SEMICOLON) -> null
            else -> expression()
        }
        consume(TokenType.SEMICOLON, "Expected ';' after loop condition")

        val increment = when {
            check(TokenType.RIGHT_PAREN) -> null
            else -> expression()
        }
        consume(TokenType.RIGHT_PAREN, "Expected ')' after for clauses")

        // Desugar for-loop into a while-loop
        var body = statement()
        increment?.let {
            // attach increment at end of loop body
            body = Stmt.Block(listOf(body, Stmt.Expression(it)))
        }
        // turn it into a while loop, with default 'always true' condition
        body = Stmt.While(
            condition = condition ?: Expr.Literal(true),
            body = body
        )
        // if there's an initializer, prepend it
        initializer?.let {
            body = Stmt.Block(listOf(it, body))
        }
        return body
    }

    // ifStmt         → "if" "(" expression ")" statement
    //                  ( "else" statement )? ;
    private fun ifStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expected '(' after if.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expected ')' after if condition.")

        val thenBranch = statement()
        var elseBranch: Stmt? = if (match(TokenType.ELSE)) statement() else null

        return Stmt.If(condition, thenBranch, elseBranch)
    }

    // returnStmt     → "return" expression? ";" ;
    private fun returnStatement(): Stmt {
        val keyword = previous()
        val expr = if (!check(TokenType.SEMICOLON)) expression() else null
        consume(TokenType.SEMICOLON, "Expected ';' after return value.")
        return Stmt.Return(keyword, expr)
    }

    // whileStmt      → "while" "(" expression ")" statement ;
    private fun whileStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expected '(' after while.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expected ')' after while condition.")

        val body = statement()
        return Stmt.While(condition, body)
    }

    // block          → "{" declaration* "}" ;
    private fun block(): List<Stmt> {
        val statements = mutableListOf<Stmt>()

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            declaration()?.let { statements += it }
        }

        consume(TokenType.RIGHT_BRACE, "Expected closing '}' after block.")
        return statements
    }

    // printStmt      → "print" expression ";" ;
    private fun printStatement(): Stmt {
        val expr = expression()
        consume(TokenType.SEMICOLON, "Expecting ; after value.")
        return Stmt.Print(expr)
    }

    // exprStmt       → expression ";" ;
    private fun expressionStatement(): Stmt {
        val expr = expression()
        consume(TokenType.SEMICOLON, "Expecting ; after expression.")
        return Stmt.Expression(expr)
    }

    // expression     → assignment ;
    private fun expression(): Expr = assignment()

    // assignment     → IDENTIFIER "=" assignment
    //                | logic_or ;
    private fun assignment(): Expr {
        val expr = or()
        if (match(TokenType.EQUAL)) {
            val equals = previous()
            val value = assignment()
            if (expr is Expr.Variable) { // was an l-value, not an r-value
                return Expr.Assign(expr.name, value)
            }

            error(equals, "Invalid assignment target.")
        }
        return expr
    }

    // logic_or       → logic_and ( "or" logic_and )* ;
    private fun or(): Expr {
        val left = and()
        if (match(TokenType.OR)) {
            val operator = previous()
            val right = and()
            return Expr.Logical(left, operator, right)
        }
        return left
    }

    // logic_and      → equality ( "and" equality )* ;
    private fun and(): Expr {
        val left = equality()
        if (match(TokenType.AND)) {
            val operator = previous()
            val right = equality()
            return Expr.Logical(left, operator, right)
        }
        return left
    }

    // equality       → comparison ( ( "!=" | "==" ) comparison )* ;
    private fun equality(): Expr {
        var expr = comparison()

        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            val operator = previous()
            val right = comparison()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    // comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
    private fun comparison(): Expr {
        var expr = term()
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            val operator = previous()
            val right = term()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    // term           → factor ( ( "-" | "+" ) factor )* ;
    private fun term(): Expr {
        var expr = factor()
        while (match(TokenType.MINUS, TokenType.PLUS)) {
            val operator = previous()
            val right = factor()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    // factor         → unary ( ( "/" | "*" ) unary )* ;
    private fun factor(): Expr {
        var expr = unary()
        while (match(TokenType.SLASH, TokenType.STAR)) {
            val operator = previous()
            val right = unary()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    // unary          → ( "!" | "-" ) unary | call ;
    private fun unary(): Expr {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            val operator = previous()
            val right = unary()
            return Expr.Unary(operator, right)
        }
        return call()
    }

    // call           → primary ( "(" arguments? ")" )* ;
    // arguments      → expression ( "," expression )* ;
    private fun call(): Expr {

        fun finishCall(callee: Expr): Expr {
            val arguments = mutableListOf<Expr>()
            if (!check(TokenType.RIGHT_PAREN)) {
                do {
                    if (arguments.size >= 255) {
                        error(peek(), "Can't have more than 255 arguments.")
                    }
                    arguments += expression()
                } while (match(TokenType.COMMA))
            }
            val paren = consume(TokenType.RIGHT_PAREN, "Expecting ')' after function arguments")

            return Expr.Call(callee, paren, arguments)
        }

        var expr = primary()
        while (match(TokenType.LEFT_PAREN)) {
            expr = finishCall(expr)
        }
        return expr
    }

    // primary        → "true" | "false" | "nil" | NUMBER | STRING | "(" expression ")" | IDENTIFIER;
    private fun primary(): Expr {
        if (match(TokenType.TRUE)) return Expr.Literal(true)
        if (match(TokenType.FALSE)) return Expr.Literal(false)
        if (match(TokenType.NIL)) return Expr.Literal(null)

        if (match(TokenType.NUMBER, TokenType.STRING)) {
            return Expr.Literal(previous().literal)
        }

        if (match(TokenType.LEFT_PAREN)) {
            val expr = expression()
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.")
            return Expr.Grouping(expr)
        }

        if (match(TokenType.IDENTIFIER)) {
            return Expr.Variable(previous())
        }

        throw error(peek(), "Expect expression.")
    }

    private fun match(vararg types: TokenType): Boolean {
        types.forEach { type ->
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun check(type: TokenType): Boolean {
        if (isAtEnd()) return false
        return peek().type == type
    }

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun isAtEnd(): Boolean = peek().type === TokenType.EOF
    private fun peek(): Token = tokens[current]
    private fun previous(): Token = tokens[current - 1]

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw error(peek(), message)
    }

    private fun synchronize() {
        advance()

        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) return
            when (peek().type) {
                TokenType.CLASS,
                TokenType.FUN,
                TokenType.VAR,
                TokenType.FOR,
                TokenType.IF,
                TokenType.WHILE,
                TokenType.PRINT,
                TokenType.RETURN -> return
                else -> {}
            }
        }
        advance()
    }

    private fun error(token: Token, message: String): ParseError {
        Error.report(token, message)
        return ParseError()
    }

    private class ParseError : RuntimeException()
}
