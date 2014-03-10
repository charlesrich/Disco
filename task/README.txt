Copyright (c) 2008-2009 Charles Rich and Worcester Polytechnic Institute

MIT License: Permission is hereby granted, free of charge, to any person
obtaining a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including without
limitation the rights to use, copy, modify, merge, publish, distribute,
sublicense, and/or sell copies of the Software, and to permit persons to whom
the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
________________________________________________________________

README file for the toplevel directory of the CE Task Engine.

This is an implementation of a "task engine," as depicted in
Figure 4 of the ANSI/CEA-2018 Task Model Description (CE TASK 1.0)
standard, which may be purchased at http://ce.org/cea-2018.

NB: This software requires JRE 1.6 (Java SE 6.0), which includes
JavaScript.

Basically, this system implements a simple command line interface,
edu.wpi.cetask.guide.Guide, which uses the task engine
edu.wpi.cetask.TaskEngine.  In keeping with the position of CEA-2018
not being about interface appearance, this is not a proposal for a
real user interface, but rather a way to demonstrate and debug the
capabilities of the underlying task engine.

For more information, see the enclosed file file Rich2009.pdf, which contains an article
titled "Building Task-Based User Interfaces with ANSI/CEA-2018", which appeared in IEEE
Computer in August 2009 (Vol. 42, No. 8).

The system release archive (zip) file contains the following folder structure:

   java         Java source files directory
     [unnamed package]      GuiExample
     /edu/wpi/cetask        Basic task engine 
                            [note RelaxNG schema resource CEA-2018.rnc]    

     /edu/wpi/cetask/guide  Example applications using task engine
                            [Guide and SpeechGuide]

   bin          Unix shell scripts directory [see README.txt]

   docs         documentation
     /api       				Javadoc documentation director
     CEA-2018.pdf				Specification for standard
     Rich2009.pdf	 			IEEE Computer paper on CETask
	
   lib          library directory
                [note contains pre-compiled system guide.jar] 
                [note XML schema CEA-2018.xsd]
                
   speech       library directory for SpeechGuide 
                [using Sphinx4 and FreeTTS]
                               
   models       Task models directory [see README.txt]
   test         Test script directory [see README.txt]
   disco.jar	 See below
   
   license.terms   Open source license terms
   .classpath   Eclipse configuration file
   .project     Eclipse configuration file

To simply run the precompiled version of the system, add lib/guide.jar
and lib/msv-rng.jar (RelaxNG validation utility) to your classpath and
invoke main class edu.wpi.cetask.guide.Guide.  If you leave guide.jar
in place, you can also run the system without changing your classpath
by launching the jar from the parent directory, i.e.,

     java -jar lib/guide.jar

[Hint: to change the temporary directory where the log file is written
 to someplace more convenient, add a JRE argument, such as -Djava.io.tmpdir=/tmp ]
 
To run the speech-enabled version in place:

     java -mx512m -jar lib/guide.jar -speech

Note that the speech version requires all the jar files in the speech
directory.

*** The best way to familiarize yourself further with CE Task engine
    is to look next at test/README.txt ***    

Please send bug reports to email address below and make sure to
include both the Guide.test file (from directory indicated at
system startup) and all task model files involved.

For Eclipse Users:

You can easily create an Eclipse project (called task) containing the source
by importing the system release archive (zip) file as follows:

          File > Import > General > *Existing...* > Next 
          > Select Archive > Browse > Select All > Finish
          
NB: If you are developing your own system in Eclipse using the CE Task engine
it would be best to make your system be a *separate* project, rather than
adding your code to this project.  You can simply add the task project to the
project dependencies (see Build Path) of your new project.  Note that the exports of
the task project include all the necessary libraries, including those for speech,
but assumes that the task source has been compiled.  If you cannot or do not build
the task project, then add the precompiled lib/guide.jar as a library of
your new project.

rich@wpi.edu

________________________________________________________________

KNOWN DEFICIENCIES in VERSION 1.0 - Released May 20, 2010

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
