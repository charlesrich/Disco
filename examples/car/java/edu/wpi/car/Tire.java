package edu.wpi.car;

public class Tire extends PhysObj {

	public Tire (String name, CarWorld world) { 
	   super(name, null, world); 
	}
	
	// package copy constructor
	Tire (Tire tire, CarWorld world) { 
	   super(tire.name, tire.location, world); 
	}

}
