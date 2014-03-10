/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.lang;

import edu.wpi.cetask.*;
import edu.wpi.disco.Disco;

/**
 * Builtin semantics for proposals in Disco.xml
 */
public interface Propose {
   
   /**
    * @param contributes may be null
    */
   void accept (Plan contributes, boolean implicit);
   
   /**
    * @param contributes may be null
    */
   void reject (Plan contributes, boolean implicit);
 
   // this class here just to hold interpret method
   abstract static class Interpret {

      private static boolean interpret (Utterance utterance, Plan contributes,
                                        boolean continuation) {
         if ( Utterance.interpret(utterance, contributes, continuation) ) 
            // matches to same Propose (e.g., from Ask)
            contributes = contributes.getParent();
         else if ( contributes != null ) 
            utterance.reconcileStack(contributes, continuation);
         utterance.getDisco().getSegment().add(utterance);
         interpretPropose(utterance, contributes);
         return contributes != null;
      }
      
      private static void interpretPropose (Utterance utterance, Plan contributes) {
         Propose propose = (Propose) utterance;
         Disco disco = utterance.getDisco();
         if ( utterance.isUser() ) { 
            // TODO fix asymmetry: eventually there needs to be some systematic
            //      way for agent not to automatically accept user utterances
            propose.accept(contributes, false);
            // accept can change liveness and achieved
            disco.clearLiveAchieved();
         } else disco.push(new Plan(
               new Accept(disco, !utterance.getExternal(), propose), 
               contributes));
      } 
   
      private Interpret () {}
   }
   
   /**
    * Utterance for proposing the value of a global JavaScript variable
    */
   public static class Global extends Utterance implements Propose {

      public static TaskClass CLASS;
      
      // for TaskClass.newStep
      public Global (Disco disco, Decomposition decomp, String step, boolean repeat) { 
         super(Global.class, disco, decomp, step, repeat);
      }

      public Global (Disco disco, Boolean external, 
                     String variable, String type, Object value) { 
         super(Global.class, disco, external);
         if ( variable != null ) setSlotValue("variable", variable);
         if ( type != null ) setSlotValue("type", type);
         if ( value != null ) setSlotValue("value", value);
      }
      
      public String getVariable () { return (String) getSlotValue("variable"); }
      public String getVariableType () { return (String) getSlotValue("type"); }
      public Object getValue () { return getSlotValue("value"); }
      
      @Override
      public boolean interpret (Plan contributes, boolean continuation) { 
         return Interpret.interpret(this, contributes, continuation); 
      }
      
      @Override
      public void accept (Plan contributes, boolean implicit) {
         if ( isDefinedSlot("variable") && isDefinedSlot("value") )
            getDisco().setGlobal(getVariable(), getValue());
      }
      
      @Override
      public void reject (Plan contributes, boolean implicit) {
         if ( isDefinedSlot("variable") && isDefinedSlot("value") ) {
            String variable = getVariable();
            if ( getDisco().isDefinedGlobal(variable) &&
                  !getDisco().getGlobal(variable).equals(getValue()) )
               getDisco().deleteGlobal(variable);
         }
      }
      
      @Override
      public String getProperty(String key) {
         String variable = getVariable();
         String type = getVariableType();
         Object value = getValue();
         StringBuilder buffer = new StringBuilder(getType().getPropertyId());
         buffer.append('(').append(variable).append(',').append(type);
         int length = buffer.length();
         buffer.append(',').append(toPropertyString(value)).append(')').append(key);
         // first look for Propose.Global(variable,type,value)key
         String format = getType().getModel().getProperty(buffer.toString());
         if (format != null && format.length() > 0) return format;
         // otherwise look for Propose.Global(variable,type)key
         buffer.delete(length, buffer.length());  
         buffer.append(')').append(key);
         format = getType().getModel().getProperty(buffer.toString());
         if (format != null && format.length() > 0) return format;
         // otherwise look for default Propose.Globalkey
         return super.getProperty(key);      
      }

      // TODO better default formatTask and toHistoryString for Propose.Global
   }
   
