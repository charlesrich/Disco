README file for task/test directory.

This directory contains test scripts and corresponding log files.  For
each test script, e.g., ABC, the corresponding log file (if there is
one) is ABC.test.  Reading the log files is a good way to learn about
the CE Task Engine.   

Test scripts ABC, AnnexB and TailRecursion are examples from the
CEA-2018 standard document.  The Library1, Library2 and Library3
scripts are from a class exercise on task modeling---reading these is
a very good way to get started.  

You will also want to look at the model files that are loaded. The
other scripts are for miscellaneous development regression testing
(cf. bin/all shell command).

*** See models/README.txt regarding editing your own task models. ***

Test scripts are also *very* useful for the process of developing
(debugging) your own task models.  Note that you don't have to end the
script with 'quit' unless you are automatically going to run a batch
of them (with bin/all).

To run a test script, make the 'task' directory (parent of this
directory) be the current working directory and either:

* provide the filename as the argument to the 'source' command in task
  engine command line

* provide the filename as a parameter to the main method of
  edu.wpi.cetask.Guide, e.g., 

     java -jar lib/guide.jar <filename>

* use the bin/guide shell command (see command file for
  documentation), which automatically diff's the resulting temporary
  log with the corresponding stored log.

rich@wpi.edu
1/10/08
