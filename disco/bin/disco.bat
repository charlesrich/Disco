@ECHO OFF
java -cp "%~d1%~p0..\class;%~d1%~p0..\..\task\class;%~d1%~p0..\..\task\lib/msv-rng.jar" edu.wpi.disco.Disco "%1"
