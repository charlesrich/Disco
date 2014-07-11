/* Copyright (c) 2010 Philip Hanson and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.secrets.levels;

import com.golden.gamedev.GameEngine;
import com.golden.gamedev.object.Sprite;
import com.golden.gamedev.object.font.SystemFont;

import java.util.Map;

import edu.wpi.disco.game.gt.GTLevel;

public abstract class Level extends GTLevel {
   
   public Level(GameEngine engine, Map<String,Object> world, Map<String,SystemFont> fonts) {
      super(engine, world, fonts);
   }
   
   @Override
   protected void init () {
      sidekick.setDestination(player.getLocation());
      titleCard = new Sprite(getImage("/edu/wpi/secrets/resources/images/TitleCard.png"));
      titleCard.setLocation(0, 0);
   }
}

