# Header
Name: agility-adapters-core-aws
Summary: Agility - Core Functionality for Amazon Web Services Adapters
Version: %rpm_version
Release: %rpm_revision
Vendor: CSC Agility Dev
URL: http://www.csc.com/
Group: Services/Cloud
License: Apache-2.0
BuildArch: noarch
Requires: jre >= 1.8.0
Requires: agility-platform-common

# Sections
%description
Core functionality for AWS adapters utilized with the Agility Platform.

%license_text

%files
%defattr(644,smadmin,smadmin,755)
/opt/agility-platform/deploy/*
