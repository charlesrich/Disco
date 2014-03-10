using UnityEngine;
using System.Collections;

public class MyEntity : MonoBehaviour {
	
	// cache name for printing in Disco, because Unity only allows 
	// access to these fields from Unity thread (see My.Game.ToString)
	public string myName;
	
	void Awake () {	myName = name; }
	
	// trivial sensing
	
	public Vector3 myPosition;
	
	public void Sense () // called on Unity thread (see MyDisco.Sensing)
	{
		myPosition = gameObject.transform.position;
	}
	
	// trivial navigation

	public Transform destination;
	
	void FixedUpdate () 
	{  
		if ( destination != null )
			transform.position = Vector3.MoveTowards(transform.position, destination.position, 50*Time.deltaTime);
	}
	
	void OnTriggerEnter(Collider other)
	{   
		destination = null;
	}
}	
	
	
	