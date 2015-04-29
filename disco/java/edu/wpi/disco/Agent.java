/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco;

import edu.wpi.cetask.*;
import edu.wpi.disco.Agenda.Plugin;
import edu.wpi.disco.lang.*;
import edu.wpi.disco.plugin.*;

import java.util.*;

public class Agent extends Actor {
  
   public Agent (String name) {
      super(name);
      new AskShouldTopPlugin(agenda, 300); // only generates when stack empty
      new AskShouldPassablePlugin(agenda, 275); // ask optional before input 
      new AskHowPassablePlugin(agenda, 275);
      new AskWhatNoBindingPlugin(agenda, 250);
      new AuthorizedPlugin(agenda, 225);
      new ProposeShouldSelfPlugin(agenda, 100, false);
      new AskShouldPlugin(agenda, 80); 
      new ProposeShouldOtherPlugin(agenda, 75);
      new AskWhatPlugin(agenda, 25);
      new AskWhoPlugin(agenda, 20);
      new ProposeHowPlugin(agenda, 20); // higher priority than AskHow
      new AskHowPlugin(agenda, 15);
   }
   
   private int max = 5;
   
   /**
    * Return maximum number of non-utterance tasks on agent turn (default 5).
    */
   public int getMax () {  return max;  }

   /**
    * Set maximum number of non-utterance tasks on agent turn.
    */
   public void setMax (int max) { this.max = max; }

   @Override
   public Plugin.Item generateBest (Interaction interaction) {
     return generateBest(interaction, false);
   }

   private Plugin.Item generateBest (Interaction interaction, Boolean guess) {
      Disco disco = interaction.getDisco();
      Plan focus = disco.getFocusExhausted(true);
      // decompose *all* live plan plans in current focus 
      // with either exactly one *applicable* decomposition (that has not been
      // de-authorized) or first applicable authorized one
      List<Plan> live = null;
      if ( focus != null ) {
         live = focus.getLiveDescendants();
         if ( focus.isLive() ) live.add(focus);
      } else {
         live = new ArrayList<Plan>(5);
         for (Plan top : disco.getTops()) 
            if ( top.isLive() ) live.add(top);
      }
      for (Plan plan : live) {
         if ( !plan.isPrimitive() && !plan.isDecomposed() ) {
            Task goal = plan.getGoal();
            List<DecompositionClass> decomps = plan.getDecompositions();
            if ( decomps.isEmpty() ) continue;
            DecompositionClass choice = null;
            if ( decomps.size() == 1 ) {
               DecompositionClass decomp = decomps.get(0);
               if ( decomp.getProperty("@authorized", true)
                     || decomp == plan.getDecompositionClass() )
                  choice = decomp;
            } else {
               for (DecompositionClass decomp : decomps) {
                  if ( decomp.getProperty("@authorized", false) ) {
                     choice = decomp; break;
                  }
               }
            } 
            if ( choice != null ) { 
               plan.apply(choice);
               plan.decomposeAll();
               // new live plans may have been create above
               return generateBest(interaction, guess);
            } else if ( guess == null || guess ) {
               Plugin.Item item = generateBest(interaction); // no guessing
               if ( item != null ) return item;
               for (DecompositionClass decomp : decomps)
                  if ( !decomp.isInternal() // don't guess dialog tree choices
                        && decomp.getProperty("@authorized", true) // only if authorized
                        && Utils.equals(decomp.isApplicable(goal), guess) ) {
                     plan.apply(decomp); // one guess
                     plan.decomposeAll();
                     // new live plans may have been create above
                     item = generateBest(interaction); // no guessing
                     return item != null ? item : 
                        // recursion ends when no more live non-decomposed plans
                        generateBest(interaction, guess); // continue guessing
                  }
            }
         }
      }
      return chooseBest(interaction);
   }
   
   /**
    * Override this method to choose best on basis other than priorities.
    * Call {@link #generate(Interaction)} to generate candidates.
    */
   protected Plugin.Item chooseBest (Interaction interaction) {
      if ( Disco.TRACE ) {
         List<Plugin.Item> items = generate(interaction);
         if ( items.isEmpty() ) return null;
         if ( items.size() > 1) Utils.print(items, interaction.getDisco().getOut());
         return items.get(0);
      } //else 
      return super.generateBest(interaction);
   }
   
