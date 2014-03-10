echo "MAKE SURE YOU HAVE REBUILT DISCO.JAR!"
..\..\disco\mono\ikvm-0.46.0.1\bin\ikvmc -r:MyProject\Assets\DiscoUnity\lib\Jint.dll -r:MyProject\Assets\DiscoUnity\lib\Antlr3.Runtime.dll -r:mscorlib.dll ..\disco\lib\disco.jar -target:library -out:MyProject\Assets\DiscoUnity\lib\disco.dll
