/* Copyright (c) 2010 Philip Hanson and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.secrets.objects;

import com.golden.gamedev.object.sprite.AdvanceSprite;

import edu.wpi.disco.Actor;
import edu.wpi.disco.game.*;
import edu.wpi.disco.game.actions.*;
import edu.wpi.disco.game.gt.*;

import java.util.*;
import java.lang.Object;

/**
 * Shack that the player and sidekick make in the Shelter level
 * 
 * @author Philip Hanson <phanson@wpi.edu>
 */
public class Shack extends InteractableObject {

	private int status;
	private int attempt;
	
	private final static int NONE    =  0;
	private final static int LPILLAR =  1;
	private final static int RPILLAR =  2;
	private final static int LWALL   =  4;
	private final static int TWALL   =  8;
	private final static int RWALL   = 16;
	private final static int ROOF    = 32;
		
	public Shack(GTLevel level) {
		super(level);

		status = 0;
		attempt = 0;
		
		sprite = new ShackSprite();
		((ShackSprite)sprite).setImages(level.getImages("resources/images/Shack.png", 5, 5));
	}
	
	public boolean hasWall(String placement) {
		if (placement.equals("left")) {
			return (status & Shack.LWALL) > 0;
		} else if (placement.equals("right")) {
			return (status & Shack.RWALL) > 0;
		} else if (placement.equals("top")) {
			return (status & Shack.TWALL) > 0;
		}
		return false;
	}
	
	public boolean hasRoof() {
		return (status & Shack.ROOF) > 0;
	}
	
