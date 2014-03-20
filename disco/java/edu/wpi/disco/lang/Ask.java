/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.lang;

import java.util.List;
import edu.wpi.cetask.*;
import edu.wpi.disco.Disco;

/**
 * Builtin semantics for utterances in Disco.xml
 * 
 * Ask = Propose.Should(Propose...)
 */
public abstract class Ask extends Propose.Should {
   
   // for extensions
   protected Ask (Class<? extends Ask> cls, Disco disco,
                  Decomposition decomp, String step, boolean repeat) {
      super(cls, disco, decomp, step, repeat);
   }
   
   // for extensions
   protected Ask (Class<? extends Ask> cls, Disco disco, Boolean external, Task goal) {
      super(cls, disco, external, goal);
      checkCircular("goal", goal);
      if ( goal != null && !(goal instanceof Propose) ) 
         throw new IllegalArgumentException("Goal is not instance of Propose: "+goal); 
      if ( goal != null ) {
         Boolean goalExternal = goal.getExternal();
         if ( goalExternal != null ) {
            if ( external == null ) setExternal(Utils.not(goalExternal));
            else if ( external != Utils.not(goalExternal))
               throw new IllegalArgumentException(
                     "External slot of Ask must be opposite of: "+goalExternal);
         }
      }
      this.goal = goal;
   }
   
   // no inheritance in CETask, so need to store this locally
   private Task goal;
   protected boolean cache;
   
   @Override
   public Task getGoal () {
      if ( goal != null ) return goal;
      Task newGoal = newGoal();
      if ( cache ) goal = newGoal;
      return newGoal;
   }
   
   abstract protected Task newGoal ();
     
   /**
    * Ask.Global
    */
   public static class Global extends Ask {
      
      public static TaskClass CLASS;
      
      // for TaskClass.newStep
      public Global (Disco disco, Decomposition decomp, String step, boolean repeat) { 
         super(Global.class, disco, decomp, step, repeat);
      }
         
      public Global (Disco disco, Boolean external, Propose.Global task) {
         super(Global.class, disco, external, task);
         if ( task != null ) {
            String variable = task.getVariable();
            if ( variable != null ) setSlotValue("variable", variable);
            String type = task.getVariableType();
            if ( type != null ) setSlotValue("type", type);
         }
      }
      
      public Global (Disco disco, Boolean external, String variable, String type) {
         this(disco, external, null);
         setSlotValue("variable", variable);
         setSlotValue("type", type);
         cache = true;
      }
 
      @Override
      public Propose.Global getGoal () { return (Propose.Global) super.getGoal(); }
      
      @Override
      protected Propose.Global newGoal () {
         return new Propose.Global(getDisco(), Utils.not(getExternal()), 
                                   getVariable(), getVariableType(), null);
      }
      
      public String getVariable () { return (String) getSlotValue("variable"); }
      public String getVariableType () { return (String) getSlotValue("type"); }
   }
   
   /**
    * Base class for Ask with nested task.
    */
   public abstract static class Nested extends Ask {
      
      // for extensions
      protected Nested (Class<? extends Ask.Nested> cls, Disco disco,
                        Decomposition decomp, String step, boolean repeat) {
         super(cls, disco, decomp, step, repeat);
      }
      
      // for extension
      protected Nested (Class<? extends Ask.Nested> cls, Disco disco,
                        Boolean external, Propose.Nested goal) {
         super(cls, disco, external, goal);
         // replace goal slot with nested task
         if ( goal != null ) setSlotValue("goal", goal.getGoal());
      }
      
      public Task getNestedGoal () { return (Task) getSlotValue("goal"); }
      
      @Override  // skip nested Propose.Should and Propose
      protected Task getPropertyNested () { return getNestedGoal(); }
   }
   
   /**
    * Ask.Should = Propose.Should(Propose.Should(NestedGoal...)) 
    * - utterance to ask about optional step
    */
   public static class Should extends Ask.Nested {
      
      public static TaskClass CLASS;
       
      // for TaskClass.newStep
      public Should (Disco disco, Decomposition decomp, String step, boolean repeat) { 
         super(Should.class, disco, decomp, step, repeat);
      }
      
      // for extensions
      protected Should (Class<? extends Ask.Should> cls, Disco disco,
                        Decomposition decomp, String step, boolean repeat) { 
         super(cls, disco, decomp, step, repeat);
      }

      // for extension
      protected Should (Class<? extends Ask.Should> cls, Disco disco,
                        Boolean external, Propose.Nested goal) {
         super(cls, disco, external, goal);
      }
      
      public Should (Disco disco, Boolean external, Propose.Should task) {
         super(Should.class, disco, external, task);
         if ( task != null ) {
            Task nested = task.getGoal();
            if ( nested != null ) setSlotValue("goal", nested);
         }
      }
      
