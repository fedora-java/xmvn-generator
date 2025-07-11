%bcond_with bootstrap

Name:           xmvn-generator
Version:        [...]
Release:        %autorelease
Summary:        RPM dependency generator for Java
License:        Apache-2.0
URL:            https://github.com/fedora-java/xmvn-generator
BuildArch:      noarch
ExclusiveArch:  %{java_arches} noarch

Source:         https://github.com/fedora-java/xmvn-generator/releases/download/%{version}/xmvn-generator-%{version}.tar.zst

%if %{with bootstrap}
BuildRequires:  javapackages-bootstrap
%else
BuildRequires:  maven-local-openjdk25
BuildRequires:  mvn(io.kojan:dola-bsx-api)
BuildRequires:  mvn(org.apache.commons:commons-compress)
BuildRequires:  mvn(org.easymock:easymock)
BuildRequires:  mvn(org.junit.jupiter:junit-jupiter)
BuildRequires:  mvn(org.ow2.asm:asm)
%endif
Requires:       dola-bsx
# TODO Remove in Fedora 46
Obsoletes:      %{name}-javadoc < 2.0.2-9

%description
XMvn Generator is a dependency generator for RPM Package Manager
written in Java and Lua.

%prep
%autosetup -p1 -C
%mvn_file : %{name}

%build
%mvn_build -j -- -P\!quality

%install
%mvn_install
install -D -p -m 644 src/main/lua/xmvn-generator.lua %{buildroot}%{_rpmluadir}/xmvn-generator.lua
install -D -p -m 644 src/main/rpm/macros.xmvngen %{buildroot}%{_rpmmacrodir}/macros.xmvngen
install -D -p -m 644 src/main/rpm/macros.xmvngenhook %{buildroot}%{_sysconfdir}/rpm/macros.xmvngenhook
install -D -p -m 644 src/main/rpm/xmvngen.attr %{buildroot}%{_fileattrsdir}/xmvngen.attr
install -D -p -m 644 src/main/conf/xmvn-generator.conf %{buildroot}%{_javaconfdir}/dola/classworlds/90-xmvn-generator.conf

%files -f .mfiles
%{_rpmluadir}/*
%{_rpmmacrodir}/*
%{_fileattrsdir}/*
%{_sysconfdir}/rpm/*
%{_javaconfdir}/dola/classworlds/90-xmvn-generator.conf
%license LICENSE NOTICE
%doc README.md

%changelog
%autochangelog