   /**
    * An utterance always ends a turn.  If the <tt>ok</tt> flag is true, then 
    * a turn always ends in an utterance (using 'Ok' if needed).  The
    * maximum number of non-utterances in a turn is specified by {@link #getMax()}.
    * Thus if <tt>ok</tt> is false, this method may execute no tasks.
    * <p> 
    * Note this is a <em>very</em> 
    * simple-minded turn-taking strategy, which does not take account of
    * initiative or discourse structure.
    * 
    * @param ok force turn to end with 'Ok' if necessary
    * @param guess guess decompositions (see {@link #generateBest(Interaction,Boolean)})
    * @param retry try other decompositions if failure (see {@link #retry(Disco)})
    */
   @Override
   protected boolean synchronizedRespond (Interaction interaction, boolean ok, boolean guess, boolean retry) {
      Disco disco = interaction.getDisco();
      if ( retry ) retry(disco); // see also in done
      for (int i = max; i-- > 0;) {
         Plugin.Item item = respondIf(interaction, guess, retry);
         if ( item == null ) {
            // say "Ok" when nothing else to say and end of turn is required
            if ( ok ) item = newOk(disco);
            else return false;
         }
         done(interaction, item, retry);
         if ( item.task instanceof Utterance) return true; // end of turn
      }
      // maximum number of non-utterances
      if ( ok ) done(interaction, newOk(disco), retry);
      return true;
   }
   
   public Plugin.Item respondIf (Interaction interaction, boolean guess, boolean retry) {
      Disco disco = interaction.getDisco();
      if ( retry) retry(disco); // see also in done
      disco.decomposeAll();
      Plugin.Item item = generateBest(interaction);
      if ( guess ) {
         // guess first true applicable decomposition
         if ( item == null ) item = generateBest(interaction, true);
         // guess first unknown applicable decomposition
         if ( item == null ) item = generateBest(interaction, null);
      }
      return item;
   }
   
   private static Plugin.Item newOk (Disco disco) {
      return Agenda.newItem(new Ok(disco, false), null);
   }

   /**
    * This method is for overriding in an agent to substitute more sophisticated
    * natural language generation and/or pass the utterance to TTS or GUI.  
    * Empty definition by default.
    * 
    * To get default Disco text translation use {@link Interaction#format(Utterance)}
    */
   public void say (Interaction interaction, Utterance utterance) {}

   protected Utterance lastUtterance;
   
   public Utterance getLastUtterance () { return lastUtterance; }
   
   @Override
   public void clear (Interaction interaction) { 
      super.clear(interaction);
      lastUtterance = null; 
   }
   
   /**
    * Thread-safe method for notifying interaction that given plugin item
    * has occurred.
    * 
    * @param retry try other decompositions if failure (see {@link #retry(Disco)})
    */
   public void done (Interaction interaction, Plugin.Item item, boolean retry) { 
      synchronized (interaction) { // typically used in dialogue loop
         interaction.done(this == interaction.getExternal(), 
               item.task, item.contributes);
         if ( item.task instanceof Utterance ) { // after occurred
            lastUtterance = (Utterance) item.task;
            say(interaction, (Utterance) item.task);
         }
         if ( retry ) retry(interaction.getDisco());  // see also in respond
      }
   }

   protected void retry (Disco disco) {
      synchronized (disco.getInteraction()) { // called in DiscoUnity agent
         Stack<Segment> stack = disco.getStack();
         for (int i = stack.size(); i-- > 1;) {
            Plan plan = stack.get(i).getPlan();
            if ( plan.isFailed() ) {
               Plan retried = plan.retry();
               if ( retried != null ) {
                  // expose retried plan
                  while ( disco.getFocus() != retried ) disco.pop();
                  // substitute copy of failed goal
                  disco.getSegment().setPlan(retried.getRetryOf());
                  disco.pop(); disco.push(retried);
                  break;
               }
            }
         }
      }
   }

}
     
