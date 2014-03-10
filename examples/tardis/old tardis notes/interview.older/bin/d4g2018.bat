ECHO OFF
SETLOCAL

:: Usage: d4g2018 <basename> [ <external> ]
:: 
:: Transforms file  <basename>.d4g.xml in D4g format to <basename>.xml in ANSI/CEA-2018 format.  Also validates syntax of source file.
:: Optional argument is name of actor that will be 'external' (defaults to player).

IF {%1}=={} ECHO "Usage: d4g2018 <basename> [ <external> ]" & GOTO :end

SET folder=%~d1%~p0

ECHO Validating %1.d4g.xml...
java -jar "%folder%..\lib\jing.jar" -c "%folder%..\models\schemas\d4g.rnc" %1.d4g.xml || GOTO :end

IF NOT {%2}=={} SET param="external=%2"

ECHO Transforming %1.d4g.xml to %1.xml...
java -jar "%folder%..\lib\saxon9he.jar" -o:%1.xml %1.d4g.xml "%folder%d4g.xslt" %param%

ECHO Validating %1.xml...
java -jar "%folder%..\lib\jing.jar" -c  "%folder%..\models\schemas\cea-2018-ext.rnc" %1.xml

:end
