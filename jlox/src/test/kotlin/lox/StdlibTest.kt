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

    @Test
    fun `high level array`() {
        executeLox("""
            var arr = Array();
            print arr.length;
            
            arr.add("Hey!");
            print arr.length;
            
            arr.removeLast();
            print arr.length;
        """.trimIndent())
        assertPrintedLines("0", "1", "0")
    }

    @Test
    fun `large high level array`() {
        executeLox("""
            var arr = Array();
            
            for (var i = 0; i < 100; i = i + 1) {
              arr.add("woop");
            }
            
            print arr.length;
        """.trimIndent())
        assertPrintedLines("100")
    }
}
