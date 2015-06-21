/* Copyright (c) 2008 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.cetask;

import java.io.Reader;
import javax.script.*;

// wrapper for script engine used in TaskEngine 
// a complete implementation of ScriptEngine is not required

abstract class ScriptEngineWrapper extends AbstractScriptEngine {

   // these three methods added to handle type coercion from Jint
   public abstract Boolean evalBoolean (String script, Bindings bindings) 
                   throws ScriptException;
   
   public abstract Double evalDouble (String script, Bindings bindings) 
                   throws ScriptException;
   
   public abstract Long evalLong (String script, Bindings bindings) 
                   throws ScriptException;
   
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

      @Override
      @Deprecated
      public Object eval (ScriptContext context) {
         throw new RuntimeException("Unimplemented");
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
   public Object eval (String script, ScriptContext context) {
      throw new RuntimeException("Unimplemented");
   }

   @Override
   @Deprecated
   public Object eval (Reader reader, ScriptContext context) {
      throw new RuntimeException("Unimplemented");
   }

   @Override
   @Deprecated
   public Bindings createBindings () {
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
