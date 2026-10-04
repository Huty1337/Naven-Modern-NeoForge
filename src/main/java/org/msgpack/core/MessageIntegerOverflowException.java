package org.msgpack.core;

import java.math.BigInteger;

public class MessageIntegerOverflowException extends MessageTypeException {
   private final BigInteger bigInteger;

   public MessageIntegerOverflowException(BigInteger bigInteger) {
      this.bigInteger = bigInteger;
   }

   public MessageIntegerOverflowException(long value) {
      this(BigInteger.valueOf(value));
   }

   public MessageIntegerOverflowException(String message, BigInteger bigInteger) {
      super(message);
      this.bigInteger = bigInteger;
   }

   public BigInteger getBigInteger() {
      return this.bigInteger;
   }

   @Override
   public String getMessage() {
      return this.bigInteger.toString();
   }
}
