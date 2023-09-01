#include "common.h"

#include <stdio.h>
#include "chunk.h"
#include "debug.h"

int main() {
    Chunk chunk;
    Chunk_init(&chunk);
    Chunk_write(&chunk, OP_RETURN);

    Chunk_disassemble(&chunk, "test chunk");

    Chunk_free(&chunk);
    return 0;
}
