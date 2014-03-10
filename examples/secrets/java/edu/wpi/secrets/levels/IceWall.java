/* Copyright (c) 2010 Philip Hanson and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.secrets.levels;

import com.golden.gamedev.GameEngine;
import com.golden.gamedev.object.font.SystemFont;

import edu.wpi.disco.Actor;
import edu.wpi.disco.game.NWayInteraction;
import edu.wpi.disco.game.actions.*;
import edu.wpi.disco.game.gt.*;
import edu.wpi.secrets.objects.Door;

import java.util.*;

/**
 * Level for <em>Secrets of the Rime</em> that demonstrates climbing over a wall cooperatively
 * 
 * @author Philip Hanson <phanson@wpi.edu>
 */
public class IceWall extends Level {
	// these are to keep track of when to play the sounds
	private boolean climbed = false;
	private boolean clambered = false;
	private boolean opened = false;
	private ClimbableWall climbableWall;
	private ClamberTrigger clamberTrigger;
	private Door door;
	
	public IceWall(GameEngine engine, Map<String,Object> world, Map<String,SystemFont> fonts) {
		super(engine, world, fonts);
		levelName = "The Ice Wall";
	}
	
	@Override
	public void init() {
	   super.init();
		loadMap("/edu/wpi/secrets/resources/maps/IceWall.txt");
		
		player.getLocation().setLocation(1, 8);
		sidekick.getLocation().setLocation(1, 10);
		
		door = new Door(this);
		door.getLocation().setLocation(16, 10);
		objects.add(door);
		world.put("door", door);
		
		climbableWall = new ClimbableWall(this);
		climbableWall.getLocation().setLocation(15, 6);
		objects.add(climbableWall);
		world.put("climbableWall", climbableWall);
		
		clamberTrigger = new ClamberTrigger(this);
		clamberTrigger.getLocation().setLocation(16, 6);
		objects.add(clamberTrigger);
		world.put("clamberTrigger", clamberTrigger);
		
		walkableTiles = new ArrayList<Integer>(Arrays.asList(new Integer[] {
				4, 34, 39
		}));
		
		loadModel(player.getPlayer(), sidekick.getNPC(), "edu/wpi/secrets/resources/models/IceWall.xml");
		player.getPlayer().doAction(new LevelReadyAction(world));
	}
	
	@Override
	public void update(long elapsedTime) {
		super.update(elapsedTime);
		
		if (!climbed && !climbableWall.isActive()) {    // deactivated by climbing
			climbed = true;
			playSound("/edu/wpi/secrets/resources/effects/Climb.wav");
		}
		if (!clambered && !clamberTrigger.isActive()) { // deactivated by clambering
			clambered = true;
			playSound("/edu/wpi/secrets/resources/effects/Slide.wav");
		}
		if (!opened && door.isOpen()) {
			opened = true;
			playSound("/edu/wpi/secrets/resources/effects/DoorOpen.wav");
		}
	}
	
	//
	// Triggers
	//
	
	public class ClamberTrigger extends Trigger {
		{ triggerAction = "clamber"; }
		public ClamberTrigger(GTLevel level) {
			super(level);
			activate();
		}
		
		@Override
		public void trigger(Map<String,Object> world, NWayInteraction interaction, Actor a) {
			interaction.getPlayer().doAction(new ExecuteTaskAction(a, "urn:secrets.wpi.edu:models:IceWall", "Clamber", null));
		}
		
		public void clamber() { deactivate(); }
	}
	
	public class ClimbableWall extends Trigger {
		{ triggerAction = "climb"; }
		public ClimbableWall(GTLevel level) {
			super(level);
			activate();
		}
		
		@Override
		public void trigger(Map<String,Object> world, NWayInteraction interaction, Actor a) {
			interaction.getPlayer().doAction(new ExecuteTaskAction(a, "urn:secrets.wpi.edu:models:IceWall", "Boost", null));
		}
		
		public void climb() { deactivate(); }
	}
	
}
