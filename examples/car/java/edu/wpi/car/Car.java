package edu.wpi.car;

import java.io.PrintStream;

public class Car extends PhysObj {
   
	public final Hub RFhub, LFhub, RRhub, LRhub; 

	public Car (CarWorld world) { 
	   super("My Car", null, world);
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
	   super(car.name, car.location, world);
	   RFhub = new Hub(car.RFhub, this);
	   LFhub = new Hub(car.LFhub, this);
	   RRhub = new Hub(car.RRhub, this);
	   LRhub = new Hub(car.LRhub, this);
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

	// equals and hashCode generated by Eclipse
	
   @Override
   public boolean equals (Object obj) {
      if ( this == obj )
         return true;
      if ( !super.equals(obj) )
         return false;
      if ( getClass() != obj.getClass() )
         return false;
      Car other = (Car) obj;
      if ( LFhub == null ) {
         if ( other.LFhub != null )
            return false;
      } else if ( !LFhub.equals(other.LFhub) )
         return false;
      if ( LRhub == null ) {
         if ( other.LRhub != null )
            return false;
      } else if ( !LRhub.equals(other.LRhub) )
         return false;
      if ( RFhub == null ) {
         if ( other.RFhub != null )
            return false;
      } else if ( !RFhub.equals(other.RFhub) )
         return false;
      if ( RRhub == null ) {
         if ( other.RRhub != null )
            return false;
      } else if ( !RRhub.equals(other.RRhub) )
         return false;
      return true;
   }
   
   @Override
   public int hashCode () {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((LFhub == null) ? 0 : LFhub.hashCode());
      result = prime * result + ((LRhub == null) ? 0 : LRhub.hashCode());
      result = prime * result + ((RFhub == null) ? 0 : RFhub.hashCode());
      result = prime * result + ((RRhub == null) ? 0 : RRhub.hashCode());
      return result;
   }


}