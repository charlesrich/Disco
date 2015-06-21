/* Copyright (c) 2008 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.cetask;

import javax.script.*;

// simple wrapper for Nashorn script engine included in JDK 1.8

class NashornScriptEngine extends ScriptEngineWrapper 
                          implements Invocable, Compilable {
   
   private final ScriptEngine nashorn;
   private final Object scope; // for invokeFunction ?????????????
   
   public NashornScriptEngine (ScriptEngine nashorn) {
      this.nashorn = nashorn;
      try { scope = eval("new Object()"); }
      catch (ScriptException e) { throw new RuntimeException(e); }
   }

   @Override
   boolean isScriptable () { return false; }  //////  TRUE!!!
    
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
      throw new UnsupportedOperationException("get("+object+","+field+")");
   }
      
   @Override
   void put (Object object, String field, Object value) {
      throw new UnsupportedOperationException("put("+object+","+field+","+value+")");
   }
   
   @Override
   void delete (Object object, String field) {
      throw new UnsupportedOperationException("delete("+object+","+field+")");
   }
   
   @Override
   public Object invokeFunction (String name, Object... args)
         throws ScriptException, NoSuchMethodException {
      for (int i = args.length; i-- > 0;)
         args[i] = javaToJS(args[i], scope);
      return ((Invocable) nashorn).invokeFunction(name, args);
   }
   
   @Override
   public Object invokeMethod (Object thiz, String name, Object... args)
         throws ScriptException, NoSuchMethodException {
      for (int i = args.length; i-- > 0;)
         args[i] = javaToJS(args[i], thiz);
      return ((Invocable) nashorn).invokeMethod(thiz, name, args);
   }
   
   private static Object javaToJS (Object value, Object scriptable) {
      throw new UnsupportedOperationException("javaToJS("+value+","+scriptable+")");
   }
   
 /* TODO Use below to avoid calling 'eval' for slots with Java objects
    See Task.isScriptable(String)
 
   private static Object jsToJava (Object value, Class target) {
      switch (thisPackage) {
         case Package1:
             try {
               sun.org.mozilla.javascript.internal.Context.enter();
               return sun.org.mozilla.javascript.internal.Context.jsToJava(value, target); 
             } finally { sun.org.mozilla.javascript.internal.Context.exit(); }   
         case Package2:
            try {
               org.mozilla.javascript.Context.enter();
               return org.mozilla.javascript.Context.jsToJava(value, target); 
            } finally { org.mozilla.javascript.Context.exit(); }   
         case Package3:
            try {
               sun.org.mozilla.javascript.Context.enter();
               return sun.org.mozilla.javascript.Context.jsToJava(value, target); 
            } finally { sun.org.mozilla.javascript.Context.exit(); }   
         default: throw new IllegalArgumentException("Cannot perform jsToJava on "+value);
      }
   }
   
*/
   
   @Override
   public Bindings getBindings (int scope) { return nashorn.getBindings(scope); }
   
   @Override
   public ScriptContext getContext () { return nashorn.getContext(); }

   @Override
   public void setContext (ScriptContext context) { nashorn.setContext(context); }

   @Override
   public Object eval (String script, Bindings bindings) throws ScriptException {
      return nashorn.eval(script, bindings);
   }

   @Override
   public Object eval (String script) throws ScriptException {
      return nashorn.eval(script);
   }
   
   @Override
   public Boolean evalBoolean (String script, Bindings bindings) 
                  throws ScriptException {
      return (Boolean) eval(script, bindings);
   }

   @Override
   public Double evalDouble (String script, Bindings bindings) 
                  throws ScriptException {
      return (Double) eval(script, bindings);
   }
   
   @Override
   public Compiled compile (String script) throws ScriptException {
      return new RhinoCompiled(script);
   }

   private class RhinoCompiled extends Compiled {

      private final CompiledScript compiled;

      public RhinoCompiled (String script) throws ScriptException {
         compiled = ((Compilable) nashorn).compile(script);
      }

      @Override
      public Object eval (Bindings bindings) throws ScriptException {
         synchronized (bindings) { return compiled.eval(bindings); }
      }
      
      @Override
      public Boolean evalBoolean (Bindings bindings) throws ScriptException {
         return (Boolean) eval(bindings);
      }
   }

}
