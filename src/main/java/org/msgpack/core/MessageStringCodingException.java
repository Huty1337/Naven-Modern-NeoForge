package org.msgpack.core;

import java.nio.charset.CharacterCodingException;

public class MessageStringCodingException extends MessagePackException {
   public MessageStringCodingException(String message, CharacterCodingException cause) {
      super(message, cause);
   }

   public MessageStringCodingException(CharacterCodingException cause) {
      super(cause);
   }

   public CharacterCodingException getCause() {
      return (CharacterCodingException)super.getCause();
   }
}
