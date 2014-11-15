package edu.wpi.car;

import java.io.PrintStream;

public class Car extends PhysObj {
   
	public final Hub RFhub, LFhub, RRhub, LRhub; 

	public Car (CarWorld world) { 
	   super("My Car", null, null);
	   LFhub = new Hub("LF hub", Location.plus(location, 10, 10, 0), this);
	   RFhub = new Hub("RF hub", Location.plus(location, -10, -10, 0), this);
	   RRhub = new Hub("RR hub", Location.plus(location, 10, -10, 0), this);
	   LRhub = new Hub("LR hub", Location.plus(location, -10, 10, 0), this);
	   LFhub.setTire(world.LFtire);
      RFhub.setTire(world.RFtire);
      RRhub.setTire(world.RRtire);
      LRhub.setTire(world.LRtire);
	} 
	
	// package copy constructor
	Car (Car car, CarWorld world) { 
	   super(car.name, car.location, null);
	   RFhub = new Hub(car.RFhub, this, world);
	   LFhub = new Hub(car.LFhub, this, world);
	   RRhub = new Hub(car.RRhub, this, world);
	   LRhub = new Hub(car.LRhub, this, world);
	}
	
	@Override
	public void setLocation (Location location) {
	   throw new UnsupportedOperationException("Cannot move the car!");
	}
		
	@Override
   public void print (PrintStream stream, String indent) {
		indent = "  " + indent;
		stream.append("." + this.name + "\n");
		stream.append(indent + ".RFhub = ");
		RFhub.print(stream, indent);
		stream.append(indent + ".LFhub = ");
		LFhub.print(stream, indent);
		stream.append(indent + ".RRhub = ");
		RRhub.print(stream, indent);
		stream.append(indent + ".LRhub = ");
		LRhub.print(stream, indent);
	}

}