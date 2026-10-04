package com.heypixel.heypixelmod.obsoverlay.utils;

public final class StencilUtils {
   private static final float EPSILON = 1.0E-4F;

   private enum Phase {
      IDLE,
      MASK,
      CLIP
   }

   private static Phase phase = Phase.IDLE;
   private static boolean maskVisible;
   private static float[] maskPerimeter;
   private static float[] maskPlanes;

   private StencilUtils() {
   }

   public static void write(boolean renderClipLayer) {
      phase = Phase.MASK;
      maskVisible = renderClipLayer;
      maskPerimeter = null;
      maskPlanes = null;
   }

   public static void erase(boolean invert) {
      phase = invert && maskPlanes != null ? Phase.CLIP : Phase.IDLE;
   }

   public static void dispose() {
      phase = Phase.IDLE;
      maskVisible = false;
      maskPerimeter = null;
      maskPlanes = null;
   }

   public static boolean isWritingMask() {
      return phase == Phase.MASK;
   }

   public static boolean isMaskVisible() {
      return maskVisible;
   }

   public static boolean isClipping() {
      return phase == Phase.CLIP;
   }

   public static boolean isAvailable() {
      return true;
   }

   public static void setupFBO() {
   }

   public static void registerMask(float[] perimeter) {
      if (perimeter == null || perimeter.length < 6) {
         return;
      }

      maskPerimeter = perimeter;
      maskPlanes = buildPlanes(perimeter);
   }

   public static float[] clipToActiveMask(float[] subject) {
      if (phase != Phase.CLIP || maskPlanes == null || subject == null || subject.length < 6) {
         return null;
      }

      float minX = Float.MAX_VALUE;
      float minY = Float.MAX_VALUE;
      float maxX = -Float.MAX_VALUE;
      float maxY = -Float.MAX_VALUE;
      for (int i = 0; i < subject.length; i += 2) {
         float px = subject[i];
         float py = subject[i + 1];
         if (px < minX) {
            minX = px;
         }
         if (px > maxX) {
            maxX = px;
         }
         if (py < minY) {
            minY = py;
         }
         if (py > maxY) {
            maxY = py;
         }
      }

      float[] current = subject;
      boolean cut = false;
      for (int i = 0; i + 2 < maskPlanes.length; i += 3) {
         float nx = maskPlanes[i];
         float ny = maskPlanes[i + 1];
         float d = maskPlanes[i + 2];
         if (nx == 0.0F && ny == 0.0F) {
            continue;
         }

         float smallest = (nx > 0.0F ? minX : maxX) * nx + (ny > 0.0F ? minY : maxY) * ny;
         if (smallest >= d - EPSILON) {
            continue;
         }

         current = clipHalfPlane(current, nx, ny, d);
         if (current == null || current.length < 6) {
            return null;
         }
         cut = true;
      }

      return cut ? current : subject;
   }

   private static float[] buildPlanes(float[] perimeter) {
      int points = perimeter.length / 2;
      float[] planes = new float[points * 3];
      float insideX = 0.0F;
      float insideY = 0.0F;
      for (int i = 0; i < points; i++) {
         insideX += perimeter[i * 2];
         insideY += perimeter[i * 2 + 1];
      }
      insideX /= points;
      insideY /= points;

      for (int i = 0; i < points; i++) {
         int next = (i + 1) % points;
         float ax = perimeter[i * 2];
         float ay = perimeter[i * 2 + 1];
         float bx = perimeter[next * 2];
         float by = perimeter[next * 2 + 1];
         float dx = bx - ax;
         float dy = by - ay;
         float length = (float)Math.sqrt(dx * dx + dy * dy);
         if (length < EPSILON) {
            planes[i * 3] = 0.0F;
            planes[i * 3 + 1] = 0.0F;
            planes[i * 3 + 2] = -Float.MAX_VALUE;
            continue;
         }

         float nx = dy / length;
         float ny = -dx / length;
         float d = nx * ax + ny * ay;
         if (nx * insideX + ny * insideY < d) {
            nx = -nx;
            ny = -ny;
            d = -d;
         }

         planes[i * 3] = nx;
         planes[i * 3 + 1] = ny;
         planes[i * 3 + 2] = d;
      }

      return planes;
   }

   private static float[] clipHalfPlane(float[] polygon, float nx, float ny, float d) {
      int points = polygon.length / 2;
      FloatBuffer output = new FloatBuffer(points + 2);
      float previousX = polygon[(points - 1) * 2];
      float previousY = polygon[(points - 1) * 2 + 1];
      float previousDistance = nx * previousX + ny * previousY - d;
      boolean previousInside = previousDistance >= -EPSILON;

      for (int i = 0; i < points; i++) {
         float currentX = polygon[i * 2];
         float currentY = polygon[i * 2 + 1];
         float currentDistance = nx * currentX + ny * currentY - d;
         boolean currentInside = currentDistance >= -EPSILON;

         if (currentInside != previousInside) {
            float denominator = previousDistance - currentDistance;
            if (Math.abs(denominator) > EPSILON) {
               float t = previousDistance / denominator;
               output.add(previousX + (currentX - previousX) * t, previousY + (currentY - previousY) * t);
            }
         }

         if (currentInside) {
            output.add(currentX, currentY);
         }

         previousX = currentX;
         previousY = currentY;
         previousDistance = currentDistance;
         previousInside = currentInside;
      }

      return output.size() < 3 ? null : output.toArray();
   }

   private static final class FloatBuffer {
      private float[] data;
      private int size;

      private FloatBuffer(int expectedPairs) {
         this.data = new float[Math.max(8, expectedPairs * 2)];
      }

      private void add(float x, float y) {
         if (this.size + 2 > this.data.length) {
            float[] grown = new float[this.data.length * 2];
            System.arraycopy(this.data, 0, grown, 0, this.size);
            this.data = grown;
         }

         this.data[this.size++] = x;
         this.data[this.size++] = y;
      }

      private int size() {
         return this.size / 2;
      }

      private float[] toArray() {
         float[] result = new float[this.size];
         System.arraycopy(this.data, 0, result, 0, this.size);
         return result;
      }
   }
}
