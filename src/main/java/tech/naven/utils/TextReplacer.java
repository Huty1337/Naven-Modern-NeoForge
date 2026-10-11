package tech.naven.utils;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

public final class TextReplacer {
   private static String nameFrom;
   private static String nameToPlain;
   private static String nameToLegacy;
   private static String serverFrom;
   private static String serverToPlain;
   private static String serverToLegacy;

   private TextReplacer() {
   }

   public static void setName(String from, String toPlain, String toLegacy) {
      if (from == null || from.isEmpty() || toPlain == null || toLegacy == null || toLegacy.contains(from)) {
         nameFrom = null;
         nameToPlain = null;
         nameToLegacy = null;
      } else {
         nameFrom = from;
         nameToPlain = toPlain;
         nameToLegacy = toLegacy;
      }
   }

   public static void setServer(String from, String toPlain, String toLegacy) {
      if (from == null || from.isEmpty() || toPlain == null || toLegacy == null || toLegacy.contains(from)) {
         serverFrom = null;
         serverToPlain = null;
         serverToLegacy = null;
      } else {
         serverFrom = from;
         serverToPlain = toPlain;
         serverToLegacy = toLegacy;
      }
   }

   public static String getNameFrom() {
      return nameFrom;
   }

   public static String getNameToPlain() {
      return nameToPlain;
   }

   public static String getServerFrom() {
      return serverFrom;
   }

   public static String getServerToPlain() {
      return serverToPlain;
   }

   public static boolean isEmpty() {
      return nameFrom == null && serverFrom == null;
   }

   public static boolean contains(String text) {
      if (text == null) {
         return false;
      }
      if (serverFrom != null && text.contains(serverFrom)) {
         return true;
      }
      return nameFrom != null && text.contains(nameFrom);
   }

   public static String replace(String text) {
      if (text == null || text.isEmpty()) {
         return text;
      }
      String out = text;
      if (serverFrom != null && out.contains(serverFrom)) {
         out = out.replace(serverFrom, serverToLegacy);
      }
      if (nameFrom != null && out.contains(nameFrom)) {
         out = out.replace(nameFrom, nameToLegacy);
      }
      return out;
   }

   public static FormattedText rewriteText(FormattedText text) {
      if (text == null || isEmpty()) {
         return text;
      }
      String flat = text.getString();
      if (!contains(flat)) {
         return text;
      }
      return FormattedText.of(replace(flat));
   }

   public static FormattedCharSequence rewriteSequence(FormattedCharSequence text) {
      if (text == null || isEmpty()) {
         return text;
      }
      StringBuilder flat = new StringBuilder();
      text.accept((index, style, codePoint) -> {
         flat.appendCodePoint(codePoint);
         return true;
      });
      if (!contains(flat.toString())) {
         return text;
      }
      List<Style> styles = new ArrayList<>();
      List<String> runs = new ArrayList<>();
      StringBuilder current = new StringBuilder();
      Style[] lastStyle = new Style[]{null};
      text.accept((index, style, codePoint) -> {
         if (lastStyle[0] != null && !lastStyle[0].equals(style)) {
            styles.add(lastStyle[0]);
            runs.add(current.toString());
            current.setLength(0);
         }
         lastStyle[0] = style;
         current.appendCodePoint(codePoint);
         return true;
      });
      if (lastStyle[0] != null) {
         styles.add(lastStyle[0]);
         runs.add(current.toString());
      }
      return sink -> {
         int[] index = new int[]{0};
         for (int i = 0; i < runs.size(); i++) {
            if (!emit(sink, index, styles.get(i), runs.get(i))) {
               return false;
            }
         }
         return true;
      };
   }

   private static boolean emit(FormattedCharSink sink, int[] index, Style style, String text) {
      int pos = 0;
      while (pos < text.length()) {
         int hit = -1;
         String from = null;
         String to = null;
         if (serverFrom != null) {
            int at = text.indexOf(serverFrom, pos);
            if (at >= 0) {
               hit = at;
               from = serverFrom;
               to = serverToPlain;
            }
         }
         if (nameFrom != null) {
            int at = text.indexOf(nameFrom, pos);
            if (at >= 0 && (hit < 0 || at < hit)) {
               hit = at;
               from = nameFrom;
               to = nameToPlain;
            }
         }
         if (hit < 0) {
            return emitPlain(sink, index, style, text.substring(pos));
         }
         if (!emitPlain(sink, index, style, text.substring(pos, hit))) {
            return false;
         }
         if (!emitPlain(sink, index, style.withColor(ChatFormatting.LIGHT_PURPLE), to)) {
            return false;
         }
         pos = hit + from.length();
      }
      return true;
   }

   private static boolean emitPlain(FormattedCharSink sink, int[] index, Style style, String text) {
      for (int i = 0; i < text.length(); ) {
         int codePoint = text.codePointAt(i);
         if (!sink.accept(index[0]++, style, codePoint)) {
            return false;
         }
         i += Character.charCount(codePoint);
      }
      return true;
   }
}
