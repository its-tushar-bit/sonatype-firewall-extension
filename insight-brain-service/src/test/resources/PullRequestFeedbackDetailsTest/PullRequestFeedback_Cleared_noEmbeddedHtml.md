### :smiley: All Clear! Nexus IQ didn't find any policy violations introduced by this PR

Well done. The committed code does not violate any of your organization's Nexus IQ policies.

&#8192;

---
### :sunglasses: Nexus IQ determined that you fixed outstanding policy violations:

#### :white_check_mark: com.h2database : h2 : 1.4.190

| **Threat (of 10)** | **Policy** | **Violation Details** |
| --- | --- | --- |
| 10 | Unlikely Test Policy | **Nonsensical Constraint:** Found security vulnerability: [CVE-2018-14335](http://localhost:1122/ui/links/vln/CVE-2018-14335). Found licenses in the 'Liberal' license threat group ('BSD-3-Clause'). Found license threat group 'Weak Copyleft' with level >= 1 (level = 2). Match state was 'Exact'. Component does not contain proprietary packages. Relative popularity was < 100% (relative popularity = 3%). **Illogical Constraint:** Found component older than 1 days. Coordinates were com.h2database : h2 : 1.4.190 (match com.h2database : h2 : * : * : 1.4.190). Identification Source was Sonatype. Found label 'Architecture-Blacklisted'. Found 'MPL-2.0' license. License status was Open. Coordinates were com.h2database : h2 : 1.4.190 (matches package URL pkg:maven/com.h2database/h2@1.4.190?classifier=*&type=jar).  |
| 7 | Security-Medium | **Medium risk CVSS score:** Found security vulnerability: [CVE-2018-14335](http://localhost:1122/ui/links/vln/CVE-2018-14335).  |

&#8192;

#### :white_check_mark: org.springframework.security : spring-security-web : 4.2.3.RELEASE

| **Threat (of 10)** | **Policy** | **Violation Details** |
| --- | --- | --- |
| 10 | Unlikely Test Policy | **Nonsensical Constraint:** Found security vulnerabilities: [CVE-2018-1199](http://localhost:1122/ui/links/vln/CVE-2018-1199), [sonatype-2017-0507](http://localhost:1122/ui/links/vln/sonatype-2017-0507), [sonatype-2017-0641](http://localhost:1122/ui/links/vln/sonatype-2017-0641), [sonatype-2019-0341](http://localhost:1122/ui/links/vln/sonatype-2019-0341), [sonatype-2019-0469](http://localhost:1122/ui/links/vln/sonatype-2019-0469). Found licenses in the 'Liberal' license threat group ('Apache-2.0'). Match state was 'Exact'. Component does not contain proprietary packages. Relative popularity was < 100% (relative popularity = 20%). **Illogical Constraint:** Found component older than 1 days. Identification Source was Sonatype. License status was Open.  |
| 9 | Security-High | **High risk CVSS score:** Found security vulnerability: [sonatype-2017-0641](http://localhost:1122/ui/links/vln/sonatype-2017-0641).  |
| 7 | Security-Medium | **Medium risk CVSS score:** Found security vulnerabilities: [CVE-2018-1199](http://localhost:1122/ui/links/vln/CVE-2018-1199), [sonatype-2017-0507](http://localhost:1122/ui/links/vln/sonatype-2017-0507), [sonatype-2019-0469](http://localhost:1122/ui/links/vln/sonatype-2019-0469).  |
| 3 | Security-Low | **Low risk CVSS score:** Found security vulnerability: [sonatype-2019-0341](http://localhost:1122/ui/links/vln/sonatype-2019-0341).  |

&#8192;

#### :white_check_mark: webgoat-server-8.0.0.M1.jar

| **Threat (of 10)** | **Policy** | **Violation Details** |
| --- | --- | --- |
| 2 | Component-Unknown | **Unknown 3rd party component:** Match state was 'Unknown'. Component does not contain proprietary packages.  |

&#8192;

#### :white_check_mark: html-tampering-8.0.0.M1.jar

| **Threat (of 10)** | **Policy** | **Violation Details** |
| --- | --- | --- |
| 2 | Component-Unknown | **Unknown 3rd party component:** Match state was 'Unknown'. Component does not contain proprietary packages.  |

&#8192;

### Nexus IQ Report Details
**Application**: TEST APP   
**Organization**: TEST ORG   
**Date**: 2020-06-21 09:15:32 UTC

**PR Branch**: Release Stage - [Full Report](http://localhost:1122/ui/links/application/TEST_APP_PUBLIC_ID/report/toScanId)   
**Default Branch**: Build Stage - [Full Report](http://localhost:1122/ui/links/application/TEST_APP_PUBLIC_ID/report/fromScanId)

[Give feedback](https://community.sonatype.com/t/user-feedback-github-pr-reviews/3811)