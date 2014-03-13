/* Copyright (c) 2010 Philip Hanson and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.secrets.objects;

import com.golden.gamedev.object.sprite.AdvanceSprite;

import edu.wpi.disco.Actor;
import edu.wpi.disco.game.*;
import edu.wpi.disco.game.gt.*;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.*;

public class Window extends InteractableObject {
	private boolean open;
	
	public Window(GTLevel level) {
		super(level);
		actions.put(0, "Open window");
		
		BufferedImage[] images = new BufferedImage[2];
		images[0] = level.getImage("resources/images/Window-Closed.png");
		images[1] = level.getImage("resources/images/Window-Open.png");
		
		sprite = new WindowSprite();
		((WindowSprite)sprite).setImages(images);
		((WindowSprite)sprite).setStatus(0);
	}
	
	public synchronized void open () {
		open = true;
	}
	
	public synchronized void close () {
		open = false;
	}
	
	public boolean isOpen () {
		return open;
	}

	@Override
	public void doAction (int index, Map<String,Object> world, NWayInteraction interaction, Actor a) {
		switch(index) {
		case 0:
			open();
			break;
		default:
			break;
		}
	}

	@Override
	public Map<Integer, String> getActions() {
		return actions;
	}
	
	@Override
	public void update(long elapsedTime, Map<String,Object> world) {
		// trigger: close window when there is no one to hold it open
		if (open && !((EmbodiedObject) world.get("player")).getLocation().equals(
						new Point(location.x, location.y - 1))) {
			close();
		}
		
		// change image on boundary conditions
		if (open && ((WindowSprite)sprite).getStatus() == 0) {
			((WindowSprite)sprite).setStatus(1);
		} else if (!open && ((WindowSprite)sprite).getStatus()== 1) {
			((WindowSprite)sprite).setStatus(0);
		}
		super.update(elapsedTime, world);
	}
	
	@SuppressWarnings("serial")
	private class WindowSprite extends AdvanceSprite {
		@Override
		protected void animationChanged(int oldStat, int oldDir, int status, int direction) {
			// status = index of image to use
			if (status < getImages().length) setAnimationFrame(status, status);
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.wpi.disco.game.gt.InteractableObject#shouldShowActions()
	 */
	@Override
	public boolean shouldShowActions() {
		return !open && super.shouldShowActions();
	}

}
