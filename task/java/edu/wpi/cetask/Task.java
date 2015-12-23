/* Copyright (c) 2008 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.cetask;

import java.text.DateFormat;
import java.util.*;
import javax.script.*;
import edu.wpi.cetask.Description.Input;
import edu.wpi.cetask.Description.Output;
import edu.wpi.cetask.Description.Slot;
import edu.wpi.cetask.ScriptEngineWrapper.Compiled;
import edu.wpi.cetask.TaskClass.*;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

/**
 * Representation for instances of a task class.
 */
public class Task extends Instance {
 
   protected Task (TaskClass type, TaskEngine engine) { 
      super(type, engine); 
      synchronized (bindings) {
         // create JavaScript instance object (no initial slot properties)
         // do not use Instance.eval below because Decomposition.Step includes
         // call to updateBindings, but during constructor, steps are not yet added
         Object object = engine.newObject();
         bindings.put("$this", object);
         bindings.put("$plan", null);
         type.updateBindings(this); // extension
         // see section 8.3 of ANSI/CEA-2018
         engine.put(object, "model", type.getNamespace());
         engine.put(object, "task", type.getId());
      }
   }
   
   @Override
   public TaskClass getType () { return (TaskClass) super.getType(); }

   // for hashing/comparing of Tasks in Agenda.generate()

   @Override
   public boolean equals (Object object) {     
      if ( !(object instanceof Task) ) return false;
      Task task = (Task) object;
      if ( getType() != task.getType() ) return false;
      for (Slot slot : getType().getInputs())
         if ( !Utils.equals(slot.getSlotValue(this), slot.getSlotValue(task)) ) 
            return false;
      for (Slot slot : getType().getOutputs())
         if ( !Utils.equals(slot.getSlotValue(this), slot.getSlotValue(task)) ) 
            return false;
      return true;
   }

   @Override 
   public int hashCode () {
      int factor = 31;
      int hash = getType().hashCode()*factor;
      for (Slot slot : getType().getInputs()) {
         Object value = slot.getSlotValue(this);
         if ( value != null ) {
            factor *= 31;
            hash += value.hashCode()*factor;
         }
      }
      for (Slot slot : getType().getOutputs()) {
         Object value = slot.getSlotValue(this);
         if ( value != null ) {
            factor *= 31;
            hash += value.hashCode()*factor;
         }
      }
      return hash;
   }
  
   // see Javascript function definitions in default.js
   final static String cloneThis = "edu_wpi_cetask_clone($this);",
                       cloneSlot = "$this[$$value] = edu_wpi_cetask_clone($this[$$value]);";
                       
   static Compiled compiledCloneThis, compiledCloneSlot;
   
   /**
    * Return value of the named slot.<br>
    * <br>
    * Note that this method returns a Java object, but the <em>real</em>
    * values are stored in JavaScript and must be converted to Java objects when
    * returned by this method. Refer to Chapter 12 of O'Reilly JavaScript guide
    * for details of conversion, but note following subtleties here:
    * <ul>
    * <li> JavaScript objects and arrays (i.e., everything except for numbers,
    * strings and booleans) convert to Java objects of "opaque" type. These
    * values can be passed back to JavaScript, but have an unpublished API that
    * is not intended for use by Java programs.
    * <li> <em>Both</em> null and undefined in JavaScript convert to null
    * </ul>
    * Since there is no attribute for "optional" slots in CEA-2018, all input
    * slot values must be defined before executing a task and all output slots
    * will be defined after execution. However, a null value can be used unless
    * a pre- or postcondition specifies otherwise.
    * 
    * @see #isDefinedSlot(String)
    * @see #removeSlotValue(String)
    * @see #isDefinedInputs()
    * @see #isDefinedOutputs()
    */
   public Object getSlotValue (String name) { 
      checkIsSlot(name);
      if ( isScriptable(name) ) {
         Object object = bindings.get("$this");
         return engine.isDefined(object, name) ? engine.get(object, name) : null;
         // evalFinal below to avoid returning cached value in clonedInputs
      } else return evalFinal("$this."+name, "getSlotValue"); 
   }

   private void checkIsSlot (String name) {
      if ( !getType().isSlot(name) ) 
         throw new IllegalArgumentException(name+" is not slot of "+getType());
   }
   
   protected Boolean getSlotValueBoolean (String name) {
      checkIsSlot(name);
      if ( isScriptable(name) ) {
         Object object = bindings.get("$this");
         return engine.isDefined(object, name) ? (Boolean) engine.get(object, name) : null;
         // evalConditionFinal below to avoid returning cached value in clonedInputs
      } else return evalConditionFinal("$this."+name, "getSlotValueBoolean"); 
   }
   
   private boolean isScriptable (String name) {
      // for Java objects (or if could be Java object), must use LiveConnect
      Slot slot = getType().getSlot(name);
      return slot.getType() != null && slot.getJava() == null;
   }

