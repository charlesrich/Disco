package edu.wpi.car;

public class Nut extends PhysObj {

	public Nut (String name) { super(name, null, null); }
	
	// package copy constructor
	Nut (Nut nut) { super(nut.name, nut.location, null); }
	
}