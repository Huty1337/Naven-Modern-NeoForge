package org.msgpack.value;

import java.math.BigInteger;

public interface NumberValue extends Value {
   byte toByte();

   short toShort();

   int toInt();

   long toLong();

   BigInteger toBigInteger();

   float toFloat();

   double toDouble();
}
