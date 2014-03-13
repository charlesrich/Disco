package edu.wpi.disco.game;

import edu.wpi.disco.*;

/**
 * This is a specialized interaction pattern developed for use
 * in the Semaine project, in which the there are two dialogues going
 * on: the "main" dialogue (between player and NPC) and the "coaching"
 * dialogue (between the player and the coach).  The coach always has 
 * the first priority to respond.
 *
 */
public class CoachedInteraction extends NWayInteraction {

   public static void main (String[] args) {
      new CoachedInteraction(
            new Player("user"),
            new Agent("agent"),
            new Agent("coach"),
            args.length > 0 && args[0].length() > 0 ? args[0] : null)
      .run();
   }
   
   private final Actor agent, coach;
   private final SingleInteraction main, coaching;
   
   public SingleInteraction getMain () { return main; }
   public SingleInteraction getCoaching () { return coaching; }
   
   public CoachedInteraction (Player player, Actor agent, Actor coach, String from) {
      super(null, player, agent, from, true); // paused not used
      this.agent = agent;
      this.coach = coach;
      main = activeInteractions.get(0);
      coaching = new SingleInteraction(coach, player, null, this);
      coaching.setConsole(getConsole());
      activeInteractions.add(coaching);
      coaching.startRunning();
      coaching.setOk(false);
      main.setOk(false);
   }

   // only used for running in console
   @Override
   public void run () {
      // first time only to load coaching model(s)
      setInteraction(coaching);
      console.respond(coaching);
      while (running) {
         // always "drain" coaching interaction first
         setInteraction(coaching);
         boolean responded = true;
         while (responded) { 
            try { responded = coach.respond(coaching, false, true); } // guess true
            catch (Throwable e) { console.exception(e); }
            if ( responded && 
                 (!console.isRespond() || player.generateBest(coaching) != null) )
               console.respond(coaching);
         }
         if ( !running ) break;
         setInteraction(main);
         try { agent.respond(main, false, true); } // guess true
         catch (Throwable e) { console.exception(e); }
         console.respond(main);
      }
      cleanup();
   }

   private void setInteraction (SingleInteraction interaction) {
      console.setInteraction(interaction);
      synchronized (interaction) { console.init(interaction.getDisco()); }
   }
}