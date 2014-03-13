/* Copyright (c) 2010 Philip Hanson, Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.game.gt;

import com.golden.gamedev.object.Sprite;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;

public class EmbodiedObject {
   
   protected final Point location = new Point();
	protected Sprite sprite;
	protected boolean movable, walkable;
	private int tileSize = 20;
	protected final GTLevel level;
	
	public EmbodiedObject (GTLevel level) {
	   this.level = level;
	}

   public Point getLocation () { return location; }
   
	public void setTileSize (int tileSize) {
		this.tileSize = tileSize;
	}
	
	/**
	 * Assign a specific image to this object as a Sprite
	 * @param filename Relative or absolute path to image
	 */
	public void setImage (BufferedImage image) {
		if (sprite == null) {
			sprite = new Sprite(image);
		} else {
			sprite.setImage(image);
		}
	}

	public Sprite getSprite () {
		return sprite;
	}
	
	/**
	 * Called on each game loop cycle to update object state.
	 * <p>
	 * Derived classes can override this method to add specific functionality
	 * such as triggers, time-based activities, or physics.
	 */
	public void update (long elapsedTime, Map<String,Object> world) {
		if (sprite != null) {
			sprite.setLocation(location.getX() * tileSize, location.getY()
					* tileSize);
			sprite.update(elapsedTime);
		}
	}

	public void render (Graphics2D g) {
		if (sprite != null) {
			sprite.render(g);
		}
	}
	
	public boolean isMovable () {
		return movable;
	}
	
	public void setMovable (boolean m) {
		movable = m;
	}
	
	/**
	 * @return <code>true</code> if object can be walked through/on.
	 */
	public boolean isWalkable () {
		return walkable;
	}
}
