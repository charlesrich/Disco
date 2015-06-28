/* Copyright (c) 2008 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.cetask;

import java.io.Reader;
import javax.script.*;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

// wrapper for script engine used in TaskEngine 
// a complete implementation of ScriptEngine is not required

abstract class ScriptEngineWrapper extends AbstractScriptEngine {
   
   public static ScriptEngineWrapper getScriptEngine () {
      ScriptEngineManager mgr = new ScriptEngineManager();
      for (ScriptEngineFactory factory : mgr.getEngineFactories()) 
         if ( factory.getNames().contains("ECMAScript") ) {
            ScriptEngineWrapper.JSR_223 wrapper = factory instanceof NashornScriptEngineFactory ?
               new NashornScriptEngine(
                     ((NashornScriptEngineFactory) factory).getScriptEngine(NashornScriptEngine.OPTIONS)) :
               new RhinoScriptEngine(factory.getScriptEngine());
            wrapper.jsr.setBindings(mgr.getBindings(), ScriptContext.GLOBAL_SCOPE);
            return wrapper;
         }
      if ( JintScriptEngine.EXISTS ) {
         // for Mono need to instantiate Jint engine manually
         JintScriptEngine jint = new JintScriptEngine();
         jint.setBindings(mgr.getBindings(), ScriptContext.GLOBAL_SCOPE);
         return jint;
      } else throw new IllegalStateException("No JavaScript engine found!");
   }
   
   // these three methods added to handle type coercion from Jint
   
   public Boolean evalBoolean (String script, Bindings bindings) 
                  throws ScriptException {
      return (Boolean) eval(script, bindings);
   }

   public Double evalDouble (String script, Bindings bindings) 
                  throws ScriptException {
      return (Double) eval(script, bindings);
   }

   public Long evalLong (String script, Bindings bindings) 
                  throws ScriptException {
      return (Long) eval(script, bindings);
   }
   
   boolean isScriptable () { return false; }
   boolean isScriptable (Object value) { return false; }
   
   // following four methods only used for scriptable engines
   
   boolean isDefined (Object value) { 
      throw new RuntimeException("Unimplemented"); 
   }
   
   Object undefined () { throw new RuntimeException("Unimplemented");  }
   
   Object get (Object object, String field) {
      throw new RuntimeException("Unimplemented"); 
   }
   
   void put (Object object, String field, Object value) { 
      throw new RuntimeException("Unimplemented"); 
   }
    
   void delete (Object object, String field) { 
      throw new RuntimeException("Unimplemented"); 
   }
   
   @SuppressWarnings("unused")
   public Object invokeMethod (Object thiz, String name, Object... args) 
         throws ScriptException, NoSuchMethodException {
      throw new RuntimeException("Unimplemented");
   }

   @SuppressWarnings("unused")
   public Object invokeFunction (String name, Object... args) 
         throws ScriptException, NoSuchMethodException {
      throw new RuntimeException("Unimplemented");
   }
   
   public Compiled compile (String script) throws ScriptException {
      throw new ScriptException("Unimplemented"); 
   }

   protected abstract class Compiled extends CompiledScript {

      // this method added to handle type coercion from Jint
      public abstract Boolean evalBoolean (Bindings bindings) throws ScriptException;

      @Override
      public ScriptEngine getEngine () { return ScriptEngineWrapper.this; }
   }
  
   protected static class JSR_223 extends ScriptEngineWrapper {
      
      protected final ScriptEngine jsr;
      
      protected JSR_223 (ScriptEngine jsr) {
         this.jsr = jsr;
      }

      @Override
      public Bindings createBindings () {
        return jsr.createBindings();
      }
      
      @Override
      public Bindings getBindings (int scope) { return jsr.getBindings(scope); }

      @Override
      public ScriptContext getContext () { return jsr.getContext(); }

      @Override
      public void setContext (ScriptContext context) { jsr.setContext(context); }

      @Override
      public Object eval (String script, ScriptContext context) throws ScriptException {
         return jsr.eval(script, context);
      }

      @Override
      public Object eval (String script) throws ScriptException {
         return jsr.eval(script);
      }
      
      @Override
      public Compiled compile (String script) throws ScriptException {
         return new CompiledJRS_223(script);
      }

      protected class CompiledJRS_223 extends Compiled {

         private final CompiledScript compiled;

         public CompiledJRS_223 (String script) throws ScriptException {
            compiled = ((Compilable) jsr).compile(script);
         }

         @Override
         public Object eval (ScriptContext context) throws ScriptException {
            return compiled.eval(context);
         }

         @Override
         public Boolean evalBoolean (Bindings bindings) throws ScriptException {
            return (Boolean) eval(bindings);
         }
      }
   }
   
   // unused methods

   @Override
   @Deprecated
   public ScriptEngineFactory getFactory () {
      throw new RuntimeException("Unimplemented");
   }

   @Override
   @Deprecated
   public Object eval (Reader reader, ScriptContext context) {
      throw new RuntimeException("Unimplemented");
   }

   @Deprecated
   public CompiledScript compile (Reader script) {
      throw new RuntimeException("Unimplemented");
   }
   
   @Deprecated
   public <T> T getInterface (Class<T> clasz) {
      throw new RuntimeException("Unimplemented");
   }

   @Deprecated
   public <T> T getInterface (Object thiz, Class<T> clasz) {
      throw new RuntimeException("Unimplemented");
   }

}
