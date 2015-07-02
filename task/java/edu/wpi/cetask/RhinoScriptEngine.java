/* Copyright (c) 2008 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.cetask;

import javax.script.*;

// simple wrapper for Rhino script engine included in JDK 1.7

class RhinoScriptEngine extends ScriptEngineWrapper.JSR_223
                        implements Invocable, Compilable {
   
   private final Object scope; // for invokeFunction 
   
   public RhinoScriptEngine (ScriptEngine rhino) {
      super(rhino);
      try { scope = eval("new Object()"); }
      catch (ScriptException e) { throw new RuntimeException(e); }
   }

   /* There appear to be three different versions of Rhino in circulation
    * using different package names.  As of Nov 2013, Package1 is used for 
    * default JDK for OS X 10.8.5 and Package3 for Ubuntu 12.04.  Package2
    * appears to be obsolete.
    */
   private enum Package { Package1, Package2, Package3 }
   private static Object notFound, undefined;
   static Package thisPackage;

   static {
      // note lib/js.jar contains classes for compilation on all platforms
      // but not execution classes
      try { 
         undefined = sun.org.mozilla.javascript.internal.Context.getUndefinedValue();
         notFound = sun.org.mozilla.javascript.internal.Scriptable.NOT_FOUND;
         thisPackage = Package.Package1;
      } catch (Throwable e1) {
         try { 
            undefined = org.mozilla.javascript.Context.getUndefinedValue();
            notFound = org.mozilla.javascript.Scriptable.NOT_FOUND;
            thisPackage = Package.Package2;
         } catch (Throwable e2) {
            try { 
               undefined = sun.org.mozilla.javascript.Context.getUndefinedValue();
               notFound = sun.org.mozilla.javascript.Scriptable.NOT_FOUND;
               thisPackage = Package.Package3;
            } catch (Throwable e3) {}
         }
      }
   }
 
   @Override
   boolean isDefined (Object object, String field) {
      Object value = get(object, field);
      return !( value == notFound || value == undefined );
   }
  
   @Override
   Object get (Object object, String field) {
      switch (thisPackage) {
         case Package1:
            sun.org.mozilla.javascript.internal.Scriptable scriptable1 = 
               (sun.org.mozilla.javascript.internal.Scriptable) object;
            return scriptable1.get(field, scriptable1);
         case Package2:
            org.mozilla.javascript.Scriptable scriptable2 = 
               (org.mozilla.javascript.Scriptable) object;
            return scriptable2.get(field, scriptable2);
         case Package3:
            sun.org.mozilla.javascript.Scriptable scriptable3 = 
               (sun.org.mozilla.javascript.Scriptable) object;
            return scriptable3.get(field, scriptable3);
         default: throw new IllegalArgumentException("Cannot perform get on "+object);
      }
   }
      
   @Override
   void put (Object object, String field, Object value) {
      switch (thisPackage) {
         case Package1:
            sun.org.mozilla.javascript.internal.Scriptable scriptable1 = 
               (sun.org.mozilla.javascript.internal.Scriptable) object;
            scriptable1.put(field, scriptable1, javaToJS(value, scriptable1));
            break;
         case Package2:
            org.mozilla.javascript.Scriptable scriptable2 = 
               (org.mozilla.javascript.Scriptable) object;
            scriptable2.put(field, scriptable2, javaToJS(value, scriptable2));
            break;
         case Package3:
            sun.org.mozilla.javascript.Scriptable scriptable3 = 
               (sun.org.mozilla.javascript.Scriptable) object;
            scriptable3.put(field, scriptable3, javaToJS(value, scriptable3));
            break;
         default: throw new IllegalArgumentException("Cannot perform put on "+object);
      }
   }
   
   @Override
   void remove (Object object, String field) {
      switch (thisPackage) {
         case Package1:
            sun.org.mozilla.javascript.internal.Scriptable scriptable1 = 
               (sun.org.mozilla.javascript.internal.Scriptable) object;
            scriptable1.delete(field);
            break;
         case Package2:
            org.mozilla.javascript.Scriptable scriptable2 = 
               (org.mozilla.javascript.Scriptable) object;
            scriptable2.delete(field);
            break;
         case Package3:
            sun.org.mozilla.javascript.Scriptable scriptable3 = 
               (sun.org.mozilla.javascript.Scriptable) object;
            scriptable3.delete(field);
            break;
         default: throw new IllegalArgumentException("Cannot perform delete on "+object);
      }
   }
   
   @Override
   public Object invokeFunction (String name, Object... args)
         throws ScriptException, NoSuchMethodException {
      for (int i = args.length; i-- > 0;)
         args[i] = javaToJS(args[i], scope);
      return ((Invocable) jsr).invokeFunction(name, args);
   }
   
   @Override
   public Object invokeMethod (Object thiz, String name, Object... args)
         throws ScriptException, NoSuchMethodException {
      for (int i = args.length; i-- > 0;)
         args[i] = javaToJS(args[i], thiz);
      return ((Invocable) jsr).invokeMethod(thiz, name, args);
   }
   
   private static Object javaToJS (Object value, Object scriptable) {
      switch (thisPackage) {
         case Package1:
             try {
               sun.org.mozilla.javascript.internal.Context.enter();
               return sun.org.mozilla.javascript.internal.Context.javaToJS(value, 
                     (sun.org.mozilla.javascript.internal.Scriptable) scriptable);
             } finally { sun.org.mozilla.javascript.internal.Context.exit(); }   
         case Package2:
            try {
               org.mozilla.javascript.Context.enter();
               return org.mozilla.javascript.Context.javaToJS(value, 
                     (org.mozilla.javascript.Scriptable) scriptable);
            } finally { org.mozilla.javascript.Context.exit(); }   
         case Package3:
            try {
               sun.org.mozilla.javascript.Context.enter();
               return sun.org.mozilla.javascript.Context.javaToJS(value, 
                     (sun.org.mozilla.javascript.Scriptable) scriptable);
            } finally { sun.org.mozilla.javascript.Context.exit(); }   
         default: throw new IllegalArgumentException("Cannot perform javaToJS on "+value);
      }
   }
 
}