   /**
    * Return the string obtained by calling <em>JavaScript</em> toString method 
    * on value of given slot.   Good for debugging output.
    * 
    * @see TaskEngine#toString(Object)
    */
   public String getSlotValueToString (String name) {
      return engine.toString(getSlotValue(name));
   }

   /**
    * Test whether slot value is defined. Note if getSlotValue() returns null,
    * isDefinedSlot() may return true <em>or</em> false.
    * 
    * @see #getSlotValue(String)
    * @see #removeSlotValue(String)
    */
   public boolean isDefinedSlot (String name) {
      checkIsSlot(name);
      updateBindingsTask(false);
      return engine.isDefined(bindings.get("$this"), name); 
   }     
   
   // see Decomposition
   private boolean modified = true;
   boolean isModified () { return modified; }
   void setModified (boolean modified) { this.modified = modified; }
   
   /**
    * Set the value of named slot to given value.<br>
    * <br>
    * Note that the given Java object must be converted to a JavaScript object
    * for storage. Refer to Chapter 12 of O'Reilly JavaScript guide for details
    * of conversion, but note following subtleties here:
    * <ul>
    * <li> Java objects and arrays (i.e., everything but numbers, booleans and 
    * strings) convert to instances of JavaObject and JavaArray 
    * </ul>
    * 
    * @see #setSlotValue(String,Object,boolean)
    * @see #getSlotValue(String)
    * @see #setSlotValueScript(String,String,String)
    */
   public Object setSlotValue (String name, Object value) {
      return setSlotValue(name, value, true);
   }   
   
   /**
    * Set the value of named slot to given value.<br>
    *
    * @see #setSlotValue(String,Object)
    */
   public Object setSlotValue (String name, Object value, boolean check) { 
      checkIsSlot(name);
      checkCircular(name, value);
      String type = getType().getSlotType(name);
      if ( !check || checkSlotValue(name, type, value) ) {
         if ( engine.isScriptable(value) ) {
            modified = true;
            engine.put(bindings.get("$this"), name, value);
         } else { 
            synchronized (bindings) {
               try {
                  bindings.put("$$value", value); 
                  modified = true;
                  evalFinal("$this."+name+" = $$value;", "setSlotValue");
               } finally { bindings.remove("$$value"); }
            }
         }
         if ( clonedInputs != null ) engine.put(clonedInputs, name, value);
      } else failCheck(name, value == null ? "null" : value.toString(), "setSlotValue");
      return value;
   }  
      
   protected void checkCircular (String name, Object value) {
      if ( value == this ) 
         // will cause infinite loop when printing
         throw new RuntimeException("Attempting to store pointer to self in slot "
               +name+" of "+this);
   }
   
   private boolean checkSlotValue (String name, String type, Object value) {
      if ( type == null || 
            ( type.equals("boolean") && value instanceof Boolean ) ||
            ( type.equals("string") && value instanceof String ) ||
            ( type.equals("number") && value instanceof Number ) )
         return true;
      Slot slot = getType().getSlot(name);
      // ignore undefined and allow any slot to be set to null (for "optional" inputs)
      if ( value == null ) {
         if ( slot instanceof Input && !((Input) slot).isOptional() )
            getErr().println("WARNING: Setting null value for non-optional input slot "+name+" of "+this);
         return true;
      }
      if ( slot.getJava() != null ) return slot.getJava().isInstance(value);
      if ( engine.getScriptEngine() instanceof JintScriptEngine // for Unity only
            && Utils.startsWith(type, "Packages.") )
         try { return Class.forName(type.substring(9)).isInstance(value); }
         catch (ClassNotFoundException e) { return false; }
      synchronized (bindings) {
         try {
            bindings.put("$$value", value); 
            return Utils.booleanValue(
                  eval( (type.equals("boolean") || type.equals("string") || type.equals("number")) ?
                         ("typeof $$value === '"+type+"'") :
                         ("$$value instanceof "+type), 
                        "checkSlotValue"));
         } finally { bindings.remove("$$value"); }
      }
   }

   /**
    * Set the value of named slot to result of evaluating given JavaScript
    * expression. This method avoids issues of conversion from Java to
    * JavaScript objects.
    * 
    * @see #setSlotValue(String,Object)
    */
   public void setSlotValueScript (String name, String expression, String where) {
      setSlotValueScript(name, expression, null, where);
   }

   protected void setSlotValueScript (String name, String expression, Compiled compiled, String where) {
      // only evaluate expression once (in case of side effects)
      if ( !evalConditionFinal(compiled != null ? null :
                  makeExpression("$this", getType(), name, expression, false),
               compiled, bindings, where) )
         failCheck(name, expression, where);
      else modified = true;
   }
   
   /**
    * Similar to {@link #setSlotValueScript(String,String,String)} but with extra 
    * bindings to use for evaluation of expression (instead of task bindings)
    */
   protected void setSlotValueScript (String name, String expression, Compiled compiled, 
         Bindings extra, String where) {
      try {
         extra.put("$$this", bindings.get("$this"));
         if ( !evalConditionFinal(compiled != null ? null :
                    makeExpression("$$this", getType(), name, expression, true), 
                  compiled, extra, where) )
            failCheck(name, expression, where);
         else modified = true;
      } finally { extra.remove("$$this"); }
   }
   