   /**
    * Base class for proposals that have a nested task argument.
    */
   abstract static class Nested extends edu.wpi.disco.lang.Nested 
                                implements Propose {
      
      private final Task goal;
   
      // for extensions
      protected Nested (Class<? extends Nested> cls, Disco disco,
                      Decomposition decomp, String step, boolean repeat) { 
         super(cls, disco, decomp, step, repeat);
         goal = null;
      }

      // for extensions
      protected Nested (Class<? extends Nested> cls, Disco disco, 
                      Boolean external, Task goal) { 
         super(cls, disco, external);
         this.goal = goal;
         if ( goal != null && getType().isSlot("goal") ) 
            setSlotValue("goal", goal);
      }
      
      public Task getGoal () { 
         return getType().isSlot("goal") ? (Task) getSlotValue("goal") : goal; }
      
      @Override
      protected Task getNested () { return getGoal(); }

      /*
       * Design Note: Getting the "real" nested task
       * 
       * Depending on how the proposal is created, the task instance in the
       * goal slot may only unify with the "target" task instance (for example,
       * this would be the case when the proposal is created by the semantic
       * component of a NL front-end).   In that case, the task instance
       * whose slots needs to be set, etc., will be the instance to which
       * proposal contributes.
       */
      
      protected Task getNested (Plan contributes) {
         Task nested = getNested();
         return ( nested != null && contributes != null 
                  && nested.matches(contributes.getGoal()) ) ? contributes.getGoal() 
                  : nested;
      }

      @Override
      public boolean interpret (Plan contributes, boolean continuation) { 
         return Interpret.interpret(this, contributes, continuation); 
      }
      
      @Override
      public void accept (Plan contributes, boolean implicit) {
         respond(contributes, implicit, true);
      }
      
      @Override
      public void reject (Plan contributes, boolean implicit) {
         // note we are taking "strong" interpretation of rejection here, i.e.,
         // not just that proposition is not true, but that it is false
         respond(contributes, implicit, false);
      }

      protected void respond (Plan contributes, boolean implicit, boolean truth) {}
      
      @Override
      protected String getKey () { return "propose@word"; }

      protected String formatProposeShould (boolean not) {
         String format = getDisco().getFormat(this);
         if ( format != null) return formatTask(format, null);
         Boolean who = getExternal();
         Task should = getGoal();
         StringBuilder buffer = new StringBuilder();
         if ( should != null && should.isPrimitive() ) {
            Boolean shouldWho = should.getExternal();
            if ( who != null ) {
               if ( who == shouldWho ) {
                  appendKeySp(buffer, "im@word"); 
                  if ( not ) appendKeySp(buffer, "not@word");
                  appendKeySp(buffer, "goingto@word");
               } else if ( shouldWho == null ) {
                  if ( not) {
                     appendKeySp(buffer, "lets@word"); appendKeySp(buffer, "not@word");
                  } else {
                     appendKeySp(buffer, "someone@word"); 
                     appendKeySp(buffer, "should@word");
                  }
               } else {
                  appendKeySp(buffer, "please@word"); // other
                  if ( not ) { 
                     appendKeySp(buffer, "do@word"); appendKeySp(buffer, "not@word"); 
                  }
               }
            } else { // unknown speaker
               appendSp(buffer, getDisco().getWho(should));  
               appendKeySp(buffer, "should@word");
               if ( not ) appendKeySp(buffer, "not@word");
            }
         } else {
            appendKeySp(buffer, "lets@word");
            if ( not ) appendKeySp(buffer, "not@word");
         }
         if ( should == null ) {
            appendKeySp(buffer, "do@word");
            appendKey(buffer, "something@word");
         } else buffer.append(should.formatTask());
         if ( this instanceof Propose.Should.Repeat ) appendSpKey(buffer, "again@word");
         return buffer.toString();
      }
   }

   /**
    * Propose.Should(Task...) - utterance to initiate execution of specified task.
    * 
    * @see Ask
    */
   public static class Should extends Nested {
      
      public static TaskClass CLASS;
      
      // for extensions
      protected Should (Class<? extends Should> cls, Disco disco,
                        Decomposition decomp, String step, boolean repeat) { 
         super(cls, disco, decomp, step, repeat);
      }

      // for extensions
      protected Should (Class<? extends Should> cls, Disco disco,
                        Boolean external, Task goal) {
         super(cls, disco, external, goal);
      }
      
