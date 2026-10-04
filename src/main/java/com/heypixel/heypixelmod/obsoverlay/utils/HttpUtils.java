package com.heypixel.heypixelmod.obsoverlay.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import org.apache.commons.io.FileUtils;

public class HttpUtils {
   public static final String DEFAULT_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0";

   public HttpUtils() {
      HttpURLConnection.setFollowRedirects(true);
   }

   public static HttpURLConnection make(String url, String method, String agent) throws IOException {
      HttpURLConnection httpConnection = (HttpURLConnection)new URL(url).openConnection();
      httpConnection.setRequestMethod(method);
      httpConnection.setConnectTimeout(5000);
      httpConnection.setReadTimeout(10000);
      httpConnection.setRequestProperty("User-Agent", agent);
      httpConnection.setInstanceFollowRedirects(true);
      httpConnection.setDoOutput(true);
      return httpConnection;
   }

   public static String request(String url, String method, String agent) {
      try {
         HttpURLConnection connection = make(url, method, agent);
         BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
         StringBuilder stringBuilder = new StringBuilder();

         String line;
         while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
         }

         bufferedReader.close();
         return stringBuilder.toString();
      } catch (SocketTimeoutException var7) {
         System.err.println("Read timed out");
         return null;
      } catch (IOException var8) {
         System.err.println("Error while making request");
         return null;
      }
   }

   public static String requestSingleLine(String url, String method, String agent) {
      try {
         HttpURLConnection connection = make(url, method, agent);
         BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
         StringBuilder stringBuilder = new StringBuilder();

         String line;
         while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
         }

         bufferedReader.close();
         return stringBuilder.toString();
      } catch (SocketTimeoutException var7) {
         System.err.println("Read timed out");
         return null;
      } catch (IOException var8) {
         System.err.println("Error while making request");
         return null;
      }
   }

   public static String get(String url) throws IOException {
      return request(url, "GET", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
   }

   public static String get2(String url) throws IOException {
      return requestSingleLine(url, "GET", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
   }

   public static void download(String url, File file) throws IOException {
      FileUtils.copyInputStreamToFile(make(url, "GET", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0").getInputStream(), file);
   }

   public static String getResultFormStream(InputStream in) throws IOException {
      BufferedReader reader = new BufferedReader(new InputStreamReader(in));
      StringBuilder builder = new StringBuilder();

      String line;
      while ((line = reader.readLine()) != null) {
         builder.append(line);
      }

      return builder.toString();
   }
}
