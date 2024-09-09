%global debug_package %{nil}
%bcond_with bootstrap

Name:           xmvn-generator
Version:        [...]
Release:        %autorelease
Summary:        RPM dependency generator for Java
License:        Apache-2.0
URL:            https://github.com/fedora-java/xmvn-generator
ExclusiveArch:  %{java_arches}

Source:         https://github.com/fedora-java/xmvn-generator/releases/download/%{version}/xmvn-generator-%{version}.tar.zst

BuildRequires:  gcc
BuildRequires:  rpm-devel
BuildRequires:  lujavrite
%if %{with bootstrap}
BuildRequires:  javapackages-bootstrap
%else
BuildRequires:  maven-local
BuildRequires:  mvn(org.apache.commons:commons-compress)
BuildRequires:  mvn(org.apache.maven.plugins:maven-antrun-plugin)
BuildRequires:  mvn(org.easymock:easymock)
BuildRequires:  mvn(org.junit.jupiter:junit-jupiter)
BuildRequires:  mvn(org.ow2.asm:asm)
%endif

Requires:       rpm-build
Requires:       lujavrite
Requires:       java-21-openjdk-headless

%description
XMvn Generator is a dependency generator for RPM Package Manager
written in Java and Lua, that uses LuJavRite library to call Java code
from Lua.

%{?javadoc_package}

%prep
%autosetup -p1 -C
%mvn_file : %{name}

%build
%mvn_build

%install
%mvn_install
install -D -p -m 644 src/main/lua/xmvn-generator.lua %{buildroot}%{_rpmluadir}/xmvn-generator.lua
install -D -p -m 644 src/main/rpm/macros.xmvngen %{buildroot}%{_rpmmacrodir}/macros.xmvngen
install -D -p -m 644 src/main/rpm/macros.xmvngenhook %{buildroot}%{_sysconfdir}/rpm/macros.xmvngenhook
install -D -p -m 644 src/main/rpm/xmvngen.attr %{buildroot}%{_fileattrsdir}/xmvngen.attr

%files -f .mfiles
%{_rpmluadir}/*
%{_rpmmacrodir}/*
%{_fileattrsdir}/*
%{_sysconfdir}/rpm/*
%license LICENSE NOTICE
%doc README.md

%changelog
%autochangelog
