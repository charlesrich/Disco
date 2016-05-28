/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco;

import edu.wpi.cetask.*;
import edu.wpi.cetask.TaskClass.Input;
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
      new ProposeWhatPlugin(agenda, 78); // higher priority than ProposeShouldOther
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

   /**
    * Thread-safe method to generate list of tasks for agent, 
    * sorted according to plugin priorities (with guessing)
    * 
    * @see #generate(Interaction,boolean)
    * @see #generateBest(Interaction)
    */
   @Override
   public List<Plugin.Item> generate (Interaction interaction) {
     return generate(interaction, true);
   }
   
   /**
    * Thread-safe method to return highest priority task for agent (with guessing)
    * 
    * @see #generateBest(Interaction,boolean)
    * @see #generate(Interaction)
    */
   @Override
   public Plugin.Item generateBest (Interaction interaction) {
      return generateBest(interaction, true);
   }
   
   /**
    * Thread-safe method to generate list of tasks for agent, 
    * sorted according to plugin priorities.
    * 
    * @param guess guess among applicable decompositions
    * 
    * @see #generateBest(Interaction,boolean)
    */
   public List<Plugin.Item> generate (Interaction interaction, boolean guess) {
      List<Plugin.Item> items = generate(interaction, false, false); // no guessing
      if ( guess ) {
         // guess first true applicable decomposition
         if ( items == null || items.isEmpty() ) items = generate(interaction, true, false);
         // guess first unknown applicable decomposition
         if ( items == null || items.isEmpty() ) items = generate(interaction, null, false);
      }            
      return items;
   }

   /**
    * Thread-safe method to return highest priority task for agent.
    * 
    * @param guess guess among applicable decompositions
    * 
    * @see #generate(Interaction,boolean)
    */
   public Plugin.Item generateBest (Interaction interaction, boolean guess) {
      List<Plugin.Item> items = generate(interaction, false, true); // no guessing
      if ( guess ) {
         // guess first true applicable decomposition
         if ( items == null ) items = generate(interaction, true, true);
         // guess first unknown applicable decomposition
         if ( items == null ) items = generate(interaction, null, true);
      }
      return items == null ? null : items.get(0);
   }

   private List<Plugin.Item> generate (Interaction interaction, Boolean guess, boolean onlyBest) {
      Disco disco = interaction.getDisco();
      Plan focus = disco.getFocusExhausted(true);
      // decompose *all* live plan in current focus with either chosen decomposition,
      // or exactly one *applicable* decomposition (that has not been
      // de-authorized), or first applicable authorized one
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
         if ( plan.isPrimitive() ) continue;
         if ( !plan.isDecomposed() ) {
            Task goal = plan.getGoal();
            List<DecompositionClass> decomps = plan.getDecompositions();
            if ( decomps.isEmpty() ) continue; // possible loop exit without return
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
               // new live plans may have been created above
               return generate(interaction, guess, onlyBest);
            } else if ( guess == null || guess ) {
               List<Plugin.Item> items = generate(interaction, false, onlyBest); // no guessing
               if ( items != null  && !items.isEmpty() ) return items;
               for (DecompositionClass decomp : decomps)
                  if ( !decomp.isInternal() // don't guess dialog tree choices
                        && decomp.getProperty("@authorized", true) // only if authorized
                        && Utils.equals(decomp.isApplicable(goal), guess) ) {
                     plan.apply(decomp); // one guess
                     plan.decomposeAll();
                     // new live plans may have been created above
                     items = generate(interaction, false, onlyBest); // no guessing
                     return items != null && !items.isEmpty() ? items : 
                        // recursion ends when no more live non-decomposed plans
                        generate(interaction, guess, onlyBest); // continue guessing
                  } // possible loop exit without return
            }
         }
      }
      if ( onlyBest ) {
         Plugin.Item item = chooseBest(interaction);
         return item == null ? null : Collections.singletonList(item);
      } // else 
      return super.generate(interaction); // Actor method
   }
   
   /**
    * When this variable is non-null, agent uses this value to randomly choose
    * among primitive tasks that are generated by plugins (i.e., ignoring
    * priorities). Use {@link Random#Random(long)} in test cases for reproducibility.
    * <p>
    * This is most likely to be useful for variability in D4g dialogue trees in
    * which multiple agent choices are provided. In this case add
    * {@link DecompositionPlugin} as follows:
    * <p>
    * <code>
    * new DecompositionPlugin(disco.getInteraction().getSystem().getAgenda(), 25, true true)
    * </code>
    * <p>
    * See test/Random1.test
    * 
    * @see #chooseBest(Interaction)
    */
   public static Random RANDOM;
   
   /**
    * Override this method to choose best on basis other than priorities or randomly.
    * Call {@link #generate(Interaction,boolean)} to generate candidates.
    * 
    * @see Agent#RANDOM
    */
   protected Plugin.Item chooseBest (Interaction interaction) {
      if ( Disco.TRACE || RANDOM != null ) {
         List<Plugin.Item> items = super.generate(interaction); // Actor method
         if ( items.isEmpty() ) return null;
         if ( Disco.TRACE ) Utils.print(items, interaction.getDisco().getOut());
         return RANDOM == null ? items.get(0) :
            items.get(RANDOM.nextInt(items.size()));
      } //else 
      return super.generateBest(interaction); // Actor method
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
    * @param guess guess decompositions (see {@link #generateBest(Interaction,boolean)})
    * @param retry try other decompositions if failure (see {@link #retry(Disco)})
    * @return true if some response was made
    */
   @Override
   protected boolean synchronizedRespond (Interaction interaction, boolean ok, boolean guess, boolean retry) {
      Disco disco = interaction.getDisco();
      if ( retry ) disco.retry(); // see also in occurred
      for (int i = max; i-- > 0;) {
         Plugin.Item item = respondIf(interaction, guess, retry);
         if ( item == null ) {
            // say "Ok" when nothing else to say and end of turn is required
            if ( ok ) item = newOk(disco);
            else return false;
         }
         occurred(interaction, item, retry);
         if ( item.task instanceof Utterance) return true; // end of turn
      }
      // maximum number of non-utterances
      if ( ok ) occurred(interaction, newOk(disco), retry);
      return true;
   }
  
   /**
    * Return best response or null if there is no response.
    * 
    * @param guess guess decompositions (see {@link #generateBest(Interaction,boolean)})
    * @param retry try other decompositions if failure (see {@link #retry(Disco)})
    */
   public Plugin.Item respondIf (Interaction interaction, boolean guess, boolean retry) {
      Disco disco = interaction.getDisco();
      if ( retry ) disco.retry(); // see also in occurred
      disco.decomposeAll();
      return generateBest(interaction, guess);
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
   public void occurred (Interaction interaction, Plugin.Item item, boolean retry) { 
      synchronized (interaction) { // typically used in dialogue loop
         interaction.occurred(this == interaction.getExternal(), 
               item.task, item.contributes);
         if ( item.task instanceof Utterance ) { // after occurred
            lastUtterance = (Utterance) item.task;
            say(interaction, (Utterance) item.task);
         }
         if ( retry ) retry(interaction);  // see also in respond
      }
   }

   protected void retry (Interaction interaction) {
      synchronized (interaction) { // called in DiscoUnity agent
         interaction.getDisco().retry();
      }
   }
     
   // support for private beliefs

   /**
    * Return private belief about value (including null) of given input of 
    * given plan.
    * 
    * @see #isDefinedSlot(Plan,TaskClass.Input)
    * @see #setSlotValue(Plan,TaskClass.Input,Object)
    */
   public Object getSlotValue (Plan plan, Input input) { 
      // convenient default for testing 
      return inputs.get(new InputKey(plan, input));
   }

   /**
    * Set private belief about value (including null) of given input of 
    * given plan.
    * 
    * @see #isDefinedSlot(Plan,TaskClass.Input)
    * @see #getSlotValue(Plan,TaskClass.Input)
    */
   public void setSlotValue (Plan plan, Input input, Object value) { 
      // convenient default for testing
      inputs.put(new InputKey(plan, input), value);
   }

   /**
    * Tests whether this agent has private knowledge about given input of given
    * plan.
    * 
    * @see #getSlotValue(Plan,TaskClass.Input)
    * @see #setSlotValue(Plan,TaskClass.Input,Object)
    */
   public boolean isDefinedSlot (Plan plan, Input input) {
      // convenient default for testing 
      return inputs.containsKey(new InputKey(plan, input));
   }

   // for testing above
   
   final private Map<InputKey,Object> inputs = new HashMap<InputKey,Object>(); 

   private static class InputKey {
      private final Plan plan;
      private final String input;
   
      private InputKey (Plan plan, Input input) {
         this.plan = plan;
         this.input = input.toString();
      }
      
      @Override
      public int hashCode () {
         final int prime = 31;
         int result = 1;
         result = prime * result + input.hashCode();
         result = prime * result + plan.hashCode();
         return result;
      }

      @Override
      public boolean equals (Object obj) {
         if ( obj == null || getClass() != obj.getClass() )
            return false;
         InputKey other = (InputKey) obj;
         return plan == other.plan && input.equals(other.input);
      }

      @Override
      public String toString () { return input; } 
   }
}
     