   static String makeExpression (String self, TaskClass type, String name, 
         String value, boolean onlyDefined) {
      StringBuilder buffer = new StringBuilder();
      buffer.append("(function(value){");
      if ( onlyDefined ) buffer.append("if (value === undefined) return true; ");
      buffer.append("if ( ").append(checkExpression(type, name)).append(" ) {")
      // note using [ ] to protect keywords
      .append(self).append("['").append(name).append("']")
      .append(" = value; return true;} return false;})(")
      .append(value).append(")");
      return buffer.toString();
   }
   
   void failCheck (String name, String expression, String where) {
      throw new IllegalArgumentException(
            "Type error: Cannot assign result of "+expression+" to "
            +getType().getSlotType(name)+" slot "+name
            +" of "+getType()+" in "+where);
   }
  
   private static String checkExpression (TaskClass task, String name) {
      String type = task.getSlotType(name);
      return type == null ? "true" :
          // ignore undefined and allow any slot to be set to null (for "optional" inputs)
          ("value === undefined || value === null || "+ 
             ("boolean".equals(type) ? "typeof value == \"boolean\"" :
               "string".equals(type) ? "typeof value == \"string\"" :
                  "number".equals(type) ? "typeof value == \"number\"" :
                        "value instanceof "+type));
   }
   
   public int countSlotValues () {
      int n = 0;
      for (Slot slot : getType().getSlots()) 
         if ( slot.isDefinedSlot(this) ) n++;
      return n;
   }
   
   /**
    * Make named slot undefined.
    * 
    * @see #isDefinedSlot(String)
    */
   public void removeSlotValue (String name) {
      removeSlotValueFinal(name);
      updateBindingsTask(true); 
   }
  
   protected final void removeSlotValueFinal (String name) {
      checkIsSlot(name);
      engine.remove(bindings.get("$this"), name);
      if ( clonedInputs != null ) engine.remove(clonedInputs, name); 
      modified = true;
   }
   
   public Boolean isApplicable () {
      Precondition condition = getType().getPrecondition();
      return condition == null ? null : condition.evalCondition(this);
   }
   
   private Boolean achieved;
   
   public void setAchieved (Boolean achieved) { 
      this.achieved = achieved;
      if ( achieved ) setSuccess(true);
      engine.clearLiveAchieved();
   }
   
   public Boolean isAchieved () {
      if ( achieved != null ) return achieved; // overrides condition
      if ( engine.containsAchieved(this) )  // check cached value
         return engine.isAchieved(this);
      Postcondition condition = getType().getPostcondition();
      if ( condition == null ) return null;
      Boolean achieved;
      if ( getType().hasModifiedInputs() ) {
         if ( isOccurred() ) {
            if ( clonedInputs == null ) // not checking each modified inputs
               throw new IllegalStateException("Modified inputs have not been cloned: "+this);
            synchronized (bindings) {
               Object old = bindings.get("$this");
               try {
                  bindings.put("$this", clonedInputs);
                  achieved = condition.evalCondition(this);
               } finally { bindings.put("$this", old); }
            }
         } else try { // not occurred
            // temporarily set modified outputs for sufficient postconditions
            setModifiedOutputs();
            achieved = condition.evalCondition(this);
         } finally {
            for (Input input : getType().getDeclaredInputs()) {
               Output output = input.getModified();
               if ( output != null ) output.deleteSlotValue(this);
            }
         }
      } else achieved = condition.evalCondition(this);
      return engine.setAchieved(this, achieved);// store cache
   }	
 
   @Override
   protected Object eval (String expression, Compiled compiled, Bindings extra, String where) {
      updateBindingsTask(false);
	   if  ( clonedInputs == null ) return evalFinal(expression, compiled, extra, where);
	   synchronized (extra) {
	      Object old = extra.get("$this");
		   try {
			   extra.put("$this", clonedInputs);	
			   return evalFinal(expression, compiled, extra, where);	
		   } finally { extra.put("$this", old); } 
	   }
   }	
    
   @Override
   protected Boolean evalCondition (String expression, Compiled compiled, Bindings extra, String where) {
	   updateBindingsTask(false);
	   if ( clonedInputs == null ) return evalConditionFinal(expression, compiled, extra, where);
       synchronized (extra) {
          Object old = extra.get("$this");
          try {
             extra.put("$this", clonedInputs);
             return evalConditionFinal(expression, compiled, extra, where);
           } finally { extra.put("$this", old); }
       }
   }		
   
   /**
    * Test whether all declared input slots (i.e., not including
    * "external") have defined values.
    * 
    * @see #getSlotValue(String)
    */
   public boolean isDefinedInputs() {
      for (String name : getType().declaredInputNames)
         if ( !isDefinedSlot(name) ) return false;
      return true;
   }