      // for TaskClass.newStep
      public Should (Disco disco, Decomposition decomp, String step, boolean repeat) { 
         super(Should.class, disco, decomp, step, repeat);
      }

      private Should (Disco disco, Boolean external, Task goal) { 
         super(Should.class, disco, external, goal);
      }
      
      public static Propose.Should newInstance (Disco disco, 
                                                Boolean external, Task goal) {
         // terminological classification
         return goal instanceof Propose.What ? 
            new Ask.What(disco, external, (Propose.What) goal) : 
               goal instanceof Propose.Which ?
                  new Ask.Which(disco, external, (Propose.Which) goal) :
                     goal instanceof Propose.Should.Repeat ?
                        new Ask.Should.Repeat(disco, external, (Propose.Should.Repeat) goal) :
                           goal instanceof Propose.Should ? 
                              new Ask.Should(disco, external, (Propose.Should) goal) :
                                 goal instanceof Propose.How ?
                                    new Ask.How(disco, external, (Propose.How) goal) :
                                       goal instanceof Propose.Global ?
                                          new Ask.Global(disco, external, (Propose.Global) goal) :
                                             goal instanceof Propose.Who ?
                                                new Ask.Who(disco, external, (Propose.Who) goal) :
                                                   new Propose.Should(disco, external, goal); 
      }
      
      @Override
      public boolean contributes (Plan plan) {
         return super.contributes(plan) && !plan.isExhausted();
      }

      @Override
      public boolean interpret (Plan contributes, boolean continuation) {
         boolean added = false;
         if ( Utterance.interpret(this, contributes, continuation) ) {
            // matches to same Propose (e.g., from Ask)
            getDisco().getSegment().add(this); added = true;
            contributes = contributes.getParent();
         } else if ( contributes != null ) 
            reconcileStack(contributes, continuation);
         Task should = getNested();
         if ( contributes != null ) {
            reconcileStack(contributes, continuation);
            if ( contributes.getType() == should.getType() )
               // TODO this match should really be moved to accept method below, but that
               //      would require modeling of agent's private beliefs
               contributes.match(should);
            else contributes = new Plan(should, contributes);	
         } else {
            Plan focus = getDisco().getFocusExhausted(true);
            contributes = new Plan(should,
                  focus != null && !focus.isPrimitive() && focus.getType().getDecompositions().isEmpty() ?
                     // e.g., Demonstration
                     focus : 
                     // interruption or new toplevel plan
                     null); 
         }
         getDisco().push(contributes);
         Interpret.interpretPropose(this, contributes);
         if ( !added ) getDisco().getSegment().add(this);
         // it would look a little odd for Propose.Should to be unexplained
         return true;
      }
      
      @Override
      protected void respond (Plan contributes, boolean implicit, boolean should) {
         if ( !implicit ) getNested(contributes).setShould(should);
      } 

      // TODO Classify to use appropriate formatTask from Ask as in newInstance above
      
      @Override
      public String formatTask () { return formatProposeShould(false); }
      
      public static class Repeat extends Propose.Should {
         
         public static TaskClass CLASS;
         
         // for TaskClass.newStep
         public Repeat (Disco disco, Decomposition decomp, String step, boolean repeat) { 
            super(Repeat.class, disco, decomp, step, repeat);
         }

         public Repeat (Disco disco, Boolean external, Task goal) {
            super(Repeat.class, disco, external, goal); 
         }
      }
   }
   
   /**
    * Propose.ShouldNot(Task...) - utterance to reject unstarted task.
    * 
    * @see Ask
    * @see Propose.Stop
    */
   public static class ShouldNot extends Nested {
      
      public static TaskClass CLASS;
      
      protected ShouldNot (Class<? extends ShouldNot> cls, Disco disco,
                        Decomposition decomp, String step, boolean repeat) { 
         super(cls, disco, decomp, step, repeat);
      }

      // for extensions
      protected ShouldNot (Class<? extends ShouldNot> cls, Disco disco,
                        Boolean external, Task goal) {
         super(cls, disco, external, goal);
      }
      
      // for TaskClass.newStep
      public ShouldNot (Disco disco, Decomposition decomp, String step, boolean repeat) { 
         super(ShouldNot.class, disco, decomp, step, repeat);
      }

