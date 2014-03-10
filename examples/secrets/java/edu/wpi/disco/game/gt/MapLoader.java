/* Copyright (c) 2010 Philip Hanson, Charles Rich and Worcester Polytechnic Institute.
 * All Rights Reserved.  Use is subject to license terms.  See the file
 * "license.terms" for information on usage and redistribution of this
 * file and for a DISCLAIMER OF ALL WARRANTIES.
 */
package edu.wpi.disco.game.gt;

import java.io.*;
import java.net.URL;

/**
 * Loads a map definition file exported from Tile Studio
 */
public class MapLoader {
	private enum ReadState {
		Tileset,
		MapSize,
		MapData,
		Done
	}
	
	private String tileSetFile = null;
	private int tilesetX = 0, tilesetY = 0;
	private int mapX = 0, mapY = 0;
	private int[][] map;
	
	private int currRow = 0;
	
	public MapLoader( String mapFile) throws IOException, MapFormatException { loadMap(mapFile); }
	
	public String getTilesetFilename () { return tileSetFile; }
	public int getTilesetX () { return tilesetX; }
	public int getTilesetY () { return tilesetY; }
	
	public int getMapX () { return mapX; }
	public int getMapY () { return mapY; }
	public int[][] getTiles () { return map; }
	
	private void loadMap (String mapFile) throws IOException, MapFormatException {
		BufferedReader in = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(mapFile)));
		String line;
		ReadState state = ReadState.Tileset;  // first line always tileset info
		
		while((line = in.readLine()) != null && state != ReadState.Done) {
			try {
				state = processLine(line, state);
			} catch (IllegalArgumentException e) {
				in.close();
				throw new MapFormatException(e);
			}
		}
		
		in.close();
	}
	
	private ReadState processLine (String line, ReadState state) {
		switch (state) {
		case Tileset:
			getTilesetData(line);
			return ReadState.MapSize;
		case MapSize:
			getMapSize(line);
			return ReadState.MapData;
		case MapData:
			return getMapData(line);
		case Done:
			break;
		}
		return ReadState.Done;
	}

	private void getTilesetData (String line) {
		// tileset line is like:
		// <image file name> <columns> <rows>
		
		String[] tokens = line.split("\\s+");
		if (tokens.length != 3) throw new IllegalArgumentException("Tileset line has wrong number of tokens");
		
		URL ts = getClass().getResource(tokens[0]);
		if (ts == null) throw new IllegalArgumentException("Tileset does not exist");
		
		tileSetFile = new String(tokens[0]);
		try {
			tilesetX = Integer.parseInt(tokens[1]);
			tilesetY = Integer.parseInt(tokens[2]);
		} catch(NumberFormatException e) {
			throw new IllegalArgumentException("Bad number in tileset info", e);
		}
	}

	private void getMapSize (String line) {
		// map size line is like:
		// <mapX> <mapY>
		
		String[] tokens = line.split("\\s+");
		if (tokens.length != 2) throw new IllegalArgumentException("Map size line has wrong number of tokens");
		
		try {
			mapX = Integer.parseInt(tokens[0]);
			mapY = Integer.parseInt(tokens[1]);
		} catch(NumberFormatException e) {
			throw new IllegalArgumentException("Bad number in map size", e);
		}
		
		map = new int[mapX][mapY];
	}
	
	private ReadState getMapData (String line) {
		// data should be like:
		// <tilenum> <tilenum> ...
		// forming a mapX x mapY grid
		
		String[] tokens = line.trim().split("\\s+");
		if (tokens.length != mapX) throw new IllegalArgumentException("Map data array does not match map size parameters");
		
		for (int currColumn = 0; currColumn < mapX; currColumn++) {
			map[currColumn][currRow] = Integer.parseInt(tokens[currColumn]);
		}
		
		currRow++;
		
		return currRow == mapY ? ReadState.Done : ReadState.MapData;
	}
	
	@SuppressWarnings("serial")
	public class MapFormatException extends Exception {
		public MapFormatException(Exception e) {
			super(e);
		}
	}
}