   /**
    * Test whether all declared output slots (i.e., not including "success" and
    * "when") have defined values. 
    * 
    * @see #getSlotValue(String)
    */
   public boolean isDefinedOutputs() {
      for (String name : getType().declaredOutputNames)
         if ( !isDefinedSlot(name) ) return false;
      return true;
   }

   /**
    * Test whether this task starts given plan.  (For extensions.)
    * 
    * @see Plan#isStarted()
    */
   public boolean isStarter (Plan plan) { return true; };
   
   /**
    * Test whether this task is instance of primitive type.
    */
   public boolean isPrimitive () { return getType().isPrimitive(); }
   
   /**
    * Test whether this task is instance of an internal type.
    */
   public boolean isInternal () { return getType().isInternal(); }
   
   
   /**
    * Test whether this task is temporary.  For extension to
    * Disco with Accept utterance.  See {@link Plan#isExhausted()}
    */
   public boolean isTemporary () { return false; }
   
   // predefined slots
   
   /**
    * Get time when this task occurred, if it did.
    *
    * @return milliseconds or 0 (if not occurred)
    * @see #setWhen(long)
    */
   public long getWhen () {
      Object value = getSlotValue("when"); 
      return value == null ? 0 : (Long) value;
   }

   /**
    * Set the time when this task occurred. 
    * 
    * @see #getWhen()
    */
   public void setWhen (long milliseconds) {
      setSlotValue("when", milliseconds);
   }
   
   /**
    * Test whether this task instance occurred.
    */
   public boolean isOccurred () { 
      // do not call isDefinedSlot to avoid circularity
      return engine.isDefined(bindings.get("$this"), "when");
   }
      
   /**
    * Get the value of the predefined 'success' slot 
    * @return TRUE, FALSE or null
    */
   public Boolean getSuccess () { return getSlotValueBoolean("success");  }

   /**
    * Set the predefined 'success' output slot of this task.
    * 
    * @param success - note type boolean so converts to JavaScript boolean
    */
   public void setSuccess (boolean success) {
      setSlotValue("success", success);
      engine.clearLive();  // not clearLivedAchieved (infinite loop)
   }
   
   protected Boolean checkAchieved () {
      Boolean condition = isAchieved();
      // if condition is unknown, then don't change success slot
      // since current value may come from communication or grounding script
      if ( condition != null ) {
         Boolean slot = getSuccess();
         boolean sufficient = getType().isSufficient();
         if ( slot == null ) {
            if ( Utils.isFalse(condition) || sufficient )
               setSuccess(condition);
         } else if ( !condition.equals(slot) ) { // conflicting values
            if ( Utils.isFalse(condition) ) {
               // if condition indicates failure, then be conservative
               setSuccess(condition);
               getErr().println("WARNING: Ignoring true success slot in "+this);
            } else if ( sufficient ) 
               // if slot indicates failure, then be conservative
               getErr().println("WARNING: Ignoring true sufficient postcondition in "+this);
         }
      }
      return getSuccess();
   }

   /**
    * Get the value of the predefined 'external' input slot.
    *  
    * @return TRUE, FALSE or null
    */
   public Boolean getExternal () {
      return (Boolean) getSlotValue("external"); 
   }
   
   /**
    * Set the predefined 'external' input slot of this <em>primitive</em> task.
    * 
    * @param external - note type boolean so converts to JavaScript boolean
    */
   public void setExternal (boolean external) {
      if ( !isPrimitive() ) 
         throw new UnsupportedOperationException("Cannot set external slot of non-primitive task: "+this);
      setSlotValue("external", external);
   }

   /**
    * Test whether this task <em>must</em> be executed by user.  It must be
    * be executed by the user iff the 'external' slot is true. 
    * Always returns false for non-primitive tasks.  
    *  <p>
    * Note that
    * both {@link #isUser()} and {@link #isSystem()} may return false, but
    * both may not return true.
    * 
    * @see #canUser()
    */
   public boolean isUser () {  return Utils.isTrue(getExternal()); }

   /**
    * Test whether this task <em>must</em> be executed by system. It must be
    * executed by system iff the 'external' slot is false. 
    * Always returns false for non-primitive tasks.
    * <p> 
    * Note that
    * both {@link #isUser()} and {@link #isSystem()} may return false, but 
    * both may not return true.
    * 
    * @see #canSystem()
    */
   public boolean isSystem () { return Utils.isFalse(getExternal()); } 

   /**
    * Test whether this task <em>can</em> be executed by user.  It can be
    * executed by user iff it is primitive and it is not true that it must 
    * be executed by system.  Always returns false for non-primitive tasks.  
    * <p>
    * Note that both {@link #canUser()} and {@link #canSystem()} 
    * may return true, but both may not return false.
    * 
    * @see #isUser()
    */
   public boolean canUser() { return !isSystem() && isPrimitive(); }
   
   /**
    * Test whether this task <em>can</em> be executed by system. It can be
    * executed by system iff it is primitive and it is not true that it must
    * be executed by user.  Always returns false for non-primitive tasks.    
    * <p> 
    * Note that both 
    * {@link #canUser()} and {@link #canSystem()} may return true, but both may 
    * not return false.
    * 
    * @see #isSystem()
    */
   public boolean canSystem() {  return !isUser() && isPrimitive(); }
   
