//
// Created by Fredrik Bystam on 2023-09-04.
//

#include "vm.h"
#include "debug.h"
#include "compilers.h"
#include <stdio.h>

VM vm;

static void resetStack() {
    vm.stackTop = vm.stack;
}

void VM_init() {
    resetStack();
}

void VM_free() {
}

static InterpretResult run() {
#define READ_BYTE() (*vm.ip++)
#define READ_CONSTANT() (vm.chunk->constants.values[READ_BYTE()])
#define BINARY_OP(operator) \
    do { \
        double b = VM_stackPop(); \
        double a = VM_stackPop(); \
        VM_stackPush(a operator b); \
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
            case OP_ADD:      BINARY_OP(+); break;
            case OP_SUBTRACT: BINARY_OP(-); break;
            case OP_MULTIPLY: BINARY_OP(*); break;
            case OP_DIVIDE:   BINARY_OP(/); break;
            case OP_NEGATE: {
                VM_stackPush(-VM_stackPop());
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