      public ShouldNot (Disco disco, Boolean external, Task goal) { 
         super(ShouldNot.class, disco, external, goal);
      }
      
      @Override
      protected void respond (Plan contributes, boolean implicit, boolean should) {
         if ( !implicit ) getNested(contributes).setShould(!should);
      }
      
      @Override
      public String formatTask () { return formatProposeShould(true); }
   }
   
   /**
    * Propose.Stop(Task...) - utterance to stop execution of started task.
    * 
    * @see Propose.ShouldNot
    */
   public static class Stop extends ShouldNot {
  
      public static TaskClass CLASS;
      
      // for TaskClass.newStep
      public Stop (Disco disco, Decomposition decomp, String step, boolean repeat) { 
         super(Stop.class, disco, decomp, step, repeat);
      }

      public Stop (Disco disco, Boolean external, Task goal) { 
         super(Stop.class, disco, external, goal);
      }
      
      @Override
      protected String getKey () { return "stop@word"; }
      
      @Override 
      public String formatTask () { return formatNested(); }
   }
   
   /**
    * Propose.What(Task...) - utterance to provide value for slot.
    * 
    * @see Ask.What
    */
   public static class What extends Nested {
    
      public static TaskClass CLASS;
      
      // for TaskClass.newStep
      public What (Disco disco, Decomposition decomp, String step, boolean repeat) { 
         super(What.class, disco, decomp, step, repeat);
      }

      public What (Disco disco, Boolean external, Task goal, 
                  String slot, Object value) { 
         super(What.class, disco, external, goal);
         if ( slot != null ) setSlotValue("slot", slot);
         if ( value != null ) setSlotValue("value", value);
      }
      
      public String getSlot () { return (String) getSlotValue("slot"); }
      public Object getValue () { return getSlotValue("value"); }
      
      @Override
      public void accept (Plan contributes, boolean implicit) {
         if ( isDefinedSlot("goal") && isDefinedSlot("slot") && isDefinedSlot("value") ) 
            // note setting slot of goal that proposal contributes to
            getNested(contributes).setSlotValue(getSlot(), getValue());
      }
      
      @Override
      public void reject (Plan contributes, boolean implicit) {
         // see RejectProposeWhatPlugin
         if ( isDefinedSlot("goal") ) {
            Task nested = getNested(contributes);
            if ( isDefinedSlot("slot") ) {
               String slot = getSlot();
               // note there is currently no way to remember rejection of a value
               if ( isDefinedSlot("value") ) {
                  if ( nested.isDefinedSlot(slot) && nested.getSlotValue(slot).equals(getValue()) )
                     nested.deleteSlotValue(slot);
               } else setOptional(nested, slot); // set given undefined optional slot to null
            } else if ( !isDefinedSlot("value") ) {
               // set all undefined optional slots to null 
               for (String input : nested.getType().getDeclaredInputNames()) 
                  setOptional(nested, input);
            }
         }
      }

      private void setOptional (Task goal, String slot) {
         if ( goal.getType().getSlot(slot).isOptional() && !goal.isDefinedSlot(slot) ) 
            goal.setSlotValue(slot, null);
      }
      
      @Override
      protected String formatSlot (String name, boolean defined) {
         Task nested = getNested();
         String slot = getSlot();
         return defined || nested == null || slot == null || !name.equals("value") ? 
               super.formatSlot(name, defined) :
               // special case to infer type from slot 
               nested.getType().formatSlot(slot, false);
      }
      
      @Override
      public String formatTask () {
         String format = getDisco().getFormat(this);
         if ( format != null) return formatTask(format, null);
         Task goal = getNested();
         String slot = getSlot();
         Object value = getValue();
         StringBuilder buffer = new StringBuilder();
         if ( goal != null && slot != null && value != null ) {
            appendSp(buffer, goal.getType().formatSlot(slot, true));
            appendKeySp(buffer, "is@word").append(getSlotValueToString("value"));
         } else {
            appendKeySp(buffer,"propose@word");
            if ( goal != null && slot != null )
               buffer.append(goal.getType().formatSlot(slot, true));
            else appendKey(buffer, "avalue@word");
         }
         return buffer.toString();
      }
      
