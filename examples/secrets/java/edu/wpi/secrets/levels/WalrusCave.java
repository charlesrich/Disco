/* Copyright (c) 2010 Philip Hanson and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.secrets.levels;

import com.golden.gamedev.GameEngine;
import com.golden.gamedev.object.Timer;
import com.golden.gamedev.object.font.SystemFont;

import edu.wpi.disco.game.NPC;
import edu.wpi.disco.game.actions.*;
import edu.wpi.disco.game.gt.GTNPC;
import edu.wpi.secrets.objects.*;

import java.awt.Color;
import java.util.*;

/**
 * Level for <em>Secrets of the Rime</em> that demonstrates multiple-actor interactions
 * 
 * @author Philip Hanson <phanson@wpi.edu>
 */
public class WalrusCave extends Level {
	private final int dripTimeVariation = 2000;
	private final int minDripTime = 400;
	private Timer dripTimer;
	private Random prng;
	private final String[] dripSounds = {
			"/edu/wpi/secrets/resources/effects/water-droplet-1.wav",
			"/edu/wpi/secrets/resources/effects/water-droplet-2.wav"
	};
	
	public WalrusCave(GameEngine engine, Map<String,Object> world, Map<String,SystemFont> fonts) {
		super(engine, world, fonts);
		levelName = "Walrus Cave";
		prng = new Random();
	}

	@Override
	public void init() {
		super.init();
		
		loadMap("/edu/wpi/secrets/resources/maps/WalrusCave.txt");
		
		player.getLocation().setLocation(0, 5);
		sidekick.getLocation().setLocation(0, 6);
		
		GTNPC walrus = new GTNPC(new NPC("walrus"), this);
		walrus.getLocation().setLocation(30, 8);
		walrus.setImage(getImage("/edu/wpi/secrets/resources/images/Walrus.png"));
		walrus.setBgColor(Color.BLACK);
		walrus.setFgColor(Color.WHITE);
		world.put("walrus", walrus);
		npcs.add(walrus);
		
		Window window = new Window(this);
		window.getLocation().setLocation(14, 15);
		objects.add(window);
		world.put("caveWindow", window);
		
		Fountain fountain = new Fountain(this);
		fountain.getLocation().setLocation(14, 12);
		objects.add(fountain);
		world.put("fountain", fountain);
		
		/*  modeling not finished
		WalrusStatue statue = new WalrusStatue(this);
		statue.getLocation().setLocation(15, 4);
		objects.add(statue);
		world.put("statue", statue);
		
		Diamond diamond = new Diamond(this);
		diamond.getLocation().setLocation(-4, -4);
		objects.add(diamond);
		world.put("diamond", diamond);
		*/
		
		walkableTiles = new ArrayList<Integer>(Arrays.asList(new Integer[] {
				4, 12, 34, 39
		}));
		
		player.getPlayer().doAction(new AddActorAction(walrus.getNPC()));
		loadModel(player.getPlayer(), sidekick.getNPC(), "edu/wpi/secrets/resources/models/WalrusCave-PS.xml");
		loadModel(player.getPlayer(), walrus.getNPC(), "edu/wpi/secrets/resources/models/WalrusCave-PW.xml");
		loadModel(sidekick.getNPC(), walrus.getNPC(), "edu/wpi/secrets/resources/models/WalrusCave-SW.xml");
		player.getPlayer().doAction(new RemoveActorAction(walrus.getNPC()));
		player.getPlayer().doAction(new LevelReadyAction(world));
		
		dripTimer = new Timer(minDripTime+prng.nextInt(dripTimeVariation));
		dripTimer.setActive(true);
	}
	
	@Override
	public void update(long elapsedTime) {
		super.update(elapsedTime);
		
		if (dripTimer.action(elapsedTime)) {
			playSound(dripSounds[(int)elapsedTime % dripSounds.length]);
			dripTimer.setDelay(minDripTime+prng.nextInt(dripTimeVariation));
		}
	}

}
