/* Copyright (c) 2008 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.cetask;

import javax.script.*;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

// simple wrapper for Nashorn script engine included in JDK 1.8

class NashornScriptEngine extends ScriptEngineWrapper.JSR_223 
                          implements Invocable, Compilable {
   
   /**
    *  --global-per-engine option required below since each task instance 
    *  uses its own ENGINE_SCOPE, but they need to share a single GLOBAL_SCOPE
    *  (e.g., for loading task library initializations).  Note that separate
    *  instances of TaskEngine still have independent global name spaces.
    */
   public static String[] OPTIONS = new String[] {"--global-per-engine"};
   
   private final Object scope; // for invokeFunction ?????????????
   
   public NashornScriptEngine (ScriptEngine nashorn) {
      super(nashorn);
      try { scope = eval("new Object()"); }
      catch (ScriptException e) { throw new RuntimeException(e); }
   }

   @Override
   boolean isScriptable () { return false; }  //////// TRUE!!! 
    
   @Override
   boolean isScriptable (Object value) { return isScriptable(); }
   
   @Override
   boolean isDefined (Object value) {
      return value != jdk.nashorn.internal.runtime.ScriptRuntime.UNDEFINED;
   }
   
   @Override
   Object undefined () { return jdk.nashorn.internal.runtime.ScriptRuntime.UNDEFINED; }
   
   @Override
   Object get (Object object, String field) {
      return ((ScriptObjectMirror) object).get(field);
   }
      
   @Override
   void put (Object object, String field, Object value) {
      ((ScriptObjectMirror) object).put(field, value);
   }
   
   @Override
   void delete (Object object, String field) {
      ((ScriptObjectMirror) object).remove(field);
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
      throw new UnsupportedOperationException("javaToJS("+value+","+scriptable+")");
   }
}
