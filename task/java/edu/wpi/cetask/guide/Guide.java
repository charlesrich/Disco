/* Copyright (c) 2008 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.cetask.guide;

import edu.wpi.cetask.*;

import java.util.*;

public class Guide extends Shell {

   public static String VERSION = "0.9.6 beta";
   
   public static void main (String[] args) {
      make(args).loop();
   }
   
   public static Guide make (String[] args) {  // for use in other mains
      // for convenience to start both Guide and SpeechGuide from same jar file
      return args.length > 0 && args[0].equals("-speech") ?
         SpeechGuide.make(Utils.restArgs(args)) : 
         new Guide(new TaskEngine(), args.length > 0 ? args[0] : null);
   }
   
   /**
    * @param from if non-null, change default name of log file to correspond
    *        to name of given source and immediately start reading from that source
    * 
    * @see #source(String)
    */
   protected Guide (TaskEngine engine, String from) {
      super(engine, from);
   }
    
   @Override
   protected void printVersion () {
      super.printVersion();
      getOut().print(" / Guide "+VERSION);
   }

   // see default.properties for system conversational utterances
   // TODO internationalize using resource bundles
   
   private void respondq (String key, TaskClass type, String name) {
      println(Utils.capitalize(
            getEngine().getProperty(key)+' '+type.formatSlot(name, true)+'?'));
   }
   
   private void respond (String key, Plan plan) {
      respond(key, plan.getGoal());
   }

   private void respond (String key, Task task) {
      respond(key, task, false);
   }

   private void respondq (String key, Task task) {
      respond(key, task, true);
   }
   
   private void respond (String key, Task task, boolean question) {
      respond(key, TaskEngine.DEBUG ? task : task.format(), question);
   }
   
   protected void respond (String key, Object object, boolean question) {
      println(Utils.capitalize(
            getEngine().getProperty(key)+' '+object+(question ? '?' : '.')));
   }
  
   // simple discourse state
   
   private Plan proposed;
   
   protected Plan getProposed () { return proposed; } // for overriding
   
   protected Plan accepted, rejected;
   
   private boolean authorized;
   
   /**
    * Check whether this guide is authorized to perform system tasks without asking for
    * permission.  Default false;
    */
   public boolean isAuthorized () { return authorized; }
   
   /**
    * Authorize this guide to perform system tasks without asking for permission.
    */
   public void setAuthorized (boolean authorized) {
      this.authorized = authorized;
   }

   /**
    * Method for system turn.  Called once each time around command loop.
    * Intended extension point.
    */
   @Override
   protected void system () { 
      Plan focus = getEngine().getFocus();
      // see retry check also in completed()
      if ( focus != null && focus.isFailed() ) focus.retry(); 
      if ( accepted != null && isSystem(accepted) ) {
         accepted.execute();
         // note some outputs may be undefined (especially if task failed)
         if ( accepted.isDone() ) respond("done@word", accepted.getGoal());
         accepted = null;
         completed();
         updateFocus();
      } else if ( getProposed() == null ) { // unless outstanding proposal
         for (Plan next : getLive()) {
            if ( next != rejected && isSystem(next) ) {
               Task goal = next.getGoal();
               if ( authorized ) {
                  next.execute();
                  updateFocus();
               } else {
                  respondq("shall@word", goal);
                  propose(next);
               }
               break;
            }
         }
      }
   } 
   
   private boolean completed () {
      Plan focus = getEngine().getFocus();
      Plan top;
      if ( focus != null ) {
         if ( focus.isDone() && !focus.occurred() ) {
            respond("completed@word", focus);
            return true;
         } else if ( focus.isFailed() ) {
            focus.retry(); // see check also in system()
            respond("failed@word", focus);
            return true;
         } else if ( (top = getEngine().getTop(focus)).isDone() ) {
            // special case for completed toplevel plan
            respond("completed@word", top);
            return true;
         }
      }
      return false;
   }
   
   private void updateFocus () {
      Plan focus = getEngine().getFocus();
      if ( focus != null && !focus.isLive() ) 
         setFocus(getEngine().newFocus(focus));
   }

   private List<Plan> getLive () {
      Plan focus = getEngine().getFocus();
      if ( focus == null ) return Collections.emptyList();
      List<Plan> live = focus.getLiveDescendants();
      if ( focus.isLive() ) live.add(focus);
      if ( live.isEmpty() ) live = getEngine().getTop(focus).getLive();
      return live;
   }
  
   private static boolean isSystem (Plan plan) {
      Task task = plan.getGoal();
      // note for compatibility with published CETask paper, only
      // offer to do task if there is a script defined
      return plan.isLive() && task.canSystem() && task.getScript() != null
         // TODO ask for input values if needed
         // user can provide inputs using 'task' command
         && task.isDefinedInputs(); 
   }
   
   protected void propose (Plan plan) {
      proposed = plan;
      if ( plan != null ) setFocus(plan);
      accepted = null;
      rejected = null;
   }
           
   // commands 
   
   @Override
   protected void help () {
      getOut().println("    task <id> [<namespace>] [ / <value> ]*");
      getOut().println("                        - set current task");
      getOut().println("                          (namespace optional if unambiguous id)");
      getOut().println("                          (slot values optional)");
      getOut().println("    next                - tell me what to do next");
      getOut().println("    by <id>             - select decomposition class for current task");
      getOut().println("    done [<id> [<namespace>]] [ / <value> ]*");
      getOut().println("                        - I have performed this task");
      getOut().println("                          (defaults to suggested next task)");
      getOut().println("                          (all slot values required)");
      getOut().println("    execute [<id> [<namespace>]] [ / <value> ]*");
      getOut().println("                        - Like 'done', except runs script if any");
      getOut().println("    no                  - I don't want to do suggested next task");
      getOut().println("    yes                 - answer to yes/no question");
      super.help();
   }
 
   /**
    * Set current task focus. Namespace may be omitted if it is unambiguous
    * in currently loaded task models. JavaScript to
    * compute all <em>declared</em> input and output slot values is optional
    * (in order declared). Skipped slots can be specified by '/ /'.<br>
    * <br>
    * Hint: If Javascript contains '/', use 'eval' command to set temporary
    * variable and use variable in 'done'.
    * 
    * @param args &lt;id&gt [&lt;namespace&gt] [ / &lt;value&gt ]*
    */
   public void task (String args) {
      Task task = processTask(args, getProposed(), false);
      propose(null); // clear proposals, if any
      if ( task != null ) {
         Plan focus = null;
         Plan match = getEngine().explainBest(task, true);
         if ( match != null ) {
            match.match(task);
            focus = match;
            respond("ok@word");
         } else {
            getEngine().addTop(focus = new Plan(task));
            if ( !task.isPrimitive() ) respond("top@word", task);
         }
         // always change focus
         setFocus(focus);
      }
   } 
      
   private void setFocus (Plan focus) {
      Plan old = getEngine().getFocus();
      if ( getEngine().setFocus(focus) && !old.isFailed() ) 
         // unexpected focus shift
         respond("shift@word", old);
   }
   
   /**
    * Select decomposition class for current focus task.
    * 
    * @param args &lt;id&gt
    */
   public void by (String args) {
      Plan focus = getEngine().getFocus();
      if ( focus != null ) {
        Task goal = focus.getGoal();
        DecompositionClass decomp = goal.getType().getDecomposition(args.trim());
        if ( decomp != null && !Utils.isFalse(decomp.isApplicable(goal)) ) {
           focus.apply(decomp);
           return;
        }
      }
      warning("No such decomposition for current focus.");
   }
   
   /**
    * Report user performance of primitive task. Task class defaults to
    * suggested next task. JavaScript to compute all <em>declared</em> input
    * and output slot values is required (in order declared). Success slot value is
    * optional (and last). Skipped slots can be specified by '/ /'.<br>
    * <br>
    * Hint: If Javascript contains '/', use 'eval' command to set temporary
    * variable and use variable in 'done'.
    * 
    * @param args [&lt;id&gt [&lt;namespace&gt]] [ / &lt;value&gt ]*
    */
   public void done (String args) {
      Task occurrence = processTaskIf(args, getProposed(), true);
      if ( occurrence != null ) done(occurrence); 
   }
   
   protected void done (Task occurrence) {
      if ( getProposed() != null ) {
         Task task = getProposed().getGoal();
         if ( task.matches(occurrence) )
            occurrence.copySlotValues(task);
      }
      if ( occurrence.isDefinedInputs() ) { 
         Plan match = getEngine().done(occurrence);
         if ( match != null ) {
            if ( !completed() ) respond("ok@word");
            setFocus(match);
         } else {
            respond("unknown@word");
            Plan plan = new Plan(occurrence),
                 focus = getEngine().getFocus();
            if ( focus == null ) getEngine().addTop(plan);
            else focus.add(plan);
         } 
         updateFocus();
         propose(null); // clear proposals, if any
      } else warning("All input values must be defined--ignored."); 
   }
 
   /**
    * Like 'done', but first executes script associated with primitive task, if
    * any. Convenient for running simulations.
    * 
    * @see #done(String)
    */
   public void execute (String args) {
      Task occurrence = processTaskIf(args, getProposed(), true);
      if ( occurrence != null ) {
         if ( occurrence.isDefinedInputs() ) {
            Script script = occurrence.getScript();
            if ( script != null) script.eval(occurrence);
            done(occurrence);
         } else warning("All input values must be defined--ignored.");
      }
   }
 
   /**
    * Advise the user what needs to be done next.
    */
   public void next (String ignore) {
      completed();
      updateFocus();
      List<Plan> live = getLive();
      // remove rejected options
      for (Iterator<Plan> i = live.iterator(); i.hasNext();) 
         if ( i.next() == rejected ) i.remove();
      // first preference is for system to do tasks for which it has all info
      for (Plan next : live) 
         if ( isSystem(next) ) {
            propose(null); // clear proposals, if any
            return;
         }
      // next preference is live tasks user can do
      for (Plan next : live) {
         Task task = next.getGoal();
         if ( task.canUser() ) {
            respond((next.isOptional() ? "optional@word" : "please@word"), task);
            propose(next);
            return;
         }
      }
      // next preference is to ask for unbound input
      for (Plan next : live) {
         Task task = next.getGoal();
         TaskClass type = task.getType();
         for (String name : type.getDeclaredInputNames())
               if ( !task.isDefinedSlot(name) ) {
                  propose(next);
                  respondq("what@word", type, name);
                  return;
               }
      }
      // final preference is to choose decomposition
      for (Plan next : live ) {
         if ( !next.isPrimitive() && !next.isDecomposed() ) {
            List<DecompositionClass> decomps = next.getDecompositions();
            if ( !decomps.isEmpty() ) {
               propose(next);
               // if there is only one applicable choice or an authorized choice, 
               // make it without asking user
               DecompositionClass choice = null;
               if ( decomps.size() == 1 ) choice = decomps.get(0);
               else {
                  for (DecompositionClass decomp : decomps) {
                     if ( decomp.getProperty("@authorized", false) )
                        choice = decomp; break;
                     }
               } 
               if ( choice != null ) {
                  next.apply(decomps.get(0));
                  next(null); // note recursive call to next method
               } else respondq("how@word", next.getGoal());
               return;
            }
         }
      }
      // otherwise just reiterate current focus, if any
      Plan focus = getEngine().getFocus();
      if ( focus != null && focus.hasLiveDescendants() ) {
         respond("lets@word", focus);
         propose(focus);
      } else {
         respond(focus == null ? "nofocus@word" : "nonext@word");
         propose(null); // clear proposals, if any
      }
   }

   /**
    * Accept system proposals.
    */
   public void yes (String ignore) {
      if ( getProposed() != null ) {
         accepted = getProposed();
         proposed = null;
         rejected = null;
         respond("ok@word");
      }
   }

   /**
    * Reject system proposals.
    */
   public void no (String ignore) {
      if ( getProposed()!= null ) {
         rejected = getProposed();
         proposed = null;
         accepted = null;
         respond("ok@word");
         setFocus(getEngine().newFocus(rejected));
      }
   }
}

