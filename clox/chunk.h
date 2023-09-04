//
// Created by Fredrik Bystam on 2023-09-01.
//

#ifndef clox_chunk_h
#define clox_chunk_h

#include "common.h"
#include "value.h"

typedef enum {
    OP_CONSTANT,
    OP_ADD,
    OP_SUBTRACT,
    OP_MULTIPLY,
    OP_DIVIDE,
    OP_NEGATE,
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
