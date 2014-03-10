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

import java.util.*;

public class Fountain extends InteractableObject {
	
	public Fountain(GTLevel level) {
		super(level);
		
		sprite = new FountainSprite();
		((FountainSprite)sprite).setImages(level.getImages("resources/images/Fountain.png", 2, 1));
		((FountainSprite)sprite).setStatus(0);

		actions.put(0, "hold");
	}
	
	@SuppressWarnings("serial")
	public class FountainSprite extends AdvanceSprite {
		@Override
		protected void animationChanged(int oldStat, int oldDir, int status, int direction) {
			if (status < getImages().length) setAnimationFrame(status, status);
		}
	}

	@Override
	public void doAction(int index, Map<String,Object> world, NWayInteraction interaction, Actor a) {
		// TODO Auto-generated method stub

	}

}
