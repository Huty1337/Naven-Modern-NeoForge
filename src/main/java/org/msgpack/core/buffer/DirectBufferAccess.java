package org.msgpack.core.buffer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;

class DirectBufferAccess {
   static Method mCleaner;
   static Method mClean;
   static Method mInvokeCleaner;
   static Constructor<?> byteBufferConstructor;
   static Class<?> directByteBufferClass;
   static DirectBufferAccess.DirectBufferConstructorType directBufferConstructorType;
   static Method memoryBlockWrapFromJni;

   private DirectBufferAccess() {
   }

   private static void setupCleanerJava9(ByteBuffer direct) {
      Object obj = AccessController.doPrivileged(new PrivilegedAction<Object>() {
         @Override
         public Object run() {
            return DirectBufferAccess.getInvokeCleanerMethod(direct);
         }
      });
      if (obj instanceof Throwable) {
         throw new RuntimeException((Throwable)obj);
      } else {
         mInvokeCleaner = (Method)obj;
      }
   }

   private static Object getCleanerMethod(ByteBuffer direct) {
      try {
         Method m = direct.getClass().getDeclaredMethod("cleaner");
         m.setAccessible(true);
         m.invoke(direct);
         return m;
      } catch (NoSuchMethodException var2) {
         return var2;
      } catch (InvocationTargetException var3) {
         return var3;
      } catch (IllegalAccessException var4) {
         return var4;
      }
   }

   private static Object getCleanMethod(ByteBuffer direct, Method mCleaner) {
      try {
         Method m = mCleaner.getReturnType().getDeclaredMethod("clean");
         Object c = mCleaner.invoke(direct);
         m.setAccessible(true);
         m.invoke(c);
         return m;
      } catch (NoSuchMethodException var4) {
         return var4;
      } catch (InvocationTargetException var5) {
         return var5;
      } catch (IllegalAccessException var6) {
         return var6;
      }
   }

   private static Object getInvokeCleanerMethod(ByteBuffer direct) {
      try {
         Method m = MessageBuffer.unsafe.getClass().getDeclaredMethod("invokeCleaner", ByteBuffer.class);
         m.invoke(MessageBuffer.unsafe, direct);
         return m;
      } catch (NoSuchMethodException var2) {
         return var2;
      } catch (InvocationTargetException var3) {
         return var3;
      } catch (IllegalAccessException var4) {
         return var4;
      }
   }

   static void clean(Object base) {
      try {
         if (MessageBuffer.javaVersion <= 8) {
            Object cleaner = mCleaner.invoke(base);
            mClean.invoke(cleaner);
         } else {
            mInvokeCleaner.invoke(MessageBuffer.unsafe, base);
         }
      } catch (Throwable var2) {
         throw new RuntimeException(var2);
      }
   }

   static boolean isDirectByteBufferInstance(Object s) {
      return directByteBufferClass.isInstance(s);
   }

   static ByteBuffer newByteBuffer(long address, int index, int length, ByteBuffer reference) {
      if (byteBufferConstructor == null) {
         throw new IllegalStateException(
            "Can't create a new DirectByteBuffer. In JDK17+, two JVM options needs to be set: --add-opens=java.base/java.nio=ALL-UNNAMED and --add-opens=java.base/sun.nio.ch=ALL-UNNAMED"
         );
      } else {
         try {
            switch (directBufferConstructorType) {
               case ARGS_LONG_LONG:
                  return (ByteBuffer)byteBufferConstructor.newInstance(address + (long)index, (long)length);
               case ARGS_LONG_INT_REF:
                  return (ByteBuffer)byteBufferConstructor.newInstance(address + (long)index, length, reference);
               case ARGS_LONG_INT:
                  return (ByteBuffer)byteBufferConstructor.newInstance(address + (long)index, length);
               case ARGS_INT_INT:
                  return (ByteBuffer)byteBufferConstructor.newInstance((int)address + index, length);
               case ARGS_MB_INT_INT:
                  return (ByteBuffer)byteBufferConstructor.newInstance(memoryBlockWrapFromJni.invoke(null, address + (long)index, length), length, 0);
               default:
                  throw new IllegalStateException("Unexpected value");
            }
         } catch (Throwable var6) {
            throw new RuntimeException(var6);
         }
      }
   }

   static {
      try {
         ByteBuffer direct = ByteBuffer.allocateDirect(1);
         directByteBufferClass = direct.getClass();
         Constructor<?> directByteBufferConstructor = null;
         DirectBufferAccess.DirectBufferConstructorType constructorType = null;
         Method mbWrap = null;

         try {
            directByteBufferConstructor = directByteBufferClass.getDeclaredConstructor(long.class, long.class);
            constructorType = DirectBufferAccess.DirectBufferConstructorType.ARGS_LONG_LONG;
         } catch (NoSuchMethodException var12) {
            try {
               directByteBufferConstructor = directByteBufferClass.getDeclaredConstructor(long.class, int.class, Object.class);
               constructorType = DirectBufferAccess.DirectBufferConstructorType.ARGS_LONG_INT_REF;
            } catch (NoSuchMethodException var11) {
               try {
                  directByteBufferConstructor = directByteBufferClass.getDeclaredConstructor(long.class, int.class);
                  constructorType = DirectBufferAccess.DirectBufferConstructorType.ARGS_LONG_INT;
               } catch (NoSuchMethodException var10) {
                  try {
                     directByteBufferConstructor = directByteBufferClass.getDeclaredConstructor(int.class, int.class);
                     constructorType = DirectBufferAccess.DirectBufferConstructorType.ARGS_INT_INT;
                  } catch (NoSuchMethodException var9) {
                     Class<?> aClass = Class.forName("java.nio.MemoryBlock");
                     mbWrap = aClass.getDeclaredMethod("wrapFromJni", int.class, long.class);
                     mbWrap.setAccessible(true);
                     directByteBufferConstructor = directByteBufferClass.getDeclaredConstructor(aClass, int.class, int.class);
                     constructorType = DirectBufferAccess.DirectBufferConstructorType.ARGS_MB_INT_INT;
                  }
               }
            }
         }

         byteBufferConstructor = directByteBufferConstructor;
         directBufferConstructorType = constructorType;
         memoryBlockWrapFromJni = mbWrap;
         if (byteBufferConstructor == null) {
            throw new RuntimeException("Constructor of DirectByteBuffer is not found");
         } else {
            try {
               byteBufferConstructor.setAccessible(true);
            } catch (RuntimeException var13) {
               if (!"java.lang.reflect.InaccessibleObjectException".equals(var13.getClass().getName())) {
                  throw var13;
               }

               byteBufferConstructor = null;
            }

            setupCleanerJava9(direct);
         }
      } catch (Exception var14) {
         throw new RuntimeException(var14);
      }
   }

   static enum DirectBufferConstructorType {
      ARGS_LONG_LONG,
      ARGS_LONG_INT_REF,
      ARGS_LONG_INT,
      ARGS_INT_INT,
      ARGS_MB_INT_INT;
   }
}
