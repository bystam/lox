//
// Created by Fredrik Bystam on 2023-09-04.
//

#ifndef CLOX_VALUE_H
#define CLOX_VALUE_H

#include "common.h"

typedef double Value;

void Value_print(Value value);

typedef struct {
    int capacity;
    int count;
    Value *values;
} ValueArray;

void ValueArray_init(ValueArray *array);
void ValueArray_write(ValueArray *array, Value value);
void ValueArray_free(ValueArray *array);

#endif //CLOX_VALUE_H
