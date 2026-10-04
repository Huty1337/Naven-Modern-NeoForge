package org.msgpack.core;

public class MessageTypeCastException extends MessageTypeException {
   public MessageTypeCastException() {
   }

   public MessageTypeCastException(String message) {
      super(message);
   }

   public MessageTypeCastException(String message, Throwable cause) {
      super(message, cause);
   }

   public MessageTypeCastException(Throwable cause) {
      super(cause);
   }
}
