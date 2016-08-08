# Core AWS

The Core AWS bundle is provided to aid in the development of a CSC Agility Platform&trade; adapter for Amazon Web Services&trade; (AWS). This bundle provides communications and utility functions for interacting with AWS via Query Requests.

## Core AWS Usage
The primary interfaces in Core AWS for communications are:
* com.servicemesh.agility.adapters.core.aws.AWSConnection
* com.servicemesh.agility.adapters.core.aws.AWSEndpoint

`AWSEnpdoint` represents the access point for an AWS service API, including data serialization.

`AWSConnection` provides communication operations to an AWSEndpoint and employs AWS Signature Version 4.

com.servicemesh.agility.adapters.core.aws.util.EC2SecurityGroupOperations manages AWS Elastic Compute Cloud&trade; (EC2) Security Groups.

The Core AWS bundle uses Apache Log4j and has two levels to assist in adapter troubleshooting - *DEBUG* and the finer-grained *TRACE* - that by default are not enabled. To enable both, add the following line to `/opt/agility-platform/etc/com.servicemesh.agility.logging.cfg`:
```
log4j.logger.com.servicemesh.agility.adapters.core.aws=TRACE
```
To only enable the *DEBUG* level, use *DEBUG* instead of *TRACE* in `com.servicemesh.agility.logging.cfg`.

### Build/Eclipse Configuration
Core AWS is compatible with Java 8 and Apache Ant 1.9.3.

It is recommended that any new development is done on the develop branch.

Core AWS is dependent on the [csc-agility-platform-sdk project](https://github.com/csc/csc-agility-platform-sdk). The Core AWS ant build file requires that csc-agility-platform-sdk be built first.

If you want to edit Core AWS using Eclipse you'll need to define Eclipse build path variables:
* IVY-LIB: Contains the path to the *ivy-lib* directory under csc-agility-platform-sdk
* COMMON-LIB: Contains the path to the *lib* directory under csc-agility-platform-sdk
* DIST: Contains the path to the *dist* directory under csc-agility-platform-sdk

### Reference Implementations
Examples of utilizing the Core AWS bundle with various AWS APIs are provided with the unit tests:
* TestELBIntegration.java: AWS Elastic Load Balancing&trade; (ELB)
* TestRDSIntegration.java: AWS Relational Database Service&trade; (RDS)
* TestS3Integration.java: AWS Simple Storage Service&trade; (S3)
* TestSecurityGroupIntegration.java: EC2 Security Groups

## Unit Testing
For maximum unit testing that includes direct interaction with AWS, populate a junit.properties file in the base directory with valid credentials:
```
aws_access_key=<unit-test-access-key>
aws_secret_key=<unit-test-secret-key>
```

To generate and view code coverage metrics, open the coverage/report/index.html file after running this command:
```
$ ant clean compile coverage-report -Dcoverage.format=html
```

## License
Core AWS is distributed under the Apache 2.0 license. See the [LICENSE](https://github.com/csc/com.servicemesh.agility.adapters.core.aws/blob/master/LICENSE) file for full details.
