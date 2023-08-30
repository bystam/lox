package lox

class Scanner(
    private val source: String
) {

    private val tokens = mutableListOf<Token>()

    private var start: Int = 0
    private var current: Int = 0
    private var line: Int = 1

    fun scanTokens(): List<Token> {
        while (!isAtEnd()) {
            start = current
            scanToken()
        }

        tokens += Token(TokenType.EOF, "", null, line)
        return tokens
    }

    private fun isAtEnd(): Boolean = current >= source.length

    private fun scanToken() {
        val c = advance()
        when (c) {
            // single char
            '(' -> addToken(TokenType.LEFT_PAREN)
            ')' -> addToken(TokenType.RIGHT_PAREN)
            '{' -> addToken(TokenType.LEFT_BRACE)
            '}' -> addToken(TokenType.RIGHT_BRACE)
            ',' -> addToken(TokenType.COMMA)
            '.' -> addToken(TokenType.DOT)
            '-' -> addToken(TokenType.MINUS)
            '+' -> addToken(TokenType.PLUS)
            ';' -> addToken(TokenType.SEMICOLON)
            '*' -> addToken(TokenType.STAR)

            // two-character conditionals
            '!' -> addToken(if (match('=')) TokenType.BANG_EQUAL else TokenType.BANG)
            '=' -> addToken(if (match('=')) TokenType.EQUAL else TokenType.EQUAL)
            '<' -> addToken(if (match('=')) TokenType.LESS_EQUAL else TokenType.LESS)
            '>' -> addToken(if (match('=')) TokenType.GREATER_EQUAL else TokenType.GREATER)
            '/' -> {
                if (match('/')) { // comment
                    while (peek() != '\n' && !isAtEnd()) advance()
                } else {
                    addToken(TokenType.SLASH)
                }
            }

            // whitespace
            ' ', '\t', '\r' -> { /* Ignore */ }
            '\n' -> line++

            // literals
            '"' -> string()

            else -> {
                if (isDigit(c)) {
                    number()
                } else if (isAlpha(c)) {
                    identifier()
                } else {
                    Error.report(line, "Unexpected character.")
                }
            }
        }
    }

    private fun advance(): Char = source[current++]
    private fun peek(): Char = if (isAtEnd()) Char.MIN_VALUE else source[current]
    private fun peekNext(): Char = if (current + 1 >= source.length) Char.MIN_VALUE else source[current + 1]
    private fun match(expected: Char): Boolean = when {
        isAtEnd() -> false
        source[current] != expected -> false
        else -> {
            current++
            true
        }
    }
    private fun isDigit(c: Char) = c in '0'..'9'
    private fun isAlpha(c: Char) = c in 'a' .. 'z' || c in 'A' .. 'Z' || c == '_'
    private fun isAlphanumeric(c: Char) = isAlpha(c) || isDigit(c)

    private fun string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++
            advance()
        }
        if (isAtEnd()) {
            Error.report(line, "Unterminated string.")
        }
        advance() // closing "

        val string = source.substring(start + 1, current - 1) // without ""
        addToken(TokenType.STRING, literal = string)
    }

    private fun number() {
        while (isDigit(peek())) advance()

        if (peek() == '.' && isDigit(peekNext())) {
            advance()

            while (isDigit(peek())) advance()
        }

        val number = source.substring(start, current).toDouble()
        addToken(TokenType.NUMBER, literal = number)
    }

    private fun identifier() {
        while (isAlphanumeric(peek())) advance()
        val text = source.substring(start, current)
        val type = KEYWORDS[text] ?: TokenType.IDENTIFIER
        addToken(type)
    }

    private fun addToken(type: TokenType, literal: Any? = null) {
        val text = source.substring(start, current)
        tokens += Token(
            type = type,
            lexeme = text,
            literal = literal,
            line = line
        )
    }

    private companion object {
        val KEYWORDS: Map<String, TokenType> = mapOf(
            "and" to TokenType.AND,
            "class" to TokenType.CLASS,
            "else" to TokenType.ELSE,
            "false" to TokenType.FALSE,
            "for" to TokenType.FOR,
            "fun" to TokenType.FUN,
            "if" to TokenType.IF,
            "nil" to TokenType.NIL,
            "or" to TokenType.OR,
            "print" to TokenType.PRINT,
            "return" to TokenType.RETURN,
            "super" to TokenType.SUPER,
            "this" to TokenType.THIS,
            "true" to TokenType.TRUE,
            "var" to TokenType.VAR,
            "while" to TokenType.WHILE,
        )
    }
}
