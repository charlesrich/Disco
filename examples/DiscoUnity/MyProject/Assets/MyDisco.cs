using UnityEngine;
using System.Collections;
using System;

// A very simple example of how to display NPC utterance and player menus
public class MyDisco : DiscoUnity 
{
	
	override public void Awake () 
	{
		base.Awake(); // don't forget this!
	    // See MyGame.cs
		My.Game.NPC = GetEntity("MyNPC");
		My.Game.PLAYER = GetEntity("MyPlayer");
		My.Game.GREEN_CUBE= GetEntity("GreenCube");
		My.Game.RED_CUBE = GetEntity("RedCube");
		// cubes don't move
		My.Game.GREEN_CUBE.Sense();
		My.Game.RED_CUBE.Sense();
	}
	
	private static MyEntity GetEntity (string name) {
		return GameObject.Find(name).GetComponent<MyEntity>();
	}
	
	// Sensing: since Unity will not allow access to any game state information
	// on a non-Unity thread, this method is used to periodically copy information
	// to cached object(s).  For example, here is where you would model field of
	// of view, etc.
	
	override protected void Sensing () 
	{
		My.Game.NPC.Sense();
	    My.Game.PLAYER.Sense();
	}
	
	// Dialogue handling
	
	// Note: Specify models to load a Start in Models field in Inspector
	//       (comma-separated list of filenames)
	
	private String say = "";

	// overriding Say to write NPC utterance on the screen
	// currently utterance stays there until the NPC or the player next says 
	// something next (see below).  You might also clear it after some fixed
	// amount of time or if some specific kind of action happens
	override protected void Say (String utterance) 
	{
		say = utterance;
	}
	
	private String[] choices;
	
	override protected void UpdateMenu (String[] choices) 
	{
		this.choices = choices;	
	}
	
	void OnGUI () 
	{   
		// show NPC utterance
		GUI.Label(new Rect(20,20,500,100), say);
		
		if ( choices != null && choices.Length > 5 ) 
			Debug.Log("WARNING: Ignoring menu items after fifth!");
		
		// a simple fixed menu layout with maximum of 5 choices
	    if ( GUI.Button(new Rect(20, 250, 500, 20), GetChoice(0)) )
			choose(choices, 0);
		if ( GUI.Button(new Rect(20, 275, 500, 20), GetChoice(1)) )
			choose(choices, 1);
		if ( GUI.Button(new Rect(20, 300, 500, 20), GetChoice(2)) )
			choose(choices, 2);
		if ( GUI.Button(new Rect(20, 325, 500, 20), GetChoice(3)) )
		    choose(choices, 3);
		if ( GUI.Button(new Rect(20, 350, 500, 20), GetChoice(4)) )
			choose(choices, 4);
    }
	
	private void choose (String[] choices, int chosen) {
		// note calling MenuChoice method of superclass
		MenuChoice(choices, chosen);
		say = "";
	}
	
	private String GetChoice (int i)
	{
	   	return choices == null || i >= choices.Length ? "" : choices[i];
	}
}
