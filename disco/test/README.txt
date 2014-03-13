README file for disco/test directory (see also task/test).

This directory contains test scripts and corresponding log files.  For
each test script, e.g., ABC, the corresponding log file (if there is
one) is ABC.test.  Reading the log files is a good way to learn about
Disco.

The Library3 and Library4 scripts are from a class exercise on task
modeling---reading these is a very good way to get started.

The 'game' subdirectory contains some test scripts to be run with the
game scaffolding classes in the unnamed package (see toplevel
README.txt), which uses the GameExample main class (see bin/game).

You will also want to look at the model files that are loaded. The
other scripts are for miscellaneous development regression testing
(cf. bin/all shell command).

*** See models/README.txt regarding editing your own task models. ***

Test scripts are also *very* useful for the process of developing
(debugging) your own task models.  Note that you don't have to end the
script with 'quit' unless you are automatically going to run a batch
of them (with bin/all).

To run a test script, make the 'disco' directory (parent of this
directory) be the current working directory and either:

* provide the filename as the argument to the 'source' command in Disco
  command line

* provide the filename as a parameter to the main method of
  edu.wpi.disco.Disco, e.g., 

     java -jar disco.jar <filename>

* use the bin/disco shell command (see command file for
  documentation), which automatically diff's the resulting temporary
  log with the corresponding stored log.

rich@wpi.edu

