package org.msgpack.core;

import org.msgpack.core.annotations.Nullable;
import org.msgpack.core.annotations.VisibleForTesting;

public final class Preconditions {
   private Preconditions() {
   }

   public static void checkArgument(boolean expression) {
      if (!expression) {
         throw new IllegalArgumentException();
      }
   }

   public static void checkArgument(boolean expression, @Nullable Object errorMessage) {
      if (!expression) {
         throw new IllegalArgumentException(String.valueOf(errorMessage));
      }
   }

   public static void checkArgument(boolean expression, @Nullable String errorMessageTemplate, @Nullable Object... errorMessageArgs) {
      if (!expression) {
         throw new IllegalArgumentException(format(errorMessageTemplate, errorMessageArgs));
      }
   }

   public static void checkState(boolean expression) {
      if (!expression) {
         throw new IllegalStateException();
      }
   }

   public static void checkState(boolean expression, @Nullable Object errorMessage) {
      if (!expression) {
         throw new IllegalStateException(String.valueOf(errorMessage));
      }
   }

   public static void checkState(boolean expression, @Nullable String errorMessageTemplate, @Nullable Object... errorMessageArgs) {
      if (!expression) {
         throw new IllegalStateException(format(errorMessageTemplate, errorMessageArgs));
      }
   }

   public static <T> T checkNotNull(T reference) {
      if (reference == null) {
         throw new NullPointerException();
      } else {
         return reference;
      }
   }

   public static <T> T checkNotNull(T reference, @Nullable Object errorMessage) {
      if (reference == null) {
         throw new NullPointerException(String.valueOf(errorMessage));
      } else {
         return reference;
      }
   }

   public static <T> T checkNotNull(T reference, @Nullable String errorMessageTemplate, @Nullable Object... errorMessageArgs) {
      if (reference == null) {
         throw new NullPointerException(format(errorMessageTemplate, errorMessageArgs));
      } else {
         return reference;
      }
   }

   public static int checkElementIndex(int index, int size) {
      return checkElementIndex(index, size, "index");
   }

   public static int checkElementIndex(int index, int size, @Nullable String desc) {
      if (index >= 0 && index < size) {
         return index;
      } else {
         throw new IndexOutOfBoundsException(badElementIndex(index, size, desc));
      }
   }

   private static String badElementIndex(int index, int size, String desc) {
      if (index < 0) {
         return format("%s (%s) must not be negative", desc, index);
      } else if (size < 0) {
         throw new IllegalArgumentException("negative size: " + size);
      } else {
         return format("%s (%s) must be less than size (%s)", desc, index, size);
      }
   }

   public static int checkPositionIndex(int index, int size) {
      return checkPositionIndex(index, size, "index");
   }

   public static int checkPositionIndex(int index, int size, @Nullable String desc) {
      if (index >= 0 && index <= size) {
         return index;
      } else {
         throw new IndexOutOfBoundsException(badPositionIndex(index, size, desc));
      }
   }

   private static String badPositionIndex(int index, int size, String desc) {
      if (index < 0) {
         return format("%s (%s) must not be negative", desc, index);
      } else if (size < 0) {
         throw new IllegalArgumentException("negative size: " + size);
      } else {
         return format("%s (%s) must not be greater than size (%s)", desc, index, size);
      }
   }

   public static void checkPositionIndexes(int start, int end, int size) {
      if (start < 0 || end < start || end > size) {
         throw new IndexOutOfBoundsException(badPositionIndexes(start, end, size));
      }
   }

   private static String badPositionIndexes(int start, int end, int size) {
      if (start < 0 || start > size) {
         return badPositionIndex(start, size, "start index");
      } else {
         return end >= 0 && end <= size
            ? format("end index (%s) must not be less than start index (%s)", end, start)
            : badPositionIndex(end, size, "end index");
      }
   }

   @VisibleForTesting
   static String format(String template, @Nullable Object... args) {
      template = String.valueOf(template);
      StringBuilder builder = new StringBuilder(template.length() + 16 * args.length);
      int templateStart = 0;
      int i = 0;

      while (i < args.length) {
         int placeholderStart = template.indexOf("%s", templateStart);
         if (placeholderStart == -1) {
            break;
         }

         builder.append(template.substring(templateStart, placeholderStart));
         builder.append(args[i++]);
         templateStart = placeholderStart + 2;
      }

      builder.append(template.substring(templateStart));
      if (i < args.length) {
         builder.append(" [");
         builder.append(args[i++]);

         while (i < args.length) {
            builder.append(", ");
            builder.append(args[i++]);
         }

         builder.append(']');
      }

      return builder.toString();
   }
}
