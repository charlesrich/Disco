/* Copyright (c) 2010 Philip Hanson, Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.game.gt;

import com.golden.gamedev.*;
import com.golden.gamedev.object.*;
import com.golden.gamedev.object.Timer;
import com.golden.gamedev.object.background.TileBackground;
import com.golden.gamedev.object.font.SystemFont;

import edu.wpi.cetask.Utils;
import edu.wpi.disco.Actor;
import edu.wpi.disco.game.*;
import edu.wpi.disco.game.actions.*;
import edu.wpi.disco.game.gt.MapLoader.MapFormatException;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Base class for a single level. 
 */
public abstract class GTLevel extends GameObject {
	
   protected final Map<String,Object> world;
	protected final NWayInteraction nway; 
	protected final GTPlayer player;
	protected final GTNPC sidekick;
	protected final List<GTNPC> npcs = new LinkedList<GTNPC>();
	protected final List<EmbodiedObject> objects = new LinkedList<EmbodiedObject>();
	
	protected MapLoader map;
	protected  int[][] mapTiles;
	protected Sprite titleCard;
	private TileBackground bg;
	
	private final Map<Actor,InteractableObject> currentObjects = new HashMap<Actor, InteractableObject>();
	protected List<Integer> walkableTiles = null;
	private Timer npcMoveTimer;
	
	private List<String> ttSayList;
	private SystemFont ttSayFont;
	private SystemFont actionMenuFont;
	private SystemFont levelNameFont;
	private String ttSaySelectSound = "/edu/wpi/disco/game/gt/click.wav";
	protected double conversationDistance = 15;
	
	protected String levelName;
	
	public GTPlayer getPlayer () { return player; }

	public GTLevel (GameEngine engine, Map<String,Object> world, Map<String,SystemFont> fonts) {
		super(engine);
		
		this.world = world;
		NPC npc = new NPC("sidekick");
		nway =  new NWayInteraction(world, new Player("player"), npc, null, false);
		player = new GTPlayer(nway.getPlayer(), this);
		world.put("player", player);
		sidekick = new GTSidekick(npc, this);
		npcs.add(sidekick);
		world.put("sidekick", sidekick);
		
		engine.nextGameID++;
		
		if (fonts != null) {  // using fonts==null as a 'testing' flag
			npcMoveTimer = new Timer(250);
			ttSayFont = fonts.get("TTSay");
			actionMenuFont = fonts.get("ActionMenu");
			levelNameFont = fonts.get("LevelName");
		}
	}
	
	@Override
	public BufferedImage getImage (String imagefile) {
	   return bsLoader == null ? null : super.getImage(imagefile); 
	}
	
	@Override
	public BufferedImage getImage (String imagefile, boolean useMask) {
		return bsLoader == null ? null: super.getImage(imagefile, useMask);
	}

	@Override
	public BufferedImage[] getImages (String imagefile, int col, int row) {
	   return bsLoader == null ? null : super.getImages(imagefile, col, row);
	}
	
	protected void loadMap (String mapFile) {
		try {
			map = new MapLoader(mapFile);
		}
		catch (IOException e) {
			System.err.println("IO exception accessing map");
		}
		catch (MapFormatException e) {
			System.err.println("Bad map file: " + e.getCause());
		}
		
		mapTiles = map.getTiles();
		BufferedImage[] bgimg = getImages(map.getTilesetFilename(), map.getTilesetX(), map.getTilesetY());
		if (bgimg != null) {
			bg = new TileBackground(bgimg, mapTiles);
		}
	}
	
	protected void loadModel (Actor a, Actor b, String filename) {
		player.getPlayer().doAction(new LoadModelAction(a, b, "edu/wpi/disco/game/gt/GT.xml"));
		player.getPlayer().doAction(new LoadModelAction(a, b, filename));
	}
	
	@Override
	public final void initResources () {
      init();
      new Thread(nway, levelName).start(); // start AI loop
	}
	
	protected abstract void init ();