	public boolean hasPillar(String placement) {
		if (placement.equals("left")) {
			return (status & Shack.LPILLAR) > 0;
		} else if (placement.equals("right")) {
			return (status & Shack.RPILLAR) > 0;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see edu.wpi.disco.game.gt.InteractableObject#doAction(int, edu.wpi.disco.game.World, edu.wpi.disco.game.NWayInteraction, edu.wpi.disco.Actor)
	 */
	@Override
	public void doAction(int index, Map<String,Object> world, NWayInteraction interaction, Actor a) {
		if ((index != 0) || !((GTPlayer)world.get("player")).getPlayer().equals(a)) return;
		
		Action n = null;
		Object carried = ((GTPlayer)world.get("player")).getCarriedObject();
		if (carried instanceof Panel) {
			HashMap<String, Object> slots = new HashMap<String, Object>();
			if (!hasWall("left")) {
				if ((attempt & Shack.LWALL) != 0) { return; }
				attempt |= Shack.LWALL;
				slots.put("placement", "left");
			} else if (!hasWall("top")) {
				if ((attempt & Shack.TWALL) != 0) { return; }
				attempt |= Shack.TWALL;
				slots.put("placement", "top");
			} else if (!hasWall("right")) {
				if ((attempt & Shack.RWALL) != 0) { return; }
				attempt |= Shack.RWALL;
				slots.put("placement", "right");
			} else if (!hasRoof()) {
				if ((attempt & Shack.ROOF) != 0) { return; }
				attempt |= Shack.ROOF;
				slots.put("placement", "roof");
			} else {
				return;  // nothing left to place!
			}
			slots.put("panel", carried);
			n = new ExecuteTaskAction(a, "urn:secrets.wpi.edu:models:Shelter", "PlacePanel", slots);
		} else if (carried instanceof IceShard) {
			HashMap<String, Object> slots = new HashMap<String, Object>();
			if (!hasPillar("left")) {
				if ((attempt & Shack.LPILLAR) != 0) { return; }
				attempt |= Shack.LPILLAR;
				slots.put("placement", "left");
			} else if (!hasPillar("right")) {
				if ((attempt & Shack.RPILLAR) != 0) { return; }
				attempt |= Shack.RPILLAR;
				slots.put("placement", "right");
			} else {
				return;  // all pillars accounted for
			}
			slots.put("shard", carried);
			n = new ExecuteTaskAction(a, "urn:secrets.wpi.edu:models:Shelter", "PlaceShard", slots);
		}
		
		// note that the final 'else's above should be impossible, as there are only 4 panels and 2 ice shards...
		
		if (n != null) interaction.getPlayer().doAction(n);
	}
	
	public void place(String placement, IceShard shard) {
		if (placement.equals("left")) {
			status |= Shack.LPILLAR;
		} else if (placement.equals("right")) {
			status |= Shack.RPILLAR;
		}
		((ShackSprite)sprite).setStatus(status);
	}
	
	public void place(String placement, Panel panel) {
		if (placement.equals("left")) {
			status |= Shack.LWALL;
		} else if (placement.equals("top")) {
			status |= Shack.TWALL;
		} else if (placement.equals("right")) {
			status |= Shack.RWALL;
		} else if (placement.equals("roof")) {
			status |= Shack.ROOF;
		}
		((ShackSprite)sprite).setStatus(status);
	}

	@Override
	public Map<Integer,String> getActions() {
		actions.clear();
		Object carried = level.getPlayer().getCarriedObject();
		if (carried != null) {
			if (carried instanceof Panel) {
				if (!hasWall("left") || !hasWall("top") || !hasWall("right")) {
					actions.put(0, "place wall");
				} else if (!hasRoof()) {
					actions.put(0, "place roof");
				}
			} else if (carried instanceof IceShard) {
				if (!hasPillar("left") || !hasPillar("right")) {
					actions.put(0, "place pillar");
				}
			}
		}
		return actions;
	}
	
	@SuppressWarnings("serial")
	private class ShackSprite extends AdvanceSprite {
		@Override
		protected void animationChanged(int oldStat, int oldDir, int status, int direction) {
			int frame = 0;
			// yes, there's a more intelligent way of doing this.
			// I don't care.
			switch (status) {
			case Shack.NONE:
				frame = 0;
				break;
			case Shack.LPILLAR:
				frame = 10;
				break;
			case Shack.RPILLAR:
				frame = 11;
				break;
			case Shack.LWALL:
				frame = 17;
				break;
			case Shack.TWALL:
				frame = 18;
				break;
			case Shack.RWALL:
				frame = 19;
				break;
			case Shack.LPILLAR + Shack.RPILLAR:
				frame = 9;
				break;
			case Shack.LWALL + Shack.TWALL:
				frame = 14;
				break;
			case Shack.LWALL + Shack.RWALL:
				frame = 15;
				break;
			case Shack.TWALL + Shack.RWALL:
				frame = 16;
				break;
			case Shack.LWALL + Shack.TWALL + Shack.RWALL:
				frame = 13;
				break;
			case Shack.LWALL + Shack.TWALL + Shack.LPILLAR + Shack.RPILLAR:
				frame = 4;
				break;
			case Shack.LWALL + Shack.RWALL + Shack.LPILLAR + Shack.RPILLAR:
				frame = 3;
				break;
			case Shack.TWALL + Shack.RWALL + Shack.LPILLAR + Shack.RPILLAR:
				frame = 5;
				break;
			case Shack.LWALL + Shack.TWALL + Shack.RWALL + Shack.LPILLAR:
				frame = 2; // oops
				break;
			case Shack.LWALL + Shack.TWALL + Shack.RWALL + Shack.RPILLAR:
				frame = 2; // oops
				break;
			case Shack.LWALL + Shack.TWALL + Shack.RWALL + Shack.LPILLAR + Shack.RPILLAR:
				frame = 2;
				break;
			case Shack.LWALL + Shack.TWALL + Shack.RWALL + Shack.LPILLAR + Shack.RPILLAR + Shack.ROOF:
				frame = 1;
				break;
			case Shack.LWALL + Shack.TWALL + Shack.RWALL + Shack.ROOF:
				frame = 12;
				break;
			}
			setAnimationFrame(frame, frame);
		}
	}

}