      @Override
      public String getPropertySlot () { return getSlot(); }
   }  
     
   /**
    * Propose.Which(Task...) - utterance to provide choices for value of slot.
    * 
    * TODO Nuance working on
    * 
    * @see Ask.Which
    */
   public static class Which extends Nested {  // extends What?
    
      public static TaskClass CLASS;
      
      // for TaskClass.newStep
      public Which (Disco disco, Decomposition decomp, String step, boolean repeat) { 
         super(Which.class, disco, decomp, step, repeat);
      }
   } 
   
   /**
    * Propose.Who(Task) - utterance to propose whether agent or system
    *                        should execute this primitive task
    * 
    * @see Ask.Who
    */
   public static class Who extends Nested {

      public static TaskClass CLASS;
      
      // for TaskClass.newStep
      public Who (Disco disco, Decomposition decomp, String step, boolean repeat) { 
         super(Who.class, disco, decomp, step, repeat);
      }

      public Who (Disco disco, Boolean external, Task goal, Boolean value) { 
         super(Who.class, disco, external, goal);
         if ( goal == null || !goal.isPrimitive() ) 
            throw new IllegalArgumentException(
                  "Propose.Who for unknown or non-primitive task "+goal);
         if ( value != null ) setSlotValue("value", value);
      }
      
      public Boolean getValue () { return (Boolean) getSlotValue("value"); }
      
      @Override
      public void accept (Plan contributes, boolean implicit) {
         if ( isDefinedSlot("goal") && isDefinedSlot("value") ) {
            Task nested = getNested(contributes);
            Boolean value = getValue();
            nested.setExternal(value);
            nested.setShould(true); // override @authorized = false
         }
      }
      
      @Override
      public void reject (Plan contributes, boolean implicit) {       
         if ( isDefinedSlot("goal") && isDefinedSlot("value") ) 
            getNested(contributes).setExternal(!getValue()); // somebody must do it
      }
      
      @Override
      public String formatTask () {
         String format = getDisco().getFormat(this);
         if ( format != null) return formatTask(format, null);
         Task nested = getNested();
         Boolean value = getValue();
         StringBuilder buffer = new StringBuilder();
         if ( nested != null && value != null ) {
            buffer.append(getWho(getExternal(), value));
         } else {
            appendKeySp(buffer, "propose@word");
            appendKey(buffer, "who@word");
         }
         if ( nested != null ) {
            buffer.append(' ');
            appendKeySp(buffer, "should@word").append(nested.formatTask());
         }
         return buffer.toString();
      }
   }  

   public abstract static class Success extends Nested {

      // for extensions
      protected Success (Class<? extends Success> cls, Disco disco,
                      Decomposition decomp, String step, boolean repeat) { 
         super(cls, disco, decomp, step, repeat);
      }

      // for extensions
      protected Success (Class<? extends Success> cls, Disco disco, Boolean external, Task goal) { 
         super(cls, disco, external, goal);
      }
      
      protected void respondSuccess (Plan contributes, boolean implicit, boolean success) {
         getNested(contributes).setSuccess(success);
         if ( !success && contributes != null )
            contributes.getGoal().setShould(false);  // no further negotiation
      }

      protected String formatSuccess (String success) {
         String format = getDisco().getFormat(this);
         if ( format != null) return formatTask(format, null);
         Task nested = getNested();
         StringBuilder buffer = new StringBuilder();
         if ( nested != null ) appendSp(buffer, nested.formatTask());
         else appendKeySp(buffer, "something@word");
         appendKey(buffer, success);
         return buffer.toString();
      }
   }
   
   /**
    * Propose.Succeeded(Task...) - declare task to have been successful
    */
   public static class Succeeded extends Success {

      public static TaskClass CLASS;
      
      // for TaskClass.newStep
      public Succeeded (Disco disco, Decomposition decomp, String step, boolean repeat) { 
         super(Succeeded.class, disco, decomp, step, repeat);
      }

      public Succeeded (Disco disco, Boolean external, Task goal) { 
         super(Succeeded.class, disco, external, goal);
      }

