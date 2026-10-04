package org.msgpack.value;

import java.time.Instant;

public interface TimestampValue extends ExtensionValue {
   long getEpochSecond();

   int getNano();

   long toEpochMillis();

   Instant toInstant();
}
