package edu.wpi.disco;

import java.util.*;
import edu.wpi.cetask.*;
import edu.wpi.disco.Agenda.Plugin;
import edu.wpi.disco.Dual.TranslateException;
import edu.wpi.disco.plugin.DecompositionPlugin;

/**
 * Theory of mind component that can be added to any agent.
 * <p>
 * Similar to {@link Dual}, except that agent and Disco only used for prediction.
 * 
 * @see #predict()
 * @see #occurred(Task)
 */
public class ToM extends Interaction {

   /**
    * This is the key ToM method. It returns the other's primitive task predicted by
    * the theory or mind, or null.
    */
   public Task predict () {
      // NB: This may not always give exactly same answer as generateBest (even with
      // DecompositionPlugin added below).
      Plugin.Item item = getSystem().generateBestDry(this);
      return item == null ? null: item.task;
   }

   /**
    * Call this method with tasks that actually occurred for both self and other
    * to update ToM.  Returns copy used in ToM or null.
    */
   public Task occurred (Task occurrence) {
      try {
         Task copy = Dual.translate(occurrence, this);
         occurred(!occurrence.getExternal(), copy, null, false); // reverse perspective
         copy.setWhen(occurrence.getWhen()); // keep original timestamp
         return copy;
      } catch (TranslateException e) {
         System.err.println("WARNING: Ignoring untranslatable occurrence "+occurrence);
         return null;
      }
   }

   @Override
   protected boolean doTurn (boolean ok) { 
      responded = true;
      return true; 
   }

   /**
    * Optional console window just for inspecting state of ToM
    */
   public ToM (Actor system, boolean console) {
      super(system, null, null, false, null, system.getName());
      isConsole = console;
      if ( system.getAgenda().getPlugin(DecompositionPlugin.class) == null )
         // for agent to "look ahead" at decomposition choices (for alternative assumptions),
         // but not make them permanently (see use of generateBestDry above)
         new DecompositionPlugin(system.getAgenda(), 25, true, true);
   }

   private final boolean isConsole;
   private ConsoleWindow window; 
   
   public ConsoleWindow getWindow () { return window; }
   
   @Override
   public void start () {
      if ( isConsole ) {
         System.setProperty("java.awt.headless", "false"); // make sure can have window
         window = new ConsoleWindow(this, 600, 500, 14, Shell.newLog("ToM"));
         window.setTitle("Theory of Mind");
         window.setVisible(true);
         super.start();
      }
   }
  
   @Override
   public void cleanup () {
      super.cleanup();
      if ( window != null ) window.close();
   }
   
   /**
    * This is useful utility class for representing alternative predictions, based
    * on different assumptions about the other.  See use in testing main below.
    */
   public static class Prediction {
      public final Task task;
      public Object assumption;

      public Prediction (Task task, Object assumption) {
         this.task = task;
         this.assumption = assumption;
      }

      @Override
      public String toString () {
         return "Prediction["+task+','+assumption+']';
      }
   }

   private final List<Prediction> predictions = new ArrayList<Prediction>();

   public List<Prediction> getPredictions () { return predictions; }
   
   /**
    * For testing.  See test/ToM/ToM1.test
    */
   public static void main (String[] args) { 

      TestAgent agent = new TestAgent("agent",

         // ToM here is a default Disco agent with same task model, but it doesn't have to be
         new ToM(new Agent("ToM"), true) {

            @Override
            public Task occurred (Task occurrence) { 
               Task copy = super.occurred(occurrence);
               if ( copy != null && copy.isSystem() ) {
                  // for testing, compare actual user occurrence with predictions
                  for (Prediction p : getPredictions()) {
                     p.task.removeSlotValue("external"); // ignore for matching
                     p.task.removeSlotValue("when"); // ignore for matching
                     console.println(
                           (copy.isMatch(p.task) ? "CONSISTENT: " : "INCONSISTENT: ")+p);
                  }
               }
               return copy;
            }
         });
           
      Interaction interaction = new Interaction(agent, new User("User"),
            args.length > 0 && args[0].length() > 0 ? args[0] : null) {
         
         @Override
         protected synchronized Plan occurred (boolean external, Task occurrence, 
               Plan contributes, boolean eval) {
            Plan plan = super.occurred(external, occurrence, contributes, eval);
            ((TestAgent) getSystem()).getToM().occurred(occurrence);
            return plan; 
         }
         
         @Override
         public void cleanup () { 
            super.cleanup();
            Interaction toM = ((TestAgent) getSystem()).getToM();
            toM.exit();
         }
      };
         
      agent.getToM().load("models/Restaurant.xml");       
      interaction.load("models/Restaurant.xml");
      console = interaction.getConsole(); // force output to main console
      agent.getToM().start(); // start ToM window first to avoid race condition
      interaction.start(true);
   }

   // for testing (showing how ToM component can be added to any agent class)
   private static class TestAgent extends Agent {

      private final ToM toM;
      public ToM getToM () { return toM; }

      public TestAgent (String name, ToM toM) { 
         super(name);
         this.toM = toM; 
      }
      
      @Override
      protected boolean synchronizedRespond (Interaction interaction, boolean ok, boolean guess) {
         boolean responded = super.synchronizedRespond(interaction, ok, guess);
         // make ToM predictions immediately after agent response,
         // before world state changes due to user action
         toM.getPredictions().clear();
         predict("dominant = true", "dominant");
         predict("dominant = false", "submissive");
         return responded;
      }
      
      private void predict (String eval, Object assumption) {
         toM.getDisco().eval(eval, "ToM.TestAgent");
         Task task = toM.predict();
         if ( task != null ) toM.getPredictions().add(new Prediction(task, assumption));
      }
   }

   private static Console console;
}