   private boolean unexplained;
   
   /**
    * Test whether this task occurrence is explained (e.g., contributes) to
    * current discourse state.
    */
   public boolean isUnexplained () { return unexplained; }
   
   /**
    * Set whether this task occurrence is explained (e.g., contributes) to
    * current discourse state.@param unexplained
    */
   public void setUnexplained (boolean unexplained) { this.unexplained = unexplained; }

   /**
    * Return modifiable list of decomposition classes <em>applicable</em> to this task
    * and not rejected.
    * 
    * @see TaskClass#getDecompositions()
    */
   public List<DecompositionClass> getDecompositions () {
      List<DecompositionClass> decomps = new ArrayList<DecompositionClass>(
            getType().getDecompositions());
      for (Iterator<DecompositionClass> i = decomps.iterator(); i.hasNext();) {
         DecompositionClass next = i.next();
         // remove decompositions not applicable to or rejected for this task instance
         if ( rejected.contains(next) || Utils.isFalse(next.isApplicable(this)) )      
            i.remove();
      }
      return decomps; 
   }

   // update 2018-ext self slot bindings on task
   protected void updateBindingsTask (boolean modified) {
      // never update slots on occurrence
      if ( !isOccurred() ) getType().updateBindings(this);       
   }
   
   /**
    * Update engine state based on occurrence of this task.
    * 
    * @param contributes plan to which this occurrence has been found to contribute
    *                    or null if no such plan could be found
    * @param continuation true if contributes started before this task matched                   
    * @return false iff this task is unexplained
    */
   public boolean interpret (Plan contributes, boolean continuation) { 
      // continuation unused here but needed in Disco
      if ( contributes != null ) {
         contributes.match(this);
         return true;
      } else return false;
   }
   
   /**
    * Test whether given task instance unifies with this task instance. Two task
    * instances match (symmetric relationship) iff they are both instances of the
    * same type and the values of each corresponding slot are either equal or
    * one is undefined.
    * 
    * @see #copySlotValues(Task)
    */
   public boolean isMatch (Task goal) {
      if ( goal == this || goal instanceof Any ) return true;
      if ( !getType().equals(goal.getType()) ) return false;
      for (String name : getType().inputNames)
         if ( !isMatchSlot(goal, name) ) return false;
      for (String name : getType().outputNames)
         if ( !isMatchSlot(goal, name) ) return false;
      return true;
   }
   
   private boolean isMatchSlot (Task goal, String name) {
      if ( !isDefinedSlot(name) || !goal.isDefinedSlot(name) ) return true;
      Object value = getSlotValue(name);
      Object goalValue = goal.getSlotValue(name);
      if ( Utils.equals(value, goalValue) ) return true;
      // otherwise use JavaScript equals if possible
      ScriptEngine script = engine.getScriptEngine();
      if ( script instanceof Invocable && value instanceof ScriptObjectMirror )
         try {
            return (boolean) ((Invocable) script).invokeMethod(value, "equals", goalValue);
         } catch (ScriptException | NoSuchMethodException e) { return false; }
      else return false;
   }

   /**
    * Test whether this task either matches goal of given plan or could
    * be viewed as a part of the plan for given task, i.e., whether
    * given plan explains this task.  
    * <p>
    * This method is for use in extensions.  In CETask, it is equivalent 
    * to {@link #isMatch(Task)}.   
    */
   public boolean contributes (Plan plan) {
      return isMatch(plan.getGoal());
   }
   
   /**
    * Test whether this task can directly contribute to a task of given type.
    * (For extension to plan recognition.)
    */
   public boolean contributes (TaskClass type) {
      return type == Any.CLASS || getType() == type;
   }
   
   /**
    * Test whether this task can indirectly contribute to a task of given type.  Returns
    * false if {@link #contributes(TaskClass)} returns true.
    * (For extension to plan recognition.)
    */
   public boolean isPathFrom (TaskClass type) {
      return getType().isPathFrom(type);
   }
   
   /**
    * Copies the values of defined slots <em>from</em> given task to this task. Note this
    * side-effects this task, overwriting existing slot values.
    * 
    * @param from - task of same type
    * @return true if any slots of this task overwritten (were defined)
    * 
    * @see #isMatch(Task)
    */
   public boolean copySlotValues (Task from) {
      if ( !getType().equals(from.getType()) ) 
         throw new IllegalArgumentException("Cannot copy slot values from "+
               from.getType()+" to "+getType()); 
      boolean overwrite = false;
      for (String name : getType().inputNames)
         overwrite = copySlotValue(from, name, name, true, false) || overwrite;
      for (String name : getType().outputNames)
         overwrite = copySlotValue(from, name, name, true, false) || overwrite;
      if ( from.clonedInputs != null ) {
         if ( clonedInputs != null ) overwrite = true;
         clonedInputs = from.clonedInputs;
      }
      return overwrite;
   }
   
