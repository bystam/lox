//
// Created by Fredrik Bystam on 2023-09-04.
//

#include <stdio.h>
#include <stdlib.h>
#include "compilers.h"
#include "scanner.h"

#ifdef DEBUG_PRINT_CODE
#include "debug.h"
#endif

typedef struct {
    Token current;
    Token previous;
    bool hadError;
    bool panicMode;
} Parser;

typedef enum {
    PREC_NONE,
    PREC_ASSIGNMENT,  // =
    PREC_OR,          // or
    PREC_AND,         // and
    PREC_EQUALITY,    // == !=
    PREC_COMPARISON,  // < > <= >=
    PREC_TERM,        // + -
    PREC_FACTOR,      // * /
    PREC_UNARY,       // ! -
    PREC_CALL,        // . ()
    PREC_PRIMARY
} Precedence;

typedef void (*ParseFn)();

typedef struct {
    ParseFn prefix;
    ParseFn infix;
    Precedence precedence;
} ParseRule;
extern ParseRule rules[];


Parser parser;
Chunk *compilingChunk;

static void advance();
static void consume(TokenType type, const char *errorMessage);
static void emitByte(uint8_t byte);
static void endCompiler();

static void expression();

static void errorAt(Token* token, const char* message);
static void errorAtCurrent(const char* message);
static void error(const char* message);

bool compile(const char *source, Chunk *chunk) {
    Scanner_init(source);
    compilingChunk = chunk;
    parser.hadError = false;
    parser.panicMode = false;

    advance();
    expression();
    consume(TOKEN_EOF, "Expected end of expression.");
    endCompiler();
    return !parser.hadError;
}

// ===== BUILDING BLOCKS =====

static void advance() {
    parser.previous = parser.current;

    for (;;) {
        parser.current = Scanner_nextToken();
        if (parser.current.type != TOKEN_ERROR) break;

        errorAtCurrent(parser.current.start);
    }
}

static void consume(TokenType type, const char *errorMessage) {
    if (parser.current.type == type) {
        advance();
        return;
    }
    errorAtCurrent(errorMessage);
}

static Chunk *currentChunk() {
    return compilingChunk;
}

static void emitByte(uint8_t byte) {
    Chunk_write(currentChunk(), byte, parser.previous.line);
}

static void emitBytes(uint8_t byte1, uint8_t byte2) {
    emitByte(byte1);
    emitByte(byte2);
}

static uint8_t makeConstant(Value value) {
    int constant = Chunk_addConstant(currentChunk(), value);
    if (constant > UINT8_MAX) {
        error("Too many constants in one chunk.");
        return 0;
    }

    return (uint8_t)constant;
}
static void emitConstant(Value value) {
    emitBytes(OP_CONSTANT, makeConstant(value));
}

static void emitReturn() {
    return emitByte(OP_RETURN);
}

static void endCompiler() {
#ifdef DEBUG_PRINT_CODE
    if (!parser.hadError) {
        Chunk_disassemble(currentChunk(), "code");
    }
#endif
    emitReturn();
}


// ===== EXPRESSIONS =====

ParseRule *getRule(TokenType tokenType) {
    return &rules[tokenType];
}

static void parsePrecedence(Precedence precedence) {
    advance();
    ParseFn prefixRule = getRule(parser.previous.type)->prefix;
    if (prefixRule == NULL) {
        error("Expect expression.");
        return;
    }
    prefixRule(); // parse left (maybe only) side

    // When input precedence is low, this will most likely hit and keep calling recursively
    // but if it is high, this is unlikely to happen right now.
    //
    // Examples:
    //   1 + 2 * 3
    //     - First prefixRule is 'number' (the 1)
    //     - First getRule-call inside while detects the 'binary' through '+' token
    //     - First infixRule is 'binary'
    //     - Second prefixRule is also number (the 2)
    //     - Second getRule-call inside while detects the 'binary' through '*' token, with higher precedence than +
    //     - Second infixRule is binary again, properly evaluating * before +
    //   1 * 2 + 3
    //     - First prefixRule is 'number' (the 1)
    //     - First getRule-call inside while detects the 'binary' through '*' token
    //     - First infixRule is 'binary'
    //     - Second prefixRule is also number (the 2)
    //     - Second getRule-call inside while detects the 'binary' through '+' token, which LOWER precedence than *
    //     - Second call bails early
    //     - First call eventually evaluates the +
    //
    while (precedence <= getRule(parser.current.type)->precedence) {
        advance(); // consume operator
        ParseFn infixRule = getRule(parser.previous.type)->infix;
        infixRule(); // parse right side
    }
}

static void number() {
    double value = strtod(parser.previous.start, NULL);
    emitConstant(NUMBER_VAL(value));
}

static void grouping() {
    expression();
    consume(TOKEN_RIGHT_PAREN, "Expected ')' after expression.");
}

static void binary() {
    TokenType operatorType = parser.previous.type;
    ParseRule *rule = getRule(operatorType);
    parsePrecedence((Precedence)rule->precedence + 1); // parse deeper, but only for "more important" rules

    switch (operatorType) {
        case TOKEN_BANG_EQUAL: emitBytes(OP_EQUAL, OP_NOT); break;
        case TOKEN_EQUAL_EQUAL: emitByte(OP_EQUAL); break;
        case TOKEN_LESS: emitByte(OP_LESS); break;
        case TOKEN_LESS_EQUAL: emitBytes(OP_GREATER, OP_NOT); break;
        case TOKEN_GREATER: emitByte(OP_GREATER); break;
        case TOKEN_GREATER_EQUAL: emitBytes(OP_LESS, OP_NOT); break;
        case TOKEN_PLUS: emitByte(OP_ADD); break;
        case TOKEN_MINUS: emitByte(OP_SUBTRACT); break;
        case TOKEN_STAR: emitByte(OP_MULTIPLY); break;
        case TOKEN_SLASH: emitByte(OP_DIVIDE); break;
        default: break; // unreachable
    }
}

