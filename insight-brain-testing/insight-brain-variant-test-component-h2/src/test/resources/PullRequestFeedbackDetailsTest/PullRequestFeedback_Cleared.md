### :smiley: All Clear! Sonatype Lifecycle didn't find any policy violations introduced by this PR
Well done. The committed code does not violate any of your organization's Sonatype Lifecycle policies.

---
### :sunglasses: Sonatype Lifecycle determined that you fixed outstanding policy violations:

<details>
<summary title="Threat Level: 10 of 10"><img alt="T10" src="https://cdn.sonatype.com/iq-for-scm/1.0/red-bar.png" width="4" height="14"> 
<b>10&nbsp;com.h2database : h2 : 1.4.190</b>&nbsp;&nbsp; :white_check_mark:</summary>
<p></p>

Threat (of 10) | Policy | Violation Details
--- | --- | --- |
10 | Unlikely Test Policy | <b>Nonsensical Constraint:</b><ul><li>Found 1 security vulnerability: [CVE-2018-14335](http://localhost:1122/ui/links/vln/CVE-2018-14335)</li><li>Found licenses in the 'Liberal' license threat group ('BSD-3-Clause')</li><li>Found license threat group 'Weak Copyleft' with level >= 1 (level = 2)</li><li>Match state was 'Exact'</li><li>Component does not contain proprietary packages</li><li>Relative popularity was < 100% (relative popularity = 3%)</li></ul><b>Illogical Constraint:</b><ul><li>Found component older than 1 days</li><li>Coordinates were com.h2database : h2 : 1.4.190 (match com.h2database : h2 : \* : \* : 1.4.190)</li><li>Identification Source was Sonatype</li><li>Found label 'Architecture-Blacklisted'</li><li>Found 'MPL-2.0' license</li><li>License status was Open</li><li>Coordinates were com.h2database : h2 : 1.4.190 (matches package URL pkg:maven/com.h2database/h2@1.4.190?classifier=\*&type=jar)</li></ul>
7 | Security-Medium | <b>Medium risk CVSS score:</b><ul><li>Found 1 security vulnerability: [CVE-2018-14335](http://localhost:1122/ui/links/vln/CVE-2018-14335)</li></ul>

</details>

<details>
<summary title="Threat Level: 10 of 10"><img alt="T10" src="https://cdn.sonatype.com/iq-for-scm/1.0/red-bar.png" width="4" height="14"> 
<b>10&nbsp;org.springframework.security : spring-security-web : 4.2.3.RELEASE</b>&nbsp;&nbsp; :white_check_mark:</summary>
<p></p>

Threat (of 10) | Policy | Violation Details
--- | --- | --- |
10 | Unlikely Test Policy | <b>Nonsensical Constraint:</b><ul><li>Found 5 security vulnerabilities: [CVE-2018-1199](http://localhost:1122/ui/links/vln/CVE-2018-1199), [sonatype-2017-0507](http://localhost:1122/ui/links/vln/sonatype-2017-0507), [sonatype-2017-0641](http://localhost:1122/ui/links/vln/sonatype-2017-0641), [sonatype-2019-0341](http://localhost:1122/ui/links/vln/sonatype-2019-0341), [sonatype-2019-0469](http://localhost:1122/ui/links/vln/sonatype-2019-0469)</li><li>Found licenses in the 'Liberal' license threat group ('Apache-2.0')</li><li>Match state was 'Exact'</li><li>Component does not contain proprietary packages</li><li>Relative popularity was < 100% (relative popularity = 20%)</li></ul><b>Illogical Constraint:</b><ul><li>Found component older than 1 days</li><li>Identification Source was Sonatype</li><li>License status was Open</li></ul>
9 | Security-High | <b>High risk CVSS score:</b><ul><li>Found 1 security vulnerability: [sonatype-2017-0641](http://localhost:1122/ui/links/vln/sonatype-2017-0641)</li></ul>
7 | Security-Medium | <b>Medium risk CVSS score:</b><ul><li>Found 3 security vulnerabilities: [CVE-2018-1199](http://localhost:1122/ui/links/vln/CVE-2018-1199), [sonatype-2017-0507](http://localhost:1122/ui/links/vln/sonatype-2017-0507), [sonatype-2019-0469](http://localhost:1122/ui/links/vln/sonatype-2019-0469)</li></ul>
3 | Security-Low | <b>Low risk CVSS score:</b><ul><li>Found 1 security vulnerability: [sonatype-2019-0341](http://localhost:1122/ui/links/vln/sonatype-2019-0341)</li></ul>

</details>

<details>
<summary title="Threat Level: 7 of 10"><img alt="T7" src="https://cdn.sonatype.com/iq-for-scm/1.0/orange-bar.png" width="4" height="14"> 
<b>7&nbsp;&nbsp;&nbsp;org.apache.kafka : kafka-clients : 3.7.0</b>&nbsp;&nbsp; :white_check_mark:</summary>
<p></p>

Threat (of 10) | Policy | Violation Details
--- | --- | --- |
7 | Security-Medium | <b>Medium risk CVSS score:</b><ul><li>Found 1 security vulnerability: [CVE-2018-14335](http://localhost:1122/ui/links/vln/CVE-2018-14335)</li></ul>

</details>

----
### Sonatype Lifecycle Report Detail
**Application**: TEST APP
**Organization**: TEST ORG
**Date**: 2020-06-21 09:15:32 UTC
**PR Branch**: Release Stage - [Full Report](http://localhost:1122/ui/links/application/TEST_APP_PUBLIC_ID/report/toScanId?source=pr-commenting)
**Base Branch**: Build Stage - [Full Report](http://localhost:1122/ui/links/application/TEST_APP_PUBLIC_ID/report/fromScanId?source=pr-commenting)
**Application Priorities** - [View](http://localhost:1122/ui/links/developer/priorities/TEST_APP_PUBLIC_ID/toScanId)

[Give feedback](https://community.sonatype.com/t/user-feedback-github-pr-reviews/3811)
