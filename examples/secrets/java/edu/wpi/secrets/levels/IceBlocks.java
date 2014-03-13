/* Copyright (c) 2010 Philip Hanson and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.secrets.levels;

import com.golden.gamedev.GameEngine;
import com.golden.gamedev.object.Timer;
import com.golden.gamedev.object.font.SystemFont;

import edu.wpi.disco.Actor;
import edu.wpi.disco.game.NWayInteraction;
import edu.wpi.disco.game.actions.*;
import edu.wpi.disco.game.gt.*;
import edu.wpi.secrets.objects.*;

import java.awt.*;
import java.util.*;

/**
 * Level for <em>Secrets of the Rime</em> that demonstrates a river crossing
 * 
 * @author Philip Hanson <phanson@wpi.edu>
 */
public class IceBlocks extends Level {
	private Rope rope;
	private CatchRopeTrigger ropeTrigger;
	private SwimTrigger swimTrigger1, swimTrigger2;
	private Timer swimSoundTimer;
	private boolean ropeWasThrown = false;

	public IceBlocks(GameEngine engine, Map<String,Object> world, Map<String,SystemFont> fonts) {
		super(engine, world, fonts);
		levelName = "Ice Blocks";
		swimSoundTimer = new Timer(250);
	}

	@Override
	public void init() {
		super.init();
		
		loadMap("/edu/wpi/secrets/resources/maps/IceBlocks.txt");
		
		player.getLocation().setLocation(1, 8);
		sidekick.getLocation().setLocation(1, 10);

		// areas
		Area island1 = new Area("first island", new Point(6, 8));
		Area island2 = new Area("second island", new Point(19, 11));
		Area farSide = new Area("far side", new Point(29, 8));
		world.put("nearSide", new Area("near side", new Point(1, 8)));
		world.put("island1", island1);
		world.put("island2", island2);
		world.put("farSide", farSide);
		
		// blocks
		IceBlock[] blocks = new IceBlock[] {
			new IceBlock(this, island1, false, false),
			new IceBlock(this, farSide, false, false)
		};
		
		blocks[0].getLocation().setLocation(new Point(2,6));
		blocks[1].getLocation().setLocation(new Point(24,8));
		
		for (int i = 0; i < 2; i++) {
			world.put("block" + Integer.toString(i+1), blocks[i]);
			objects.add(blocks[i]);
		}
		
		ropeTrigger = new CatchRopeTrigger(this);
		ropeTrigger.getLocation().setLocation(12, 12);
		world.put("ropetrigger", ropeTrigger);
		objects.add(ropeTrigger);
		
		rope = new Rope(this);
		rope.getLocation().setLocation(19, 12);
		world.put("rope", rope);
		objects.add(rope);
		
		swimTrigger1 = new SwimTrigger(this, (Area)world.get("nearSide"), island1);
		swimTrigger1.getLocation().setLocation(island1.getWalkToLocation().x - 3, island1.getWalkToLocation().y);
		objects.add(swimTrigger1);
		swimTrigger2 = new SwimTrigger(this, island2, farSide);
		swimTrigger2.getLocation().setLocation(farSide.getWalkToLocation().x - 3, farSide.getWalkToLocation().y);
		objects.add(swimTrigger2);

		// associate blocks with areas
		island1.setIceBlock(blocks[0]);
		farSide.setIceBlock(blocks[1]);
		
		walkableTiles = new ArrayList<Integer>(Arrays.asList(new Integer[] {
				1, 3, 5, 11, 13, 15, 21, 23, 25, 31, 33, 34, 35, 41, 42
		}));
		
		loadModel(player.getPlayer(), sidekick.getNPC(), "edu/wpi/secrets/resources/models/IceBlocks.xml");
		player.getPlayer().doAction(new LevelReadyAction(world));
	}
	
	@Override
	public void update(long elapsedTime) {
		super.update(elapsedTime);
		
		if (swimTrigger1.isActive()) swimTrigger1.getLocation().y = player.getLocation().y;
		if (swimTrigger2.isActive()) swimTrigger2.getLocation().y = player.getLocation().y;
		
		// ice blocks
		IceBlock block;
		if (mapTiles[3][8] % 2 == 0) {
			block = (IceBlock) world.get("block1");
			if (block != null && block.isInWater()) {
				for (int x = 3; x <= 5; x++) {
					for (int y = 8; y <= 10; y++) {
						mapTiles[x][y] = map.getTiles()[x][y] + 1;
					}
				}
				swimTrigger1.deactivate();
				playSound("/edu/wpi/secrets/resources/effects/slide-2.wav");
			}
		}
		if (mapTiles[26][8] % 2 == 0) {
			block = (IceBlock) world.get("block2");
			if (block != null && block.isInWater()) {
				for (int x = 26; x <= 28; x++) {
					for (int y = 8; y <= 10; y++) {
						mapTiles[x][y] = map.getTiles()[x][y] + 1;
					}
				}
				swimTrigger2.deactivate();
				playSound("/edu/wpi/secrets/resources/effects/slide-2.wav");
			}
		}
		
		if (swimSoundTimer.action(elapsedTime) &&
				((mapTiles[player.getLocation().x][player.getLocation().y] == 12) ||
				 (mapTiles[sidekick.getLocation().x][sidekick.getLocation().y] == 12))) {
			playSound("/edu/wpi/secrets/resources/effects/Swim.wav");
		}
	}
	
	@Override
	public void render(Graphics2D g) {
		super.render(g);
		
		if (rope.isThrown()) {
			g.setColor(Color.LIGHT_GRAY);
			g.drawLine(rope.getLocation().x*20, rope.getLocation().y*20+5, ropeTrigger.getLocation().x*20, ropeTrigger.getLocation().y*20+5);
			if (!ropeWasThrown) {
				ropeWasThrown = true;
				playSound("/edu/wpi/secrets/resources/effects/Rope.wav");
			}
		} else if(ropeWasThrown) {
			ropeWasThrown = false;
			playSound("/edu/wpi/secrets/resources/effects/GrabRope.wav");
		}
	}
	
	//
	// Triggers
	//
	
	private class SwimTrigger extends Trigger {
		private Area from, to;
		{ triggerAction = "swim"; }
		public SwimTrigger(GTLevel level, Area from, Area to) {
			super(level);
			this.from = from;
			this.to = to;
			activate();
		}
		
		@Override
		public void trigger(Map<String,Object> world, NWayInteraction interaction, Actor a) {
			HashMap<String, Object> slots = new HashMap<String, Object>();
			slots.put("from", from);
			slots.put("to", to);
			interaction.getPlayer().doAction(new ExecuteTaskAction(a, "urn:secrets.wpi.edu:models:IceBlocks", "Swim", slots));
			playSound("/edu/wpi/secrets/resources/effects/Swim.wav");
		}
	}
	
	private class CatchRopeTrigger extends Trigger {
		{ triggerAction = "grab rope"; }
		public CatchRopeTrigger(GTLevel level) {
			super(level);
			deactivate();
		}
		
		@Override
		public void trigger(Map<String,Object> world, NWayInteraction interaction, Actor a) {
			interaction.getPlayer().doAction(new ExecuteTaskAction(a, "urn:secrets.wpi.edu:models:IceBlocks", "CatchRope", null));
			playSound("/edu/wpi/secrets/resources/effects/GrabRope.wav");
		}
	}
}
