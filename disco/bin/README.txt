README file for disco/bin directory.

This directory contains convenient Unix shell scripts for use with
developing task models. The main command is:

     disco - run Disco (debug console version)

The following two commands are for use with Disco for Games (D4g):

     d4g2018 - transform given .d4g.xml file to .xml file in CEA 2018
     game - run GTGame

The following two commands are for regression testing:

    testall - run all Disco regression tests
    testd4g - run all D4g regression test

See individual files for more detailed descriptions.

To use these scripts on Windows, install Cygwin (www.cygwin.com).  Also, if you want to use
an Cygwin bash shell from within Emacs, here are some installation hints:

* Add following to ~/.emacs

  (setq explicit-shell-file-name "c:\\cygwin\\bin\\bash.exe")

* Add following to ~/.bashrc

  export PS1="\w> "

* Add c:\cygwin\bin to the Windows PATH environment variable

* Set the value of the Windows CYGWIN environment variable to "nodosfilewarning"
  
rich@wpi.edu

