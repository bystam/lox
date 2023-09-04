//
// Created by Fredrik Bystam on 2023-09-04.
//

#include "value.h"
#include "memory.h"
#include <stdio.h>

void Value_print(Value value) {
    printf("%g", AS_NUMBER(value));
}

void ValueArray_init(ValueArray *array) {
    array->count = 0;
    array->capacity = 0;
    array->values = NULL;
}

void ValueArray_write(ValueArray *array, Value value) {
    if (array->capacity <= array->count) {
        int oldCapacity = array->capacity;
        array->capacity = GROW_CAPACITY(array->capacity);;
        array->values = GROW_ARRAY(Value, array->values, oldCapacity, array->capacity);
    }
    array->values[array->count++] = value;
}

void ValueArray_free(ValueArray *array) {
    FREE_ARRAY(Value, array->values, array->capacity);
    ValueArray_init(array);
}
