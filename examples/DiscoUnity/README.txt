DISCO UNITY PACKAGE

This folder contains a release of a Unity package for Disco and an
example project using it.  Its key elements are:

   Disco.unityPackage    import into your project

   MyProject             example project (open in Unity)
     Assets

       DiscoUnity        source folder for Disco.unityPackage

       MyGame.cs         scripts for example game
       MyEntity.cs      
       MyDisco.cs

       MyModel.d4g.xml   source for example task model
       MyModel.xml       loaded task model
       MyModel.properties
       MyModel.translate.properties

  ikvmc-disco.bat        ignore (for development)

NOTES:

(1) Make sure that any project using DiscoUnity has full .NET
compatibility in the player:

  Edit > Project Settings > Player > API Compability > .NET 2.0 [not Subset!]

Otherwise you will get a compilation error about not being able to
access System.Windows.Forms.Control.

(2) Currently (due to a bug in Jint), if you try to invoke a void C#
method from Javascript, you will get an error message, although the
method will actually execute (the problem happens on the return).  The
solution is to define a non-void method (e.g., return null) that calls
the void method.

(3) It is a good idea in general, and particularly with DiscoUnity, to
STOP the UnityEditor before you edit source code in attached MonoDevelop.
Otherwise, you may get a problem with "unregistering of Win32
windows".

(4) The first time the example project is run in a fresh UnityEditor,
it runs much slower than subsequent times.  I believe this is due to
JIT compilation of OpenJDK.

(5) Each time the UnityEditor is stopped and started again (using
"play" button), a new instance of Disco is created and the task
model(s) are loaded in.  Thus you can edit task models without
restarting the UnityEditor.

(6) Disco does not automatically create _toplevel_ goals or terminate
them _before_ they are completed. Toplevel goal management is left to
the programmer using C# code called in the DiscoUnity.UpdateDisco(Disco) method.

The following instance methods of Disco are useful for toplevel goal
management:

   TaskClass getTaskClass (String) : get the task class in the currently 
   	     		  	     loaded libraries whose id is given string

   Plan addTop (TaskClass) : make instance of given task class a toplevel plan

   Plan addTop (Task) : make given task a toplevel plan

   void removeTop (Plan) : remove given toplevel plan

   void clear () : remove all toplevel plans

The following is a useful instance method of TaskClass:

   Task newInstance () : create a new instance

The following are useful instance methods of Task:

   void setSlotValue (String, Object) : set the value of slot with given name to given object	
			     
   Boolean isApplicable () : tests whether task can be started

(7) If you are using the automatic menu generation facilities of Disco,
note that a new toplevel plan can be added via the interpretation
of a Propose.Should utterance ("Let's ..."), and that the current toplevel
plan can be stopped (but not removed) by the interpretation of a
Propose.ShouldNot utterance ("Let's not ...").  

Both of these utterance types are automatically added to the player
menu unless suppressed. (Both are suppressed for tasks whose id starts
with _ or have the @internal property, and the latter via the
@ProposShouldNot property.)

It may also be convenient to use addTop in the 'eval' attribute
of dialogue tree elements, as in:

   <agent text="Is it time to rendezvous?">
     <user text="Yes" eval="$disco.addTop($disco.getTaskClass('MyTop'))"/>
     <user text="Not now"/>
   </agent>

C. Rich 11/24/11
