//
// Created by Fredrik Bystam on 2023-09-01.
//

#ifndef clox_chunk_h
#define clox_chunk_h

#include "common.h"

typedef enum {
    OP_RETURN
} OpCode;

typedef struct {
    int count;
    int capacity;
    uint8_t *code;
} Chunk;

void Chunk_init(Chunk *chunk);
void Chunk_write(Chunk *chunk, uint8_t byte);
void Chunk_free(Chunk *chunk);

#endif
