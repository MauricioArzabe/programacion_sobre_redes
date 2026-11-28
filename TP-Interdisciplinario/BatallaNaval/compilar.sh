#!/bin/sh
# Compila el proyecto y genera dist/BatallaNaval.jar (Linux / VM Debian / Alpine)
rm -rf out dist && mkdir -p out dist
javac --release 17 -encoding UTF-8 -d out $(find src -name "*.java")
jar cf dist/BatallaNaval.jar -C out .
echo "Listo: dist/BatallaNaval.jar"
