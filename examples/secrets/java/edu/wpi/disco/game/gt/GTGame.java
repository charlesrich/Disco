/* Copyright (c) 2010 Philip Hanson, Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.game.gt;

import com.golden.gamedev.*;
import com.golden.gamedev.object.font.SystemFont;

import edu.wpi.disco.game.actions.LevelReadyAction;

import java.awt.*;
import java.util.*;

/**
 * Base class for Golden T Game.  Can be instantiated for testing.
 * For example, try with models/Knock.xml.
 */
public class GTGame extends GameEngine {

   // { distribute = true; }
   
   protected final Map<String, SystemFont> fonts = new HashMap<String, SystemFont>();
   {
      fonts.put("LevelName", new SystemFont(new Font("Arial", Font.PLAIN, 16)));
      fonts.put("TTSay", new SystemFont(new Font("Arial", Font.PLAIN, 12)));
      fonts.put("ActionMenu", new SystemFont(new Font("Arial", Font.PLAIN, 12)));
   }
   
   private String model;  // for testing only
   
   @Override
   public GameObject getGame (int id) {
   
      return new GTLevel(this, new HashMap<String,Object>(), fonts) {
         
         { levelName = "Testing"; }
         
         @Override
         public void init () {
            loadModel(player.getPlayer(), sidekick.getNPC(), model);
            player.getPlayer().doAction(new LevelReadyAction(world));
         }
         @Override
         protected boolean levelCompleted() { return false; }
      };
   }
   
   public static void main (String[] args) {
      GameLoader loader = new GameLoader();
      GTGame game = new GTGame();
      game.model = args[0];
      loader.setup(game, new Dimension(640, 480), false);
      loader.start();
   }

}