   /**
    * If named slot of given task is defined, copy value <em>from</em> 
    * slot name of given task to given slot of this task.
    *
    * @return true if slot of this task is overwritten (was defined)
    */
   public boolean copySlotValue (Task from, String fromSlot, String thisSlot) {
      return copySlotValue(from, fromSlot, thisSlot, true, true);
   }
   
   boolean copySlotValue (Task from, String fromSlot, String thisSlot, 
         boolean onlyDefined, boolean check) {
      from.updateBindingsTask(false);
      Object fromObject = from.bindings.get("$this");
      if ( onlyDefined && !engine.isDefined(fromObject, fromSlot) ) return false;
      Object value = engine.get(fromObject, fromSlot);
      checkCircular(thisSlot, value);
      if ( check ) checkSlotValue(thisSlot, getType().getSlotType(thisSlot), value);
      Object task = bindings.get("$this");
      boolean overwrite = engine.isDefined(task, thisSlot);
      engine.put(task, thisSlot, value);
      modified = true;
      return overwrite;
   }

   /**
    * Execute primitive <em>system</em> task. 
    * 
    * @see TaskEngine#done(Task)
    */
   void execute (Plan plan) {
      occurred(false); // before script executed
      eval(plan);
   }
   
   protected  void evalIf (Plan plan) { if ( isSystem() ) eval(plan); }
   
   // public for Console.execute()
   public void eval (Plan plan) { eval(plan, getType()); }
   
   protected void eval (Plan plan, TaskClass type) {  // for Task.Any
      cloneInputs(); //  cache modified inputs before grounding script executed
      Grounding script = getGrounding();
      if ( script != null ) 
         synchronized (bindings) {
            bindings.put("$plan", plan);
            script.eval(this);      
         }
      // set outputs to modified inputs
      if ( type.getPostcondition() != null )
         for (Input input : type.declaredInputs) {
            Output modified = input.getModified();
            if ( modified != null ) 
               engine.put(clonedInputs, modified.getName(), 
                     getSlotValue(modified.getName()));
      }
   }
   
   public void occurred (boolean external) {
      synchronized (engine.synchronizer) {
         setExternal(external); 
         occurred();
      }
   }
   
   public void occurred () {
	  if ( !isDefinedSlot("external") ) 
		  throw new IllegalStateException("Occurrence must have external slot value "+this);
      setModifiedOutputs();
      synchronized (engine.synchronizer) {
         setWhen(System.currentTimeMillis());
         engine.tick();
         engine.setLastOccurrence(this);
      }
   }

   private void setModifiedOutputs () {
      TaskClass type = getType();
      for (Input input : type.declaredInputs){
         Output modified = input.getModified();
         if ( modified != null ) {
            if ( input.isDefinedSlot(this) ) {
               Object value = input.getSlotValue(this); 
               if ( modified.isDefinedSlot(this) ) {
                  if ( modified.getSlotValue(this) != value ) 
                     throw new IllegalStateException("Output of modified input not identical "+input.getName()); 
               } else // propagate modified input object to output
                  modified.setSlotValue(this, value);
            }
         }
      }
   }

   // copy of $this with cloned inputs (includes outputs)
   private Object clonedInputs;  
   
   /**
    * Clone and cache copy of all modified inputs <em>before</em> modification (for
    * use in evaluating postconditions).  Note that for reported actions by
    * user, this method should be called before done method.
    * 
    * @see #cloneInput(String)
    */
   public void cloneInputs () {
      for (Input input : getType().declaredInputs)
         if ( input.getModified() != null ) cloneInput(input.getName());
   }
   
   /**
    * Clone and cache copy of named input. 
    * 
    * @see #cloneInputs()
    */
   public void cloneInput (String name) {
      Slot slot = getType().getSlot(name);
      if ( slot == null || !(slot instanceof Input) || ((Input) slot).getModified() == null )
         throw new IllegalArgumentException("Not a modified input: "+name);
      if ( clonedInputs == null ) 
         clonedInputs = eval(cloneThis, compiledCloneThis, bindings, "cloneInput");
      synchronized (bindings) {
         Object old = bindings.get("$this");
         try {
            bindings.put("$this", clonedInputs);
            bindings.put("$$value", name);
            eval(cloneSlot, compiledCloneSlot, bindings, "cloneInput");
         } finally { 
            bindings.put("$this", old); 
            bindings.remove("$$value"); 
         }
      }
   }
   
   /**
    * Return appropriate and applicable script to ground this primitive task, or null if none.
    */
   public Grounding getGrounding () {
      Grounding script = getType().getGrounding();
      return script == null ? null : 
         Utils.isFalse(script.isApplicable(this)) ? null : script;
   }
   
   public String getProperty (String key) { return getType().getProperty(key); }
   
   public Boolean getProperty (String key, Boolean defaultValue) { 
      return getType().getProperty(key, defaultValue);
   }
      
