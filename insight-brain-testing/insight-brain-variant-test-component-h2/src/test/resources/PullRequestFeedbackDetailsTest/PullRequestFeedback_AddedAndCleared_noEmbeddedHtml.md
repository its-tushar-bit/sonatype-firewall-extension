### :thinking: Sonatype Lifecycle found multiple policy violations introduced by this PR:

&#8192;

**com.h2database : h2 : 1.4.190** - [line comment](/projects/sonatype/repos/enhanced-commit-information/pull-requests/10/overview?commentId=12345)

:shield: **Bumping to version 1.4.200** will resolve all policy violations for this component
  - Few breaking changes - This version upgrade may require moderate effort.

| **Threat (of 10)** | **Policy** | **Violation Details** |
| --- | --- | --- |
| 10 | Unlikely Test Policy | **Nonsensical Constraint:** Found 1 security vulnerability: [CVE-2018-14335](http://localhost:1122/ui/links/vln/CVE-2018-14335). Found licenses in the 'Liberal' license threat group ('BSD-3-Clause'). Found license threat group 'Weak Copyleft' with level >= 1 (level = 2). Match state was 'Exact'. Component does not contain proprietary packages. Relative popularity was < 100% (relative popularity = 3%). **Illogical Constraint:** Found component older than 1 days. Coordinates were com.h2database : h2 : 1.4.190 (match com.h2database : h2 : \* : \* : 1.4.190). Identification Source was Sonatype. Found label 'Architecture-Blacklisted'. Found 'MPL-2.0' license. License status was Open. Coordinates were com.h2database : h2 : 1.4.190 (matches package URL pkg:maven/com.h2database/h2@1.4.190?classifier=\*&type=jar).  |
| 7 | Security-Medium | **Medium risk CVSS score:** Found 1 security vulnerability: [CVE-2018-14335](http://localhost:1122/ui/links/vln/CVE-2018-14335).  |

&#8192;

**org.springframework.security : spring-security-web : 4.2.3.RELEASE**

:shield: **Bumping to version 4.5.0.RELEASE** will resolve all policy violations for this component and its dependencies

| **Threat (of 10)** | **Policy** | **Violation Details** |
| --- | --- | --- |
| 10 | Unlikely Test Policy | **Nonsensical Constraint:** Found 5 security vulnerabilities: [CVE-2018-1199](http://localhost:1122/ui/links/vln/CVE-2018-1199), [sonatype-2017-0507](http://localhost:1122/ui/links/vln/sonatype-2017-0507), [sonatype-2017-0641](http://localhost:1122/ui/links/vln/sonatype-2017-0641), [sonatype-2019-0341](http://localhost:1122/ui/links/vln/sonatype-2019-0341), [sonatype-2019-0469](http://localhost:1122/ui/links/vln/sonatype-2019-0469). Found licenses in the 'Liberal' license threat group ('Apache-2.0'). Match state was 'Exact'. Component does not contain proprietary packages. Relative popularity was < 100% (relative popularity = 20%). **Illogical Constraint:** Found component older than 1 days. Identification Source was Sonatype. License status was Open.  |
| 9 | Security-High | **High risk CVSS score:** Found 1 security vulnerability: [sonatype-2017-0641](http://localhost:1122/ui/links/vln/sonatype-2017-0641).  |
| 7 | Security-Medium | **Medium risk CVSS score:** Found 3 security vulnerabilities: [CVE-2018-1199](http://localhost:1122/ui/links/vln/CVE-2018-1199), [sonatype-2017-0507](http://localhost:1122/ui/links/vln/sonatype-2017-0507), [sonatype-2019-0469](http://localhost:1122/ui/links/vln/sonatype-2019-0469).  |
| 3 | Security-Low | **Low risk CVSS score:** Found 1 security vulnerability: [sonatype-2019-0341](http://localhost:1122/ui/links/vln/sonatype-2019-0341).  |

&#8192;

**org.apache.kafka : kafka-clients : 3.7.0**

:shield: **Bumping to version 3.8.0** will resolve all policy violations for this component
:white_check_mark: No breaking changes - This version upgrade requires minimal effort.

| **Threat (of 10)** | **Policy** | **Violation Details** |
| --- | --- | --- |
| 7 | Security-Medium | **Medium risk CVSS score:** Found 1 security vulnerability: [CVE-2018-14335](http://localhost:1122/ui/links/vln/CVE-2018-14335).  |

&#8192;

**webgoat-server-8.0.0.M1.jar**

:warning: No recommended versions are available for this component

| **Threat (of 10)** | **Policy** | **Violation Details** |
| --- | --- | --- |
| 3 | Component-Unknown | **Unknown 3rd party component:** Match state was 'Unknown'. Component does not contain proprietary packages.  |

&#8192;

**html-tampering-8.0.0.M1.jar**

:warning: No recommended versions are available for this component

| **Threat (of 10)** | **Policy** | **Violation Details** |
| --- | --- | --- |
| 2 | Component-Unknown | **Unknown 3rd party component:** Match state was 'Unknown'. Component does not contain proprietary packages.  |

&#8192;


&#8192;

---
### :sunglasses: Sonatype Lifecycle determined that you fixed outstanding policy violations:

:white_check_mark: **com.h2database : h2-cleared : 1.4.190**

| **Threat (of 10)** | **Policy** | **Violation Details** |
| --- | --- | --- |
| 10 | Unlikely Test Policy | **Nonsensical Constraint:** Found 1 security vulnerability: [CVE-2018-14335](http://localhost:1122/ui/links/vln/CVE-2018-14335). Found licenses in the 'Liberal' license threat group ('BSD-3-Clause'). Found license threat group 'Weak Copyleft' with level >= 1 (level = 2). Match state was 'Exact'. Component does not contain proprietary packages. Relative popularity was < 100% (relative popularity = 3%). **Illogical Constraint:** Found component older than 1 days. Coordinates were com.h2database : h2 : 1.4.190 (match com.h2database : h2 : \* : \* : 1.4.190). Identification Source was Sonatype. Found label 'Architecture-Blacklisted'. Found 'MPL-2.0' license. License status was Open. Coordinates were com.h2database : h2 : 1.4.190 (matches package URL pkg:maven/com.h2database/h2@1.4.190?classifier=\*&type=jar).  |
| 7 | Security-Medium | **Medium risk CVSS score:** Found 1 security vulnerability: [CVE-2018-14335](http://localhost:1122/ui/links/vln/CVE-2018-14335).  |

&#8192;

:white_check_mark: **org.springframework.security : spring-security-web-cleared : 4.2.3.RELEASE**

| **Threat (of 10)** | **Policy** | **Violation Details** |
| --- | --- | --- |
| 10 | Unlikely Test Policy | **Nonsensical Constraint:** Found 5 security vulnerabilities: [CVE-2018-1199](http://localhost:1122/ui/links/vln/CVE-2018-1199), [sonatype-2017-0507](http://localhost:1122/ui/links/vln/sonatype-2017-0507), [sonatype-2017-0641](http://localhost:1122/ui/links/vln/sonatype-2017-0641), [sonatype-2019-0341](http://localhost:1122/ui/links/vln/sonatype-2019-0341), [sonatype-2019-0469](http://localhost:1122/ui/links/vln/sonatype-2019-0469). Found licenses in the 'Liberal' license threat group ('Apache-2.0'). Match state was 'Exact'. Component does not contain proprietary packages. Relative popularity was < 100% (relative popularity = 20%). **Illogical Constraint:** Found component older than 1 days. Identification Source was Sonatype. License status was Open.  |
| 9 | Security-High | **High risk CVSS score:** Found 1 security vulnerability: [sonatype-2017-0641](http://localhost:1122/ui/links/vln/sonatype-2017-0641).  |
| 7 | Security-Medium | **Medium risk CVSS score:** Found 3 security vulnerabilities: [CVE-2018-1199](http://localhost:1122/ui/links/vln/CVE-2018-1199), [sonatype-2017-0507](http://localhost:1122/ui/links/vln/sonatype-2017-0507), [sonatype-2019-0469](http://localhost:1122/ui/links/vln/sonatype-2019-0469).  |
| 3 | Security-Low | **Low risk CVSS score:** Found 1 security vulnerability: [sonatype-2019-0341](http://localhost:1122/ui/links/vln/sonatype-2019-0341).  |

&#8192;

:white_check_mark: **org.apache.kafka : kafka-clients-cleared : 3.7.0**

| **Threat (of 10)** | **Policy** | **Violation Details** |
| --- | --- | --- |
| 7 | Security-Medium | **Medium risk CVSS score:** Found 1 security vulnerability: [CVE-2018-14335](http://localhost:1122/ui/links/vln/CVE-2018-14335).  |

&#8192;

:white_check_mark: **webgoat-server-8.0.0.M1.jar**

| **Threat (of 10)** | **Policy** | **Violation Details** |
| --- | --- | --- |
| 3 | Component-Unknown | **Unknown 3rd party component:** Match state was 'Unknown'. Component does not contain proprietary packages.  |

&#8192;

:white_check_mark: **html-tampering-8.0.0.M1.jar**

| **Threat (of 10)** | **Policy** | **Violation Details** |
| --- | --- | --- |
| 2 | Component-Unknown | **Unknown 3rd party component:** Match state was 'Unknown'. Component does not contain proprietary packages.  |

&#8192;

### Sonatype Lifecycle Report Details
**Application**: TEST APP   
**Organization**: TEST ORG   
**Date**: 2020-06-21 09:15:32 UTC

**Source Branch**: Release Stage - [Full Report](http://localhost:1122/ui/links/application/TEST_APP_PUBLIC_ID/report/toScanId?source=pr-commenting)   
**Target Branch**: Build Stage - [Full Report](http://localhost:1122/ui/links/application/TEST_APP_PUBLIC_ID/report/fromScanId?source=pr-commenting)   
**Application Priorities** - [View](http://localhost:1122/ui/links/developer/priorities/TEST_APP_PUBLIC_ID/toScanId)

[Give feedback](https://community.sonatype.com/t/user-feedback-github-pr-reviews/3811)
