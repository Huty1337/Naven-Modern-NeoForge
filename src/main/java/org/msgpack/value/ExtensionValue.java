package org.msgpack.value;

public interface ExtensionValue extends Value {
   byte getType();

   byte[] getData();
}
