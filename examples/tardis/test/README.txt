This folder contains sample regression tests.  To run a regression
test (only works for Unix/Mac, for Win see bin/README.txt re Cygwin):

   > ./bin/game test/GoodInterview
     [Agent and User Shake Hands now]
     Succeed GoodInterview (9 sec)

If test fails, you can run diff to look at detailed differences.

Note that you can also use the 'source' command in the Disco debug
shell to read in the test or similar files (especially without the
'quit' at the end), which is very helpful for debugging, e.g.

   > ./bin/game
     Welcome to Disco! (Type 'help' for command list)
     player&sidekick  > source test/Interview.base

Note that the file 'resume.json' is a sample resume.  See code
at end of InterviewTop.xml.
