#!/bin/sh
set -eux

mkdir generated
cd ./generated

echo 'module foo{}' >module-info.java
javac --release 9 module-info.java

jar cf simple.jar module-info.class
jar cf mr.jar --release 9 module-info.class
