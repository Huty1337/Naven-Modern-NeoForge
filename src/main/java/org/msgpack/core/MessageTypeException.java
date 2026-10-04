package org.msgpack.core;

public class MessageTypeException extends MessagePackException {
   public MessageTypeException() {
   }

   public MessageTypeException(String message) {
      super(message);
   }

   public MessageTypeException(String message, Throwable cause) {
      super(message, cause);
   }

   public MessageTypeException(Throwable cause) {
      super(cause);
   }
}
