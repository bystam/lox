//
// Created by Fredrik Bystam on 2023-09-11.
//

#ifndef CLOX_TABLE_H
#define CLOX_TABLE_H

#include "common.h"
#include "object.h"

typedef struct {
    ObjString *key;
    Value value;
} Entry;

typedef struct {
    int count;
    int capacity;
    Entry *entries;
} Table;

void Table_init(Table *table);
void Table_free(Table *table);
bool Table_set(Table *table, ObjString *key, Value value);
bool Table_get(Table *table, ObjString *key, Value *value);
bool Table_delete(Table *table, ObjString *key);
void Table_addAll(Table *src, Table *dst);

ObjString *Table_findString(Table *table, const char *chars, int length, uint32_t hash);

#endif //CLOX_TABLE_H
