package edu.wpi.disco.ldt;

import edu.wpi.disco.*;
import edu.wpi.disco.Agenda.Plugin;
import edu.wpi.disco.plugin.*;

public class UnifiedAgent extends Agent {
   
   // use this main instead of Interaction.main() for console debugging
   // also see bin/learn-do-teach bash script
   public static void main (String[] args) {
      Interaction interaction = new Interaction(new UnifiedAgent("agent"),
            new User("human"), args.length > 0 && args[0].length() > 0 ? args[0]
               : null);
      Disco disco = interaction.getDisco();
      // tweak glossing
      disco.setProperty("execute@word", "do");
      disco.setProperty("achieve@word", "do");
      interaction.start(true);
   }

   public UnifiedAgent (String name) { 
      super(name);
      // prevent asking about recipes (see interaction@guess below)
      getAgenda().remove(AskHowPlugin.class);
      // announce when done current non-primitive (highest priority)
      new ProposeDonePlugin(agenda, 300);
   }
    
   @Override
   protected boolean synchronizedRespond (Interaction interaction, boolean ok, boolean guess, boolean retry) {
      // override default turn-taking to simply do one action if possible
      Plugin.Item item = respondIf(interaction, true, retry); 
      if ( item == null ) return false;
      occurred(interaction, item, retry);
      return true;
   }
}
