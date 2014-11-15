package edu.wpi.car;

import java.io.PrintStream;

// NB: PhysObj must cloneable because it is used in modified inputs

public class PhysObj implements Cloneable {

	// final fields public for convenience in JavaScript
	public final String name;
	public final PhysObj parent;

	protected Location location;
	
	public Location getLocation () { return location; }

	public void setLocation(Location location) { this.location = location; }
	
	public void setLocation(int x,int y,int z) { this.location = new Location(x,y,z); }

	public PhysObj (String name, Location location, PhysObj parent) {
		this.name = name;
		this.location = location == null ? new Location(0, 0, 0) : location;
		this.parent = parent;
	}

   @Override
   public Object clone() {
      try { return super.clone(); } // default method will copy the fields above
      catch (CloneNotSupportedException e) { return null; }// cannot happen
   }
   
   public void print (PrintStream stream, String indent) {
      stream.append(name + "\n");
   }
   
	/**  
	 * Two physical objects are equal iff they have the same names or they are identical
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PhysObj other = (PhysObj) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

}
