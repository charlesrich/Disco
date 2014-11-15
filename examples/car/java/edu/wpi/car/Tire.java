package edu.wpi.car;

public class Tire extends PhysObj {

	public Tire (String name) { super(name, null, null); }
	
	// package copy constructor
	Tire (Tire tire) { super(tire.name, tire.location, null); }

}
