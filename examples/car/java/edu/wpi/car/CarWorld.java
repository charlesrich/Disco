package edu.wpi.car;

import java.io.PrintStream;
import java.util.*;

public class CarWorld {
   
   public final Car MyCar;
   
   public final Tire LFtire, RFtire, RRtire, LRtire;
   
   public final List<Nut> LOOSE_NUTS = new ArrayList<Nut>();
   
   public CarWorld () {
      // must create tires first (see Car constructor)
      LFtire = new Tire("LF tire");
      RFtire = new Tire("RF tire");
      RRtire = new Tire("RR tire");
      LRtire = new Tire("LR tire");
      MyCar = new Car(this);
   }
   
   /**
    * public copy constuctor
    * 
    * @see edu.wpi.cetask.Utils#copy(Object)
    */
   public CarWorld (CarWorld world) {
      // must copy tires first (see Hub copy constructor)
      LFtire = new Tire(world.LFtire);
      RFtire = new Tire(world.RFtire);
      RRtire = new Tire(world.RRtire);
      LRtire = new Tire(world.LRtire);
      MyCar = new Car(world.MyCar, this);
      for (Nut nut : world.LOOSE_NUTS)
         LOOSE_NUTS.add(new Nut(nut));
   }
   
   public void print (PrintStream stream) {
		if (!LOOSE_NUTS.isEmpty()){
			stream.append("\nLOOSE_NUTS:");		
			for (Nut nut : LOOSE_NUTS) {
				stream.append(nut.name + ", ");
			}
			stream.append("\n");
		}
		MyCar.print(stream, "");
	}
}
