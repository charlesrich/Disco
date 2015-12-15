package edu.wpi.disco.ldt;
import java.io.File;
import edu.wpi.disco.*;

// TODO move to Console.Dual ?
//      will run by itself if auto turntaking?

// better model would be for each agent to have separate instance of Disco,
// but this is ok for proof of concept

public class DualConsole extends Console {

   public static void main (String[] args) {
      // to do
   }
   
   public DualConsole (String from, Interaction interaction, File log) { 
      super(from, interaction, log);
   }
   
   public void external (String arg) {  // respond?
      // call respond on external actor (agent)
   }
}
