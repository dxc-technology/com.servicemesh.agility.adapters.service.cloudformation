# Header
Name: agility-adapters-kubernetes
Summary: Agility Platform - Kubernetes Service Adapter
Version: %rpm_version
Release: %rpm_revision
Vendor: CSC Agility Dev
URL: http://www.csc.com/
Group: Services/Cloud
License: Commercial
BuildArch: noarch
Requires: jre >= 1.8.0
Requires: agility-platform-common

# Sections
%description
Enables orchestration of container based workloads into Kubernetes cluster.

%license_text

%files
%defattr(644,smadmin,smadmin,755)
/opt/agility-platform/deploy/*
