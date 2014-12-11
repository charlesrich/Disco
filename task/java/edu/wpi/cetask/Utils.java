/* Copyright (c) 2008 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.cetask;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Static utility methods.
 */
public abstract class Utils {

   private Utils () {}  // non-instantiatable class for static methods only
   
   /**
    * Invoke clone method or copy constructor on given object.
    */
   public static Object copy (Object object) {
      Class<?> type = object.getClass();
      if ( object instanceof Cloneable ) 
         try { return type.getMethod("clone").invoke(object); }
         catch (Exception e) {
            throw new RuntimeException("Error calling clone method for: "+type);
         }
      try { return type.getConstructor(type).newInstance(object); } 
      catch (Exception e) {
         throw new RuntimeException("Error calling copy constructor for: "+type);
      }
   }

   // three-valued logic (null represents unknown)
   
   public static boolean isTrue (Boolean b) {
      return b != null && b;
   }
   
   public static boolean isFalse (Boolean b) {
      return b != null && !b;
   }

   public static Boolean not (Boolean b) {
      return b == null ? null : !b;
   }
   
   public static boolean booleanValue (Object o) {   
      return o instanceof Boolean ? (Boolean) o :
         // for Mono/Unity
         o.toString().equals(cli.System.Boolean.TrueString); 
   }
   
   /**
    * Like {@link Boolean#parseBoolean(String)}, except throws {@link IllegalArgumentException}
    * if non-true non-null non-empty string is not, ignoring case, equal to {@code "false"}
    */
   public static boolean parseBoolean (String string) {
      if ( Boolean.parseBoolean(string) ) return true;
      if ( string != null && string.length() > 0 && !"false".equalsIgnoreCase(string) )
         throw new IllegalArgumentException("Expected false: "+string);
      return false;
   }
   
   /**
    * Test whether given objects are either both null or
    * {@link Object#equals(Object)}. 
    */
   public static boolean equals (Object object1, Object object2) {
      return object1 == null ? object2 == null : object1.equals(object2);
   }

   /**
    * Returns the list view of given array or null if array is null.
    */
   public static List<Object> asList (Object[] array) {
      return array == null ? null : Arrays.asList(array);
   }
   
   /**
    * Return null if given string is empty, otherwise string.  
    */
   public static String emptyNull (String string) {
      return string.length() == 0 ? null : string;
   }
   
   /**
    * Convert the given source string to a URL.  If given string is not a well-formed
    * URL, then see if it names a system resource; otherwise treat it as a filename.
    */
   public static URL toURL (String source) throws MalformedURLException {
      try { return new URL(source); }
      catch (MalformedURLException e) {
         URL url = ClassLoader.getSystemResource(
               source.startsWith("/") ? source.substring(1) : source);
         // sic toURI() first (see usage note in File.toURL)
         return url == null ? new File(source).toURI().toURL() : url;
      }
   }
   
   /**
    * Load properties file, if any, from given source. 
    * 
    * @param source interpreted as in {@link #toURL(String)}
    * @return null if no properties file
    */
   public static Properties loadProperties (String source) {
      // TODO internationalize with resource bundles
      InputStream stream = null;
      try { stream = Utils.toURL(source).openStream(); }
      catch (IOException e) {
         // try adding .txt extension (for DiscoUnity)
         try { stream = Utils.toURL(source+".txt").openStream(); }
         catch (IOException o) { return null; } // ignore if no properties file
      }
      return loadProperties(stream);
   }
   
   /**
    * Load properties file from given input stream.
    */
   public static Properties loadProperties (InputStream stream) { 
      Properties properties = new Properties();
      try { properties.load(stream); }
      catch (IOException e) { throw new RuntimeException(e); }
      finally { try { stream.close(); } catch (IOException e) {} }
      return properties;
   }
  
   /**
    * Read given input stream until end and return contents as a string.
    */
   public static String toString (InputStream input) {
      Scanner scanner = new Scanner(input);
      try { return scanner.useDelimiter("\\A").next(); }   
      finally { scanner.close(); }
   }
   
