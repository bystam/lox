#include "common.h"

#include <stdio.h>
#include "chunk.h"
#include "debug.h"

int main() {
    Chunk chunk;
    Chunk_init(&chunk);
    Chunk_write(&chunk, OP_RETURN, 123);

    int constant = Chunk_addConstant(&chunk, 1.2);
    Chunk_write(&chunk, OP_CONSTANT, 123);
    Chunk_write(&chunk, constant, 123);

    Chunk_disassemble(&chunk, "test chunk");

    Chunk_free(&chunk);
    return 0;
}
