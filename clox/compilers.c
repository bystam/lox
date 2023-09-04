//
// Created by Fredrik Bystam on 2023-09-04.
//

#include <stdio.h>
#include "compilers.h"
#include "scanner.h"

void compile(const char *source) {
    Scanner_init(source);
    int line = -1;
    for (;;) {
        Token token = Scanner_nextToken();
        if (token.line != line) {
            printf("%4d ", token.line);
            line = token.line;
        } else {
            printf("   | ");
        }
        printf("%2d '%.*s'\n", token.type, token.length, token.start);

        if (token.type == TOKEN_EOF) break;
    }
}
