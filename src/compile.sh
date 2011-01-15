export CLASSPATH=.:../lib
javac -J-mx100m -target 1.6 java_cup/*.java java_cup/runtime/*.java JLex/*.java

java -server -Xmx100000000 java_cup.Main < astex/parser/parser.cup
java JLex.Main astex/parser/parser.lex
mv astex/parser/parser.lex.java astex/parser/Yylex.java
mv parser.java astex/parser/parser.java
mv sym.java astex/parser/sym.java
find . -name '*.j' | parallel 'cpp -P -C < {} > {}ava'
javac -Xlint:deprecation -target 1.6 MoleculeViewerApplet.java
jar c0f ../MoleculeViewer.jar java_cup/runtime/*.class astex/generic/*.class astex/anasurface/*.class astex/parser/*.class astex/*.class astex/design/*.class *.properties images/textures/*.jpg images/*.jpg fonts/* thinlet/*.class astex/splitter/*.class astex/thinlet/*.class astex/thinlet/*.properties astex/thinlet/*.gif MoleculeViewerApplet.class
jar u0f ../MoleculeViewer.jar -C ../lib nanoxml
jar u0f ../MoleculeViewer.jar -C ../lib jclass
jar u0fm ../MoleculeViewer.jar AstexViewer.manifest
