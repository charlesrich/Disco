/* Copyright (c) 2008 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.cetask;

import cli.Jint.*;
import cli.Jint.Expressions.Program;
import cli.Jint.Native.*;

import java.util.Map.Entry;

import javax.script.*;

// wrapper for Jint to support running CETask/Disco under Mono (for Unity)
// not a complete implementation of ScriptEngine

class JintScriptEngine extends ScriptEngineWrapper 
                       implements Invocable, Compilable {

   // check if we are running in Mono
   public final static boolean EXISTS; 

   static {
      Object exists = null;
      try { exists = cli.Jint.JintEngine.class; }
      catch (Exception e) {}
      EXISTS = exists != null;
   }
   
   private final JintEngine jint = new JintEngine();
   
   private final JsInstance True, False;
   
   public JintScriptEngine () {
      jint.SetDebugMode(false); // to prevent calling ToString in Unity
      jint.DisableSecurity();
      jint.set_AllowClr(true);
      IGlobal global = jint.get_Global();
      JsBooleanConstructor bool = global.get_BooleanClass();
      True = bool.get_True();
      False = bool.get_False();
   }

   @Override
   boolean isScriptable () { return true; }
   
   @Override
   boolean isScriptable (Object value) { return value instanceof JsObject; }
   
   @Override
   boolean isDefined (Object value) { 
      // note null below means "not initialized", not JsNull.Instance
      return !( value == null || value == JsUndefined.Instance );
   }
   
   @Override
   Object undefined () { return JsUndefined.Instance; }
   
   // the type checks below are doing what LiveConnect does for Rhino
   
   @Override
   Object get (Object object, String field) {
      JsObject js = (JsObject) object;
      Descriptor descriptor = js.GetDescriptor(field);
      if ( descriptor == null ) return null;
      Object value = descriptor.Get(js);
      return value instanceof JsBoolean ? ((JsBoolean) value).ToBoolean() : 
         value instanceof JsString ? ((JsString) value).ToString() :
            value instanceof JsNumber ? ((JsNumber) value).ToNumber() :
               value;
   }

   @Override
   void put (Object object, String field, Object value) {
      if ( value instanceof Boolean ) 
         value = (Boolean) value ? True : False;
      else if ( value instanceof String )
         value = jint.get_Global().get_StringClass().New((String) value);
      else if ( value instanceof java.lang.Number )
         value = jint.get_Global().get_NumberClass().New(
               ((java.lang.Number) value).doubleValue());
      ((JsObject) object).DefineOwnProperty(field, (JsInstance) value);
   }

   @Override
   public Bindings getBindings (int scope) { 
      if ( scope == ScriptContext.ENGINE_SCOPE )
         throw new IllegalStateException("ENGINE_SCOPE for Jint not implemented!");
      return super.getBindings(scope);
   }
      
   @Override
   public Object eval (String script, Bindings bindings) throws ScriptException {
      try { 
         bindGlobal(bindings);
         return eval(script); 
      } finally { unbindGlobal(bindings); }
   }
   
   // TODO There probably is a more efficient way to do this binding and
   //      unbinding using the scoping structures of Jint
   
   private void bindGlobal (Bindings bindings) {
      bind(bindings);
      bind(getBindings(ScriptContext.GLOBAL_SCOPE));
   }
   
   private void bind (Bindings bindings) {
      synchronized (bindings) {
         for (Entry<String, Object> entry : bindings.entrySet()) 
            jint.SetParameter(entry.getKey(), entry.getValue());
      }
   }
   
   private void unbindGlobal (Bindings bindings) {
      unbind(bindings);
      unbind(getBindings(ScriptContext.GLOBAL_SCOPE));
   }
   
   private void unbind (Bindings bindings) {
      JsScope scope = jintScope();
      synchronized (bindings) {
         for (String key : bindings.keySet()) scope.Delete(key);
      }
   }

   private JsScope jintScope () {
      return ((ExecutionVisitor) jint.get_Global().get_Visitor())
             .get_GlobalScope();
   }

   @Override
   public Object eval (String script) throws ScriptException {
      try { 
         if (null != null) throw new JintException(); // to fool compiler
         return jint.Run(script);
      } catch (JintException e) { throw new ScriptException(e.toString()); }
   }
   
   @Override
   public Boolean evalBoolean (String script, Bindings bindings) 
                  throws ScriptException {
      cli.System.Object value = (cli.System.Object) eval(script, bindings);
      return value == null ? null :
         cli.System.Boolean.TrueString.equals(value.ToString());
   }

   @Override
   public Double evalDouble (String script, Bindings bindings) 
                  throws ScriptException {
      cli.System.Object value = (cli.System.Object) eval(script, bindings);
      return value == null ? Double.NaN : new Double(value.ToString());
   }

   @Override
   public Compiled compile (String script) throws ScriptException {
      try {
         if (null != null) throw new JintException(); // to fool compiler
         return new JintCompiled(script);
      } catch (JintException e) { throw new ScriptException(e.toString()); }
   }

   private class JintCompiled extends Compiled {

      private final Program program;

      public JintCompiled (String script) {
         program = JintEngine.Compile(script, true); //debug info   
      }

      @Override
      public Object eval (Bindings bindings) throws ScriptException {
         try {
            bindGlobal(bindings);    
            if (null != null) throw new JintException(); // to fool compiler
            return jint.Run(program); } 
         catch (JintException e) { throw new ScriptException(e.toString()); }
         finally { unbindGlobal(bindings); }
      }
      
      @Override
      public Boolean evalBoolean (Bindings bindings) throws ScriptException {
         cli.System.Object value = (cli.System.Object) eval(bindings);
         return value == null ? null :
            cli.System.Boolean.TrueString.equals(value.ToString());
      }
   }
   
   @Override
   public Object invokeFunction (String name, Object... args)
         throws ScriptException, NoSuchMethodException {
      JsScope scope = jintScope();
      Descriptor f = scope.GetDescriptor(name);
      if ( f == null ) throw new NoSuchMethodException("Javascript function not found: "+name);
      return jint.CallFunction((JsFunction) f.Get(scope), args);
   }
 
   // for testing
   
   public static void main (String[] args) {
      try {
         JintScriptEngine engine = new JintScriptEngine();
         Bindings bindings = new SimpleBindings();
         System.out.println(engine.eval("3+3", bindings));
         System.out.println(engine.compile("4+4").eval(bindings));
         bindings.put("x", "foo");
         System.out.println(engine.eval("x", bindings));
         try { 
            engine.invokeFunction("print", "hello"); 
            engine.invokeFunction("FOO");
         } catch (NoSuchMethodException e) { System.out.println(e); }
         try { System.out.println(engine.eval(")--x;")); }
         catch (ScriptException e) { System.out.println(e); }
      } catch (ScriptException e) { System.out.println(e); }
   }

}