	@Override
	public void render (Graphics2D g) {
		if (world.get("readyFlag") == null) {
			if ( titleCard != null ) titleCard.render(g);
			return;
		}
		
		if ( bg == null ) g.clearRect(0, 0, getWidth(), getHeight()); 
		else bg.render(g);
		
		int lineSpacing = 4; // 4 pixels between lines
		
		for (EmbodiedObject o : objects) {
			o.render(g);
			if ((o instanceof InteractableObject) && ((InteractableObject)o).shouldShowActions()) {
				// TODO expand beyond one action
				if (((InteractableObject)o).getActions().size() > 0) {
					String s = ((InteractableObject)o).getActions().get(0);
					int x, y, width, height;
					width = actionMenuFont.getWidth(s) + 8;
					height = actionMenuFont.getHeight() + lineSpacing + 2;
					x = (o.getLocation().x * 20 + 10) - (width / 2);
					y = (o.getLocation().y * 20 + 10) - (height / 2);
					// draw menu background
					g.setColor(Color.GREEN.darker().darker());
					g.fillRect(x, y, width, height);
					// draw text
					g.setColor(Color.WHITE);
					actionMenuFont.drawString(g, s, x + 2, y + 2);
				}
			}
		}
		
		for (GTNPC npc : npcs) {
			npc.render(g);
		}
		
		player.render(g);
		
		// NPC dialogue boxes
		int msgHeight, msgWidth, lines;
		int msgX, msgY;
		String msg;
		for (GTNPC npc : npcs) {
			if (npc.getNPC().hasUtterance()) {
				msg = npc.getNPC().getUtterance();
				msgWidth = ttSayFont.getWidth(msg);
				lines = msgWidth / getWidth() + 1;
				msgHeight = lines * (ttSayFont.getHeight() + lineSpacing);
				
				g.setColor(npc.getBgColor());
				if ( map == null ) { msgX = msgY = 30;	} 
				else {
				   // center over npc
				   int npcX = (int)npc.getLocation().getX() * (getWidth() / mapTiles.length);
				   int npcY = (int)npc.getLocation().getY() * (getHeight() / mapTiles[0].length);
				   
				   if (msgWidth > getWidth()) msgWidth = getWidth();
				
				   msgX = npcX - (msgWidth / 2) + ((getWidth() / mapTiles.length) / 2);
				   if (msgX < 0) msgX = 0;
				   if (msgX + msgWidth > getWidth()) msgX = getWidth() - msgWidth;
				
				   msgY = npcY - (getHeight() / mapTiles[0].length / 2) - msgHeight;
				   if (msgY < 0) msgY = npcY + 2*(getHeight() / mapTiles[0].length);
				   if (msgY + msgHeight > getHeight()) msgY = 0;
				}
				
				g.fillRoundRect(msgX-2, msgY-2, msgWidth+8, msgHeight+2, 5, 5);
				g.setColor(npc.getFgColor());
				ttSayFont.drawText(g, Utils.capitalize(msg), GameFont.LEFT, msgX, msgY, msgWidth*2, 0, 0);
				
				// TODO add divot pointing to avatar
				// TODO prevent from covering other actors
			}
		}
		
		// TTSay list
		if (ttSayList != null && ttSayList.size() > 0) {
			g.setColor(Color.WHITE);
			g.fillRoundRect(8, 324 + ttSayFont.getHeight() + 7, 624,
					(ttSayFont.getHeight() + 2) * ttSayList.size() + 4,
					2, 2);
			g.setColor(Color.BLACK);
			int j = 0;
			while ((ttSayList != null) && ttSayList.size() > j) {
				try {
					ttSayFont.drawText(g,
							Integer.toString(j + 1) + " " + Utils.capitalize(ttSayList.get(j)),
							GameFont.LEFT, 10, 324 + ttSayFont.getHeight() + 5
									+ 2 + (j * (ttSayFont.getHeight() + 2)),
							620, 0, 0);
				} catch(IndexOutOfBoundsException e) {}
				j++;
			}
		}
		
		if ((levelName != null) && (levelNameFont != null)) {
			g.setColor(Color.LIGHT_GRAY);
			g.fillRect(3, 3, levelNameFont.getWidth(levelName) + 3, levelNameFont.getHeight());
			g.setColor(Color.BLACK);
			levelNameFont.drawString(g, levelName, 5, 3);
		}
	}

