Copyright (c) 2014 Charles Rich and Worcester Polytechnic Institute

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

README file for the toplevel directory of Disco.

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

NB: This is pure Java software and is supported only in Java 8 (JRE
1.8) which includes the Nashorn JavaScript (ECMAScript) engine. Due to
a bug in early versions of Nashorn, this software requires Java
Version 8u60 or higher.  It has been tested with Java SE 8u60 in the
Windows x64, Mac OS X x64 and Linux x64 releases from Oracle
(java.oracle.com). OpenJDK Java 8 releases are not available at time
of this writing.

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