      public Should (Disco disco, Boolean external, Task nested) {
         this(disco, external, null);
         setSlotValue("goal", nested);
         cache = true;
      }
   
      @Override
      public Propose.Should getGoal () { return (Propose.Should) super.getGoal(); }
      
      @Override
      protected Propose.Should newGoal () {
         return Propose.Should.newInstance(getDisco(), Utils.not(getExternal()), 
                                           getNestedGoal());
      }

      @Override
      public String formatTask () {
         String format = getDisco().getFormat(this);
         if ( format != null) return formatTask(format, null);
         Boolean who = getExternal();
         Task goal = getNestedGoal();
         StringBuilder buffer = new StringBuilder();
         if ( goal != null && goal.isPrimitive() ) {
            Boolean taskWho = goal.getExternal();
            if ( who != null ) {
               if ( who == taskWho ) {
                  appendKeySp(buffer, "should@word"); appendKeySp(buffer, "i@word");
               } else if ( taskWho == null ) {
                  appendKeySp(buffer, "should@word");
                  appendKeySp(buffer, "someone@word"); 
               } else {  // other
                  appendKeySp(buffer, "are@word"); appendKeySp(buffer, "you@word");
                  appendKeySp(buffer, "goingto@word");
               }
            } else { // unknown speaker
               appendKeySp(buffer, "should@word");
               appendSp(buffer, getDisco().getWho(goal));  
            }
         } else { appendKeySp(buffer, "should@word"); appendKeySp(buffer, "we@word"); }
         if ( goal == null ) {
            appendKeySp(buffer, "do@word"); appendSpKey(buffer, "something@word");
         }
         else buffer.append(goal.formatTask());
         return buffer.append('?').toString();
      }   
           
      /**
       * Ask.Should.Repeat - version of Ask.Should for second and subsequent
       *                     instances of repeated step
       */
      public static class Repeat extends Ask.Should {
         
         public static TaskClass CLASS;
         
         // for TaskClass.newStep
         public Repeat (Disco disco, Decomposition decomp, String step, boolean repeat) { 
            super(Repeat.class, disco, decomp, step, repeat);
         }
         
         public Repeat (Disco disco, Boolean external, Propose.Should.Repeat task) {
            super(Repeat.class, disco, external, task);
         }

         public Repeat (Disco disco, Boolean external, Task nested) {
            this(disco, external, 
                  nested == null ? null : 
                     new Propose.Should.Repeat(disco, Utils.not(external), nested));
         }
         
         @Override
         public String formatTask () {
             String format = getDisco().getFormat(this);
             if ( format != null) return formatTask(format, null);
             format = super.formatTask();
             StringBuilder buffer = new StringBuilder(format);
             boolean question = format.endsWith("?");
             if ( question ) buffer.delete(buffer.length()-1, buffer.length());
             appendSpKey(buffer, "again@word").toString();
             if ( question ) buffer.append('?');
             return buffer.toString();
         }
      }
   }
      
   // TODO   Ask.Stop
   
   /**
    * Ask.What = Propose.Should(Propose.What(NestedGoal...)) 
    * - utterance to ask for slot value 
    */
   public static class What extends Ask.Nested {      
      
      public static TaskClass CLASS;
      
      // for TaskClass.newStep
      public What (Disco disco, Decomposition decomp, String step, boolean repeat) { 
         super(What.class, disco, decomp, step, repeat);
      }
         
      public What (Disco disco, Boolean external, Propose.What task) {
         super(What.class, disco, external, task);
         if ( task != null ) {
            String slot = task.getSlot();
            if ( slot != null ) setSlotValue("slot", slot);
            Task goal = task.getGoal();
            if ( goal != null ) setSlotValue("goal", goal);
         }
      }
      
      public What (Disco disco, Boolean external, Task nested, String slot) {
         this(disco, external, null);
         setSlotValue("goal", nested);
         setSlotValue("slot", slot);
         cache = true;
      }
 
      @Override
      public Propose.What getGoal () { return (Propose.What) super.getGoal(); }
      
      @Override
      protected Propose.What newGoal () {
         return new Propose.What(getDisco(), Utils.not(getExternal()),
                                 getNestedGoal(), getSlot(), null);
      }
      
      public String getSlot () { return (String) getSlotValue("slot"); }
      
      @Override
      public String formatTask () {
         String format = getDisco().getFormat(this);
         if ( format != null) return formatTask(format, null);
         StringBuilder buffer = new StringBuilder();
         appendKeySp(buffer, "what@word");
         Task goal = getNestedGoal();
         String slot = getSlot();
         if ( goal != null && slot != null )
            buffer.append(goal.getType().formatSlot(slot, true));
         else appendKey(buffer, "avalue@word");
         return buffer.append('?').toString();
      }
      
