//
// Created by Fredrik Bystam on 2023-09-04.
//

#ifndef CLOX_COMPILERS_H
#define CLOX_COMPILERS_H

#include "chunk.h"

bool compile(const char *source, Chunk *chunk);

#endif //CLOX_COMPILERS_H
