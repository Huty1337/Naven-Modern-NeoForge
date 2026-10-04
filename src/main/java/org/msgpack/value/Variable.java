package org.msgpack.value;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.msgpack.core.MessageFormat;
import org.msgpack.core.MessageIntegerOverflowException;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageStringCodingException;
import org.msgpack.core.MessageTypeCastException;
import org.msgpack.value.impl.ImmutableBigIntegerValueImpl;

public class Variable implements Value {
   private final Variable.NilValueAccessor nilAccessor = new Variable.NilValueAccessor();
   private final Variable.BooleanValueAccessor booleanAccessor = new Variable.BooleanValueAccessor();
   private final Variable.IntegerValueAccessor integerAccessor = new Variable.IntegerValueAccessor();
   private final Variable.FloatValueAccessor floatAccessor = new Variable.FloatValueAccessor();
   private final Variable.BinaryValueAccessor binaryAccessor = new Variable.BinaryValueAccessor();
   private final Variable.StringValueAccessor stringAccessor = new Variable.StringValueAccessor();
   private final Variable.ArrayValueAccessor arrayAccessor = new Variable.ArrayValueAccessor();
   private final Variable.MapValueAccessor mapAccessor = new Variable.MapValueAccessor();
   private final Variable.ExtensionValueAccessor extensionAccessor = new Variable.ExtensionValueAccessor();
   private final Variable.TimestampValueAccessor timestampAccessor = new Variable.TimestampValueAccessor();
   private Variable.Type type;
   private long longValue;
   private double doubleValue;
   private Object objectValue;
   private Variable.AbstractValueAccessor accessor;
   private static final BigInteger LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);
   private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);
   private static final long BYTE_MIN = -128L;
   private static final long BYTE_MAX = 127L;
   private static final long SHORT_MIN = -32768L;
   private static final long SHORT_MAX = 32767L;
   private static final long INT_MIN = -2147483648L;
   private static final long INT_MAX = 2147483647L;

   public Variable() {
      this.setNilValue();
   }

   public Variable setNilValue() {
      this.type = Variable.Type.NULL;
      this.accessor = this.nilAccessor;
      return this;
   }

   public Variable setBooleanValue(boolean v) {
      this.type = Variable.Type.BOOLEAN;
      this.accessor = this.booleanAccessor;
      this.longValue = v ? 1L : 0L;
      return this;
   }

   public Variable setIntegerValue(long v) {
      this.type = Variable.Type.LONG;
      this.accessor = this.integerAccessor;
      this.longValue = v;
      return this;
   }

   public Variable setIntegerValue(BigInteger v) {
      if (0 <= v.compareTo(LONG_MIN) && v.compareTo(LONG_MAX) <= 0) {
         this.type = Variable.Type.LONG;
         this.accessor = this.integerAccessor;
         this.longValue = v.longValue();
      } else {
         this.type = Variable.Type.BIG_INTEGER;
         this.accessor = this.integerAccessor;
         this.objectValue = v;
      }

      return this;
   }

   public Variable setFloatValue(double v) {
      this.type = Variable.Type.DOUBLE;
      this.accessor = this.floatAccessor;
      this.doubleValue = v;
      this.longValue = (long)v;
      return this;
   }

   public Variable setFloatValue(float v) {
      this.type = Variable.Type.DOUBLE;
      this.accessor = this.floatAccessor;
      this.longValue = (long)v;
      return this;
   }

   public Variable setBinaryValue(byte[] v) {
      this.type = Variable.Type.BYTE_ARRAY;
      this.accessor = this.binaryAccessor;
      this.objectValue = v;
      return this;
   }

   public Variable setStringValue(String v) {
      return this.setStringValue(v.getBytes(MessagePack.UTF8));
   }

   public Variable setStringValue(byte[] v) {
      this.type = Variable.Type.RAW_STRING;
      this.accessor = this.stringAccessor;
      this.objectValue = v;
      return this;
   }

   public Variable setArrayValue(List<Value> v) {
      this.type = Variable.Type.LIST;
      this.accessor = this.arrayAccessor;
      this.objectValue = v.toArray(new Value[v.size()]);
      return this;
   }

   public Variable setArrayValue(Value[] v) {
      this.type = Variable.Type.LIST;
      this.accessor = this.arrayAccessor;
      this.objectValue = v;
      return this;
   }

   public Variable setMapValue(Map<Value, Value> v) {
      this.type = Variable.Type.MAP;
      this.accessor = this.mapAccessor;
      Value[] kvs = new Value[v.size() * 2];
      Iterator<Entry<Value, Value>> ite = v.entrySet().iterator();

      for (int i = 0; ite.hasNext(); i++) {
         Entry<Value, Value> pair = ite.next();
         kvs[i] = pair.getKey();
         kvs[i] = pair.getValue();
         i++;
      }

      this.objectValue = kvs;
      return this;
   }

   public Variable setMapValue(Value[] kvs) {
      this.type = Variable.Type.MAP;
      this.accessor = this.mapAccessor;
      this.objectValue = kvs;
      return this;
   }

   public Variable setExtensionValue(byte type, byte[] data) {
      this.type = Variable.Type.EXTENSION;
      this.accessor = this.extensionAccessor;
      this.objectValue = ValueFactory.newExtension(type, data);
      return this;
   }

   public Variable setTimestampValue(Instant timestamp) {
      this.type = Variable.Type.TIMESTAMP;
      this.accessor = this.timestampAccessor;
      this.objectValue = ValueFactory.newTimestamp(timestamp);
      return this;
   }

   @Override
   public ImmutableValue immutableValue() {
      return this.accessor.immutableValue();
   }

   @Override
   public void writeTo(MessagePacker pk) throws IOException {
      this.accessor.writeTo(pk);
   }

   @Override
   public int hashCode() {
      return this.immutableValue().hashCode();
   }

   @Override
   public boolean equals(Object o) {
      return this.immutableValue().equals(o);
   }

   @Override
   public String toJson() {
      return this.immutableValue().toJson();
   }

   @Override
   public String toString() {
      return this.immutableValue().toString();
   }

   @Override
   public ValueType getValueType() {
      return this.type.getValueType();
   }

   @Override
   public boolean isNilValue() {
      return this.getValueType().isNilType();
   }

   @Override
   public boolean isBooleanValue() {
      return this.getValueType().isBooleanType();
   }

   @Override
   public boolean isNumberValue() {
      return this.getValueType().isNumberType();
   }

   @Override
   public boolean isIntegerValue() {
      return this.getValueType().isIntegerType();
   }

   @Override
   public boolean isFloatValue() {
      return this.getValueType().isFloatType();
   }

   @Override
   public boolean isRawValue() {
      return this.getValueType().isRawType();
   }

   @Override
   public boolean isBinaryValue() {
      return this.getValueType().isBinaryType();
   }

   @Override
   public boolean isStringValue() {
      return this.getValueType().isStringType();
   }

   @Override
   public boolean isArrayValue() {
      return this.getValueType().isArrayType();
   }

   @Override
   public boolean isMapValue() {
      return this.getValueType().isMapType();
   }

   @Override
   public boolean isExtensionValue() {
      return this.getValueType().isExtensionType();
   }

   @Override
   public boolean isTimestampValue() {
      return this.type == Variable.Type.TIMESTAMP;
   }

   @Override
   public NilValue asNilValue() {
      if (!this.isNilValue()) {
         throw new MessageTypeCastException();
      } else {
         return (NilValue)this.accessor;
      }
   }

   @Override
   public BooleanValue asBooleanValue() {
      if (!this.isBooleanValue()) {
         throw new MessageTypeCastException();
      } else {
         return (BooleanValue)this.accessor;
      }
   }

   @Override
   public NumberValue asNumberValue() {
      if (!this.isNumberValue()) {
         throw new MessageTypeCastException();
      } else {
         return (NumberValue)this.accessor;
      }
   }

   @Override
   public IntegerValue asIntegerValue() {
      if (!this.isIntegerValue()) {
         throw new MessageTypeCastException();
      } else {
         return (IntegerValue)this.accessor;
      }
   }

   @Override
   public FloatValue asFloatValue() {
      if (!this.isFloatValue()) {
         throw new MessageTypeCastException();
      } else {
         return (FloatValue)this.accessor;
      }
   }

   @Override
   public RawValue asRawValue() {
      if (!this.isRawValue()) {
         throw new MessageTypeCastException();
      } else {
         return (RawValue)this.accessor;
      }
   }

   @Override
   public BinaryValue asBinaryValue() {
      if (!this.isBinaryValue()) {
         throw new MessageTypeCastException();
      } else {
         return (BinaryValue)this.accessor;
      }
   }

   @Override
   public StringValue asStringValue() {
      if (!this.isStringValue()) {
         throw new MessageTypeCastException();
      } else {
         return (StringValue)this.accessor;
      }
   }

   @Override
   public ArrayValue asArrayValue() {
      if (!this.isArrayValue()) {
         throw new MessageTypeCastException();
      } else {
         return (ArrayValue)this.accessor;
      }
   }

   @Override
   public MapValue asMapValue() {
      if (!this.isMapValue()) {
         throw new MessageTypeCastException();
      } else {
         return (MapValue)this.accessor;
      }
   }

   @Override
   public ExtensionValue asExtensionValue() {
      if (!this.isExtensionValue()) {
         throw new MessageTypeCastException();
      } else {
         return (ExtensionValue)this.accessor;
      }
   }

   @Override
   public TimestampValue asTimestampValue() {
      if (!this.isTimestampValue()) {
         throw new MessageTypeCastException();
      } else {
         return (TimestampValue)this.accessor;
      }
   }

   private abstract class AbstractNumberValueAccessor extends Variable.AbstractValueAccessor implements NumberValue {
      @Override
      public NumberValue asNumberValue() {
         return this;
      }

      @Override
      public byte toByte() {
         return Variable.this.type == Variable.Type.BIG_INTEGER ? ((BigInteger)Variable.this.objectValue).byteValue() : (byte)((int)Variable.this.longValue);
      }

      @Override
      public short toShort() {
         return Variable.this.type == Variable.Type.BIG_INTEGER ? ((BigInteger)Variable.this.objectValue).shortValue() : (short)((int)Variable.this.longValue);
      }

      @Override
      public int toInt() {
         return Variable.this.type == Variable.Type.BIG_INTEGER ? ((BigInteger)Variable.this.objectValue).intValue() : (int)Variable.this.longValue;
      }

      @Override
      public long toLong() {
         return Variable.this.type == Variable.Type.BIG_INTEGER ? ((BigInteger)Variable.this.objectValue).longValue() : Variable.this.longValue;
      }

      @Override
      public BigInteger toBigInteger() {
         if (Variable.this.type == Variable.Type.BIG_INTEGER) {
            return (BigInteger)Variable.this.objectValue;
         } else {
            return Variable.this.type == Variable.Type.DOUBLE
               ? new BigDecimal(Variable.this.doubleValue).toBigInteger()
               : BigInteger.valueOf(Variable.this.longValue);
         }
      }

      @Override
      public float toFloat() {
         if (Variable.this.type == Variable.Type.BIG_INTEGER) {
            return ((BigInteger)Variable.this.objectValue).floatValue();
         } else {
            return Variable.this.type == Variable.Type.DOUBLE ? (float)Variable.this.doubleValue : (float)Variable.this.longValue;
         }
      }

      @Override
      public double toDouble() {
         if (Variable.this.type == Variable.Type.BIG_INTEGER) {
            return ((BigInteger)Variable.this.objectValue).doubleValue();
         } else {
            return Variable.this.type == Variable.Type.DOUBLE ? Variable.this.doubleValue : (double)Variable.this.longValue;
         }
      }
   }

   private abstract class AbstractRawValueAccessor extends Variable.AbstractValueAccessor implements RawValue {
      @Override
      public RawValue asRawValue() {
         return this;
      }

      @Override
      public byte[] asByteArray() {
         return (byte[])Variable.this.objectValue;
      }

      @Override
      public ByteBuffer asByteBuffer() {
         return ByteBuffer.wrap(this.asByteArray());
      }

      @Override
      public String asString() {
         byte[] raw = (byte[])Variable.this.objectValue;

         try {
            CharsetDecoder reportDecoder = MessagePack.UTF8
               .newDecoder()
               .onMalformedInput(CodingErrorAction.REPORT)
               .onUnmappableCharacter(CodingErrorAction.REPORT);
            return reportDecoder.decode(ByteBuffer.wrap(raw)).toString();
         } catch (CharacterCodingException var3) {
            throw new MessageStringCodingException(var3);
         }
      }

      @Override
      public String toString() {
         byte[] raw = (byte[])Variable.this.objectValue;

         try {
            CharsetDecoder reportDecoder = MessagePack.UTF8
               .newDecoder()
               .onMalformedInput(CodingErrorAction.REPLACE)
               .onUnmappableCharacter(CodingErrorAction.REPLACE);
            return reportDecoder.decode(ByteBuffer.wrap(raw)).toString();
         } catch (CharacterCodingException var3) {
            throw new MessageStringCodingException(var3);
         }
      }
   }

   private abstract class AbstractValueAccessor implements Value {
      @Override
      public boolean isNilValue() {
         return this.getValueType().isNilType();
      }

      @Override
      public boolean isBooleanValue() {
         return this.getValueType().isBooleanType();
      }

      @Override
      public boolean isNumberValue() {
         return this.getValueType().isNumberType();
      }

      @Override
      public boolean isIntegerValue() {
         return this.getValueType().isIntegerType();
      }

      @Override
      public boolean isFloatValue() {
         return this.getValueType().isFloatType();
      }

      @Override
      public boolean isRawValue() {
         return this.getValueType().isRawType();
      }

      @Override
      public boolean isBinaryValue() {
         return this.getValueType().isBinaryType();
      }

      @Override
      public boolean isStringValue() {
         return this.getValueType().isStringType();
      }

      @Override
      public boolean isArrayValue() {
         return this.getValueType().isArrayType();
      }

      @Override
      public boolean isMapValue() {
         return this.getValueType().isMapType();
      }

      @Override
      public boolean isExtensionValue() {
         return this.getValueType().isExtensionType();
      }

      @Override
      public boolean isTimestampValue() {
         return false;
      }

      @Override
      public NilValue asNilValue() {
         throw new MessageTypeCastException();
      }

      @Override
      public BooleanValue asBooleanValue() {
         throw new MessageTypeCastException();
      }

      @Override
      public NumberValue asNumberValue() {
         throw new MessageTypeCastException();
      }

      @Override
      public IntegerValue asIntegerValue() {
         throw new MessageTypeCastException();
      }

      @Override
      public FloatValue asFloatValue() {
         throw new MessageTypeCastException();
      }

      @Override
      public RawValue asRawValue() {
         throw new MessageTypeCastException();
      }

      @Override
      public BinaryValue asBinaryValue() {
         throw new MessageTypeCastException();
      }

      @Override
      public StringValue asStringValue() {
         throw new MessageTypeCastException();
      }

      @Override
      public ArrayValue asArrayValue() {
         throw new MessageTypeCastException();
      }

      @Override
      public MapValue asMapValue() {
         throw new MessageTypeCastException();
      }

      @Override
      public ExtensionValue asExtensionValue() {
         throw new MessageTypeCastException();
      }

      @Override
      public TimestampValue asTimestampValue() {
         throw new MessageTypeCastException();
      }

      @Override
      public boolean equals(Object obj) {
         return Variable.this.equals(obj);
      }

      @Override
      public int hashCode() {
         return Variable.this.hashCode();
      }

      @Override
      public String toJson() {
         return Variable.this.toJson();
      }

      @Override
      public String toString() {
         return Variable.this.toString();
      }
   }

   private class ArrayValueAccessor extends Variable.AbstractValueAccessor implements ArrayValue {
      @Override
      public ValueType getValueType() {
         return ValueType.ARRAY;
      }

      @Override
      public ArrayValue asArrayValue() {
         return this;
      }

      public ImmutableArrayValue immutableValue() {
         return ValueFactory.newArray(this.array());
      }

      @Override
      public int size() {
         return this.array().length;
      }

      @Override
      public Value get(int index) {
         return this.array()[index];
      }

      @Override
      public Value getOrNilValue(int index) {
         Value[] a = this.array();
         return (Value)(a.length < index && index >= 0 ? ValueFactory.newNil() : a[index]);
      }

      @Override
      public Iterator<Value> iterator() {
         return this.list().iterator();
      }

      @Override
      public List<Value> list() {
         return Arrays.asList(this.array());
      }

      public Value[] array() {
         return (Value[])Variable.this.objectValue;
      }

      @Override
      public void writeTo(MessagePacker pk) throws IOException {
         this.immutableValue().writeTo(pk);
      }
   }

   private class BinaryValueAccessor extends Variable.AbstractRawValueAccessor implements BinaryValue {
      @Override
      public ValueType getValueType() {
         return ValueType.BINARY;
      }

      @Override
      public BinaryValue asBinaryValue() {
         return this;
      }

      public ImmutableBinaryValue immutableValue() {
         return ValueFactory.newBinary(this.asByteArray());
      }

      @Override
      public void writeTo(MessagePacker pk) throws IOException {
         byte[] data = (byte[])Variable.this.objectValue;
         pk.packBinaryHeader(data.length);
         pk.writePayload(data);
      }
   }

   private class BooleanValueAccessor extends Variable.AbstractValueAccessor implements BooleanValue {
      @Override
      public ValueType getValueType() {
         return ValueType.BOOLEAN;
      }

      @Override
      public BooleanValue asBooleanValue() {
         return this;
      }

      public ImmutableBooleanValue immutableValue() {
         return ValueFactory.newBoolean(this.getBoolean());
      }

      @Override
      public boolean getBoolean() {
         return Variable.this.longValue == 1L;
      }

      @Override
      public void writeTo(MessagePacker pk) throws IOException {
         pk.packBoolean(Variable.this.longValue == 1L);
      }
   }

   private class ExtensionValueAccessor extends Variable.AbstractValueAccessor implements ExtensionValue {
      @Override
      public ValueType getValueType() {
         return ValueType.EXTENSION;
      }

      @Override
      public ExtensionValue asExtensionValue() {
         return this;
      }

      public ImmutableExtensionValue immutableValue() {
         return (ImmutableExtensionValue)Variable.this.objectValue;
      }

      @Override
      public byte getType() {
         return ((ImmutableExtensionValue)Variable.this.objectValue).getType();
      }

      @Override
      public byte[] getData() {
         return ((ImmutableExtensionValue)Variable.this.objectValue).getData();
      }

      @Override
      public void writeTo(MessagePacker pk) throws IOException {
         ((ImmutableExtensionValue)Variable.this.objectValue).writeTo(pk);
      }
   }

   private class FloatValueAccessor extends Variable.AbstractNumberValueAccessor implements FloatValue {
      @Override
      public FloatValue asFloatValue() {
         return this;
      }

      public ImmutableFloatValue immutableValue() {
         return ValueFactory.newFloat(Variable.this.doubleValue);
      }

      @Override
      public ValueType getValueType() {
         return ValueType.FLOAT;
      }

      @Override
      public void writeTo(MessagePacker pk) throws IOException {
         pk.packDouble(Variable.this.doubleValue);
      }
   }

   private class IntegerValueAccessor extends Variable.AbstractNumberValueAccessor implements IntegerValue {
      @Override
      public ValueType getValueType() {
         return ValueType.INTEGER;
      }

      @Override
      public IntegerValue asIntegerValue() {
         return this;
      }

      public ImmutableIntegerValue immutableValue() {
         return Variable.this.type == Variable.Type.BIG_INTEGER
            ? ValueFactory.newInteger((BigInteger)Variable.this.objectValue)
            : ValueFactory.newInteger(Variable.this.longValue);
      }

      @Override
      public boolean isInByteRange() {
         return Variable.this.type == Variable.Type.BIG_INTEGER ? false : -128L <= Variable.this.longValue && Variable.this.longValue <= 127L;
      }

      @Override
      public boolean isInShortRange() {
         return Variable.this.type == Variable.Type.BIG_INTEGER ? false : -32768L <= Variable.this.longValue && Variable.this.longValue <= 32767L;
      }

      @Override
      public boolean isInIntRange() {
         return Variable.this.type == Variable.Type.BIG_INTEGER ? false : -2147483648L <= Variable.this.longValue && Variable.this.longValue <= 2147483647L;
      }

      @Override
      public boolean isInLongRange() {
         return Variable.this.type != Variable.Type.BIG_INTEGER;
      }

      @Override
      public MessageFormat mostSuccinctMessageFormat() {
         return ImmutableBigIntegerValueImpl.mostSuccinctMessageFormat(this);
      }

      @Override
      public byte asByte() {
         if (!this.isInByteRange()) {
            throw new MessageIntegerOverflowException(Variable.this.longValue);
         } else {
            return (byte)((int)Variable.this.longValue);
         }
      }

      @Override
      public short asShort() {
         if (!this.isInByteRange()) {
            throw new MessageIntegerOverflowException(Variable.this.longValue);
         } else {
            return (short)((int)Variable.this.longValue);
         }
      }

      @Override
      public int asInt() {
         if (!this.isInIntRange()) {
            throw new MessageIntegerOverflowException(Variable.this.longValue);
         } else {
            return (int)Variable.this.longValue;
         }
      }

      @Override
      public long asLong() {
         if (!this.isInLongRange()) {
            throw new MessageIntegerOverflowException(Variable.this.longValue);
         } else {
            return Variable.this.longValue;
         }
      }

      @Override
      public BigInteger asBigInteger() {
         return Variable.this.type == Variable.Type.BIG_INTEGER ? (BigInteger)Variable.this.objectValue : BigInteger.valueOf(Variable.this.longValue);
      }

      @Override
      public void writeTo(MessagePacker pk) throws IOException {
         if (Variable.this.type == Variable.Type.BIG_INTEGER) {
            pk.packBigInteger((BigInteger)Variable.this.objectValue);
         } else {
            pk.packLong(Variable.this.longValue);
         }
      }
   }

   private class MapValueAccessor extends Variable.AbstractValueAccessor implements MapValue {
      @Override
      public ValueType getValueType() {
         return ValueType.MAP;
      }

      @Override
      public MapValue asMapValue() {
         return this;
      }

      public ImmutableMapValue immutableValue() {
         return ValueFactory.newMap(this.getKeyValueArray());
      }

      @Override
      public int size() {
         return this.getKeyValueArray().length / 2;
      }

      @Override
      public Set<Value> keySet() {
         return this.immutableValue().keySet();
      }

      @Override
      public Set<Entry<Value, Value>> entrySet() {
         return this.immutableValue().entrySet();
      }

      @Override
      public Collection<Value> values() {
         return this.immutableValue().values();
      }

      @Override
      public Value[] getKeyValueArray() {
         return (Value[])Variable.this.objectValue;
      }

      @Override
      public Map<Value, Value> map() {
         return this.immutableValue().map();
      }

      @Override
      public void writeTo(MessagePacker pk) throws IOException {
         this.immutableValue().writeTo(pk);
      }
   }

   private class NilValueAccessor extends Variable.AbstractValueAccessor implements NilValue {
      @Override
      public ValueType getValueType() {
         return ValueType.NIL;
      }

      @Override
      public NilValue asNilValue() {
         return this;
      }

      public ImmutableNilValue immutableValue() {
         return ValueFactory.newNil();
      }

      @Override
      public void writeTo(MessagePacker pk) throws IOException {
         pk.packNil();
      }
   }

   private class StringValueAccessor extends Variable.AbstractRawValueAccessor implements StringValue {
      @Override
      public ValueType getValueType() {
         return ValueType.STRING;
      }

      @Override
      public StringValue asStringValue() {
         return this;
      }

      public ImmutableStringValue immutableValue() {
         return ValueFactory.newString((byte[])Variable.this.objectValue);
      }

      @Override
      public void writeTo(MessagePacker pk) throws IOException {
         byte[] data = (byte[])Variable.this.objectValue;
         pk.packRawStringHeader(data.length);
         pk.writePayload(data);
      }
   }

   private class TimestampValueAccessor extends Variable.AbstractValueAccessor implements TimestampValue {
      @Override
      public boolean isTimestampValue() {
         return true;
      }

      @Override
      public ValueType getValueType() {
         return ValueType.EXTENSION;
      }

      @Override
      public TimestampValue asTimestampValue() {
         return this;
      }

      public ImmutableTimestampValue immutableValue() {
         return (ImmutableTimestampValue)Variable.this.objectValue;
      }

      @Override
      public byte getType() {
         return ((ImmutableTimestampValue)Variable.this.objectValue).getType();
      }

      @Override
      public byte[] getData() {
         return ((ImmutableTimestampValue)Variable.this.objectValue).getData();
      }

      @Override
      public void writeTo(MessagePacker pk) throws IOException {
         ((ImmutableTimestampValue)Variable.this.objectValue).writeTo(pk);
      }

      @Override
      public long getEpochSecond() {
         return ((ImmutableTimestampValue)Variable.this.objectValue).getEpochSecond();
      }

      @Override
      public int getNano() {
         return ((ImmutableTimestampValue)Variable.this.objectValue).getNano();
      }

      @Override
      public long toEpochMillis() {
         return ((ImmutableTimestampValue)Variable.this.objectValue).toEpochMillis();
      }

      @Override
      public Instant toInstant() {
         return ((ImmutableTimestampValue)Variable.this.objectValue).toInstant();
      }
   }

   public static enum Type {
      NULL(ValueType.NIL),
      BOOLEAN(ValueType.BOOLEAN),
      LONG(ValueType.INTEGER),
      BIG_INTEGER(ValueType.INTEGER),
      DOUBLE(ValueType.FLOAT),
      BYTE_ARRAY(ValueType.BINARY),
      RAW_STRING(ValueType.STRING),
      LIST(ValueType.ARRAY),
      MAP(ValueType.MAP),
      EXTENSION(ValueType.EXTENSION),
      TIMESTAMP(ValueType.EXTENSION);

      private final ValueType valueType;

      private Type(ValueType valueType) {
         this.valueType = valueType;
      }

      public ValueType getValueType() {
         return this.valueType;
      }
   }
}
