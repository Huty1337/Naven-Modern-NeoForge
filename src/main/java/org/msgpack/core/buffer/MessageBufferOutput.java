package org.msgpack.core.buffer;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

public interface MessageBufferOutput extends Closeable, Flushable {
   MessageBuffer next(int var1) throws IOException;

   void writeBuffer(int var1) throws IOException;

   void write(byte[] var1, int var2, int var3) throws IOException;

   void add(byte[] var1, int var2, int var3) throws IOException;
}
