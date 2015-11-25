/* Copyright (c) 2009 Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco;

import edu.wpi.cetask.*;
import edu.wpi.disco.Agenda.Plugin;
import edu.wpi.disco.lang.Utterance;
import java.util.List;

/**
 * Base class for entities that execute actions and utterances.  An actor
 * may participate in multiple interactions.
 * 
 * @see Interaction
 */
public abstract class Actor {
   
   private String name;
   
   public String getName () { return name;  }
   
   public void setName (String name) { this.name = name; }

   protected final Agenda agenda;
   public Agenda getAgenda () { return agenda; }
   
   // to allow creation of Agenda in constructor below 
   protected class Agenda extends edu.wpi.disco.Agenda { 
      Agenda () { super(Actor.this); }
   }
   
   public Actor (String name) {
      this.name = name;
      this.agenda = new Agenda(); 
   }
   
   /**
    * Initialize the state of this actor in given discourse engine.
    */
   public void init (Disco disco) {}
    
   /**
    * Release resources, etc., used by this actor in preparation
    * for thread of given interaction exiting.
    */
   public void cleanup (Interaction interaction) {
      clear(interaction);
   }
   
   /**
    * Clear any discourse state information stored in this actor
    * related to given interaction.
    */
   public void clear (Interaction interaction) {}
   
   /**
    * Thread-safe method to generate highest priority task for this actor.
    */
   public Plugin.Item generateBest (Interaction interaction) { 
      return agenda.generateBest(interaction);
   }
   
   /**
    * Thread-safe method to generate tasks for this actor.
    */
   public List<Plugin.Item> generate (Interaction interaction) {
      return agenda.generate(interaction);
   }
   
   /**
    * Thread-safe method to take a turn in given interaction. <em>Note:</em>
    * Actor should call {@link Interaction#occurred(boolean,Task,Plan)} as each
    * task in turn is executed.  
    *
    * @param ok say "Ok" to end turn if nothing else to say (extension for game)
    * @param guess guess decompositions
    * @param retry try other decompositions if failure (see {@link Agent#retry(Disco)})
    * @return true if some response was made
    * 
    * @see Interaction
    */
   public final boolean respond (Interaction interaction, boolean ok, boolean guess, boolean retry) {
      synchronized (interaction) { 
         return synchronizedRespond(interaction, ok, guess, retry); 
      }
   }
   
   protected abstract boolean synchronizedRespond (Interaction interaction, 
         boolean ok, boolean guess, boolean retry);
   
   private boolean authorized = true;
   
   /**
    * Test whether this actor is pre-authorized to perform <em>all</em>
    * primitive tasks (unless specifically rejected) without asking for permission. 
    * Default true.
    * 
    * @see #isAuthorized(Plan, Interaction)
    */
   public boolean isAuthorized () { return authorized; }
   
   /**
    * Authorize this actor to perform <em>all</em> primitive tasks in all
    * interactions (unless specifically rejected) without asking for permission.
    */
   public void setAuthorized (boolean authorized) {
      this.authorized = authorized;
   }
   
   /**
    * Test whether this actor is authorized to perform given plan in given
    * interaction.  Optional steps are not authorized, unless they are user 
    * utterances (for TTSay menu).
    * 
    * @see #isAuthorized()
    * @see #isAuthorized(Task,Interaction)
    * @see Task#getShould()
    */
   public boolean isAuthorized (Plan plan, Interaction interaction) {
      Task goal = plan.getGoal();
      return isAuthorizedShould(plan.getGoal(), interaction) &&
            (!plan.isOptional() || 
             // allow optional utterances to appear on user TTSay menu            
             (goal instanceof Utterance && goal.isUser()));
   }

   /**
    * Test whether this actor is authorized to perform given task in given
    * interaction.  Returns true for utterances and false for non-primitive tasks.
    * 
    * @see #isAuthorized()
    * @see #isAuthorized(Plan,Interaction)
    * @see Task#getShould()
    */
   public boolean isAuthorized (Task task, Interaction interaction) {
      return task instanceof Utterance || isAuthorizedShould(task, interaction); 
   }
   
   private boolean isAuthorizedShould (Task task, Interaction interaction) {
      return canSelf(task, interaction) && 
        ( ( !authorized || !task.getProperty("@authorized", true) ) ?
           Utils.isTrue(task.getShould()) :
           !Utils.isFalse(task.getShould()) );
   }
   
   /**
    * Test whether given task <em>must</em> be performed by this actor in given interaction.
    * Always return false for non-primitive tasks.
    */
   public boolean isSelf (Task task, Interaction interaction) {
      return interaction.getSystem() == this ? task.isSystem() : task.isUser();
   }

   /**
    * Test whether given task <em>must</em> be performed by other actor in given interaction.
    * Always return false for non-primitive tasks.
    */
   public boolean isOther (Task task, Interaction interaction) {
      return interaction.getSystem() == this ? task.isUser() : task.isSystem();
   }

   /**
    * Test whether given task <em>can</em> be performed by this actor in given interaction.
    * Always return false for non-primitive tasks.
    * 
    * @see Task#canUser()
    * @see Task#canSystem()
    */
   public boolean canSelf (Task task, Interaction interaction) {
      return interaction.getSystem() == this ? task.canSystem() : task.canUser();
   }

   /**
    * Test whether given task <em>can</em> be performed by other actor in given interaction.
    * Always return false for non-primitive tasks.
    * 
    * @see Task#canUser()
    * @see Task#canSystem()
    */
   public boolean canOther (Task task, Interaction interaction) {
      return interaction.getSystem() == this ? task.canUser() : task.canSystem();
   }

   @Override
   public String toString () { return name; }
}
            
     