static void unary() {
    TokenType operatorType = parser.previous.type;

    parsePrecedence(PREC_UNARY);

    switch (operatorType) {
        case TOKEN_MINUS: emitByte(OP_NEGATE); break;
        case TOKEN_BANG: emitByte(OP_NOT); break;
        default: break;
    }
}

static void literal() {
    TokenType literalType = parser.previous.type;
    switch (literalType) {
        case TOKEN_NIL: emitByte(OP_NIL); break;
        case TOKEN_TRUE: emitByte(OP_TRUE); break;
        case TOKEN_FALSE: emitByte(OP_FALSE); break;
        default: break; // unreachanble
    }
}

void expression() {
    parsePrecedence(PREC_ASSIGNMENT);
}


// ===== ERROR HANDLING =====

static void errorAtCurrent(const char *message) {
    errorAt(&parser.current, message);
}

static void error(const char *message) {
    errorAt(&parser.previous, message);
}

static void errorAt(Token *token, const char *message) {
    if (parser.panicMode) return;
    parser.panicMode = true;
    fprintf(stderr, "[line %d] Error", token->line);

    if (token->type == TOKEN_EOF) {
        fprintf(stderr, " at end");
    } else if (token->type == TOKEN_ERROR) {
        // Nothing.
    } else {
        fprintf(stderr, " at '%.*s'", token->length, token->start);
    }

    fprintf(stderr, ": %s\n", message);
    parser.hadError = true;
}


ParseRule rules[] = {
        [TOKEN_LEFT_PAREN]    = {grouping, NULL,   PREC_NONE},
        [TOKEN_RIGHT_PAREN]   = {NULL,     NULL,   PREC_NONE},
        [TOKEN_LEFT_BRACE]    = {NULL,     NULL,   PREC_NONE},
        [TOKEN_RIGHT_BRACE]   = {NULL,     NULL,   PREC_NONE},
        [TOKEN_COMMA]         = {NULL,     NULL,   PREC_NONE},
        [TOKEN_DOT]           = {NULL,     NULL,   PREC_NONE},
        [TOKEN_MINUS]         = {unary,    binary, PREC_TERM},
        [TOKEN_PLUS]          = {NULL,     binary, PREC_TERM},
        [TOKEN_SEMICOLON]     = {NULL,     NULL,   PREC_NONE},
        [TOKEN_SLASH]         = {NULL,     binary, PREC_FACTOR},
        [TOKEN_STAR]          = {NULL,     binary, PREC_FACTOR},
        [TOKEN_BANG]          = {unary,    NULL,   PREC_NONE},
        [TOKEN_BANG_EQUAL]    = {NULL,     binary, PREC_EQUALITY},
        [TOKEN_EQUAL]         = {NULL,     NULL,   PREC_NONE},
        [TOKEN_EQUAL_EQUAL]   = {NULL,     binary, PREC_EQUALITY},
        [TOKEN_GREATER]       = {NULL,     binary, PREC_COMPARISON},
        [TOKEN_GREATER_EQUAL] = {NULL,     binary, PREC_COMPARISON},
        [TOKEN_LESS]          = {NULL,     binary, PREC_COMPARISON},
        [TOKEN_LESS_EQUAL]    = {NULL,     binary, PREC_COMPARISON},
        [TOKEN_IDENTIFIER]    = {NULL,     NULL,   PREC_NONE},
        [TOKEN_STRING]        = {NULL,     NULL,   PREC_NONE},
        [TOKEN_NUMBER]        = {number,   NULL,   PREC_NONE},
        [TOKEN_AND]           = {NULL,     NULL,   PREC_NONE},
        [TOKEN_CLASS]         = {NULL,     NULL,   PREC_NONE},
        [TOKEN_ELSE]          = {NULL,     NULL,   PREC_NONE},
        [TOKEN_FALSE]         = {literal,  NULL,   PREC_NONE},
        [TOKEN_FOR]           = {NULL,     NULL,   PREC_NONE},
        [TOKEN_FUN]           = {NULL,     NULL,   PREC_NONE},
        [TOKEN_IF]            = {NULL,     NULL,   PREC_NONE},
        [TOKEN_NIL]           = {literal,  NULL,   PREC_NONE},
        [TOKEN_OR]            = {NULL,     NULL,   PREC_NONE},
        [TOKEN_PRINT]         = {NULL,     NULL,   PREC_NONE},
        [TOKEN_RETURN]        = {NULL,     NULL,   PREC_NONE},
        [TOKEN_SUPER]         = {NULL,     NULL,   PREC_NONE},
        [TOKEN_THIS]          = {NULL,     NULL,   PREC_NONE},
        [TOKEN_TRUE]          = {literal,  NULL,   PREC_NONE},
        [TOKEN_VAR]           = {NULL,     NULL,   PREC_NONE},
        [TOKEN_WHILE]         = {NULL,     NULL,   PREC_NONE},
        [TOKEN_ERROR]         = {NULL,     NULL,   PREC_NONE},
        [TOKEN_EOF]           = {NULL,     NULL,   PREC_NONE},
};