//
// Created by Fredrik Bystam on 2023-09-01.
//

#ifndef clox_chunk_h
#define clox_chunk_h

#include "common.h"
#include "value.h"

typedef enum {
    OP_CONSTANT,
    OP_NIL,
    OP_TRUE,
    OP_FALSE,
    OP_POP,
    OP_GET_GLOBAL,
    OP_GET_LOCAL,
    OP_DEFINE_GLOBAL,
    OP_SET_GLOBAL,
    OP_SET_LOCAL,
    OP_EQUAL,
    OP_GREATER,
    OP_LESS,
    OP_ADD,
    OP_SUBTRACT,
    OP_MULTIPLY,
    OP_DIVIDE,
    OP_NOT,
    OP_NEGATE,
    OP_PRINT,
    OP_JUMP,
    OP_JUMP_IF_FALSE,
    OP_LOOP,
    OP_RETURN,
} OpCode;

typedef struct {
    int count;
    int capacity;
    uint8_t *code;
    int *lines;
    ValueArray constants;
} Chunk;

void Chunk_init(Chunk *chunk);
void Chunk_write(Chunk *chunk, uint8_t byte, int line);
int Chunk_addConstant(Chunk *chunk, Value value);
void Chunk_free(Chunk *chunk);

#endif
