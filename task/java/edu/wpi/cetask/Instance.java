/* Copyright (c) 2008 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.cetask;

import edu.wpi.cetask.ScriptEngineWrapper.Compiled;

import java.io.PrintStream;

import javax.script.*;

/**
 * Base class for instances of classes.
 */
abstract class Instance {
   
   protected final TaskModel.Member type;
   
   public TaskModel.Member getType () { return type; }
   
   public final TaskEngine engine; 
   
   Instance (TaskModel.Member type, TaskEngine engine) { 
      if ( type == null ) 
         throw new IllegalArgumentException("Type of instance cannot be null");
      this.type = type;
      this.engine = engine;
      bindings = engine.getScriptEngine().createBindings();
      // $id and $model are future extensions to standard
      bindings.put("$id", type.getId());
      bindings.put("$model", type.getNamespace());
   }
   
   // store slot values in JavaScript form
   protected final Bindings bindings;
   
   public Object eval (String expression, String where) {
      return eval(expression, bindings, where);
   }
   
   public Object eval (String expression, Compiled compiled, String where) {
      try {
         return compiled == null ? eval(expression, where) : compiled.eval(bindings);
      } catch (ScriptException e) { throw new RuntimeException(e); }
   }
  
   protected Object eval (String script, Bindings extra, String where) {
      return engine.eval(script, extra, where); 
   }
   
   public void eval (String expression, Object value, String where) {
      synchronized (bindings) {
         try {
            bindings.put("$$value", value); 
            eval(expression, bindings, where);
         } finally { bindings.remove("$$value"); }
      }
   }

   protected Boolean evalCondition (String expression, String where) {
      return evalConditionFinal(expression, where);
   }
   
   final protected Boolean evalConditionFinal (String expression, String where) { 
      return engine.evalBoolean(expression, bindings, where);
   }
   
   protected Boolean evalCondition (String expression, Bindings extra, String where) { 
      return engine.evalBoolean(expression, extra, where);
   }
   
   protected Boolean evalCondition (String condition, Compiled compiled, String where) {
      return compiled != null ? evalCondition(compiled, bindings, where) :
         evalCondition(condition, bindings, where);
   }
   
   protected Boolean evalCondition (Compiled compiled, Bindings extra,
                                    String where) {
      return evalConditionFinal(compiled, extra, where);
   }

   // not overridden with updateBindings by Decomposition.Step
   final protected Boolean evalConditionFinal (Compiled compiled, Bindings extra, 
         String where) {
      if ( compiled == null ) return null;
      try { return compiled.evalBoolean(extra); }
      catch (ScriptException e) { throw TaskEngine.newRuntimeException(e, where); }
      catch (ClassCastException e) { throw TaskEngine.newRuntimeException(e, where); }
   }

   @Override
   public String toString () { 
      //TODO add error check for infinite looping due to circular slot value
      return TaskEngine.VERBOSE ? toStringVerbose() : getType().getPropertyId();
   }
   
   protected String toStringVerbose () { 
      return TaskEngine.DEBUG ? 
         getType()+"@"+Integer.toHexString(System.identityHashCode(this))
         : getType().toString();
   }

   protected PrintStream getOut () { return engine.getOut(); }
   
   protected PrintStream getErr () { return engine.getErr(); }

}
