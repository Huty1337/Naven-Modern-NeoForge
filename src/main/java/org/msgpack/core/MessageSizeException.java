package org.msgpack.core;

public class MessageSizeException extends MessagePackException {
   private final long size;

   public MessageSizeException(long size) {
      this.size = size;
   }

   public MessageSizeException(String message, long size) {
      super(message);
      this.size = size;
   }

   public long getSize() {
      return this.size;
   }
}
