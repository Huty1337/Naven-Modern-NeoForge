package org.msgpack.value;

import java.math.BigInteger;
import org.msgpack.core.MessageFormat;

public interface IntegerValue extends NumberValue {
   boolean isInByteRange();

   boolean isInShortRange();

   boolean isInIntRange();

   boolean isInLongRange();

   MessageFormat mostSuccinctMessageFormat();

   byte asByte();

   short asShort();

   int asInt();

   long asLong();

   BigInteger asBigInteger();
}
