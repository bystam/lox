package lox

object StdlibParts {
    const val Array = """
        class Array {
          init() {
            this.buffer = builtin_array(4);
            this.length = 0;
          }
          
          add(value) {
            this.__ensureSize();
            this.buffer.set(this.length, value);
            this.length = this.length + 1;
          }
          
          removeLast() {
            this.length = this.length - 1;
            this.buffer.set(this.length, nil);
          }
         
          __ensureSize() {
            if (this.length == this.buffer.length) {
              var newBuffer = builtin_array(this.buffer.length * 2);
              for (var i = 0; i < this.length; i = i + 1) {
                newBuffer.set(i, this.buffer.get(i));
              }
              this.buffer = newBuffer;
            }
          }
        }
    """
}

const val Stdlib = """
    ${StdlibParts.Array}
    
"""
