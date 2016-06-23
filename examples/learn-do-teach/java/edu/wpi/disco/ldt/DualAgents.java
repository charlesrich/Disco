package edu.wpi.disco.ldt;
import java.io.File;
import edu.wpi.disco.*;

public class DualAgents extends Console {
   
   /**
    * Use this main instead of Interaction.main() for console debugging
    * for interaction with two agents in Figure 5(c).
    * Also see bin/learn-do-teach-dual bash script.
    * 
    * @see UnifiedAgent#main(String[])
    */
   public static void main (String[] args) {
      interaction = UnifiedAgent.main(null, new UnifiedAgent("agent"),
                                            new UnifiedAgent("agent'"));
      DualAgents console = new DualAgents(
            args.length > 0 && args[0].length() > 0 ? args[0] : null,
            interaction, null);
      interaction.setConsole(console);
      interaction.setRetry(false);
      console.init(UnifiedAgent.DISCO);
      interaction.start(true);
   }
   
   public DualAgents (String from, Interaction interaction, File log) { 
      super(from, interaction, log);
   }
   
   private static Interaction interaction;
   
   // convenient console commands to manually control turn-taking for
   // system and external agents
   
   public void system (String arg) { 
      interaction.getSystem().respond(interaction, false, false);
   }
   
   public void external (String arg) { 
      interaction.getExternal().respond(interaction, false, false);
   }
   
}
