package org.msgpack.core;

public class MessagePackException extends RuntimeException {
   public static final IllegalStateException UNREACHABLE = new IllegalStateException("Cannot reach here");

   public MessagePackException() {
   }

   public MessagePackException(String message) {
      super(message);
   }

   public MessagePackException(String message, Throwable cause) {
      super(message, cause);
   }

   public MessagePackException(Throwable cause) {
      super(cause);
   }

   public static UnsupportedOperationException UNSUPPORTED(String operationName) {
      return new UnsupportedOperationException(operationName);
   }
}
