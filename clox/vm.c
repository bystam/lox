//
// Created by Fredrik Bystam on 2023-09-04.
//

#include "vm.h"
#include "debug.h"
#include "compilers.h"
#include <stdio.h>
#include <stdarg.h>

VM vm;

static void resetStack() {
    vm.stackTop = vm.stack;
}

void VM_init() {
    resetStack();
}

void VM_free() {
}

static Value peek(int distance);
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
        double b = AS_NUMBER(VM_stackPop()); \
        double a = AS_NUMBER(VM_stackPop()); \
        VM_stackPush(valueType(a operator b)); \
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
                VM_stackPush(constant);
                break;
            }
            case OP_ADD:      BINARY_OP(NUMBER_VAL, +); break;
            case OP_SUBTRACT: BINARY_OP(NUMBER_VAL, -); break;
            case OP_MULTIPLY: BINARY_OP(NUMBER_VAL, *); break;
            case OP_DIVIDE:   BINARY_OP(NUMBER_VAL, /); break;
            case OP_NEGATE: {
                if (!IS_NUMBER(peek(0))) {
                    runtimeError("Operand must be a number.");
                    return INTERPRET_RUNTIME_ERROR;
                }

                VM_stackPush(NUMBER_VAL(-AS_NUMBER(VM_stackPop())));
                break;
            }
            case OP_RETURN: {
                Value_print(VM_stackPop());
                printf("\n");
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

void VM_stackPush(Value value) {
    *(vm.stackTop++) = value;
}

Value VM_stackPop() {
    return *(--vm.stackTop);
}

static Value peek(int distance) {
    return vm.stackTop[-1 - distance];
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