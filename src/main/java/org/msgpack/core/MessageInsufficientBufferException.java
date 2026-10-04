package org.msgpack.core;

public class MessageInsufficientBufferException extends MessagePackException {
   public MessageInsufficientBufferException() {
   }

   public MessageInsufficientBufferException(String message) {
      super(message);
   }

   public MessageInsufficientBufferException(Throwable cause) {
      super(cause);
   }

   public MessageInsufficientBufferException(String message, Throwable cause) {
      super(message, cause);
   }
}
