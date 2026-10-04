package org.msgpack.core;

import java.io.IOException;
import java.util.List;
import org.msgpack.core.buffer.ArrayBufferOutput;
import org.msgpack.core.buffer.MessageBuffer;
import org.msgpack.core.buffer.MessageBufferOutput;

public class MessageBufferPacker extends MessagePacker {
   protected MessageBufferPacker(MessagePack.PackerConfig config) {
      this(new ArrayBufferOutput(config.getBufferSize()), config);
   }

   protected MessageBufferPacker(ArrayBufferOutput out, MessagePack.PackerConfig config) {
      super(out, config);
   }

   @Override
   public MessageBufferOutput reset(MessageBufferOutput out) throws IOException {
      if (!(out instanceof ArrayBufferOutput)) {
         throw new IllegalArgumentException("MessageBufferPacker accepts only ArrayBufferOutput");
      } else {
         return super.reset(out);
      }
   }

   private ArrayBufferOutput getArrayBufferOut() {
      return (ArrayBufferOutput)this.out;
   }

   @Override
   public void clear() {
      super.clear();
      this.getArrayBufferOut().clear();
   }

   public byte[] toByteArray() {
      try {
         this.flush();
      } catch (IOException var2) {
         throw new RuntimeException(var2);
      }

      return this.getArrayBufferOut().toByteArray();
   }

   public MessageBuffer toMessageBuffer() {
      try {
         this.flush();
      } catch (IOException var2) {
         throw new RuntimeException(var2);
      }

      return this.getArrayBufferOut().toMessageBuffer();
   }

   public List<MessageBuffer> toBufferList() {
      try {
         this.flush();
      } catch (IOException var2) {
         throw new RuntimeException(var2);
      }

      return this.getArrayBufferOut().toBufferList();
   }

   public int getBufferSize() {
      return this.getArrayBufferOut().getSize();
   }
}
