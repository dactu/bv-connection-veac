#!/bin/bash
echo "compile source code: $PWD"
mvn compile
echo "compile source code done!"
mvn package
echo "do package source code done!"
