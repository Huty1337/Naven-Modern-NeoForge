package org.msgpack.core;

public class MessageFormatException extends MessagePackException {
   public MessageFormatException(Throwable e) {
      super(e);
   }

   public MessageFormatException(String message) {
      super(message);
   }

   public MessageFormatException(String message, Throwable cause) {
      super(message, cause);
   }
}
