package com.heypixel.heypixelmod.obsoverlay.utils;

public class Vector2f {
   public float x;
   public float y;

   public Vector2f() {
   }

   public Vector2f(float x, float y) {
      this.set(x, y);
   }

   public static float dot(Vector2f left, Vector2f right) {
      return left.x * right.x + left.y * right.y;
   }

   public static float angle(Vector2f a, Vector2f b) {
      float dls = dot(a, b) / (a.length() * b.length());
      if (dls < -1.0F) {
         dls = -1.0F;
      } else if (dls > 1.0F) {
         dls = 1.0F;
      }

      return (float)Math.acos((double)dls);
   }

   public static Vector2f add(Vector2f left, Vector2f right, Vector2f dest) {
      if (dest == null) {
         return new Vector2f(left.x + right.x, left.y + right.y);
      } else {
         dest.set(left.x + right.x, left.y + right.y);
         return dest;
      }
   }

   public static Vector2f sub(Vector2f left, Vector2f right, Vector2f dest) {
      if (dest == null) {
         return new Vector2f(left.x - right.x, left.y - right.y);
      } else {
         dest.set(left.x - right.x, left.y - right.y);
         return dest;
      }
   }

   public void set(float x, float y) {
      this.x = x;
      this.y = y;
   }

   public final float length() {
      return (float)Math.sqrt((double)this.lengthSquared());
   }

   public float lengthSquared() {
      return this.x * this.x + this.y * this.y;
   }

   public Vector2f translate(float x, float y) {
      this.x += x;
      this.y += y;
      return this;
   }

   public Vector2f negate(Vector2f dest) {
      if (dest == null) {
         dest = new Vector2f();
      }

      dest.x = -this.x;
      dest.y = -this.y;
      return dest;
   }

   public Vector2f normalise(Vector2f dest) {
      float l = this.length();
      if (dest == null) {
         dest = new Vector2f(this.x / l, this.y / l);
      } else {
         dest.set(this.x / l, this.y / l);
      }

      return dest;
   }

   @Override
   public String toString() {
      return "Vector2f[" + this.x + ", " + this.y + "]";
   }

   public final float getX() {
      return this.x;
   }

   public final void setX(float x) {
      this.x = x;
   }

   public final float getY() {
      return this.y;
   }

   public final void setY(float y) {
      this.y = y;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else if (obj == null) {
         return false;
      } else if (this.getClass() != obj.getClass()) {
         return false;
      } else {
         Vector2f other = (Vector2f)obj;
         return this.x == other.x && this.y == other.y;
      }
   }
}
