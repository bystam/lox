package lox

import org.junit.jupiter.api.Test

class StdlibTest : BaseTest() {

    @Test
    fun `native array`() {
        executeLox("""
            var arr = builtin_array(5);
            print arr;
            print arr.length;
            
            arr.set(3, "Hey!");
            print arr.get(0);
            print arr.get(3);
        """.trimIndent())
        assertPrintedLines(
            "<native array>",
            "5",
            "nil",
            "Hey!"
        )
    }
}
