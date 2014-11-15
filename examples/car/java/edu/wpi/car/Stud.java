package edu.wpi.car;

import java.io.PrintStream;

public class Stud extends PhysObj {

   private Nut nut;
   
   public Nut getNut () { return nut; }

   public void setNut (Nut nut) { this.nut = nut; }
   
	public Stud (String name, Location location, Hub parent) {
		super(name, location, parent);
		setNut(new Nut(name+" nut"));
	}

	// package copy constructor
	Stud (Stud stud, Hub parent) {
	   super(stud.name, stud.location, parent);
	   if ( stud.nut != null ) nut = new Nut(stud.nut);
	}

	@Override
   public void setLocation (Location location) {
      throw new UnsupportedOperationException("Cannot move a stud!");
   }
	
	@Override
   public void print (PrintStream stream, String indent) {
		indent = indent + "  ";
		stream.append(this.name + "\n");
		if ( nut != null ) {
			stream.append(indent + "getNut() = ");
			nut.print(stream, indent);
		}
	}

}