   /**
    * If given string ends with 'from' string, replace ending with 'to' string;
    * otherwise just append 'to' string.
    */
   public static String replaceEndsWith (String string, String from, String to) {
      return ( string.endsWith(from) ?
         string.substring(0, string.length()-from.length()) : string) 
         + to;
   }
   
   /**
    * Test if given string (or null) starts with given prefix.
    */
   public static boolean startsWith (String string, String prefix) {
      return string != null && string.startsWith(prefix);
   }
   
   /**
    * Return (possibly copy of) given string with first character lower case.
    */
   public static String decapitalize (String string) {
      if ( Character.isLowerCase(string.charAt(0)) ) return string;
      char chars[] = string.toCharArray();
      chars[0] = Character.toLowerCase(chars[0]);
      return new String(chars);
   }
   
   /**
    * Return (possibly copy of) given string with first character upper case.
    */
   public static String capitalize (String string) {
      if ( Character.isUpperCase(string.charAt(0)) ) return string;
      char chars[] = string.toCharArray();
      chars[0] = Character.toUpperCase(chars[0]);
      return new String(chars);
   }
   
   /**
    * Change first character of given string buffer to upper case.
    */
   public static void capitalize (StringBuilder buffer) {
      if ( buffer.length() > 0 ) {
         char first = buffer.charAt(0);
         if ( !Character.isUpperCase(first) ) 
            buffer.setCharAt(0, Character.toUpperCase(first));
      }
   }
   
   /**
    * Add period to end of given string buffer if doesn't already end in ! or ?.
    */
   public static void endSentence (StringBuilder buffer) {
      int i = buffer.length();
      if ( i > 0 && ".!?".indexOf(buffer.charAt(i-1)) < 0 )
         buffer.append('.');
   }

   /**
    * Strip first arg from main args
    */
   public static String[] restArgs (String[] args) {
      String[] rest = new String[args.length-1];
      if ( rest.length > 0 ) System.arraycopy(args, 1, rest, 0, rest.length);
      return rest;
   }
 
   /**
    * Return name of given class with package prefix stripped.
    * Nested classes denoted by $ (as returned by getName).  Note that 
    * Class.getSimpleName does not handle nested classes correctly.
    * 
    * @param dotted change dollars to dots for nested classes
    */
   public static String getSimpleName (Class<?> cls, boolean dotted) { 
      if ( cls == null ) return "null";
      Package pkg = cls.getPackage();
      String name = cls.getName();
      name = pkg == null ? name : name.substring(pkg.getName().length()+1);
      if ( dotted ) name = name.replace('$', '.');
      return name;
   }
   
   public static String getSimpleName (String id) {
      try { id = Utils.getSimpleName(Class.forName(id), true); }
      catch (ClassNotFoundException e) {}
      return id;
   }

   /**
    * Print each item of given list on separate line.
    */
   public static void print (List<?> list, PrintStream stream) {
      for (Object i : list) stream.println(i);
   }
   
   /**
    * For rethrowing checked exception
    */
   public static void rethrow (Throwable e) {
      throw e instanceof RuntimeException ? (RuntimeException) e:
         new RuntimeException(e);
   }
   
   /**
    * For rethrowing checked exception
    */
   public static void rethrow (String text, Throwable e) {
      throw e instanceof RuntimeException ? (RuntimeException) e:
         new RuntimeException(text, e);
   }
    
   /**
    * For logging and other applications.
    */
   public static class CopyOutputStream extends FilterOutputStream {
      private final OutputStream copy;
      public CopyOutputStream (OutputStream stream, OutputStream copy) {
         super(stream); this.copy = copy;
      }
      @Override
      public void write (int oneByte) throws IOException {
         out.write(oneByte); copy.write(oneByte);
      }
      @Override
      public void write (byte[] buffer) throws IOException {
         out.write(buffer); copy.write(buffer);
      }
      @Override
      public void write (byte[] buffer, int offset, int count) throws IOException {
         out.write(buffer, offset, count); copy.write(buffer, offset, count);
      }
      @Override
      public void flush () throws IOException {
         out.flush(); copy.flush();
      }
      @Override
      public void close () throws IOException {
         copy.close(); super.close();
      }
   }  
   
}
