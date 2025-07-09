[![build status](https://img.shields.io/github/actions/workflow/status/fedora-java/xmvn-generator/ci.yml?branch=master)](https://github.com/fedora-java/xmvn-generator/actions/workflows/ci.yml?query=branch%3Amaster)
[![License](https://img.shields.io/github/license/fedora-java/xmvn-generator.svg?label=License)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central version](https://img.shields.io/maven-central/v/org.fedoraproject.xmvn/xmvn-generator.svg?label=Maven%20Central)](https://search.maven.org/artifact/org.fedoraproject.xmvn/xmvn-generator)
![Fedora Rawhide version](https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fmdapi.fedoraproject.org%2Frawhide%2Fpkg%2Fxmvn-generator&query=%24.version&label=Fedora%20Rawhide)
[![Javadoc](https://javadoc.io/badge2/org.fedoraproject.xmvn/xmvn-generator/javadoc.svg)](https://javadoc.io/doc/org.fedoraproject.xmvn/xmvn-generator)


XMvn Generator
==============

XMvn Generator is a [dependency
generator](https://rpm-software-management.github.io/rpm/manual/dependency_generators.html)
for [RPM Package Manager](https://rpm.org/).  It is written in Java
and Lua.  It uses [LuJavRite](https://github.com/mizdebsk/lujavrite/)
library to call Java code from Lua.

XMvn Generator is free software. You can redistribute and/or modify it
under the terms of Apache License Version 2.0.

XMvn Generator was written by Mikolaj Izdebski.
