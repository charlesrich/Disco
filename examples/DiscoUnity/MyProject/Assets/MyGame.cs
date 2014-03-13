using UnityEngine;

// This class holds information about the game state that needs to be accessed
// from Javascript in the task model.  

// Note: In order to access this class from Jint (C# Javascript interpreter) 
// it must be in a namespace, but Unity does not currently allow scripts
// that extend MonoBehavior to use namespaces.  Therefore this is a separate class
// from MyDisco.

namespace My {

	public class Game
	{
		// see initializations in MyDisco
		public static MyEntity NPC, PLAYER, GREEN_CUBE, RED_CUBE;
		
		// for use in Disco shell, since Unity does not allow ToString on
		// threads other than main
		public static string ToString (MyEntity entity) {
			return entity.myName;
		}
	}	
}