      @Override
      public String getPropertySlot () { return getSlot(); }
      
   }
   
    /**
    * Ask.Which = Propose.Should(Propose.Which(NestedGoal...)) 
    * - utterance to provide choices for value of slot
    * 
    *  TODO Nuance working on
    */
   public static class Which extends Ask.Nested {      
      
      public static TaskClass CLASS;
      
      // for TaskClass.newStep
      public Which (Disco disco, Decomposition decomp, String step, boolean repeat) { 
         super(Which.class, disco, decomp, step, repeat);
      }
            
      public Which (Disco disco, Boolean external, Propose.Which task) {
         super(Which.class, disco, external, task);
         // TODO
      }
      
      @Override
      protected Task newGoal () {
         // TODO Auto-generated method stub
         return null;
      }
   }
   
   /**
    * Ask.Who = Propose.Should(Propose.Who(NestedGoal...)) 
    * - utterance to ask who should perform primitive task
    */
   public static class Who extends Ask.Nested {      
      
      public static TaskClass CLASS;
      
      // for TaskClass.newStep
      public Who (Disco disco, Decomposition decomp, String step, boolean repeat) { 
         super(Who.class, disco, decomp, step, repeat);
      }
         
      public Who (Disco disco, Boolean external, Propose.Who task) {
         super(Who.class, disco, external, task);
      }
      
      public Who (Disco disco, Boolean external, Task nested) {
         this(disco, external, null);
         if ( !nested.isPrimitive() ) 
            throw new IllegalArgumentException(
                  "Ask.Who for non-primitive task"+nested);
         setSlotValue("goal", nested);
         cache = true;
      }
 
      @Override
      public Propose.Who getGoal () { return (Propose.Who) super.getGoal(); }
      
      @Override
      protected Propose.Who newGoal () { 
         return new Propose.Who(getDisco(), Utils.not(getExternal()), 
                                getNestedGoal(), null);
      }
      
      @Override
      public String formatTask () {
         String format = getDisco().getFormat(this);
         if ( format != null) return formatTask(format, null);
         StringBuilder buffer = new StringBuilder();
         appendKeySp(buffer, "who@word");
         appendKey(buffer, "should@word");
         Task goal = getNestedGoal();
         if ( goal != null ) {
            buffer.append(' ');
            buffer.append(goal.formatTask());
         }
         return buffer.append('?').toString();
      }
   }
   
   /**
    * Ask.How = Propose.Should(Propose.How(NestedGoal...)) 
    * - utterance to ask for decomposition choice
    */
   public static class How extends Ask.Nested {      
      
      public static TaskClass CLASS;
      
      // for TaskClass.newStep
      public How (Disco disco, Decomposition decomp, String step, boolean repeat) { 
         super(How.class, disco, decomp, step, repeat);
      }
         
      public How (Disco disco, Boolean external, Propose.How task) {
         super(How.class, disco, external, task);
         if ( task != null ) { 
            Plan plan = task.getPlan();
            if ( plan != null ) setSlotValue("goal", plan.getGoal());
         }
      }
      
      public How (Disco disco, Boolean external, Task nested) {
         this(disco, external, null);
         setSlotValue("goal", nested);
         cache = true;
      }
      
      // extra constructor
      public How (Disco disco, Boolean external, Plan plan, DecompositionClass decomp) {
         this(disco, external, 
               plan == null ? null : 
                  new Propose.How(disco, Utils.not(external), plan, decomp));
      }
      
      @Override
      public Propose.How getGoal () { return (Propose.How) super.getGoal(); }
      
      @Override
      protected Propose.How newGoal () {
         // TODO: plans is null since since Ask.How does not include plan
         //       need to search for plan based on getNestedGoal()
         return new Propose.How(getDisco(), Utils.not(getExternal()), null, null);
      }
      
      @Override
      public String formatTask () {
         Task goal = getNestedGoal();
         Propose.How nested = (Propose.How) getNested();
         DecompositionClass decomp = null;
         if ( nested != null ) decomp = nested.getDecomp(); 
         String format = decomp == null ? getDisco().getFormat(this) : 
            getFormat(getClass(), goal, decomp);
         if ( format != null) return formatTask(format, null);
         StringBuilder buffer = new StringBuilder();
         if ( decomp == null ) appendKeySp(buffer, "how@word");
         appendKeySp(buffer, "shallwe@word");
         if ( goal == null ) { 
            appendKeySp(buffer, "do@word"); appendKeySp(buffer, "something@word");
         } else buffer.append(goal.formatTask());
         if ( decomp != null ) buffer.append(' ').append(decomp.format());
         return buffer.append('?').toString();
      }
   }
}

