//
// Created by Fredrik Bystam on 2023-09-01.
//

#ifndef clox_debug_h
#define clox_debug_h

#include "chunk.h"

void Chunk_disassemble(Chunk *chunk, const char *name);
int Chunk_disassembleInstruction(Chunk *chunk, int offset);

#endif //clox_debug_h
