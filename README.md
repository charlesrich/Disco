Disco - Collaborative Discourse Manager

=====

[For information, bug reports, or to just let me know you are using
this code :-), please contact me at email address below - C. Rich]

This is an implementation of a collaborative discourse manager
inspired by the same collaborative discourse theory as Collagen (see
docs).  Disco uses the ANSI/CEA-2018 Task Model Description (CETask
1.0) standard for its task representation and is built on top of the
CETask reference implementation (see 'task' project below).

Disco is intended to be integrated with other systems, such as
appliances, games, robots, etc.  However, for demonstration and
development purposes, it also provides a simple command line
interface, which is an extension of the shell in the CETask
implementation.

   task
   disco
   examples
      callcenter
      DiscoUnity
      secrets
      tardis
      team

The two main toplevel folders, task and disco, are Eclipse Java
projects containing all of the source code for Disco.  The task
project is the reference implementation for CETask; the disco project
depends on it and adds the focus stack and utterances.  The folders
under examples contain Eclipse Java projects for some examples built
using Disco.

Here are highlights of the disco project:

   java         Java source files directory

     edu/wpi/disco          Basic discourse engine
     edu/wpi/disco/lang     Lightweight language semantics [see docs]
     edu/wpi/disco/plugin   Plugins for discourse generation [see docs]
     edu/wpi/disco/game     Disco for Games (D4g) [see docs]
     ComponentExample.java  Example for integrating Disco into larger system

   bin          Unix shell scripts directory [see README.txt]
                (To use these scripts on Windows, install www.cygwin.com)

   docs         Documentation [see also task/docs]
     api                    Javadoc for Disco
     *.pdf                  Relevant papers [see README.txt] 
        
   models       Task models directory [see README.txt]

   test         Test script directory [see README.txt]

   d4g          Libraries and files for use with Disco for Games (D4g)
   
     d4g.rnc                Schema for D4g language
     d4g.xsd                Schema for D4g language
     d4g.xslt               XSLT translation rules for D4g to ANSI/CEA-2018
     golden_0_2_3.jar       Golden T Game Engine class files   
     saxon9he.jar           Saxon XSLT class files
   
   license.terms   Open source MIT license terms
   .classpath   Eclipse (Kepler) configuration file 
   .project     Eclipse (Kepler) configuration file

To simply run the precompiled version of the system, add lib/disco.jar
to your classpath and invoke main class edu.wpi.disco.Disco.  You can
also run the jar file directly as:

NB: This software requires JRE 1.7 (Java SE 7.0), which includes JavaScript.

     java -jar lib/disco.jar

Note this jar file includes all the binary class files in both the
disco and task projects (but not the libraries for disco/d4g or the speech
libraries in task/lib).

[Hint: to change the temporary directory where the log file is written
 to someplace more convenient, add a JRE argument, such as
 -Djava.io.tmpdir=/tmp ]

*** The best way to familiarize yourself further with Disco
    is to look next at test/README.txt ***    

Please send bug reports to bug-disco@wpi.edu and make sure to
include both the Console.test file (from directory indicated at
system startup) and all task model files involved.

For Eclipse Users:

You can easily create the two Eclipse projects (called task and disco)
containing the source by importing the system release archive (zip)
file as follows:

          File > Import > General > *Existing...* > Next 
          > Select Archive > Browse > Select All > Finish
          
NB: If you are developing your own system in Eclipse using Disco it
would be best to make your system be a *separate* project, rather than
adding your code to this project.  You can simply add the task project
to the project dependencies (see Build Path) of your new project.
Note that the exports of the task project include all the necessary
libraries, including those for speech, but assumes that the task
source has been compiled.  If you cannot or do not build the disco
project, then add the precompiled disco.jar as a library of your
new project.

---

Dr. Charles Rich, Professor, Computer Science Department
Interactive Media and Game Development Program
Robotics Engineering Program
Worcester Polytechnic Institute, Fuller Laboratories B25b
100 Institute Road, Worcester, MA 01609-2280

Email: rich@wpi.edu   Phone: 508-831-5945   Fax: 508-831-5776
Home: http://www.cs.wpi.edu/~rich


________________________________________________________________

RELEASE NOTES FOR DISCO 1.6 - Released June, 2012

* Upgraded to JDK 1.7

* Upgraded project files to Eclipse Juno (1.4.2)

RELEASE NOTES FOR DISCO 1.5 - Released June, 2012

* Major speed-up by adding caching.

* Minor bug fixes and improvements.

* New example illustrating mixing D4g and 2018 syntax models/Mixed.d4g.xml

* Refactorization of JavaScript engine interface to support use
  of Jint in Mono, as replacement for Rhino engine built into JDK.

* Ignore the edu.wpi.disco.javaff package, which is under development.

RELEASE NOTES FOR DISCO 1.2 - Released June, 2011

* Multiple bug fixes

* Added debugging feature: Javascript variable $disco is bound globally to the 
  currently running instance of Disco, from which you can conveniently access
  all aspects of discourse state, e.g., in console:

  > eval $disco.getPurpose().setExternal(true)
  
* "Random" variations in text strings produced by system: Task formatting 
  templates (@format properties), translation tables (in .translate.properties
  files) and text strings in <say> elements of D4g all now support a new
  syntax for specifying alternatives, e.g., "yes|sure|yah".  Alternatives
  are automatically chosen in rotation (with last used alternative stored
  in user model).

* Disco for Games (D4g), as described in docs/HansonRich2010_AIIDE.pdf,
  with following extensions (see disco/models/Knock.d4g.xml):

  (1) Javascript variables in text strings, e.g., 

     <say text="Foo bar {script1} gritch{script2}!"/>
     ->
     <step name="_d1e4_step" task="disco:edu.wpi.disco.lang.Say"/>
     <binding slot="$_d1e4_step.text" 
              value="'Foo bar '
                     +(script1)
                     +' gritch'
                     +(script2)
                     +'!'"/>

  (2) Utterance side-effects (Javascript evaluation)

      <say text="..." eval="script1"/>
      ->
      <step name="_d1e4_step" task="disco:edu.wpi.disco.lang.SayEval"/>
      <binding slot="$_d1e4_step.text" value="...">
      <binding slot="$_d1e4_step.eval" value="script1">

  (3) Applicability conditions

      <say ... applicable="script1"/>
         ...
      <say ... applicable="script2"/>
         ...
      ->
      <subtasks id="...">
        <step ... task="disco.edu.wpi.disco.lang.Say"/>
        ...
        <applicable>script1</applicable>
        ...
      </subtasks>
      <subtasks id="...">
        <step ... task="disco.edu.wpi.disco.lang.Say"/>
        <applicable>script2</applicable>
        ...
      </subtasks>

      and similarly for <do>

  (4) Convenience syntax for agent vs. user

      <agent ... /> -> <say external="false" ... />
      <user ... /> -> <say external="true" ... />

________________________________________________________________

KNOWN DEFICIENCIES in RELEASE 1.1 - Released July, 2010

-more error checking could be added
-unbounded maxOccurs not supported (approximates 10)
-user intent concepts (Section 11.4) not implemented
-side effect notation (Section 8.6) not tested--note need to cache copy of input
 when value used in postcondition
-$execute, $getModel and $occurred  functions not implemented
-grounding queries (Section 10.2) not implemented
-speech grammar (.gram) and properties files (.properties) should be
 internationalized using resource bundles
-import scheme for automatically loading speech grammar files (.gram) associated
 with task models only works for one task model at a time (due to limitation in
 JSGFGrammar only supporting one grammar at a time)
-many "TODO" notes scattered in source code, of varying priority

________________________________________________________________
