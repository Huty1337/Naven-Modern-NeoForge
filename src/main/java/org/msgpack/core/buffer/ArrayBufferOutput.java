package org.msgpack.core.buffer;

import java.util.ArrayList;
import java.util.List;

public class ArrayBufferOutput implements MessageBufferOutput {
   private final List<MessageBuffer> list;
   private final int bufferSize;
   private MessageBuffer lastBuffer;

   public ArrayBufferOutput() {
      this(8192);
   }

   public ArrayBufferOutput(int bufferSize) {
      this.bufferSize = bufferSize;
      this.list = new ArrayList<>();
   }

   public int getSize() {
      int size = 0;

      for (MessageBuffer buffer : this.list) {
         size += buffer.size();
      }

      return size;
   }

   public byte[] toByteArray() {
      byte[] data = new byte[this.getSize()];
      int off = 0;

      for (MessageBuffer buffer : this.list) {
         buffer.getBytes(0, data, off, buffer.size());
         off += buffer.size();
      }

      return data;
   }

   public MessageBuffer toMessageBuffer() {
      if (this.list.size() == 1) {
         return this.list.get(0);
      } else {
         return this.list.isEmpty() ? MessageBuffer.allocate(0) : MessageBuffer.wrap(this.toByteArray());
      }
   }

   public List<MessageBuffer> toBufferList() {
      return new ArrayList<>(this.list);
   }

   public void clear() {
      this.list.clear();
   }

   @Override
   public MessageBuffer next(int minimumSize) {
      if (this.lastBuffer != null && this.lastBuffer.size() > minimumSize) {
         return this.lastBuffer;
      } else {
         int size = Math.max(this.bufferSize, minimumSize);
         MessageBuffer buffer = MessageBuffer.allocate(size);
         this.lastBuffer = buffer;
         return buffer;
      }
   }

   @Override
   public void writeBuffer(int length) {
      this.list.add(this.lastBuffer.slice(0, length));
      if (this.lastBuffer.size() - length > this.bufferSize / 4) {
         this.lastBuffer = this.lastBuffer.slice(length, this.lastBuffer.size() - length);
      } else {
         this.lastBuffer = null;
      }
   }

   @Override
   public void write(byte[] buffer, int offset, int length) {
      MessageBuffer copy = MessageBuffer.allocate(length);
      copy.putBytes(0, buffer, offset, length);
      this.list.add(copy);
   }

   @Override
   public void add(byte[] buffer, int offset, int length) {
      MessageBuffer wrapped = MessageBuffer.wrap(buffer, offset, length);
      this.list.add(wrapped);
   }

   @Override
   public void close() {
   }

   @Override
   public void flush() {
   }
}
