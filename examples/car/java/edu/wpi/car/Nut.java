package edu.wpi.car;

public class Nut extends PhysObj {

	public Nut (String name, CarWorld world) { 
	   super(name, null, world);
	}
	
	// package copy constructor
	Nut (Nut nut, CarWorld world) { 
	   super(nut.name, nut.location, world); 
	}
	
}