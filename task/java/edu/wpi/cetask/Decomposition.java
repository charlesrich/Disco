/* Copyright (c) 2008 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.cetask;

import edu.wpi.cetask.ScriptEngineWrapper.Compiled;
import java.util.*;
import javax.script.Bindings;

/**
 * Representation for instances of a decomposition class.
 */
public class Decomposition extends Instance {
   
   Decomposition (DecompositionClass type, Plan plan) 
                 throws DecompositionClass.Contradiction { 
      super(type, type.engine);
      List<String> stepNames = type.getStepNames();
      Map<String,Repeatable> repeatableSteps = null;
      List<Plan> optionalSteps = null; // for copying bindings
      for (String name : stepNames) {
         TaskClass task = type.getStepType(name);
         boolean optional = type.isOptionalStep(name);
         Plan step = addStep(task, name, optional, false);
         for (String required : type.getRequiredStepNames(name)) {
            Plan requiredStep = getStep(required);
            if ( requiredStep == null ) 
               getErr().println("WARNING: Ignoring undefined required step "+required
                                +" in "+type);
            else step.requires(requiredStep);
            if ( repeatableSteps != null && !requiredStep.isOptionalStep() ) {
               Repeatable repeatable = repeatableSteps.get(required);
               if ( repeatable != null ) { 
                  // make current step successor (only) of all preceding
                  // optional repeatable steps so that AskShouldPassablePlugin works
                  Plan predecessor = repeatable.optional.get(repeatable.optional.size()-1);
                  predecessor.requiredBy.add(step);
               }
            }
         }
         int max = type.getMaxOccursStep(name), 
             min = type.getMinOccursStep(name);
         if ( max < 0 ) {
            // TODO handle unbounded case (will need some sort of lazy mechanism)
            getErr().println("WARNING: Unbounded maxOccurs approximated to 10 in "
                             +type+" step "+name);
            max = 10;
         }
         if ( max > 1 ) {
            if ( repeatableSteps == null ) 
               repeatableSteps = new HashMap<String,Repeatable>(stepNames.size());
            optionalSteps = new ArrayList<Plan>(max-min);
            repeatableSteps.put(name, new Repeatable(step.getGoal(), optionalSteps));
            // TODO handle repeatable steps once in DecompositionClass instead by 
            //      creating additional steps with names with 
            //      appropriate ordering and binding for inputs only 
            //      Could also handle outputs from _last_ step
            for (int i = 1; i < max; i++) { // add optional repeat steps
               Plan requires = step;
               boolean optionalRepeat = ( i >= min );
               // note $ not allowed in XML names, so cannot accidentally
               // collide with user-defined step names
               step = addStep(task, name+'$'+i, optionalRepeat, true);
               step.setRepeatStep(i);
               step.requires(requires); 
               if ( optionalRepeat ) optionalSteps.add(step);
            }       
         }
      }
      plan.setDecomposition(this); // must be before repeatables      
      if ( repeatableSteps != null ) { 
         for (Repeatable repeatable : repeatableSteps.values()) {
            // must be after updateBindings
            for (Plan repeat : repeatable.optional)
               // copy fixed binding values (esp. external)
               // (will cause unnecessary calls to updateBindings)
               repeat.getGoal().copySlotValues(repeatable.step);
         }
      }
   }

   private static class Repeatable {
      final Task step;
      final List<Plan> optional;
      private Repeatable (Task step, List<Plan> optional) {
         this.step = step;
         this.optional = optional;
      }
   }
   
   private Plan addStep (TaskClass type, String name,
                         boolean optional, boolean repeat) {
      Decomposition.Step step;
      try { step = type.isBuiltin() ?
               type.newStep(Decomposition.this, name, repeat) :
               new Step(type, engine, this, name);
      } catch (NoSuchMethodException e) { throw new RuntimeException(e); }
      Plan plan = new Plan(step);
      plan.setOptionalStep(optional);
      putStep(name, plan);
      return plan;
   }

   @Override
   public DecompositionClass getType() { 
      return (DecompositionClass) super.getType(); 
   }
   
   private Task goal;
   public Task getGoal () { return goal; }
   
   void attach (Plan plan) {
      Task goal = plan.getGoal();
      if ( this.goal != null )
         throw new IllegalStateException(
               "Decomposition already attached to "+this.goal);
      if ( goal == null || goal.getType() != getType().getGoal() )
         throw new IllegalArgumentException(
               "Goal of this decomposition must be a "+getType().getGoal());
      this.goal = goal;
      synchronized (bindings) {
         bindings.put("$this", goal.bindings.get("$this"));
         bindings.put("$plan", plan);
         updateBindings(true, null, null, null);
      }
   }

   void retract () {
      if ( goal == null )
         throw new IllegalStateException("Decomposition already retracted");
      for (String name : goal.getType().outputNames)
         goal.deleteSlotValue(name);
      goal = null;
      synchronized (bindings) { 
         bindings.put("$this", null); 
         bindings.put("$plan", null); 
      }
   }
   
