package edu.wpi.car;

import edu.wpi.cetask.*;
import edu.wpi.disco.*;

public class CarTest {
 
   // for testing simulate method
   
   public static void main (String[] args) {
       Interaction interaction = new Interaction(
             new Agent("agent"), 
             new User("user"),
             args.length > 0 && args[0].length() > 0 ? args[0] : null);
       interaction.load("models/Primitives.xml");
       Disco disco = interaction.getDisco();
       CarWorld world = new CarWorld();
       disco.setWorld(world);
       Task task = disco.getTaskClass("Unscrew").newInstance();
       task.setSlotValue("stud", world.MyCar.LFhub.studA);
       task.getGrounding().eval(task);
       ((CarWorld) disco.getWorld()).print(System.out);
       // start shell for debugging
       interaction.start(true); 
   }
}
