package edu.wpi.disco;

import java.awt.EventQueue;
import java.util.List;
import java.util.stream.Collectors;
import edu.wpi.cetask.*;
import edu.wpi.disco.Agenda.Plugin;
import edu.wpi.disco.Dual.TranslateException;

/**
 * Agent with a "theory of mind" for the user, which consists of another agent
 * with a completely separate copy of Disco.  Application-specific agent should
 * extend this class.
 * <p>
 * Similar to {@link Dual}, except that other agent and Disco only used for prediction.
 * 
 * @see #predict()
 */
public class ToM extends Agent {

   /**
    * This is the key ToM method. It returns list of predicted primitive tasks
    * or null if none.
    */
   public List<Task> predict () {
      List<Plugin.Item> items = getToM().getSystem().generate(interaction);
      return items == null || items.isEmpty() ? null : 
         items.stream().map(i -> i.task).collect(Collectors.toList());
   }
   
   /**
    * ToM class to be used with instance of this extension of Interaction.
    */
   public static class Interaction extends edu.wpi.disco.Interaction {
       
      @Override
      protected synchronized Plan occurred (boolean external, Task occurrence, 
            Plan contributes, boolean eval) {
         Plan plan = super.occurred(external, occurrence, contributes, eval);
         // update ToM with actual occurrences
         try {
            edu.wpi.disco.Interaction interaction = getSystem().getToM();
            Task copy = Dual.translate(occurrence, interaction);
            interaction.occurred(!external, copy, null, false);
            copy.setWhen(occurrence.getWhen()); // keep original timestamp
         } catch (TranslateException e) {
            System.err.println("WARNING: Ignoring untranslatable occurrence "+occurrence);
         }
         return plan;
      }
      
      public Interaction (ToM system, Actor external) {
         this(system, external, null, false, null, "edu.wpi.disco.ToM.Interaction");
      }
      
      public Interaction (ToM system, Actor external, String from) {
         this(system, external, from, true, null, "edu.wpi.disco.ToM.Interaction");
      }
      
      public Interaction (ToM system, Actor external, String from, boolean console, Disco disco, String title) {
         super(system, external, from, console, disco, title);
         if ( console )
            System.setProperty("java.awt.headless", "false"); // make sure can have window
         window = console ? new ConsoleWindow(getSystem().getToM(), 600, 500, 14, Shell.newLog("ToM"))
            : null;
         if ( window != null ) window.setTitle(getSystem().getToM().getSystem().getName());
      }
      
      @Override
      public ToM getSystem () { return (ToM) super.getSystem(); }
      
      private final ConsoleWindow window; 
      
      @Override
      public void start () { 
         if ( window != null ) window.setVisible(true);
         super.start();
         getSystem().getToM().start(true);
      }
      
      @Override
      public void cleanup () { 
         super.cleanup();
         if ( window != null ) EventQueue.invokeLater(() -> window.close());
      }
   }
   
   public ToM (String name, Actor other) { 
      super(name); 
      // this interaction only to hold other actor and for inspection
      // of state in ConsoleWindow
      interaction = new edu.wpi.disco.Interaction(other, this) {
         @Override
         protected boolean doTurn (boolean ok) { 
            responded = true;
            return true; 
         } 
      };
   }
   
   private final edu.wpi.disco.Interaction interaction;  // theory of mind
   
   public edu.wpi.disco.Interaction getToM () { return interaction; }

   /**
    * For testing.  See test/ToM/ToM1.test
    */
   public static void main (String[] args) { 
      // ToM here is just default user with same model, but it doesn't have to be
      ToM.Interaction interaction = new ToM.Interaction(
            new ToM("agent", new User("ToM")) {
            
               @Override
               protected boolean synchronizedRespond (edu.wpi.disco.Interaction interaction, boolean ok, boolean guess) {
                  boolean responded = super.synchronizedRespond(interaction, ok, guess);
                  getToM().getDisco().eval("dominant = true", "ToM.main");
                  interaction.getConsole().println("PREDICT (dominant=true): "+predict());
                  getToM().getDisco().eval("dominant = false", "ToM.main");
                  interaction.getConsole().println("PREDICT (dominant=false): "+predict());
                  return responded;
               }
            },
            new User("user"), 
            args.length > 0 && args[0].length() > 0 ? args[0] : null);
      interaction.load("models/Restaurant.xml");
      interaction.getSystem().getToM().load("models/Restaurant.xml");
      interaction.start(true);
   }
}
