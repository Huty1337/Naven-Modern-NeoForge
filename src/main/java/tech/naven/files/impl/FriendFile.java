package tech.naven.files.impl;

import tech.naven.files.ClientFile;
import tech.naven.utils.FriendManager;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

public class FriendFile extends ClientFile {
   public FriendFile() {
      super("friend.cfg");
   }

   @Override
   public void read(BufferedReader reader) throws IOException {
      String line;
      while ((line = reader.readLine()) != null) {
         FriendManager.addFriend(line);
      }
   }

   @Override
   public void save(BufferedWriter writer) throws IOException {
      for (String friend : FriendManager.getFriends()) {
         writer.write(friend + "\n");
      }
   }
}
