rem Run MoleculeViewer

rem set CLASSPATH=c:\code\external\MoleculeViewer\jar\MoleculeViewerObf.jar

rem set JAVA=c:\jdk1.1.8\bin
rem %JAVA%\java -mx1000000000 astex.MoleculeViewer %1 %2 %3 %4 %5

rem this format for newer java interpreters
set JAVA=c:\j2sdk1.4.2_16\bin
%JAVA%\java -Xmx1000000000 -jar c:\code\external\MoleculeViewer\jar\MoleculeViewer.jar %1 %2 %3 %4 %5


