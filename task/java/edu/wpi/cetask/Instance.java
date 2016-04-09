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
   
   private final TaskModel.Member type; 
   
   public TaskModel.Member getType () { return type; }
   
   public final TaskEngine engine; 
   
   Instance (TaskModel.Member type, TaskEngine engine) { 
      if ( type == null ) 
         throw new IllegalArgumentException("Type of instance cannot be null");
      this.type = type;
      this.engine = engine;
      bindings = engine.getScriptEngine().createBindings();
      bindings.put("$id", type.getId());
      bindings.put("$model", type.getNamespace());
      bindings.put("$instance", this);
   }
   
   // store slot values in JavaScript form
   protected final Bindings bindings;
          
   public Object eval (String expression, String where) {
      return eval(expression, null, bindings, where);
   }
   
   protected Object eval (String expression, Compiled compiled, Bindings extra,
         String where) {
      return evalFinal(expression, compiled, extra, where);
   }
      
   final Object evalFinal (String expression, String where) {
      return evalFinal(expression, null, bindings, where);
   }
   
   final Object evalFinal (String expression, Compiled compiled, Bindings extra, String where) {
      try { return compiled == null ? engine.eval(expression, extra, where) 
                                    : compiled.eval(extra);
      } catch (Exception e) { throw TaskEngine.newRuntimeException(e, where); }
   }
   
   public Object eval (String expression, Object value, String where) {
      synchronized (bindings) {
         try {
            bindings.put("$$value", value); 
            return eval(expression, null, bindings, where);
         } finally { bindings.remove("$$value"); }
      }
   }
   
   protected Boolean evalCondition (String expression, String where) {
      return evalCondition(expression, null, bindings, where);
   }

   protected Boolean evalCondition (String expression, Compiled compiled, Bindings extra,
                                    String where) {
      return evalConditionFinal(expression, compiled, extra, where);
   }

   final Boolean evalConditionFinal (String expression, String where) {
      return evalConditionFinal(expression, null, bindings, where);
   }
   
   final Boolean evalConditionFinal (String expression, Compiled compiled, Bindings extra,
                                    String where) {
      try { return compiled == null ? engine.evalBoolean(expression, extra, where)
                                    : compiled.evalBoolean(extra);
      } catch (Exception e) { throw TaskEngine.newRuntimeException(e, where); }
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