   private boolean failed;
   void setFailed (boolean failed) { this.failed = failed; }

   // LinkedHashMap preserves insertion order
   private final Map<String,Plan> steps = new LinkedHashMap<String,Plan>();
   
   private void putStep (String name, Plan step) { 
      steps.put(name, step); 
      synchronized (bindings) {
         bindings.put('$'+name, step.getGoal().bindings.get("$this"));
      }
   }
   
   public Plan getStep (String name) { return steps.get(name); }
   
   public Collection<Plan> getSteps () { return steps.values(); }
   
   /**
    * Return name of step for given plan or null if not a step.
    */
   public String findStep (Plan child) {
      for (Map.Entry<String,Plan> entry : steps.entrySet())
         if ( entry.getValue() == child ) return entry.getKey();
      return null;
   }
   
   boolean stepsModified = true; // to reduce calls to updateBindings
   
   // per-decomposition flag used to prevent recursive calls to updateBindings
   // NOTE: this only works if there is only one thread running!
   private boolean updatingBindings;
   
   void updateBindings (boolean modified, String onlyStep, String retractedStep, String retractedSlot) {
      if ( goal != null && !failed && !updatingBindings && 
            (modified || stepsModified || goal.isModified()) ) 
         try {
            updatingBindings = true;
            getType().updateBindings(this, onlyStep, retractedStep, retractedSlot);
            stepsModified = false;
            goal.setModified(false);
         } finally { updatingBindings = false; }
   }
   
   public static class Step extends Task {
      
      private final String name;

      // note explicit field rather than nested time to support null value
      private final Decomposition decomp;
      
      public String getName () { return name; }
      
      public Decomposition getDecomposition () { return decomp; }

      protected Step (TaskClass type, TaskEngine engine, Decomposition decomp, 
                      String name) { 
         super(type, engine);
         this.decomp = decomp;
         this.name = name;
      }
      
      public boolean isOptionalStep () {
         return decomp != null && decomp.getType().isOptionalStep(name);
      }
      
      private void updateBindings (boolean modified) {
         if ( decomp != null ) decomp.updateBindings(modified, null, null, null);
      }
      
      @Override
      void updateBindings () {
         super.updateBindings(); // do 2018-ext self bindings first
         // for binding expressions on this step that do not depend on other slots
         if ( decomp != null ) decomp.updateBindings(true, name, null, null);
      }
   
      @Override
      public Object eval (String expression, String where) {
         updateBindings(false);
         return super.eval(expression, where);
      }
      
      @Override
      protected Object eval (String script, Bindings extra, String where) {
         updateBindings(false);
         return super.eval(script, extra, where);
      }
  
      @Override
      protected Boolean evalCondition (Compiled compiled, Bindings extra, 
                                       String where) {
         updateBindings(false);
         return super.evalCondition(compiled, extra, where);
      }
      
      // need these also since SCRIPTABLE optimization does not call eval 
    
      @Override
      public Object getSlotValue (String name) {
         updateBindings(false);
         return super.getSlotValue(name);
      }
      
      @Override
      protected Boolean getSlotValueBoolean (String name) {
         updateBindings(false);
         return super.getSlotValueBoolean(name);
      }
      
      @Override
      public String getSlotValueToString (String name) {
         updateBindings(false);
         return super.getSlotValueToString(name);
      }
      
      @Override
      public boolean isDefinedSlot (String name) {
         updateBindings(false);
         return super.isDefinedSlot(name);
      }
      
      @Override
      boolean copySlotValue (Task from, String fromSlot, String thisSlot, 
                             boolean onlyDefined, boolean check) {
         if ( from instanceof Step ) ((Step) from).updateBindings(false);
         boolean overwrite = super.copySlotValue(from, fromSlot, thisSlot, 
                                                 onlyDefined, check);
         updateBindings(true);
         return overwrite;
      }
      
      // only setting slot values sets modified bit; evaluation of conditions, etc.
      // is specified not to have side effects

      @Override
      public Object setSlotValue (String name, Object value) {
         super.setSlotValue(name, value);
         updateBindings(true);
         return value;
      }
      
      @Override
      public void setSlotValueScript (String name, String expression, String where) { 
         super.setSlotValueScript(name, expression, where);
         updateBindings(true);
      }
      
      @Override
      void setSlotValueScript (String name, String expression, String where,
            Bindings extra) {
         super.setSlotValueScript(name, expression, where, extra);
         updateBindings(true);
      }
      
      @Override
      void setSlotValueScript (String name, Compiled compiled, String where,
            Bindings extra) {
         super.setSlotValueScript(name, compiled, where, extra);
         updateBindings(true);
      }
      
      @Override
      public void deleteSlotValue (String name) {
         super.deleteSlotValue(name);
         if ( decomp != null ) decomp.updateBindings(true, null, this.name, name);
      }
   }
}

