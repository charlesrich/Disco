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
   }
   
   @Override
   protected void init () {
      new AskShouldTopPlugin(agenda, 300); // only generates when stack empty
      new AskShouldPassablePlugin(agenda, 275); // ask optional before input 
      new AskHowPassablePlugin(agenda, 275);
      // higher priority than ProposeShouldOther, AskWhatNoBinding, AskWhat
      new ProposeWhatPlugin(agenda, 260);
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
   public void execute (Task occurrence, Interaction interaction, Plan contributes) {
      super.execute(occurrence, interaction, contributes);
      if ( occurrence instanceof Utterance ) { // after interpretation
         Utterance utterance = (Utterance) occurrence;
         lastUtterance = utterance;
         say(interaction, utterance);
      }
   }
   
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
      return generateBestDry(interaction); // Actor method
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
    * @return true if some response was made
    */
   @Override
   protected boolean synchronizedRespond (Interaction interaction, boolean ok, boolean guess) {
      Disco disco = interaction.getDisco();
      for (int i = max; i-- > 0;) {
         Plugin.Item item = respondIf(interaction, guess);
         if ( item == null ) {
            // say "Ok" when nothing else to say and end of turn is required
            if ( ok ) item = Agenda.newItem(new Ok(disco, null), null);
            else return false;
         }
         execute(item.task, interaction, item.contributes);
         if ( item.task instanceof Utterance) return true; // end of turn
      }
      // maximum number of non-utterances
      if ( ok ) execute(new Ok(disco, null), interaction, null);
      return true;
   }
  
   /**
    * Return best response or null if there is no response.
    * 
    * @param guess guess decompositions (see {@link #generateBest(Interaction,boolean)})
    */
   public Plugin.Item respondIf (Interaction interaction, boolean guess) {
      Disco disco = interaction.getDisco();
      disco.decomposeAll();
      return generateBest(interaction, guess);
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
   
   // support for private beliefs

   /**
    * Return private belief about value (including null) of given input of 
    * given plan.
    * 
    * @see #isDefinedSlot(Plan,TaskClass.Input)
    * @see #setSlotValue(Plan,TaskClass.Input,Object)
    */
   public Object getSlotValue (Plan plan, Input input) { 
      // default for testing only -- ignores plan 
      return inputs.get(input);
   }

   /**
    * Set private belief about value (including null) of given input of 
    * given plan.
    * 
    * @see #isDefinedSlot(Plan,TaskClass.Input)
    * @see #getSlotValue(Plan,TaskClass.Input)
    */
   public void setSlotValue (Plan plan, Input input, Object value) {
      // default for testing only -- ignores plan 
      inputs.put(input, value);
   }

   /**
    * Tests whether this agent has private knowledge about given input of given
    * plan.
    * 
    * @see #getSlotValue(Plan,TaskClass.Input)
    * @see #setSlotValue(Plan,TaskClass.Input,Object)
    */
   public boolean isDefinedSlot (Plan plan, Input input) {
      // default for testing only -- ignores plan 
      return inputs.containsKey(input);
   }

   // for testing above
   final private Map<Input,Object> inputs = new HashMap<Input,Object>(); 

}
     
