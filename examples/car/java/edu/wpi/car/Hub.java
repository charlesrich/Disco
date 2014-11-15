package edu.wpi.car;

import java.io.PrintStream;

public class Hub extends PhysObj {

   public final Stud studA, studB, studC;
   	
	private Tire tire;

	public Tire getTire () { return tire; }

	public void setTire (Tire tire) { this.tire = tire; }

	public Hub (String name, Location location, Car parent) {
		super(name, location, parent);
		studA = new Stud(name+" stud A", Location.plus(location, 0, 0, 1), this);
		studB = new Stud(name+" stud B", Location.plus(location, 0, 0, -1), this);
		studC = new Stud(name+" stud C", Location.plus(location, 0, 0, 2), this);
	}

	// package copy constructor
	Hub (Hub hub, Car parent, CarWorld world) {
	   super(hub.name, hub.location, parent);
	   studA = new Stud(hub.studA, this);
	   studB  = new Stud(hub.studB, this);
	   studC = new Stud(hub.studC, this);
	   tire = hub.getTire() == world.LFtire ? world.LFtire :
	      hub.getTire() == world.RFtire ? world.RFtire :
	         hub.getTire() == world.RRtire ? world.RRtire :
	            hub.getTire() == world.LRtire ? world.LRtire :
	               null;
	   if ( tire == null && hub.tire != null ) throw new IllegalStateException("Copy problem!");
	}
	
	@Override
   public void setLocation (Location location) {
      throw new UnsupportedOperationException("Cannot move a hub!");
   }
	
	@Override
   public void print (PrintStream stream, String indent) {
		indent = "  " + indent;
		stream.append(this.name + "\n");
		stream.append(indent + ".studA = ");
		studA.print(stream, indent);
		stream.append(indent + ".studB = ");
		studB.print(stream, indent);
		stream.append(indent + ".studC = ");
		studC.print(stream, indent);
		if ( tire != null) {
			stream.append(indent + "getTire() = ");
			tire.print(stream, indent);
		}
	}

}
