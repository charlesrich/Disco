/* Copyright (c) 2010 Philip Hanson, Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.game.gt;

/**
 * Interface for actors who can carry game objects 
 */
public interface ICarry {
	void pickUp (InteractableObject o);
	boolean isCarrying ();
	InteractableObject getCarriedObject();
	InteractableObject dropCarriedObject();
}