   protected String toPropertyString (Object value) {
      // property key should not depend on these settings
      boolean debug = TaskEngine.DEBUG, print = TaskEngine.PRINT_TASK,
            verbose = TaskEngine.VERBOSE; 
      try { 
         TaskEngine.DEBUG = TaskEngine.PRINT_TASK = TaskEngine.VERBOSE = false;
         return engine.toString(value); 
      } finally {
         TaskEngine.DEBUG = debug;
         TaskEngine.PRINT_TASK = print;
         TaskEngine.VERBOSE = verbose;
      }
   }
      
   public String getPropertySlot () { return null; }
   
   // following code is for Disco extension--not used in CE Task engine
   
   private Boolean should;
   
   /**
    * Tests whether this task should be executed (default null).  
    * 
    * @return true if task has been accepted, false if rejected, otherwise null 
    */
   public Boolean getShould () { return should; }

   /**
    * Sets belief as to whether this task should be executed (default null).
    * 
    * @see #getShould()
    */
   public void setShould (Boolean should) { this.should = should; }
   
   private List<DecompositionClass> rejected = Collections.emptyList();
   
   /**
    * Return list of decomposition classes rejected for this task.
    * 
    * @see #reject(DecompositionClass)
    */
   public List<DecompositionClass> getRejected () { return rejected; }
   
   /**
    * Add given decomposition class to list of rejects for this task.
    * 
    * @see #getRejected()
    */
   public void reject (DecompositionClass decomp) {
      if ( rejected.isEmpty() ) rejected = new ArrayList<DecompositionClass>();
      rejected.add(decomp);
   }

   /**
    * Builtin task class used for steps in which task attribute omitted
    * (CEA-2018-ext). This is essentially a way to make the task representation
    * partly second order. Note it has an input (see Disco.xml) named 'type'
    * of type TaskClass, which is specially handled below.
    */
   public static class Any extends Decomposition.Step {
      
      public static TaskClass CLASS;
      
      // for TaskClass.newStep
      public Any (TaskEngine engine, Decomposition decomp, String name, boolean repeat) { 
         super(CLASS, engine, decomp, name);
      }
      
      public Any (TaskEngine engine) { this(engine, null, null, false); }

      @Override
      public boolean isMatch (Task goal) { return true; }
      
      // TODO This implementation does not handle inputs/outputs 
      //      Add lists of inputs and outputs?
      
      // Note: do not be tempted to actually change the type and associated
      // properties, since still need input slot for changing bindings

      @Override
      public boolean copySlotValues (Task from) {
         TaskClass type = from.getType();
         if ( type == CLASS ) return super.copySlotValues(from);
         if ( !(type.getDeclaredInputs().isEmpty() && type.getDeclaredOutputs().isEmpty()) )
            throw new IllegalArgumentException("Task.Any does not yet support inputs/outputs: "+from);
         setSlotValue("type", type);
         if ( from.getExternal() != null ) setExternal(from.getExternal());
         if ( from.getSuccess() != null ) setSuccess(from.getSuccess());
         if ( from.getWhen() != 0 ) setWhen(from.getWhen());
         return true; 
      }
      
      @Override
      public void eval (Plan plan) { 
         // TODO grounding script may reference slots
         TaskClass type = (TaskClass) getSlotValue("type");
         if ( type == null ) 
            throw new IllegalArgumentException("Cannot execute grounding script for: "+plan);
         eval(plan, type);
      }
   }
   
   // *******************************************************************
   //     All code below here is printing-related stuff 
   // *******************************************************************
   
   @Override
   public String toString () {
      if ( TaskEngine.VERBOSE ) return toStringVerbose();
      StringBuilder buffer = argListBuilder(getDeclaredSlotValues());
      if ( isPrimitive() ) {
         Boolean external = getExternal();
         if ( buffer.length() > 0 ) buffer.insert(0, ',');
         if ( external != null ) buffer.insert(0, external ? 
            engine.getExternalName() : engine.getSystemName() );
      }
      String id = getType().getPropertyId();
      return buffer.length() == 0 ? id : buffer.insert(0, '(').insert(0, id).append(')').toString();
   }
   
   static private final Object undefined = new Object();
   
   private static class Duplicate {
      private final String string;
      private Duplicate (String string) { this.string = string; }
      @Override
      public String toString () { return string; }
   }
   
   public List<Object> getDeclaredSlotValues () {
      List<Object> list = new ArrayList<Object>();
      TaskClass type = getType();
      for (String name : type.declaredInputNames)
         list.add(getSlotValueIf(name));
      for (Output output : type.declaredOutputs) {
         Object value = getSlotValueIf(output.getName());
         Input input = type.getModifiedInput(output);
         if ( input != null ) {
            Object old = getSlotValueIf(input.getName());
            String string;
            list.add( (old == undefined || value == undefined) ? value :
               Utils.equals(string = toString(old), toString(value)) ? new Duplicate(string) : value);
         } else list.add(value);
      }
      return list;
   }

   private Object getSlotValueIf (String name) {
      return isDefinedSlot(name) ? 
         ( isScriptable(name) ? 
            ( clonedInputs != null ? engine.get(clonedInputs, name) :
               getSlotValue(name) )
            : eval("$this."+name, "getSlotValueIf") )
         : undefined;
   }
   
