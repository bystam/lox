//
// Created by Fredrik Bystam on 2023-09-04.
//

#include "value.h"
#include "memory.h"
#include "object.h"
#include <stdio.h>
#include <string.h>

void Value_print(Value value) {
    switch (value.type) {
        case VAL_NUMBER:
            printf("%g", AS_NUMBER(value));
            break;
        case VAL_BOOL:
            printf(AS_BOOL(value) ? "true" : "false");
            break;
        case VAL_NIL:
            printf("nil");
            break;
        case VAL_OBJ:
            Obj_print(value); break;
    }
}

bool valuesEqual(Value a, Value b) {
    if (a.type != b.type) return false;
    switch (a.type) {
        case VAL_NIL: return true;
        case VAL_BOOL: return AS_BOOL(a) == AS_BOOL(b);
        case VAL_NUMBER: return AS_NUMBER(a) == AS_NUMBER(b);
        case VAL_OBJ: {
            ObjString *aString = AS_STRING(a);
            ObjString *bString = AS_STRING(a);
            return aString->length == bString->length &&
                memcmp(aString->chars, bString->chars, aString->length) == 0;
        }
    }
    return false;
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
