@ECHO OFF
SETLOCAL

:: Usage: d4g2018 <basename> [ <external> ]
:: 
:: Transforms file  <basename>.d4g.xml in D4g format to <basename>.xml in ANSI/CEA-2018 format. 
:: Also validates syntax of source file.
:: Optional argument is name of actor that will be 'external' (defaults to player).

IF {%1}=={} ECHO "Usage: d4g2018 <basename> [ <external> ]" & GOTO :end

SET folder="%~d1%~p0.."
:: sic do not add quotes below
SET wd=%CD%

CD %folder%

ECHO Validating %1.d4g.xml...
java -cp "%folder%\lib\disco.jar" "edu.wpi.disco.Disco$Validate" "%wd%\%1.d4g.xml" "http://www.cs.wpi.edu/~rich/d4g" || GOTO :end

IF NOT {%2}=={} SET param="external=%2"

ECHO Transforming %1.d4g.xml to %1.xml...
java -jar "%folder%\d4g\saxon9he.jar" -o:"%wd%\%1.xml" "%wd%\%1.d4g.xml" "%folder%\d4g\d4g.xslt" %param%

ECHO Validating %1.xml...
java -cp "%folder%\lib\disco.jar" "edu.wpi.disco.Disco$Validate" "%wd%\%1.xml" "http://www.cs.wpi.edu/~rich/cetask/cea-2018-ext"

:end
