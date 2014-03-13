/* Copyright (c) 2010 Philip Hanson and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.secrets.levels;

import com.golden.gamedev.GameEngine;
import com.golden.gamedev.object.font.SystemFont;

import edu.wpi.disco.game.actions.LevelReadyAction;
import edu.wpi.secrets.objects.*;

import java.awt.Point;
import java.util.*;

/**
 * Level for <em>Secrets of the Rime</em> that demonstrates 
 * 
 * @author Philip Hanson<phanson@wpi.edu>
 */
public class Shelter extends Level {
	private Shack shack;

	public Shelter(GameEngine engine, Map<String,Object> world, Map<String,SystemFont> fonts) {
		super(engine, world, fonts);
		levelName = "Shelter";
	}
	
	@Override
	public void init () {
		super.init();
		
		loadMap("/edu/wpi/secrets/resources/maps/Shelter.txt");
		
		player.getLocation().setLocation(0, 5);
		sidekick.getLocation().setLocation(0, 6);

		Panel[] panels = new Panel[4];
		Point[] panelLocations = new Point[] {
			new Point(10, 7), new Point(14, 8), new Point(15, 7), new Point(18, 3)	
		};
		for (int i = 0; i < panels.length; i++) {
			panels[i] = new Panel(this);
			panels[i].getLocation().setLocation(panelLocations[i]);
			objects.add(panels[i]);
			world.put("panel" + (i+1), panels[i]);
		}
		
		IceShard shard1 = new IceShard(this);
		shard1.getLocation().setLocation(5, 2);
		objects.add(shard1);
		world.put("shard1", shard1);
		IceShard shard2 = new IceShard(this);
		shard2.getLocation().setLocation(19, 6);
		objects.add(shard2);
		world.put("shard2", shard2);
		
		shack = new Shack(this);
		shack.getLocation().setLocation(13, 4);
		objects.add(shack);
		world.put("shack", shack);
		
		walkableTiles = new ArrayList<Integer>(Arrays.asList(new Integer[] {
				4, 12, 34, 39
		}));
		
		loadModel(player.getPlayer(), sidekick.getNPC(), "edu/wpi/secrets/resources/models/Shelter.xml");
		player.getPlayer().doAction(new LevelReadyAction(world));
	}
	
	@Override
	protected boolean levelCompleted() {
		return super.levelCompleted() && shack.hasRoof();
	}

}
