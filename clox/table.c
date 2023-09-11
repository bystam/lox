//
// Created by Fredrik Bystam on 2023-09-11.
//

#include "table.h"
#include "memory.h"
#include <string.h>

#define TABLE_MAX_LOAD 0.75

static Entry *findEntry(Entry *entries, int capacity, ObjString *key);

static void adjustCapacity(Table *table, int capacity);

void Table_init(Table *table) {
    table->count = 0;
    table->capacity = 0;
    table->entries = NULL;
}

void Table_free(Table *table) {
    FREE_ARRAY(Entry, table->entries, table->capacity);
    Table_init(table);
}

bool Table_set(Table *table, ObjString *key, Value value) {
    if ((table->count + 1) > table->capacity * TABLE_MAX_LOAD) {
        int newCapacity = GROW_CAPACITY(table->capacity);
        adjustCapacity(table, newCapacity);
    }

    Entry *entry = findEntry(table->entries, table->capacity, key);
    bool isNewKey = entry->key == NULL;
    if (isNewKey && IS_NIL(entry->value)) table->count++;
    entry->key = key;
    entry->value = value;
    return isNewKey;
}

bool Table_get(Table *table, ObjString *key, Value *value) {
    if (table->count == 0) return false;
    Entry *entry = findEntry(table->entries, table->capacity, key);
    if (entry->key != key) {
        return false;
    }
    *value = entry->value;
    return true;
}

bool Table_delete(Table *table, ObjString *key) {
    if (table->capacity == 0) return false;
    Entry *entry = findEntry(table->entries, table->capacity, key);
    if (entry->key != key) return false;

    entry->key = NULL;
    entry->value = BOOL_VAL(true); // tombstone
    return true;
}

void Table_addAll(Table *src, Table *dst) {
    for (int i = 0; i < src->capacity; ++i) {
        Entry *entry = src->entries + i;
        if (entry->key != NULL) {
            Table_set(dst, entry->key, entry->value);
        }
    }
}

ObjString *Table_findString(Table *table, const char *chars, int length, uint32_t hash) {
    if (table->count == 0) return NULL;

    uint32_t index = hash % table->capacity;
    for (;;) {
        Entry *entry = table->entries + index;
        if (entry->key == NULL) {
            if (IS_NIL(entry->value)) return NULL;
        } else if (
                entry->key->length == length &&
                entry->key->hash == hash &&
                memcmp(entry->key->chars, chars, length) == 0) {
            return entry->key;
        }
        index = (index + 1) % table->capacity;
    }
}

static void adjustCapacity(Table *table, int capacity) {
    // new, empty bucket buffer
    Entry *newEntries = ALLOCATE(Entry, capacity);
    for (int i = 0; i < capacity; ++i) {
        newEntries[i].key = NULL;
        newEntries[i].value = NIL_VAL;
    }

    // move all the old values into the new buffer
    table->count = 0;
    for (int i = 0; i < table->capacity; ++i) {
        Entry *src = table->entries + i;
        if (src->key == NULL) continue;
        Entry *dst = findEntry(newEntries, capacity, src->key);
        dst->key = src->key;
        dst->value = src->value;
        table->count++;
    }

    FREE_ARRAY(Entry, table->entries, table->capacity);

    table->capacity = capacity;
    table->entries = newEntries;
}

static Entry *findEntry(Entry *entries, int capacity, ObjString *key) {
    uint32_t index = key->hash % capacity;
    Entry *tombstone = NULL;
    for (;;) {
        Entry *entry = entries + index;
        if (entry->key == NULL) {
            if (IS_NIL(entry->value)) {
                return tombstone != NULL ? tombstone : entry;
            } else {
                if (tombstone == NULL) tombstone = entry;
            }
            if (entry->key == key || entry->key == NULL) {
                return entry;
            }
        } else if (entry->key == key) {
            return entry;
        }

        index = (index + 1) % capacity;
    }
}
