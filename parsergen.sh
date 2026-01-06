#! /usr/bin/bash
rm -rf src/main/java/duongtran/vctrl/cli/parser
jjtree VctrlParser.jjt
javacc src/main/java/duongtran/vctrl/cli/parser/VctrlParser.jj
rm -f src/main/java/duongtran/vctrl/cli/parser/VctrlParser.jj