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
         // SAME TEMPORARY WORKAROUND AS IN DUAL
         //copy.setWhen(occurrence.getWhen()); // keep original timestamp
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
   
   @Override
   public String toString () { return getSystem().toString(); }
   
   /**
    * For testing.  See test/ToM/ToM1.test
    */
   public static void main (String[] args) { 

      TestAgent agent = new TestAgent("agent");
      
      // add two candidate ToM's with different initializations 
      // (using default Disco agent and same task model, but need not be so)
      ToM toM = new ToM(new Agent("dominant"), true ); // console only for first candidate
      toM.load("models/Restaurant.xml");
      toM.getDisco().eval("dominant = true;", "test1");
      agent.addToM(toM);

      toM = new ToM(new Agent("submissive"), false);
      toM.load("models/Restaurant.xml");
      toM.getDisco().eval("dominant = false;", "test2");
      agent.addToM(toM);
           
      Interaction interaction = new Interaction(agent, new User("User"),
            args.length > 0 && args[0].length() > 0 ? args[0] : null) {
         
         @Override
         protected synchronized Plan occurred (boolean external, Task occurrence, 
               Plan contributes, boolean eval) {
            TestAgent agent = (TestAgent) getSystem();
            Plan plan = super.occurred(external, occurrence, contributes, eval);
            Iterator<Task> predictions = agent.getPredictions().iterator();
            for (ToM toM : agent.getCandidateToM()) { 
               Task copy = toM.occurred(occurrence);
               // for testing, compare actual user occurrence with predictions
               if ( copy.isSystem() ) {
                  Task task = predictions.next();
                  task.removeSlotValue("external"); // ignore for matching
                  task.removeSlotValue("when"); // ignore for matching
                  console.println(
                        (copy.isMatch(task) ? "CONSISTENT: " : "INCONSISTENT: ")+toM+" "+task);
               }
            }
            return plan; 
         }
         
         @Override
         public void cleanup () { 
            super.cleanup();
            for (ToM toM : ((TestAgent) getSystem()).getCandidateToM()) 
               toM.exit();
         }
      };
         
      interaction.load("models/Restaurant.xml");
      console = interaction.getConsole(); // force output to main console

      for (ToM candidate : agent.getCandidateToM()) 
         candidate.start(); // start ToM window first to avoid race condition
      interaction.start(true);
   }

   // for testing (showing how ToMs can be added to any agent class)
   public static class TestAgent extends Agent {

      private final List<ToM> candidateToM = new ArrayList<ToM>();
      // corresponding predictions
      private final List<Task> predictions = new ArrayList<Task>();
      
      public List<ToM> getCandidateToM () { return candidateToM; }
      public List<Task> getPredictions () { return predictions; }

      public TestAgent (String name) { super(name); }
      
      public void addToM (ToM toM) { candidateToM.add(toM); }
      public void removeToM (ToM toM) { candidateToM.remove(toM); }

      @Override
      protected boolean synchronizedRespond (Interaction interaction, boolean ok, boolean guess) {
         boolean responded = super.synchronizedRespond(interaction, ok, guess);
         predictions.clear();
         // make ToM predictions immediately after agent response,
         // before world state changes due to user action
         for (ToM toM : candidateToM) predictions.add(toM.predict());
         return responded;
      }
   }

   private static Console console;

}


