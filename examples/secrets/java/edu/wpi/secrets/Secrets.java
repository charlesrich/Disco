/* Copyright (c) 2010 Philip Hanson and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.secrets;

import com.golden.gamedev.*;
import edu.wpi.disco.game.GameConsole;
import edu.wpi.disco.game.gt.GTGame;
import edu.wpi.secrets.levels.*;
import java.awt.Dimension;
import java.util.*;

/**
 * Main class for <em>Secrets of the Rime</em>.
 * 
 * @author Philip Hanson <phanson@wpi.edu>
 */
public class Secrets extends GTGame {

	@Override
	public GameObject getGame (int id) {
	   Map<String,Object> world = new HashMap<String,Object>();
	   switch (id) {
	   case 0:
			return new IceBlocks(this, world, fonts);
		case 1:
			return new IceWall(this, world, fonts);
		case 2:
			return new Shelter(this, world, fonts);
		case 3:
			return new WalrusCave(this, world, fonts);
		default:
			throw new IllegalArgumentException("Invalid level number: "+id);
		}
	}
	
	public static void main (String[] args) throws NumberFormatException {
	   System.setProperty("java.awt.headless", "false"); // make sure can have window
		GameLoader game = new GameLoader();
		game.setup(new Secrets(), new Dimension(640, 480), false);
		((GameEngine) game.getGame()).nextGameID = 
		   args.length > 0 ? Integer.parseInt(args[0]) : 0; 
		game.start();
	}

	public static class Console {
	   
	   public static void main (String[] args) {
	      System.setProperty("java.awt.headless", "false"); // make sure can have window
	      GameConsole.main(args);
	   }
	}
	 
}
