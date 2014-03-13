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
using Disco.  You do not need to install these if you don't want to.

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
