//
// Created by Fredrik Bystam on 2023-09-01.
//

#include <stdio.h>

#include "debug.h"

int simpleInstruction(const char *name, int offset);

void Chunk_disassemble(Chunk *chunk, const char *name) {
    printf("== %s ==\n", name);

    for (int offset = 0; offset < chunk->count;) {
        offset = Chunk_disassembleInstruction(chunk, offset);
    }
}

int Chunk_disassembleInstruction(Chunk *chunk, int offset) {
    uint8_t instruction = chunk->code[offset];
    switch (instruction) {
        case OP_RETURN:
            return simpleInstruction("OP_RETURN", offset);
        default:
            printf("Unknown opcode: %d\n", instruction);
            return offset + 1;
    }
}

int simpleInstruction(const char *name, int offset) {
    printf("%s\n", name);
    return offset + 1;
}