      @Override
      protected void respond (Plan contributes, boolean implicit, boolean success) {
         respondSuccess(contributes, implicit, success);
      }
      
      @Override
      public String formatTask () { return formatSuccess("succeeded@word"); }
   }  

   /**
    * Propose.Failed(Task...) - declare task to have failed
    */
   public static class Failed extends Success {

      public static TaskClass CLASS;
      
      // for TaskClass.newStep
      public Failed (Disco disco, Decomposition decomp, String step, boolean repeat) { 
         super(Failed.class, disco, decomp, step, repeat);
      }

      public Failed (Disco disco, Boolean external, Task goal) { 
         super(Failed.class, disco, external, goal);
      }

      @Override
      protected void respond (Plan contributes, boolean implicit, boolean success) {
         respondSuccess(contributes, implicit, !success);
      }
      
      @Override
      public String formatTask () { return formatSuccess("failed@word"); }
   }  

   /**
    * Propose.How(Plan...) - utterance to choose decomposition
    * 
    * @see Ask.How
    */
   public static class How extends Nested {

      public static TaskClass CLASS;
      
      // for TaskClass.newStep
      public How (Disco disco, Decomposition decomp, String step, boolean repeat) { 
         super(How.class, disco, decomp, step, repeat);
      }

      /* Design Note: The 'plan' slot
       * 
       * Since the decomposition class is a property of the <em>plan</em> for
       * a task instance (there can be multiple plans for same instance, e.g.,
       * when the first one fails), this proposal must take a plan, not a task
       * instance as input.  However, in a situation where the plan is not
       * available (e.g., for semantic component on NL front end), an instance of the
       * appropriate task type can be wrapped in a generated plan and discourse
       * interpretation (esp. plan recognition) can be relied upon to find the
       * appropriate plan to contribute to.
       */
      
      public How (Disco disco, Boolean external, Plan plan, DecompositionClass decomp) { 
         super(How.class, disco, external, null);
         if ( plan != null ) setSlotValue("plan", plan);
         if ( decomp != null ) setSlotValue("decomp", decomp);
      }
      
      public DecompositionClass getDecomp () { 
         return (DecompositionClass) getSlotValue("decomp"); 
      }
      
      public Plan getPlan () { return (Plan) getSlotValue("plan"); }
      
      @Override
      public Task getGoal () { 
         Plan plan = (Plan) getSlotValue("plan");
         return plan == null ? null : plan.getGoal();
      }
      
      @Override 
      public boolean contributes (Plan plan) {
         return super.contributes(plan) || plan == getPlan(); 
      }
      
      @Override
      public void accept (Plan contributes, boolean implicit) {
         if ( isDefinedSlot("plan") && isDefinedSlot("decomp") ) 
            getNestedPlan(contributes).setDecompositionClass(getDecomp());
      }
      
      @Override
      public void reject (Plan contributes, boolean implicit) {
         if ( isDefinedSlot("plan") && isDefinedSlot("decomp") ) {
            Plan plan = getNestedPlan(contributes);
            DecompositionClass decomp = getDecomp();
            plan.getGoal().reject(decomp);
            if ( plan.getDecompositionClass() == decomp ) {
               if ( plan.isStarted() ) getErr().println("Cannot change started plan: "+this);
               else plan.setDecomposition(null);
            }
         }
      }      
               
      private Plan getNestedPlan (Plan contributes) {
         Plan plan = getPlan();
         return ( plan != null && contributes != null
                  && plan.getGoal().matches(contributes.getGoal()) ) ? contributes : plan;
      }
      
      @Override
      public String formatTask () {
         Task nested = getNested();
         DecompositionClass decomp = getDecomp();
         String format = decomp == null ? getDisco().getFormat(this) :
            getFormat(getClass(), nested, decomp);
         if ( format != null) return formatTask(format, null);
         StringBuilder buffer = new StringBuilder();
         appendKeySp(buffer, "lets@word");
         if ( nested == null ) {
            appendKeySp(buffer, "do@word"); appendKeySp(buffer, "something@word");
         } else buffer.append(nested.formatTask());
         buffer.append(' ');
         if ( decomp == null ) appendKey(buffer, "somehow@word");
         else buffer.append(decomp.format());
         return buffer.toString();
      }
   }  
}
