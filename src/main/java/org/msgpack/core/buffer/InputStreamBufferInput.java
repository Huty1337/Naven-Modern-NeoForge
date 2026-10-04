package org.msgpack.core.buffer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import org.msgpack.core.Preconditions;

public class InputStreamBufferInput implements MessageBufferInput {
   private InputStream in;
   private final byte[] buffer;

   public static MessageBufferInput newBufferInput(InputStream in) {
      Preconditions.checkNotNull(in, "InputStream is null");
      if (in instanceof FileInputStream) {
         FileChannel channel = ((FileInputStream)in).getChannel();
         if (channel != null) {
            return new ChannelBufferInput(channel);
         }
      }

      return new InputStreamBufferInput(in);
   }

   public InputStreamBufferInput(InputStream in) {
      this(in, 8192);
   }

   public InputStreamBufferInput(InputStream in, int bufferSize) {
      this.in = Preconditions.checkNotNull(in, "input is null");
      this.buffer = new byte[bufferSize];
   }

   public InputStream reset(InputStream in) throws IOException {
      InputStream old = this.in;
      this.in = in;
      return old;
   }

   @Override
   public MessageBuffer next() throws IOException {
      int readLen = this.in.read(this.buffer);
      return readLen == -1 ? null : MessageBuffer.wrap(this.buffer, 0, readLen);
   }

   @Override
   public void close() throws IOException {
      this.in.close();
   }
}