	@Override
	public void update (long elapsedTime) {
		// quit?
		if (keyPressed(KeyEvent.VK_ESCAPE)) {
			parent.nextGameID = -1;
			endLevel();
		}
		
		if (world.get("readyFlag") == null) return;
		
		// drop to debug mode?
		if (keyPressed(KeyEvent.VK_D)) {
			player.getPlayer().doAction(new PauseDebugAction());
		}
		
		// end if we should end
		if (levelCompleted()) { endLevel(); }
		
		// player movement
		int pdx = 0, pdy = 0;
		if (keyPressed(KeyEvent.VK_RIGHT)) { pdx =  1; }
		if (keyPressed(KeyEvent.VK_LEFT)) {  pdx = -1; }
		if (keyPressed(KeyEvent.VK_UP)) {    pdy = -1; }
		if (keyPressed(KeyEvent.VK_DOWN)) {  pdy =  1; }
		if (pdx != 0 || pdy != 0) {
			Point pp = player.getLocation();
			if (validateMove(pp.x + pdx, pp.y + pdy, player.getPlayer()) && player.isMovable()){
				if (sidekick.getLocation().equals(new Point(pp.x + pdx, pp.y + pdy))) {
					sidekick.getLocation().setLocation(pp);
				}
				player.getLocation().setLocation(pp.x + pdx, pp.y + pdy);
				
				for (GTNPC npc : npcs) {
					if (player.getLocation().distance(npc.getLocation()) < conversationDistance) {
						if (!nway.isActive(npc.getNPC())) {
							// activate
							player.getPlayer().doAction(new AddActorAction(npc.getNPC()));
						}
					} else {
						if (nway.isActive(npc.getNPC())) {
							// deactivate
							player.getPlayer().doAction(new RemoveActorAction(npc.getNPC()));
						}
					}
				}
				
				// invalidate AI liveness flags, as movement can change pre/postcondition variables
				nway.tick();
			} 
		}
		
		// object action selection
		InteractableObject co;
		co = currentObjects.get(player.getPlayer());
		if (co != null && co.getActions().size() == 1) {
			co.doAction(0, world, nway, player.getPlayer());
		}
		
		// TTSay selection
		if (ttSayList != null) {
			if (keyPressed(KeyEvent.VK_1) && ttSayList.size() >= 1) {
				chooseTTSayOption(1);
				playSound(ttSaySelectSound);
			} else if (keyPressed(KeyEvent.VK_2) && ttSayList.size() >= 2) {
				chooseTTSayOption(2);
				playSound(ttSaySelectSound);
			} else if (keyPressed(KeyEvent.VK_3) && ttSayList.size() >= 3) {
				chooseTTSayOption(3);
				playSound(ttSaySelectSound);
			} else if (keyPressed(KeyEvent.VK_4) && ttSayList.size() >= 4) {
			   chooseTTSayOption(4);
				playSound(ttSaySelectSound);
			} else if (keyPressed(KeyEvent.VK_5) && ttSayList.size() >= 5) {
				chooseTTSayOption(5);
				playSound(ttSaySelectSound);
			} else if (keyPressed(KeyEvent.VK_6) && ttSayList.size() >= 6) {
				chooseTTSayOption(6);
				playSound(ttSaySelectSound);
			}
		}
		// TTSay update
		ttSayList = player.getPlayer().getTTSayList();
		
		if ( bg != null ) bg.update(elapsedTime);
		player.update(elapsedTime, world);
		for (GTNPC npc : npcs) {
			if (npcMoveTimer.action(elapsedTime)) {
				npcSeekDestination(npc);
			}
			npc.update(elapsedTime, world);
		}
		for (EmbodiedObject o : objects) {
			o.update(elapsedTime, world);
			if (o instanceof InteractableObject) {
				boolean isNear = false;
				for (GTNPC npc : npcs) {
					if (npc.getLocation().distance(o.getLocation()) < 1.9) {
						isNear = true;
						break;
					}
				}
				if (player.getLocation().distance(o.getLocation()) < 1.9) {
					isNear = true;
				}
				((InteractableObject)o).setShowActions(isNear);
			}
		}
	}

   private void chooseTTSayOption (int option) {
      player.getPlayer().doAction(new ChooseTTSayOptionAction(
            nway.getTTSayItems(), option));
   }
   
	/**
	 * End the level gracefully and save an interaction history
	 */
	protected void endLevel() {
		// TODO write interaction history output code
		finish();
	}

	/**
	 * Returns true if level should end.
	 * Derived classes should override for special conditions
	 */
	protected boolean levelCompleted() {
		// end of level == right side of screen
		return (player.getLocation().x == map.getMapX() - 1);
	}
	
	/**
	 * Naive NPC path-finding
	 */
	private void npcSeekDestination(GTNPC npc) {
		if (npc.isMovable() && (npc.getDestination() != null)) {
			Point curr = npc.getLocation();
			Point dest = npc.getDestination();
			Point n = null;
			if (Math.abs(curr.x - dest.x) <= Math.abs(curr.y - dest.y)) {
				if (curr.y != dest.y) {
					n = new Point(curr.x, curr.y + (dest.y - curr.y)/Math.abs(dest.y - curr.y));
					if (validateMove(n.x, n.y, npc.getNPC())) {
						curr.setLocation(n);
					}
				}
			} else if(curr.x != dest.x) {
				n = new Point(curr.x + (dest.x - curr.x)/Math.abs(dest.x - curr.x), curr.y);
				if (validateMove(n.x, n.y, npc.getNPC())) {
					curr.setLocation(n);
				}
			}
		}
	}
	
	/**
	 * @return <code>true</code> if the tile at the given (x,y) location is valid for movement
	 */
	private boolean validateMove (int x, int y, Actor a) {
		if ((a instanceof NPC) && (((NPC)a).isIgnoringObstacles())) return true;
		currentObjects.remove(a);
		java.awt.Point p = new java.awt.Point(x,y);
		if (player.getLocation().equals(p)) return false;
		for (EmbodiedObject o : objects) {
			if (o.getLocation().equals(p)) {
				if (o instanceof InteractableObject) {
					currentObjects.put(a, (InteractableObject)o);
				}
				if (!o.isWalkable()) return false;
			}
		}
		if (!((x < 0) || (y < 0) || (x >= map.getMapX()) || (y >= map.getMapY())) &&
			(walkableTiles != null) && walkableTiles.contains(mapTiles[x][y]))
		{
			for (GTNPC npc : npcs) {
				if (npc.getLocation().equals(p)) 
				   return a == player.getPlayer() && npc == sidekick;
			}
			return true;
		}
		return false;
	}

}
