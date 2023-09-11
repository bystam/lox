//
// Created by Fredrik Bystam on 2023-09-04.
//

#include "vm.h"
#include "debug.h"
#include "compilers.h"
#include "object.h"
#include "memory.h"
#include <stdio.h>
#include <stdarg.h>
#include <string.h>

VM vm;

static void resetStack() {
    vm.stackTop = vm.stack;
}

void VM_init() {
    resetStack();
    vm.objects = NULL;
    Table_init(&vm.strings);
}

void VM_free() {
    Table_free(&vm.strings);
    freeObjects();
}

static Value peek(int distance);
static void stackPush(Value value);
static Value stackPop();
static bool isFalsy(Value value);
static ObjString *concatenate(ObjString *a, ObjString *b);
static void runtimeError(const char* format, ...);

static InterpretResult run() {
#define READ_BYTE() (*vm.ip++)
#define READ_CONSTANT() (vm.chunk->constants.values[READ_BYTE()])
#define BINARY_OP(valueType, operator) \
    do { \
        if (!IS_NUMBER(peek(0)) || !IS_NUMBER(peek(1))) { \
            runtimeError("Operands must be numbers.");     \
            return INTERPRET_RUNTIME_ERROR; \
        }  \
        double b = AS_NUMBER(stackPop()); \
        double a = AS_NUMBER(stackPop()); \
        stackPush(valueType(a operator b)); \
    } while(false)

    for (;;) {
#ifdef DEBUG_TRACE_EXECUTION
        Chunk_disassembleInstruction(vm.chunk, (int) (vm.ip - vm.chunk->code));
        printf("          ");
        for (Value *slot = vm.stack; slot < vm.stackTop; slot++) {
            printf("[ ");
            Value_print(*slot);
            printf(" ]");
        }
        printf("\n");
#endif
        uint8_t instruction;
        switch (instruction = READ_BYTE()) {
            case OP_CONSTANT: {
                Value constant = READ_CONSTANT();
                stackPush(constant);
                break;
            }
            case OP_NIL: stackPush(NIL_VAL); break;
            case OP_TRUE: stackPush(BOOL_VAL(true)); break;
            case OP_FALSE: stackPush(BOOL_VAL(false)); break;

            case OP_EQUAL: {
                Value b = stackPop();
                Value a = stackPop();
                stackPush(BOOL_VAL(Value_equal(a, b)));
                break;
            }
            case OP_LESS:     BINARY_OP(BOOL_VAL, <); break;
            case OP_GREATER:  BINARY_OP(BOOL_VAL, >); break;
            case OP_ADD: {
                if (IS_STRING(peek(0)) && IS_STRING(peek(1))) {
                    ObjString *b = AS_STRING(stackPop());
                    ObjString *a = AS_STRING(stackPop());
                    stackPush(OBJ_VAL(concatenate(a, b)));
                } else if (IS_NUMBER(peek(0)) && IS_NUMBER(peek(1))) {
                    double b = AS_NUMBER(stackPop());
                    double a = AS_NUMBER(stackPop());
                    stackPush(NUMBER_VAL(a + b));
                } else {
                    runtimeError("Operands must be two numbers or two strings.");
                    return INTERPRET_RUNTIME_ERROR;
                }
                break;
            }
            case OP_SUBTRACT: BINARY_OP(NUMBER_VAL, -); break;
            case OP_MULTIPLY: BINARY_OP(NUMBER_VAL, *); break;
            case OP_DIVIDE:   BINARY_OP(NUMBER_VAL, /); break;
            case OP_NEGATE: {
                if (!IS_NUMBER(peek(0))) {
                    runtimeError("Operand must be a number.");
                    return INTERPRET_RUNTIME_ERROR;
                }

                stackPush(NUMBER_VAL(-AS_NUMBER(stackPop())));
                break;
            }

            case OP_NOT: {
                Value value = stackPop();
                stackPush(BOOL_VAL(isFalsy(value)));
                break;
            }

            case OP_PRINT: {
                Value_print(stackPop());
                printf("\n");
                break;
            }

            case OP_RETURN: {
                return INTERPRET_OK;
            }
        }
    }

#undef READ_BYTE
#undef READ_CONSTANT
#undef BINARY_OP
}

InterpretResult VM_interpret(const char *source) {
    Chunk chunk;
    Chunk_init(&chunk);

    if (!compile(source, &chunk)) {
        Chunk_free(&chunk);
        return INTERPRET_COMPILE_ERROR;
    }

    vm.chunk = &chunk;
    vm.ip = vm.chunk->code;

    InterpretResult result = run();

    Chunk_free(&chunk);
    return result;
}

static void stackPush(Value value) {
    *(vm.stackTop++) = value;
}

static Value stackPop() {
    return *(--vm.stackTop);
}

static Value peek(int distance) {
    return vm.stackTop[-1 - distance];
}

static bool isFalsy(Value value) {
    return IS_NIL(value) || (IS_BOOL(value) && AS_BOOL(value) == false);
}

static ObjString *concatenate(ObjString *a, ObjString *b) {
    int length = a->length + b->length;
    char *heapChars = ALLOCATE(char, length + 1);
    memcpy(heapChars, a->chars, a->length);
    memcpy(heapChars + a->length, b->chars, b->length);
    heapChars[length] = '\0';
    return ObjString_takeFrom(heapChars, length);
}

static void runtimeError(const char* format, ...) {
    va_list args;
    va_start(args, format);
    vfprintf(stderr, format, args);
    va_end(args);
    fputs("\n", stderr);

    size_t instruction = vm.ip - vm.chunk->code - 1;
    int line = vm.chunk->lines[instruction];
    fprintf(stderr, "[line %d] in script\n", line);
    resetStack();
}