   protected StringBuilder argListBuilder (List<Object> args) {
      for (int i = args.size(); i-- > 0;) { 
         Object arg = args.get(i);
         // trim undefined slots and duplicate printing modified outputs from end
         if ( arg == undefined || arg instanceof Duplicate ) args.remove(i);
         else break;
      }
      StringBuilder buffer = new StringBuilder();
      boolean first = true;
      for (Object arg : args) {
         if ( first ) first = false;
         else buffer.append(",");
         if ( arg != undefined ) 
            buffer.append(arg == this ? "!circular!" : toString(arg));
      }
      return buffer;
   }
   
   private String toString (Object value) {
      return value instanceof String ? ( "\""+value+"\"" ) : engine.toString(value);
   }
   
   @Override
   protected String toStringVerbose () { 
      StringBuilder buffer = new StringBuilder(super.toStringVerbose()).append("={ ");
      boolean first = true;
      for (String name : getType().inputNames) 
         first = appendSlot(buffer, name, first);
      for (String name : getType().outputNames) 
         if ( !name.equals("when") ) first = appendSlot(buffer, name, first);
      // handle 'when' specially (but not 'success')
      if ( TaskEngine.DEBUG && isDefinedSlot("when") ) {
         long start = engine.getStart();
         long when = getWhen();
         first = appendSlot(buffer, "when", 
               start == 0 ? 
                  DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
                     .format(new Date(when)) : 
                  Long.toString((when-start)/100), // tenth second accuracy enough
               first);
      }
      return buffer.append(" }").toString();
   }

   private boolean appendSlot (StringBuilder buffer, String name, boolean first) {
      return isDefinedSlot(name) ? 
         appendSlot(buffer, name, toString(getSlotValue(name)), first) :
            first;
   }
   
   private boolean appendSlot (StringBuilder buffer, String name, String value, 
         boolean first) {
      if ( !first ) buffer.append(", "); else first = false;
      buffer.append(name).append(':').append(value);
      return first;
   }
   
   /**
    * Return compact human-readable string for this task. 
    */
   public final String format () { return engine.format(this); }
   
   /**
    * Return compact human-readable string for this task.  For overriding
    * by extensions of Task. 
    */
   public String formatTask () { 
      return formatTask(isPrimitive() ? "execute@word" : "achieve@word");
   }
   
   protected String formatTask (String key) { 
      return formatTask(engine.getFormat(this), key);
   }

   protected String formatTask (String format, String key) { 
      TaskClass type = getType();
      List<String> inputs = type.declaredInputNames,
         outputs = type.declaredOutputNames;
      int inputsSize = inputs.size(), outputsSize = outputs.size();
      if ( format != null ) {
         Object[] slots = new Object[inputsSize+outputsSize];
         int i = 0;
         for (String name : inputs) slots[i++] = formatSlot(name);
         for (String name : outputs) slots[i++] = formatSlot(name);
         return String.format(format, slots);
      } 
      // default formatting
      String and = engine.getProperty("and@word");
      StringBuilder buffer = new StringBuilder();
      if ( key != null ) 
         buffer.append(engine.getProperty(key)).append(' ');
      buffer.append(type)
        .append(formatSlots(inputs, inputsSize, "on@word", and))
        .append(formatSlots(outputs, outputsSize, "producing@word", and));
      return buffer.toString();
   }
   
   private String formatSlot (String name) {
      return formatSlot(name, isDefinedSlot(name));
   }
   
   protected String formatSlot (String name, boolean defined) {
      if ( !defined ) return getType().formatSlot(name, false);      
      Object value = getSlotValue(name);
      return value instanceof Task ? ((Task) value).formatTask() :
         clonedInputs == null ? getSlotValueToString(name) :
            engine.toString(getClonedInputsSlotValue(name));
   }
   
   private Object getClonedInputsSlotValue (String name) {
      synchronized (bindings) {
         Object old = bindings.get("$this");
         try {
            bindings.put("$this", clonedInputs);
            return getSlotValue(name);
         } finally { bindings.put("$this", old); } 
      }
   }
   
   private StringBuilder formatSlots (List<String> slots, int size, 
                                     String key, String and) {
      StringBuilder buffer = new StringBuilder();
      boolean firstDefined = false;
      // need to process slots backwards to drop trailing undefineds
      for (int i = size; i-- > 0;) {
         String name = slots.get(i);
         boolean defined = isDefinedSlot(name);
         if ( defined || firstDefined ) {
            firstDefined = true;
            buffer.insert(0, formatSlot(name));
            if ( i > 0 ) 
               buffer.insert(0, ' ').insert(0, and).insert(0, ' '); 
         }
      }
      if ( buffer.length() > 0 )
         buffer.insert(0, ' ').insert(0, engine.getProperty(key)).insert(0, ' ');
      return buffer;
   }
   
   protected String getFormat () {
      String format = getProperty("@format"); 
      // allow empty string to undo format
      return format != null && format.length() > 0 ? format : null;
   }
  
}