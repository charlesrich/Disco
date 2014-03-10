This folder contains three model source files as an example for use in
the TARDIS project:

  Interview.d4g.xml - dialogue trees for steps in InterviewTop.
  InterviewTop.xml - toplevel goals for interview
  Coach.d4g.xml - dialogue trees for interview coaching interventions

These files should be loaded into Disco in the _order_ above.

In order to use the *.d4g.xml files above, they must first be
translated to *.xml files using the d4g2018 or d4g2018.bat command
provided in disco/bin, e.g.,

  > ./bin/d4g2018 models/Interview

Note that the following two files are derived files and should _not_
be edited directly:

  Interview.xml
  Coach.xml

The schemas folder contains XML schemas for use with 
